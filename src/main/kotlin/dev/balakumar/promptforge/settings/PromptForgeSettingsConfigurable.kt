package dev.balakumar.promptforge.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import javax.swing.JComponent
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.JPanel
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Font
import java.awt.Color
import javax.swing.border.EmptyBorder
import javax.swing.border.CompoundBorder

class PromptForgeSettingsConfigurable : Configurable {
    private val settings = PromptForgeSettings.getInstance()
    private lateinit var newFilePromptArea: JTextArea
    private lateinit var modifiedFilePromptArea: JTextArea
    private lateinit var explainCodePromptArea: JTextArea
    private lateinit var askQuestionPromptArea: JTextArea
    private lateinit var makeChangePromptArea: JTextArea
    private lateinit var excludedPackagesArea: JTextArea
    private lateinit var tabbedPane: JTabbedPane

    // UI components
    private lateinit var includeRelatedFilesCheckBox: JBCheckBox
    private lateinit var maxRelatedFilesField: JBTextField
    private lateinit var maxDepthField: JBTextField
    private lateinit var includeImplementationsCheckBox: JBCheckBox
    private lateinit var includeJavadocCheckBox: JBCheckBox
    private lateinit var smartPruningCheckBox: JBCheckBox
    private lateinit var includeDependenciesCheckBox: JBCheckBox
    private lateinit var includeDecompiledCheckBox: JBCheckBox
    private lateinit var maxDecompiledFilesField: JBTextField
    private lateinit var skipJavaPackagesCheckBox: JBCheckBox
    private lateinit var trimWhitespaceCheckBox: JBCheckBox
    private lateinit var excludeImportsInRelatedFilesCheckBox: JBCheckBox

    override fun getDisplayName(): String = "PromptForge"

    override fun createComponent(): JComponent {
        tabbedPane = JTabbedPane()

        // General settings panel
        val generalPanel = createGeneralSettingsPanel()
        tabbedPane.addTab("General", generalPanel)

        // Prompt templates panel
        val promptsPanel = createPromptsPanel()
        tabbedPane.addTab("Prompts", promptsPanel)

        // Excluded packages panel
        val packagesPanel = createPackagesPanel()
        tabbedPane.addTab("Excluded Packages", packagesPanel)

        return tabbedPane
    }

