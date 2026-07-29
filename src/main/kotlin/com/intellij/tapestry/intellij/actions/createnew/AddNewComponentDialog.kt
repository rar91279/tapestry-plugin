package com.intellij.tapestry.intellij.actions.createnew

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.DialogWrapperPeer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.tapestry.intellij.TapestryModuleSupportLoader
import com.intellij.tapestry.intellij.util.IdeaUtils
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * Dialog panel to create a new component (or page, when [isPage]).
 */
class AddNewComponentDialog(module: Module, selectedPackage: String, isPage: Boolean) {

    private val nameField = JTextField(selectedPackage).apply {
        toolTipText = "You can use \"/\" to add a path to the name. (ex: \"public/Login\")"
        putClientProperty(DialogWrapperPeer.HAVE_INITIAL_SELECTION, false)
    }
    private val classSourceDirectoryCombo = JComboBox<RootFolderWrapper>()
    private val templateSourceDirectoryCombo = JComboBox<RootFolderWrapper>()
    private val replaceExistingFilesCheck = JCheckBox("Replace existing files").apply {
        toolTipText = "If the generated class or template exists should they be replaced ?"
    }
    private val createTemplateCheck = JCheckBox("Do not create template")

    val contentPane: JPanel = panel {
        row("Name:") { cell(nameField).align(AlignX.FILL) }
        row("Class source directory:") { cell(classSourceDirectoryCombo).align(AlignX.FILL) }
        row("Template source directory:") { cell(templateSourceDirectoryCombo).align(AlignX.FILL) }
        row {
            cell(replaceExistingFilesCheck)
            cell(createTemplateCheck)
        }
    }

    init {
        val state = TapestryModuleSupportLoader.getInstance(module).state!!
        val newTemplatesSourceDirectory =
            if (isPage) state.newPagesTemplatesSourceDirectory else state.newComponentsTemplatesSourceDirectory
        val newClassesSourceDirectory =
            if (isPage) state.newPagesClassesSourceDirectory else state.newComponentsClassesSourceDirectory

        for (sourceFolder in ModuleRootManager.getInstance(module).contentEntries[0].sourceFolders) {
            if (sourceFolder.file == null) continue
            val folderWrapper = RootFolderWrapper(sourceFolder)

            templateSourceDirectoryCombo.addItem(folderWrapper)
            if (folderWrapper.toString() == newTemplatesSourceDirectory) templateSourceDirectoryCombo.selectedItem = folderWrapper

            classSourceDirectoryCombo.addItem(folderWrapper)
            if (folderWrapper.toString() == newClassesSourceDirectory) classSourceDirectoryCombo.selectedItem = folderWrapper
        }

        for (webRoot in IdeaUtils.findWebRoots(module)) {
            if (webRoot.file == null) continue
            val folderWrapper = RootFolderWrapper(webRoot)

            if (isPage) templateSourceDirectoryCombo.addItem(folderWrapper)
            if (folderWrapper.toString() == newTemplatesSourceDirectory) templateSourceDirectoryCombo.selectedItem = folderWrapper
        }

        createTemplateCheck.addActionListener { setNotCreatingTemplate(!createTemplateCheck.isSelected) }
    }

    val newComponentName: String get() = nameField.text
    val isReplaceExistingFiles: Boolean get() = replaceExistingFilesCheck.isSelected
    val isNotCreatingTemplate: Boolean get() = createTemplateCheck.isSelected
    val templateSourceDirectory: VirtualFile get() = (templateSourceDirectoryCombo.selectedItem as RootFolderWrapper).folder
    val classSourceDirectory: VirtualFile get() = (classSourceDirectoryCombo.selectedItem as RootFolderWrapper).folder
    val nameComponent: JComponent get() = nameField

    fun setNotCreatingTemplate(enabled: Boolean) {
        templateSourceDirectoryCombo.isEnabled = enabled
    }
}
