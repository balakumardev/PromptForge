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

        val process = ProcessBuilder("git", "diff", "--name-only", "HEAD", "--", relativePath)
            .directory(gitRoot)
            .redirectErrorStream(true)
            .start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readText()

        val exitCode = process.waitFor()
        return exitCode == 0 && output.trim().isNotEmpty()
    }

    fun getOriginalContent(gitRoot: File, file: VirtualFile): String? {
        val relativePath = getRelativePath(gitRoot, file) ?: return null

        val process = ProcessBuilder("git", "show", "HEAD:$relativePath")
            .directory(gitRoot)
            .redirectErrorStream(true)
            .start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readText()

        val exitCode = process.waitFor()
        return if (exitCode == 0) output else null
    }

    fun generateDiff(gitRoot: File, file: VirtualFile): String? {
        val relativePath = getRelativePath(gitRoot, file) ?: return null

        val process = ProcessBuilder("git", "diff", "HEAD", "--", relativePath)
            .directory(gitRoot)
            .redirectErrorStream(true)
            .start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readText()

        val exitCode = process.waitFor()
        return if (exitCode == 0 && output.trim().isNotEmpty()) output else null
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

    // Replace this method in FileUtils.kt
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

        // Check if file exists in git
        val relativePath = getRelativePath(gitRoot, file) ?: return true

        val process = ProcessBuilder("git", "ls-files", "--", relativePath)
            .directory(gitRoot)
            .redirectErrorStream(true)
            .start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readText()

        val exitCode = process.waitFor()
        return exitCode != 0 || output.trim().isEmpty()
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