    private fun createGeneralSettingsPanel(): JComponent {
        // Initialize UI components
        includeRelatedFilesCheckBox = JBCheckBox("Include related files", settings.state.includeRelatedFiles)
        maxRelatedFilesField = JBTextField(settings.state.maxRelatedFiles.toString())
        maxDepthField = JBTextField(settings.state.maxDepth.toString())
        includeImplementationsCheckBox = JBCheckBox("Include interface implementations", settings.state.includeImplementations)
        includeJavadocCheckBox = JBCheckBox("Include Javadoc comments in related files", settings.state.includeJavadoc)
        smartPruningCheckBox = JBCheckBox("Smart content pruning", settings.state.smartPruning)
        includeDependenciesCheckBox = JBCheckBox("Include external dependencies", settings.state.includeDependencies)
        includeDecompiledCheckBox = JBCheckBox("Include decompiled library classes", settings.state.includeDecompiled)
        maxDecompiledFilesField = JBTextField(settings.state.maxDecompiledFiles.toString())
        skipJavaPackagesCheckBox = JBCheckBox("Skip Java standard library packages", settings.state.skipJavaPackages)
        trimWhitespaceCheckBox = JBCheckBox("Trim whitespace in all files", settings.state.trimWhitespace)
        excludeImportsInRelatedFilesCheckBox = JBCheckBox("Exclude import statements in related files", settings.state.excludeImportsInRelatedFiles)

        // Create main panel with vertical BoxLayout
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.border = EmptyBorder(10, 10, 10, 10)

        // Related Files group
        val relatedFilesPanel = createGroupPanel("Related Files")
        val relatedFilesInnerPanel = JPanel(GridBagLayout())
        relatedFilesInnerPanel.border = EmptyBorder(5, 5, 5, 5)
        val gbc = GridBagConstraints()

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 2
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        relatedFilesInnerPanel.add(includeRelatedFilesCheckBox, gbc)

        gbc.gridy = 1
        gbc.gridwidth = 1
        gbc.insets = Insets(5, 20, 5, 5)
        relatedFilesInnerPanel.add(JBLabel("Max Related Files:"), gbc)
        gbc.gridx = 1
        gbc.insets = Insets(5, 5, 5, 5)
        relatedFilesInnerPanel.add(maxRelatedFilesField, gbc)

        gbc.gridx = 0
        gbc.gridy = 2
        gbc.insets = Insets(5, 20, 5, 5)
        relatedFilesInnerPanel.add(JBLabel("Max Depth:"), gbc)
        gbc.gridx = 1
        gbc.insets = Insets(5, 5, 5, 5)
        relatedFilesInnerPanel.add(maxDepthField, gbc)

        gbc.gridx = 0
        gbc.gridy = 3
        gbc.gridwidth = 2
        gbc.insets = Insets(5, 5, 5, 5)
        relatedFilesInnerPanel.add(includeImplementationsCheckBox, gbc)

        relatedFilesPanel.add(relatedFilesInnerPanel, BorderLayout.CENTER)
        mainPanel.add(relatedFilesPanel)
        mainPanel.add(Box.createVerticalStrut(10))

        // Content Options group
        val contentOptionsPanel = createGroupPanel("Content Options")
        val contentOptionsInnerPanel = JPanel(GridBagLayout())
        contentOptionsInnerPanel.border = EmptyBorder(5, 5, 5, 5)
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 2
        gbc.insets = Insets(5, 5, 5, 5)
        contentOptionsInnerPanel.add(smartPruningCheckBox, gbc)

        gbc.gridy = 1
        gbc.insets = Insets(0, 25, 5, 5)
        val descriptionLabel = JBLabel("Extract only relevant parts of files")
        descriptionLabel.font = descriptionLabel.font.deriveFont(Font.ITALIC)
        descriptionLabel.foreground = Color.GRAY
        contentOptionsInnerPanel.add(descriptionLabel, gbc)

        gbc.gridy = 2
        gbc.insets = Insets(5, 25, 5, 5)
        contentOptionsInnerPanel.add(includeJavadocCheckBox, gbc)

        gbc.gridy = 3
        contentOptionsInnerPanel.add(excludeImportsInRelatedFilesCheckBox, gbc)

        gbc.gridy = 4
        gbc.insets = Insets(5, 5, 5, 5)
        contentOptionsInnerPanel.add(trimWhitespaceCheckBox, gbc)

        contentOptionsPanel.add(contentOptionsInnerPanel, BorderLayout.CENTER)
        mainPanel.add(contentOptionsPanel)
        mainPanel.add(Box.createVerticalStrut(10))

        // External Dependencies group
        val dependenciesPanel = createGroupPanel("External Dependencies")
        val dependenciesInnerPanel = JPanel(GridBagLayout())
        dependenciesInnerPanel.border = EmptyBorder(5, 5, 5, 5)
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 2
        gbc.insets = Insets(5, 5, 5, 5)
        dependenciesInnerPanel.add(includeDependenciesCheckBox, gbc)

        gbc.gridy = 1
        dependenciesInnerPanel.add(includeDecompiledCheckBox, gbc)

        gbc.gridy = 2
        gbc.gridwidth = 1
        gbc.insets = Insets(5, 20, 5, 5)
        dependenciesInnerPanel.add(JBLabel("Max Decompiled Files:"), gbc)
        gbc.gridx = 1
        gbc.insets = Insets(5, 5, 5, 5)
        dependenciesInnerPanel.add(maxDecompiledFilesField, gbc)

        gbc.gridx = 0
        gbc.gridy = 3
        gbc.gridwidth = 2
        gbc.insets = Insets(5, 5, 5, 5)
        dependenciesInnerPanel.add(skipJavaPackagesCheckBox, gbc)

        dependenciesPanel.add(dependenciesInnerPanel, BorderLayout.CENTER)
        mainPanel.add(dependenciesPanel)

        mainPanel.add(Box.createVerticalGlue())

        // Add listeners
        includeRelatedFilesCheckBox.addChangeListener {
            val enabled = includeRelatedFilesCheckBox.isSelected
            maxRelatedFilesField.isEnabled = enabled
            maxDepthField.isEnabled = enabled
            includeImplementationsCheckBox.isEnabled = enabled
        }

        smartPruningCheckBox.addChangeListener {
            val enabled = smartPruningCheckBox.isSelected
            includeJavadocCheckBox.isEnabled = enabled
            excludeImportsInRelatedFilesCheckBox.isEnabled = enabled
        }

        includeDecompiledCheckBox.addChangeListener {
            maxDecompiledFilesField.isEnabled = includeDecompiledCheckBox.isSelected
        }

        // Initialize enabled states
        maxRelatedFilesField.isEnabled = includeRelatedFilesCheckBox.isSelected
        maxDepthField.isEnabled = includeRelatedFilesCheckBox.isSelected
        includeImplementationsCheckBox.isEnabled = includeRelatedFilesCheckBox.isSelected
        includeJavadocCheckBox.isEnabled = smartPruningCheckBox.isSelected
        excludeImportsInRelatedFilesCheckBox.isEnabled = smartPruningCheckBox.isSelected
        maxDecompiledFilesField.isEnabled = includeDecompiledCheckBox.isSelected

        return JBScrollPane(mainPanel)
    }

