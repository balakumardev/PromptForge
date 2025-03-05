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
import com.intellij.openapi.ui.DialogWrapper
import dev.balakumar.promptforge.models.RelatedFile
import dev.balakumar.promptforge.settings.PromptForgeSettings
import dev.balakumar.promptforge.utils.FileUtils
import dev.balakumar.promptforge.utils.RelatedFilesCollector
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JScrollPane
import javax.swing.JLabel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.Color
import javax.swing.border.EmptyBorder
import javax.swing.border.CompoundBorder
import javax.swing.border.LineBorder
import javax.swing.KeyStroke
import java.awt.event.KeyEvent
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent.WHEN_FOCUSED
import javax.swing.SwingUtilities
import javax.swing.BorderFactory
import javax.swing.Timer

class MakeChangeAction : AnAction() {
    private val LOG = Logger.getInstance(MakeChangeAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = FileUtils.findTargetFile(e) ?: return

        if (!FileUtils.isJavaFile(file)) {
            Messages.showErrorDialog(project, "This action only works with Java files.", "Unsupported File Type")
            return
        }

        // Show dialog to get the change request
        val changeRequest = showChangeRequestDialog(project, file) ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Preparing Change Request", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    // Get settings
                    val settings = PromptForgeSettings.getInstance()

                    // Get the current content
                    val currentContent = String(file.contentsToByteArray())

                    // Extract class name inside a read action
                    val className = ReadAction.compute<String, Throwable> {
                        FileUtils.extractClassName(file, project) ?: file.nameWithoutExtension
                    }

                    // Collect related files
                    indicator.text = "Collecting related files"
                    val relatedFiles = ReadAction.compute<List<RelatedFile>, Throwable> {
                        RelatedFilesCollector(project).collectRelatedFiles(file)
                    }

                    // Create the prompt
                    val prompt = createChangePrompt(
                        settings.state.makeChangePromptTemplate,
                        className,
                        currentContent,
                        changeRequest,
                        relatedFiles
                    )

                    // Copy to clipboard
                    ApplicationManager.getApplication().invokeLater {
                        val selection = StringSelection(prompt)
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                        Messages.showInfoMessage(
                            project,
                            "Change request prompt copied to clipboard. Paste it to your LLM for suggested changes.",
                            "Success"
                        )
                    }
                } catch (ex: Exception) {
                    LOG.error("Error preparing change request", ex)
                    showError(project, "Error: ${ex.message}")
                }
            }
        })
    }

    private fun showChangeRequestDialog(project: Project, file: VirtualFile): String? {
        val dialog = object : DialogWrapper(project, true) {
            private val textArea = JTextArea(10, 50)

            init {
                title = "Describe the Change You Want to Make"

                // Setup key bindings for the text area
                setupKeyBindings()

                init()
            }

            private fun setupKeyBindings() {
                // Map Enter key to close dialog with OK
                val enterAction = object : AbstractAction() {
                    override fun actionPerformed(e: ActionEvent) {
                        doOKAction()
                    }
                }
                textArea.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter")
                textArea.actionMap.put("enter", enterAction)

                // Map Shift+Enter to insert a newline
                val shiftEnterAction = object : AbstractAction() {
                    override fun actionPerformed(e: ActionEvent) {
                        textArea.append("\n")
                    }
                }
                textArea.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "shiftEnter")
                textArea.actionMap.put("shiftEnter", shiftEnterAction)
            }

            override fun createCenterPanel(): JComponent {
                val panel = JPanel(BorderLayout(0, 10))
                panel.preferredSize = Dimension(600, 300)
                panel.border = EmptyBorder(15, 15, 15, 15)

                // Add a header with the file name
                val headerPanel = JPanel(BorderLayout())
                headerPanel.border = EmptyBorder(0, 0, 10, 0)

                val fileNameLabel = JLabel("File: ${file.name}")
                fileNameLabel.font = fileNameLabel.font.deriveFont(Font.BOLD, 14f)
                headerPanel.add(fileNameLabel, BorderLayout.NORTH)

                val instructionLabel = JLabel("Describe the change you want to make. Press Enter to submit or Shift+Enter for a new line.")
                instructionLabel.font = instructionLabel.font.deriveFont(Font.ITALIC)
                instructionLabel.foreground = Color(100, 100, 100)
                headerPanel.add(instructionLabel, BorderLayout.SOUTH)

                panel.add(headerPanel, BorderLayout.NORTH)

                // Style the text area
                textArea.font = Font("Monospaced", Font.PLAIN, 14)
                textArea.lineWrap = true
                textArea.wrapStyleWord = true
                textArea.border = CompoundBorder(
                    LineBorder(Color(180, 180, 180), 1),
                    EmptyBorder(8, 8, 8, 8)
                )

                // Create a scroll pane with styled border
                val scrollPane = JScrollPane(textArea)
                scrollPane.border = BorderFactory.createEmptyBorder()
                scrollPane.viewportBorder = BorderFactory.createEmptyBorder()

                panel.add(scrollPane, BorderLayout.CENTER)

                return panel
            }

            override fun show() {
                super.show()

                // Use a timer to repeatedly request focus
                val timer = Timer(100, null)
                timer.addActionListener {
                    if (textArea.hasFocus()) {
                        timer.stop()
                    } else {
                        textArea.requestFocusInWindow()
                    }
                }
                timer.isRepeats = true
                timer.start()
            }

            fun getChangeRequest(): String = textArea.text
        }

        return if (dialog.showAndGet()) {
            val changeRequest = dialog.getChangeRequest().trim()
            if (changeRequest.isEmpty()) null else changeRequest
        } else null
    }

    private fun createChangePrompt(
        template: String,
        className: String,
        classContent: String,
        changeRequest: String,
        relatedFiles: List<RelatedFile>
    ): String {
        var result = template

        // Replace placeholders
        result = result.replace("{CLASS_NAME}", className)
        result = result.replace("{CLASS_CONTENT}", classContent)
        result = result.replace("{CHANGE_REQUEST}", changeRequest)

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