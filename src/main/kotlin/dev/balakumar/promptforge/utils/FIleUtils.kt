package dev.balakumar.promptforge.utils

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object FileUtils {
    private val LOG = Logger.getInstance(FileUtils::class.java)

    fun isJavaFile(file: VirtualFile): Boolean {
        return file.extension == "java"
    }

    fun findGitRoot(file: VirtualFile): File? {
        var current = File(file.path).parentFile
        while (current != null) {
            val gitDir = File(current, ".git")
            if (gitDir.exists() && gitDir.isDirectory) {
                return current
            }
            current = current.parentFile
        }
        return null
    }

    fun hasGitChanges(gitRoot: File, file: VirtualFile): Boolean {
        val relativePath = getRelativePath(gitRoot, file) ?: return false
        try {
            val process = ProcessBuilder("git", "diff", "--name-only", "HEAD", "--", relativePath)
                .directory(gitRoot)
                .redirectErrorStream(true)
                .start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            val exitCode = process.waitFor()
            if (exitCode == 0 && output.trim().isEmpty()) {
                val stagedProcess = ProcessBuilder("git", "diff", "--name-only", "--staged", "--", relativePath)
                    .directory(gitRoot)
                    .redirectErrorStream(true)
                    .start()
                val stagedReader = BufferedReader(InputStreamReader(stagedProcess.inputStream))
                val stagedOutput = stagedReader.readText()
                val stagedExitCode = stagedProcess.waitFor()
                return stagedExitCode == 0 && stagedOutput.trim().isNotEmpty()
            }
            return exitCode == 0 && output.trim().isNotEmpty()
        } catch (e: Exception) {
            LOG.error("Error checking for git changes", e)
            return false
        }
    }

    fun getOriginalContent(gitRoot: File, file: VirtualFile): String? {
        val relativePath = getRelativePath(gitRoot, file) ?: return null
        try {
            val process = ProcessBuilder("git", "show", "HEAD:$relativePath")
                .directory(gitRoot)
                .redirectErrorStream(true)
                .start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                LOG.info("Git show returned non-zero exit code: $exitCode for $relativePath")
                return null
            }
            return output
        } catch (e: Exception) {
            LOG.error("Error executing git show command", e)
            return null
        }
    }

    fun generateDiff(gitRoot: File, file: VirtualFile): String? {
        val relativePath = getRelativePath(gitRoot, file) ?: return null
        try {
            val process = ProcessBuilder("git", "diff", "HEAD", "--", relativePath)
                .directory(gitRoot)
                .redirectErrorStream(true)
                .start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            val exitCode = process.waitFor()
            if (exitCode == 0 && output.trim().isEmpty()) {
                val stagedProcess = ProcessBuilder("git", "diff", "--staged", "--", relativePath)
                    .directory(gitRoot)
                    .redirectErrorStream(true)
                    .start()
                val stagedReader = BufferedReader(InputStreamReader(stagedProcess.inputStream))
                val stagedOutput = stagedReader.readText()
                val stagedExitCode = stagedProcess.waitFor()
                if (stagedExitCode == 0 && stagedOutput.trim().isNotEmpty()) {
                    return stagedOutput
                }
            } else if (exitCode == 0 && output.trim().isNotEmpty()) {
                return output
            }
            return null
        } catch (e: Exception) {
            LOG.error("Error generating git diff", e)
            return null
        }
    }

    fun getRelativePath(gitRoot: File, file: VirtualFile): String? {
        val rootPath = gitRoot.absolutePath
        val filePath = file.path
        if (!filePath.startsWith(rootPath)) {
            LOG.error("File is not in repository: $filePath, repo: $rootPath")
            return null
        }
        return filePath.substring(rootPath.length + 1).replace('\\', '/')
    }

    fun findTestFile(project: Project, file: VirtualFile): VirtualFile? {
        val psiFile = PsiManager.getInstance(project).findFile(file)
        if (psiFile is PsiJavaFile) {
            val psiClass = psiFile.classes.firstOrNull()
            if (psiClass != null) {
                val testFinders = com.intellij.testIntegration.TestFinderHelper.findTestsForClass(psiClass)
                if (testFinders.isNotEmpty()) {
                    return testFinders.first().containingFile?.virtualFile
                }
            }
        }
        val fileName = file.nameWithoutExtension
        val fileDir = file.parent
        val testFileName = "${fileName}Test.java"
        var testFile = fileDir.findChild(testFileName)
        if (testFile != null) return testFile
        val testFileName2 = "Test${fileName}.java"
        testFile = fileDir.findChild(testFileName2)
        if (testFile != null) return testFile
        if (psiFile is PsiJavaFile) {
            val packageName = psiFile.packageName
            val packagePath = packageName.replace('.', '/')
            val testDirs = listOf(
                "src/test/java/$packagePath",
                "test/java/$packagePath",
                "test/$packagePath"
            )
            for (testDir in testDirs) {
                val testDirFile = LocalFileSystem.getInstance().findFileByPath("${project.basePath}/$testDir")
                if (testDirFile != null) {
                    testFile = testDirFile.findChild(testFileName)
                    if (testFile != null) return testFile
                    testFile = testDirFile.findChild(testFileName2)
                    if (testFile != null) return testFile
                }
            }
        }
        return null
    }

    fun extractClassName(file: VirtualFile, project: Project): String? {
        val psiFile = PsiManager.getInstance(project).findFile(file)
        if (psiFile is PsiJavaFile) {
            val classes = psiFile.classes
            if (classes.isNotEmpty()) {
                return classes[0].name
            }
        }
        return file.nameWithoutExtension
    }

    fun isNewFile(file: VirtualFile): Boolean {
        val gitRoot = findGitRoot(file) ?: return true
        try {
            val relativePath = getRelativePath(gitRoot, file) ?: return true
            var process = ProcessBuilder("git", "ls-files", "--others", "--exclude-standard", "--", relativePath)
                .directory(gitRoot)
                .redirectErrorStream(true)
                .start()
            var reader = BufferedReader(InputStreamReader(process.inputStream))
            var output = reader.readText()
            if (process.waitFor() == 0 && output.trim().isNotEmpty()) {
                return true
            }
            process = ProcessBuilder("git", "ls-files", "--", relativePath)
                .directory(gitRoot)
                .redirectErrorStream(true)
                .start()
            reader = BufferedReader(InputStreamReader(process.inputStream))
            output = reader.readText()
            if (process.waitFor() != 0 || output.trim().isEmpty()) {
                return true
            }
            process = ProcessBuilder("git", "show", "HEAD:$relativePath")
                .directory(gitRoot)
                .redirectErrorStream(true)
                .start()
            if (process.waitFor() != 0) {
                return true
            }
            return false
        } catch (e: Exception) {
            LOG.error("Error checking if file is new", e)
            return true
        }
    }

    fun findTargetFile(e: AnActionEvent): VirtualFile? {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (virtualFile != null && !virtualFile.isDirectory) {
            return virtualFile
        }
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        if (psiFile != null) {
            return psiFile.virtualFile
        }
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor != null) {
            val editorFile = FileDocumentManager.getInstance().getFile(editor.document)
            if (editorFile != null) {
                return editorFile
            }
        }
        val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        if (psiElement != null) {
            val containingFile = psiElement.containingFile
            if (containingFile != null) {
                return containingFile.virtualFile
            }
        }
        val selectedFiles = e.getData(PlatformDataKeys.SELECTED_ITEMS)
        if (selectedFiles != null && selectedFiles.isNotEmpty()) {
            for (item in selectedFiles) {
                if (item is VirtualFile && !item.isDirectory) {
                    return item
                }
            }
        }
        return null
    }

    fun trimWhitespace(content: String): String {
        val lines = content.split("\n")
        val result = StringBuilder()
        var previousWasBlank = false
        for (line in lines) {
            val trimmed = line.trimEnd()
            if (trimmed.isEmpty()) {
                if (!previousWasBlank) {
                    result.append("\n")
                    previousWasBlank = true
                }
            } else {
                result.append(trimmed).append("\n")
                previousWasBlank = false
            }
        }
        return result.toString().trimEnd()
    }
}