    private fun createGroupPanel(title: String): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = CompoundBorder(
            EmptyBorder(0, 0, 5, 0),
            BorderFactory.createTitledBorder(title)
        )
        return panel
    }

    private fun createPromptsPanel(): JComponent {
        val promptsPanel = JPanel(BorderLayout())
        promptsPanel.border = EmptyBorder(10, 10, 10, 10)
        val promptsTabbedPane = JTabbedPane()

        newFilePromptArea = JTextArea(settings.state.completeTestClassPromptTemplate, 20, 80)
        newFilePromptArea.font = Font("Monospaced", Font.PLAIN, 13)
        val newFileScrollPane = JBScrollPane(newFilePromptArea)
        val newFilePanel = JPanel(BorderLayout(0, 10))
        newFilePanel.border = EmptyBorder(10, 10, 10, 10)
        val newFileHelpPanel = JPanel(GridBagLayout())
        newFileHelpPanel.border = BorderFactory.createTitledBorder("Available Placeholders")
        val helpGbc = GridBagConstraints()
        helpGbc.gridx = 0
        helpGbc.gridy = 0
        helpGbc.anchor = GridBagConstraints.WEST
        helpGbc.fill = GridBagConstraints.HORIZONTAL
        helpGbc.insets = Insets(3, 5, 3, 5)
        newFileHelpPanel.add(createPlaceholderLabel("{CLASS_NAME}", "The name of the class being tested"), helpGbc)
        helpGbc.gridy++
        newFileHelpPanel.add(createPlaceholderLabel("{CLASS_CONTENT}", "The full content of the class being tested"), helpGbc)
        helpGbc.gridy++
        newFileHelpPanel.add(createPlaceholderLabel("{RELATED_FILES_CONTENT}", "The content of all related files for context"), helpGbc)
        newFilePanel.add(newFileHelpPanel, BorderLayout.NORTH)
        newFilePanel.add(newFileScrollPane, BorderLayout.CENTER)
        promptsTabbedPane.addTab("Complete Test Class Prompt", newFilePanel)

        modifiedFilePromptArea = JTextArea(settings.state.testMethodsForChangesPromptTemplate, 20, 80)
        modifiedFilePromptArea.font = Font("Monospaced", Font.PLAIN, 13)
        val modifiedFileScrollPane = JBScrollPane(modifiedFilePromptArea)
        val modifiedFilePanel = JPanel(BorderLayout(0, 10))
        modifiedFilePanel.border = EmptyBorder(10, 10, 10, 10)
        val modifiedFileHelpPanel = JPanel(GridBagLayout())
        modifiedFileHelpPanel.border = BorderFactory.createTitledBorder("Available Placeholders")
        helpGbc.gridy = 0
        modifiedFileHelpPanel.add(createPlaceholderLabel("{ORIGINAL_CLASS}", "The original version of the class before changes"), helpGbc)
        helpGbc.gridy++
        modifiedFileHelpPanel.add(createPlaceholderLabel("{DIFF}", "The Git diff showing what changed in the class"), helpGbc)
        helpGbc.gridy++
        modifiedFileHelpPanel.add(createPlaceholderLabel("{TEST_FILE}", "The current content of the test file"), helpGbc)
        helpGbc.gridy++
        modifiedFileHelpPanel.add(createPlaceholderLabel("{RELATED_FILES_SECTION}", "A line mentioning the number of related files included"), helpGbc)
        helpGbc.gridy++
        modifiedFileHelpPanel.add(createPlaceholderLabel("{RELATED_FILES_CONTENT}", "The content of all related files for context"), helpGbc)
        modifiedFilePanel.add(modifiedFileHelpPanel, BorderLayout.NORTH)
        modifiedFilePanel.add(modifiedFileScrollPane, BorderLayout.CENTER)
        promptsTabbedPane.addTab("Test Methods for Changes Prompt", modifiedFilePanel)

        explainCodePromptArea = JTextArea(settings.state.explainCodePromptTemplate, 20, 80)
        explainCodePromptArea.font = Font("Monospaced", Font.PLAIN, 13)
        val explainCodeScrollPane = JBScrollPane(explainCodePromptArea)
        val explainCodePanel = JPanel(BorderLayout(0, 10))
        explainCodePanel.border = EmptyBorder(10, 10, 10, 10)
        val explainCodeHelpPanel = JPanel(GridBagLayout())
        explainCodeHelpPanel.border = BorderFactory.createTitledBorder("Available Placeholders")
        helpGbc.gridy = 0
        explainCodeHelpPanel.add(createPlaceholderLabel("{CLASS_NAME}", "The name of the class being explained"), helpGbc)
        helpGbc.gridy++
        explainCodeHelpPanel.add(createPlaceholderLabel("{CLASS_CONTENT}", "The full content of the class"), helpGbc)
        helpGbc.gridy++
        explainCodeHelpPanel.add(createPlaceholderLabel("{RELATED_FILES_CONTENT}", "The content of all related files for context"), helpGbc)
        explainCodePanel.add(explainCodeHelpPanel, BorderLayout.NORTH)
        explainCodePanel.add(explainCodeScrollPane, BorderLayout.CENTER)
        promptsTabbedPane.addTab("Explain Code Prompt", explainCodePanel)

        askQuestionPromptArea = JTextArea(settings.state.askQuestionPromptTemplate, 20, 80)
        askQuestionPromptArea.font = Font("Monospaced", Font.PLAIN, 13)
        val askQuestionScrollPane = JBScrollPane(askQuestionPromptArea)
        val askQuestionPanel = JPanel(BorderLayout(0, 10))
        askQuestionPanel.border = EmptyBorder(10, 10, 10, 10)
        val askQuestionHelpPanel = JPanel(GridBagLayout())
        askQuestionHelpPanel.border = BorderFactory.createTitledBorder("Available Placeholders")
        helpGbc.gridy = 0
        askQuestionHelpPanel.add(createPlaceholderLabel("{CLASS_NAME}", "The name of the class"), helpGbc)
        helpGbc.gridy++
        askQuestionHelpPanel.add(createPlaceholderLabel("{CLASS_CONTENT}", "The full content of the class"), helpGbc)
        helpGbc.gridy++
        askQuestionHelpPanel.add(createPlaceholderLabel("{QUESTION}", "The question entered by the user"), helpGbc)
        helpGbc.gridy++
        askQuestionHelpPanel.add(createPlaceholderLabel("{RELATED_FILES_CONTENT}", "The content of all related files for context"), helpGbc)
        askQuestionPanel.add(askQuestionHelpPanel, BorderLayout.NORTH)
        askQuestionPanel.add(askQuestionScrollPane, BorderLayout.CENTER)
        promptsTabbedPane.addTab("Ask Question Prompt", askQuestionPanel)

        makeChangePromptArea = JTextArea(settings.state.makeChangePromptTemplate, 20, 80)
        makeChangePromptArea.font = Font("Monospaced", Font.PLAIN, 13)
        val makeChangeScrollPane = JBScrollPane(makeChangePromptArea)
        val makeChangePanel = JPanel(BorderLayout(0, 10))
        makeChangePanel.border = EmptyBorder(10, 10, 10, 10)
        val makeChangeHelpPanel = JPanel(GridBagLayout())
        makeChangeHelpPanel.border = BorderFactory.createTitledBorder("Available Placeholders")
        helpGbc.gridy = 0
        makeChangeHelpPanel.add(createPlaceholderLabel("{CLASS_NAME}", "The name of the class"), helpGbc)
        helpGbc.gridy++
        makeChangeHelpPanel.add(createPlaceholderLabel("{CLASS_CONTENT}", "The full content of the class"), helpGbc)
        helpGbc.gridy++
        makeChangeHelpPanel.add(createPlaceholderLabel("{CHANGE_REQUEST}", "The change request entered by the user"), helpGbc)
        helpGbc.gridy++
        makeChangeHelpPanel.add(createPlaceholderLabel("{RELATED_FILES_CONTENT}", "The content of all related files for context"), helpGbc)
        makeChangePanel.add(makeChangeHelpPanel, BorderLayout.NORTH)
        makeChangePanel.add(makeChangeScrollPane, BorderLayout.CENTER)
        promptsTabbedPane.addTab("Make Change Prompt", makeChangePanel)

        promptsPanel.add(promptsTabbedPane, BorderLayout.CENTER)
        return promptsPanel
    }

    private fun createPlaceholderLabel(placeholder: String, description: String): JPanel {
        val panel = JPanel(BorderLayout(10, 0))
        val placeholderLabel = JBLabel(placeholder)
        placeholderLabel.font = Font("Monospaced", Font.BOLD, 12)
        val descriptionLabel = JBLabel(description)
        panel.add(placeholderLabel, BorderLayout.WEST)
        panel.add(descriptionLabel, BorderLayout.CENTER)
        return panel
    }

    private fun createPackagesPanel(): JComponent {
        val packagesPanel = JPanel(BorderLayout(0, 10))
        packagesPanel.border = EmptyBorder(10, 10, 10, 10)
        val headerPanel = JPanel(BorderLayout())
        headerPanel.border = EmptyBorder(0, 0, 10, 0)
        val titleLabel = JBLabel("Excluded Packages")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 14f)
        headerPanel.add(titleLabel, BorderLayout.NORTH)
        val descriptionLabel = JBLabel("Enter one package prefix per line. Files from these packages will be excluded:")
        headerPanel.add(descriptionLabel, BorderLayout.CENTER)
        excludedPackagesArea = JTextArea(settings.state.excludedPackages.joinToString("\n"), 20, 80)
        excludedPackagesArea.font = Font("Monospaced", Font.PLAIN, 13)
        val scrollPane = JBScrollPane(excludedPackagesArea)
        packagesPanel.add(headerPanel, BorderLayout.NORTH)
        packagesPanel.add(scrollPane, BorderLayout.CENTER)
        return packagesPanel
    }

    override fun isModified(): Boolean {
        return settings.state.completeTestClassPromptTemplate != newFilePromptArea.text ||
                settings.state.testMethodsForChangesPromptTemplate != modifiedFilePromptArea.text ||
                settings.state.explainCodePromptTemplate != explainCodePromptArea.text ||
                settings.state.askQuestionPromptTemplate != askQuestionPromptArea.text ||
                settings.state.makeChangePromptTemplate != makeChangePromptArea.text ||
                settings.state.excludedPackages.joinToString("\n") != excludedPackagesArea.text ||
                settings.state.includeRelatedFiles != includeRelatedFilesCheckBox.isSelected ||
                settings.state.maxRelatedFiles.toString() != maxRelatedFilesField.text ||
                settings.state.maxDepth.toString() != maxDepthField.text ||
                settings.state.includeImplementations != includeImplementationsCheckBox.isSelected ||
                settings.state.includeJavadoc != includeJavadocCheckBox.isSelected ||
                settings.state.smartPruning != smartPruningCheckBox.isSelected ||
                settings.state.includeDependencies != includeDependenciesCheckBox.isSelected ||
                settings.state.includeDecompiled != includeDecompiledCheckBox.isSelected ||
                settings.state.maxDecompiledFiles.toString() != maxDecompiledFilesField.text ||
                settings.state.skipJavaPackages != skipJavaPackagesCheckBox.isSelected ||
                settings.state.trimWhitespace != trimWhitespaceCheckBox.isSelected ||
                settings.state.excludeImportsInRelatedFiles != excludeImportsInRelatedFilesCheckBox.isSelected
    }

    override fun apply() {
        settings.state.completeTestClassPromptTemplate = newFilePromptArea.text
        settings.state.testMethodsForChangesPromptTemplate = modifiedFilePromptArea.text
        settings.state.explainCodePromptTemplate = explainCodePromptArea.text
        settings.state.askQuestionPromptTemplate = askQuestionPromptArea.text
        settings.state.makeChangePromptTemplate = makeChangePromptArea.text
        val excludedPackages = excludedPackagesArea.text.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        settings.state.excludedPackages = excludedPackages
        settings.state.includeRelatedFiles = includeRelatedFilesCheckBox.isSelected
        settings.state.maxRelatedFiles = maxRelatedFilesField.text.toIntOrNull() ?: 5
        settings.state.maxDepth = maxDepthField.text.toIntOrNull() ?: 1
        settings.state.includeImplementations = includeImplementationsCheckBox.isSelected
        settings.state.includeJavadoc = includeJavadocCheckBox.isSelected
        settings.state.smartPruning = smartPruningCheckBox.isSelected
        settings.state.includeDependencies = includeDependenciesCheckBox.isSelected
        settings.state.includeDecompiled = includeDecompiledCheckBox.isSelected
        settings.state.maxDecompiledFiles = maxDecompiledFilesField.text.toIntOrNull() ?: 3
        settings.state.skipJavaPackages = skipJavaPackagesCheckBox.isSelected
        settings.state.trimWhitespace = trimWhitespaceCheckBox.isSelected
        settings.state.excludeImportsInRelatedFiles = excludeImportsInRelatedFilesCheckBox.isSelected
    }

    override fun reset() {
        newFilePromptArea.text = settings.state.completeTestClassPromptTemplate
        modifiedFilePromptArea.text = settings.state.testMethodsForChangesPromptTemplate
        explainCodePromptArea.text = settings.state.explainCodePromptTemplate
        askQuestionPromptArea.text = settings.state.askQuestionPromptTemplate
        makeChangePromptArea.text = settings.state.makeChangePromptTemplate
        excludedPackagesArea.text = settings.state.excludedPackages.joinToString("\n")
        includeRelatedFilesCheckBox.isSelected = settings.state.includeRelatedFiles
        maxRelatedFilesField.text = settings.state.maxRelatedFiles.toString()
        maxDepthField.text = settings.state.maxDepth.toString()
        includeImplementationsCheckBox.isSelected = settings.state.includeImplementations
        includeJavadocCheckBox.isSelected = settings.state.includeJavadoc
        smartPruningCheckBox.isSelected = settings.state.smartPruning
        includeDependenciesCheckBox.isSelected = settings.state.includeDependencies
        includeDecompiledCheckBox.isSelected = settings.state.includeDecompiled
        maxDecompiledFilesField.text = settings.state.maxDecompiledFiles.toString()
        skipJavaPackagesCheckBox.isSelected = settings.state.skipJavaPackages
        trimWhitespaceCheckBox.isSelected = settings.state.trimWhitespace
        excludeImportsInRelatedFilesCheckBox.isSelected = settings.state.excludeImportsInRelatedFiles
        maxRelatedFilesField.isEnabled = settings.state.includeRelatedFiles
        maxDepthField.isEnabled = settings.state.includeRelatedFiles
        includeImplementationsCheckBox.isEnabled = settings.state.includeRelatedFiles
        includeJavadocCheckBox.isEnabled = settings.state.smartPruning
        excludeImportsInRelatedFilesCheckBox.isEnabled = settings.state.smartPruning
        maxDecompiledFilesField.isEnabled = settings.state.includeDecompiled
    }
}