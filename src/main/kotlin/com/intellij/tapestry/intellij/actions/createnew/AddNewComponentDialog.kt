package com.intellij.tapestry.intellij.actions.createnew

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapperPeer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.tapestry.intellij.TapestryModuleSupportLoader
import com.intellij.tapestry.intellij.util.IdeaUtils
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.jps.model.java.JavaResourceRootType
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * Dialog panel to create a new component (or page, when [isPage]).
 *
 * @param module the module where the component will be created
 * @param selectedPackage the initially selected package name
 * @param isPage true if creating a page, false if creating a component
 */
class AddNewComponentDialog(module: Module, selectedPackage: String, isPage: Boolean) {

    /** Text field for entering the component/page name. Supports "/" for path-based names. */
    private val nameField = JTextField(selectedPackage).apply {
        toolTipText = "You can use \"/\" to add a path to the name. (ex: \"public/Login\")"
        putClientProperty(DialogWrapperPeer.HAVE_INITIAL_SELECTION, false)
    }

    /** Combo box for selecting the source directory where the class file will be created. */
    private val classSourceDirectoryCombo = ComboBox<RootFolderWrapper>()

    /** Combo box for selecting the source directory where the template file will be created. */
    private val templateSourceDirectoryCombo = ComboBox<RootFolderWrapper>()

    /** Checkbox to enable replacing existing class and template files. */
    private val replaceExistingFilesCheck = JCheckBox("Replace existing files").apply {
        toolTipText = "If the generated class or template exists should they be replaced ?"
    }

    /** Checkbox to skip template creation (class only). */
    private val createTemplateCheck = JCheckBox("Do not create template")

    /** The main panel containing all dialog UI components. */
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

        var resourceRoot: RootFolderWrapper? = null
        for (sourceFolder in ModuleRootManager.getInstance(module).contentEntries[0].sourceFolders) {
            if (sourceFolder.file == null) continue
            val folderWrapper = RootFolderWrapper(sourceFolder)

            templateSourceDirectoryCombo.addItem(folderWrapper)
            if (folderWrapper.toString() == newTemplatesSourceDirectory) templateSourceDirectoryCombo.selectedItem = folderWrapper

            classSourceDirectoryCombo.addItem(folderWrapper)
            if (folderWrapper.toString() == newClassesSourceDirectory) classSourceDirectoryCombo.selectedItem = folderWrapper

            if (sourceFolder.rootType == JavaResourceRootType.RESOURCE) resourceRoot = folderWrapper
        }

        IdeaUtils.findWebRoots(module).mapNotNull {
            RootFolderWrapper(it)
        }.forEach {
            if (isPage) templateSourceDirectoryCombo.addItem(it)
            if (it.toString() == newTemplatesSourceDirectory) templateSourceDirectoryCombo.selectedItem = it
        }

        // Convention: templates live on the classpath resource root (src/main/resources). Default to it
        // unless the user already chose a template directory before.
        if (newTemplatesSourceDirectory.isNullOrEmpty() && resourceRoot != null) {
            templateSourceDirectoryCombo.selectedItem = resourceRoot
        }

        createTemplateCheck.addActionListener { setNotCreatingTemplate(!createTemplateCheck.isSelected) }
    }

    /** Returns the entered component/page name. */
    val newComponentName: String get() = nameField.text

    /** Returns whether existing files should be replaced. */
    val isReplaceExistingFiles: Boolean get() = replaceExistingFilesCheck.isSelected

    /** Returns whether template creation is skipped. */
    val isNotCreatingTemplate: Boolean get() = createTemplateCheck.isSelected

    /** Returns the selected template source directory. */
    val templateSourceDirectory: VirtualFile get() = (templateSourceDirectoryCombo.selectedItem as RootFolderWrapper).folder

    /** Returns the selected class source directory. */
    val classSourceDirectory: VirtualFile get() = (classSourceDirectoryCombo.selectedItem as RootFolderWrapper).folder

    /** Returns the name field component for focus management. */
    val nameComponent: JComponent get() = nameField

    /**
     * Enables or disables the template source directory combo box.
     *
     * @param enabled true to enable the template directory selection, false to disable it
     */
    fun setNotCreatingTemplate(enabled: Boolean) {
        templateSourceDirectoryCombo.isEnabled = enabled
    }
}
