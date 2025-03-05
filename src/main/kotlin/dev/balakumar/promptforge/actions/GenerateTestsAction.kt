package dev.balakumar.promptforge.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.ui.Messages
import dev.balakumar.promptforge.models.RelatedFile
import dev.balakumar.promptforge.settings.PromptForgeSettings
import dev.balakumar.promptforge.utils.FileUtils
import dev.balakumar.promptforge.utils.RelatedFilesCollector
import java.io.File

class GenerateTestsAction : AnAction() {
    private val LOG = Logger.getInstance(GenerateTestsAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = FileUtils.findTargetFile(e) ?: return

        if (!FileUtils.isJavaFile(file)) {
            Messages.showErrorDialog(project, "This action only works with Java files.", "Unsupported File Type")
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating Tests", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    // Get settings
                    val settings = PromptForgeSettings.getInstance()

                    // Check if this is a new file or a modified file
                    val isNewFile = FileUtils.isNewFile(file)

                    if (isNewFile) {
                        // Handle new file case
                        handleNewFile(project, file, indicator)
                    } else {
                        // Handle modified file case
                        handleModifiedFile(project, file, indicator)
                    }
                } catch (ex: Exception) {
                    LOG.error("Error generating tests", ex)
                    showError(project, "Error: ${ex.message}")
                }
            }
        })
    }

    private fun handleNewFile(project: Project, file: VirtualFile, indicator: ProgressIndicator) {
        indicator.text = "Generating tests for new file"

        // Get the current content
        val currentContent = String(file.contentsToByteArray())

        // Extract class name
        val className = ReadAction.compute<String, Throwable> {
            FileUtils.extractClassName(file, project) ?: file.nameWithoutExtension
        }

        // Collect related files
        indicator.text = "Collecting related files"
        val relatedFiles = ReadAction.compute<List<RelatedFile>, Throwable> {
            RelatedFilesCollector(project).collectRelatedFiles(file)
        }

        // Create the prompt using the new file template
        val settings = PromptForgeSettings.getInstance()
        val prompt = createNewFilePrompt(
            settings.state.newFilePromptTemplate,
            className,
            currentContent,
            relatedFiles
        )

        // Copy to clipboard
        ApplicationManager.getApplication().invokeLater {
            val selection = StringSelection(prompt)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
            Messages.showInfoMessage(
                project,
                "Test generation prompt copied to clipboard. Paste it to your LLM to generate a complete test class.",
                "Success"
            )
        }
    }

    private fun handleModifiedFile(project: Project, file: VirtualFile, indicator: ProgressIndicator) {
        indicator.text = "Generating tests for modified file"

        // Get the Git root directory
        val gitRoot = FileUtils.findGitRoot(file)
        if (gitRoot == null) {
            showError(project, "Could not find Git repository for this file.")
            return
        }

        // Check if file has changes
        if (!FileUtils.hasGitChanges(gitRoot, file)) {
            showError(project, "No changes detected in the file.")
            return
        }

        // Get the original content from Git
        val originalContent = FileUtils.getOriginalContent(gitRoot, file)
        if (originalContent == null) {
            showError(project, "Could not retrieve the original content from Git.")
            return
        }

        // Generate diff
        val diff = FileUtils.generateDiff(gitRoot, file)
        if (diff.isNullOrBlank()) {
            showError(project, "No changes detected in the file.")
            return
        }

        // Find related test file
        val testFile = ReadAction.compute<VirtualFile?, Throwable> {
            FileUtils.findTestFile(project, file)
        }

        if (testFile == null) {
            showError(project, "Could not find a related test file.")
            return
        }

        val testContent = String(testFile.contentsToByteArray())

        // Collect related files
        indicator.text = "Collecting related files"
        val relatedFiles = ReadAction.compute<List<RelatedFile>, Throwable> {
            RelatedFilesCollector(project).collectRelatedFiles(file)
        }

        // Create the prompt using the modified file template
        val settings = PromptForgeSettings.getInstance()
        val prompt = createModifiedFilePrompt(
            settings.state.modifiedFilePromptTemplate,
            originalContent,
            diff,
            testContent,
            relatedFiles
        )

        // Copy to clipboard
        ApplicationManager.getApplication().invokeLater {
            val selection = StringSelection(prompt)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
            Messages.showInfoMessage(
                project,
                "Test generation prompt copied to clipboard. Paste it to your LLM to generate test methods.",
                "Success"
            )
        }
    }

    private fun createNewFilePrompt(
        template: String,
        className: String,
        classContent: String,
        relatedFiles: List<RelatedFile>
    ): String {
        var result = template

        // Replace placeholders
        result = result.replace("{CLASS_NAME}", className)
        result = result.replace("{CLASS_CONTENT}", classContent)

        // Handle related files content
        val relatedFilesContent = if (relatedFiles.isNotEmpty()) {
            val sb = StringBuilder("\n\nRelated files for context:")
            for (relatedFile in relatedFiles) {
                sb.append("\n\n// File: ${relatedFile.path}\n")
                sb.append(relatedFile.content)
            }
            sb.toString()
        } else {
            ""
        }
        result = result.replace("{RELATED_FILES_CONTENT}", relatedFilesContent)

        return result
    }

    private fun createModifiedFilePrompt(
        template: String,
        originalContent: String,
        diff: String,
        testContent: String,
        relatedFiles: List<RelatedFile>
    ): String {
        var result = template

        // Replace placeholders
        result = result.replace("{ORIGINAL_CLASS}", originalContent)
        result = result.replace("{DIFF}", diff)
        result = result.replace("{TEST_FILE}", testContent)

        // Handle related files section
        val relatedFilesSection = if (relatedFiles.isNotEmpty()) {
            "4. ${relatedFiles.size} related files for additional context"
        } else {
            ""
        }
        result = result.replace("{RELATED_FILES_SECTION}", relatedFilesSection)

        // Handle related files content
        val relatedFilesContent = if (relatedFiles.isNotEmpty()) {
            val sb = StringBuilder("\n\nRelated files for context:")
            for (relatedFile in relatedFiles) {
                sb.append("\n\n// File: ${relatedFile.path}\n")
                sb.append(relatedFile.content)
            }
            sb.toString()
        } else {
            ""
        }
        result = result.replace("{RELATED_FILES_CONTENT}", relatedFilesContent)

        return result
    }

    private fun showError(project: Project, message: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(project, message, "Error")
        }
    }

    override fun update(e: AnActionEvent) {
        // Always make it visible
        e.presentation.isVisible = true

        // Get the project
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        // Try multiple strategies to get the current file
        val file = FileUtils.findTargetFile(e)

        if (file == null) {
            e.presentation.isEnabled = false
            return
        }

        // Check if it's a Java file
        val isJavaFile = file.extension == "java"

        e.presentation.isEnabled = isJavaFile
    }
}