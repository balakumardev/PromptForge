package dev.balakumar.promptforge.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.ui.Messages
import dev.balakumar.promptforge.models.RelatedFile
import dev.balakumar.promptforge.settings.PromptForgeSettings
import dev.balakumar.promptforge.utils.FileUtils
import dev.balakumar.promptforge.utils.RelatedFilesCollector

class ExplainCodeAction : AnAction() {
    private val LOG = Logger.getInstance(ExplainCodeAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = FileUtils.findTargetFile(e) ?: return
        if (!FileUtils.isJavaFile(file)) {
            Messages.showErrorDialog(project, "This action only works with Java files.", "Unsupported File Type")
            return
        }
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Explaining Code", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val settings = PromptForgeSettings.getInstance()
                    var currentContent = String(file.contentsToByteArray())
                    if (settings.state.trimWhitespace) {
                        currentContent = FileUtils.trimWhitespace(currentContent)
                    }
                    val className = ReadAction.compute<String, Throwable> {
                        FileUtils.extractClassName(file, project) ?: file.nameWithoutExtension
                    }
                    indicator.text = "Collecting related files"
                    val relatedFiles = ReadAction.compute<List<RelatedFile>, Throwable> {
                        RelatedFilesCollector(project).collectRelatedFiles(file)
                    }
                    val prompt = createExplainPrompt(
                        settings.state.explainCodePromptTemplate,
                        className,
                        currentContent,
                        relatedFiles
                    )
                    ApplicationManager.getApplication().invokeLater {
                        val selection = StringSelection(prompt)
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                        Messages.showInfoMessage(
                            project,
                            "Code explanation prompt copied to clipboard. Paste it to your LLM for an explanation.",
                            "Success"
                        )
                    }
                } catch (ex: Exception) {
                    LOG.error("Error explaining code", ex)
                    showError(project, "Error: ${ex.message}")
                }
            }
        })
    }

    private fun createExplainPrompt(
        template: String,
        className: String,
        classContent: String,
        relatedFiles: List<RelatedFile>
    ): String {
        var result = template
        result = result.replace("{CLASS_NAME}", className)
        result = result.replace("{CLASS_CONTENT}", classContent)
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
        e.presentation.isVisible = true
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }
        val file = FileUtils.findTargetFile(e)
        if (file == null) {
            e.presentation.isEnabled = false
            return
        }
        val isJavaFile = file.extension == "java"
        e.presentation.isEnabled = isJavaFile
    }
}