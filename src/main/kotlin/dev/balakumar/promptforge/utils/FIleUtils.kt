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
        // Manual detection of Git root
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

            // If the file is new (not in HEAD), git diff might not show changes
            // So also check if it's staged
            if (exitCode == 0 && output.trim().isEmpty()) {
                // Check if it's staged
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

            // If exit code is not 0, it might be a new file or another error
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
            // First try regular diff for working tree changes
            val process = ProcessBuilder("git", "diff", "HEAD", "--", relativePath)
                .directory(gitRoot)
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()

            val exitCode = process.waitFor()

            // If no working tree changes, check staged changes
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

            // If we get here, there's no diff or there was an error
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
        // First try using TestFinderHelper
        val psiFile = PsiManager.getInstance(project).findFile(file)
        if (psiFile is PsiJavaFile) {
            val psiClass = psiFile.classes.firstOrNull()
            if (psiClass != null) {
                // Get all test finder extensions
                val testFinders = com.intellij.testIntegration.TestFinderHelper.findTestsForClass(psiClass)

                // Return the first test class's file
                if (testFinders.isNotEmpty()) {
                    return testFinders.first().containingFile?.virtualFile
                }
            }
        }

        // If TestFinderHelper didn't work, fall back to conventional patterns
        val fileName = file.nameWithoutExtension
        val fileDir = file.parent

        // Try to find a test in the same directory with "Test" suffix
        val testFileName = "${fileName}Test.java"
        var testFile = fileDir.findChild(testFileName)
        if (testFile != null) return testFile

        // Try with "Test" prefix
        val testFileName2 = "Test${fileName}.java"
        testFile = fileDir.findChild(testFileName2)
        if (testFile != null) return testFile

        // If not found, look for a test file in a "test" directory with the same structure
        if (psiFile is PsiJavaFile) {
            val packageName = psiFile.packageName
            val packagePath = packageName.replace('.', '/')

            // Look for test file in common test directories
            val testDirs = listOf(
                "src/test/java/$packagePath",
                "test/java/$packagePath",
                "test/$packagePath"
            )

            for (testDir in testDirs) {
                val testDirFile = LocalFileSystem.getInstance().findFileByPath(
                    "${project.basePath}/$testDir"
                )
                if (testDirFile != null) {
                    testFile = testDirFile.findChild(testFileName)
                    if (testFile != null) return testFile

                    // Try with "Test" prefix
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
            // Check if file is untracked
            val relativePath = getRelativePath(gitRoot, file) ?: return true

            // Check if file is untracked (not yet added to git)
            var process = ProcessBuilder("git", "ls-files", "--others", "--exclude-standard", "--", relativePath)
                .directory(gitRoot)
                .redirectErrorStream(true)
                .start()

            var reader = BufferedReader(InputStreamReader(process.inputStream))
            var output = reader.readText()

            if (process.waitFor() == 0 && output.trim().isNotEmpty()) {
                // File is untracked
                return true
            }

            // Check if file is tracked but not in HEAD (newly added)
            process = ProcessBuilder("git", "ls-files", "--", relativePath)
                .directory(gitRoot)
                .redirectErrorStream(true)
                .start()

            reader = BufferedReader(InputStreamReader(process.inputStream))
            output = reader.readText()

            if (process.waitFor() != 0 || output.trim().isEmpty()) {
                // File is not tracked by git
                return true
            }

            // Check if file exists in HEAD
            process = ProcessBuilder("git", "show", "HEAD:$relativePath")
                .directory(gitRoot)
                .redirectErrorStream(true)
                .start()

            if (process.waitFor() != 0) {
                // File doesn't exist in HEAD (newly added)
                return true
            }

            return false
        } catch (e: Exception) {
            LOG.error("Error checking if file is new", e)
            return true // Assume new file on error
        }
    }

    fun findTargetFile(e: AnActionEvent): VirtualFile? {
        // Strategy 1: Direct virtual file
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (virtualFile != null && !virtualFile.isDirectory) {
            return virtualFile
        }

        // Strategy 2: From PSI file
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        if (psiFile != null) {
            return psiFile.virtualFile
        }

        // Strategy 3: From editor
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor != null) {
            val editorFile = FileDocumentManager.getInstance().getFile(editor.document)
            if (editorFile != null) {
                return editorFile
            }
        }

        // Strategy 4: From PSI element
        val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        if (psiElement != null) {
            val containingFile = psiElement.containingFile
            if (containingFile != null) {
                return containingFile.virtualFile
            }
        }

        // Strategy 5: From selected files in project view
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
}