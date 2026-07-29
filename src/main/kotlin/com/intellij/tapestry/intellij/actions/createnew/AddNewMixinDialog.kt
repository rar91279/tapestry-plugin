package com.intellij.tapestry.intellij.actions.createnew

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.tapestry.intellij.TapestryModuleSupportLoader
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * Dialog panel to create a new mixin.
 */
class AddNewMixinDialog(module: Module, selectedPackage: String) {

    private val nameField = JTextField(selectedPackage).apply {
        toolTipText = "You can use \"/\" to add a path to the name. (ex: \"public/Login\")"
    }
    private val classSourceDirectoryCombo = JComboBox<RootFolderWrapper>()
    private val replaceExistingFilesCheck = JCheckBox("Replace existing files").apply {
        toolTipText = "If the generated class or template exists should they be replaced ?"
    }

    val contentPane: JPanel = panel {
        row("Name:") { cell(nameField).align(AlignX.FILL) }
        row("Class source directory:") { cell(classSourceDirectoryCombo).align(AlignX.FILL) }
        row { cell(replaceExistingFilesCheck) }
    }

    init {
        val newClassesSourceDirectory = TapestryModuleSupportLoader.getInstance(module).state!!.newMixinsClassesSourceDirectory

        for (sourceFolder in ModuleRootManager.getInstance(module).contentEntries[0].sourceFolders) {
            val folderWrapper = RootFolderWrapper(sourceFolder)
            classSourceDirectoryCombo.addItem(folderWrapper)
            if (folderWrapper.toString() == newClassesSourceDirectory) classSourceDirectoryCombo.selectedItem = folderWrapper
        }
    }

    val newMixinName: String get() = nameField.text
    val isReplaceExistingFiles: Boolean get() = replaceExistingFilesCheck.isSelected
    val classSourceDirectory: VirtualFile get() = (classSourceDirectoryCombo.selectedItem as RootFolderWrapper).folder
    val nameComponent: JComponent get() = nameField
}
