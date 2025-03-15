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

class CopyWithContextAction : AnAction() {
    private val LOG = Logger.getInstance(CopyWithContextAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = FileUtils.findTargetFile(e) ?: return
        if (!FileUtils.isJavaFile(file)) {
            Messages.showErrorDialog(project, "This action only works with Java files.", "Unsupported File Type")
            return
        }
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Copying File with Context", false) {
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
                    val content = buildRawContent(file.path, currentContent, relatedFiles)
                    ApplicationManager.getApplication().invokeLater {
                        val selection = StringSelection(content)
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                        Messages.showInfoMessage(
                            project,
                            "File copied with context to clipboard. You can now paste it into your LLM.",
                            "Success"
                        )
                    }
                } catch (ex: Exception) {
                    LOG.error("Error copying file with context", ex)
                    showError(project, "Error: ${ex.message}")
                }
            }
        })
    }

    private fun buildRawContent(
        filePath: String,
        fileContent: String,
        relatedFiles: List<RelatedFile>
    ): String {
        val sb = StringBuilder()
        sb.append("// File: $filePath\n")
        sb.append(fileContent)
        sb.append("\n\n")
        if (relatedFiles.isNotEmpty()) {
            sb.append("// Related files:\n\n")
            for (relatedFile in relatedFiles) {
                sb.append("// File: ${relatedFile.path}\n")
                sb.append(relatedFile.content)
                sb.append("\n\n")
            }
        }
        return sb.toString()
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