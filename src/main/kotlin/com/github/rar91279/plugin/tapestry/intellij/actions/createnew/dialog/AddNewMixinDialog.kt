package com.github.rar91279.plugin.tapestry.intellij.actions.createnew

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.actions.createnew.dialog.RootFolderWrapper
import com.intellij.ide.setToolTipText
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * Dialog panel to create a new Tapestry mixin.
 *
 * This dialog allows users to configure and create a new mixin by specifying:
 * - The mixin name (with optional path using "/" separator)
 * - The source directory for the generated class
 * - Whether to replace existing files if they conflict
 *
 * The dialog persists user preferences for source directories between invocations
 * using the Tapestry module state.
 *
 * @param module the IntelliJ module in which to create the mixin
 * @param selectedPackage the default package/name to pre-populate in the name field
 */
class AddNewMixinDialog(module: Module, selectedPackage: String) {

    /**
     * Text field for entering the mixin name.
     *
     * Users can use "/" to specify a path structure for the mixin name (e.g., "public/Login").
     * The field is initialized with the selected package name and displays a tooltip
     * explaining the path syntax.
     */
    private val nameField = JTextField(selectedPackage).apply {
        setToolTipText(HtmlChunk.text("You can use \"/\" to add a path to the name. (ex: \"public/Login\")"))
    }

    /**
     * Combo box for selecting the source directory where the mixin class will be generated.
     *
     * Populated with all source folders from the module's content entries and defaults
     * to the previously selected directory stored in the module state.
     */
    private val classSourceDirectoryCombo = ComboBox<RootFolderWrapper>()

    /**
     * Checkbox indicating whether existing files should be replaced during mixin creation.
     *
     * When checked, if a mixin class or template with the same name already exists,
     * it will be overwritten. The checkbox displays a tooltip explaining this behavior.
     */
    private val replaceExistingFilesCheck = JCheckBox("Replace existing files").apply {
        setToolTipText(HtmlChunk.text("If the generated class or template exists should they be replaced ?"))
    }

    /**
     * The main panel containing all dialog components.
     *
     * The panel is structured using the IntelliJ DSL builder with three rows:
     * - Name field for entering the mixin name
     * - Combo box for selecting the class source directory
     * - Checkbox for the replace existing files option
     */
    val contentPane: JPanel = panel {
        row("Name:") { cell(nameField).align(AlignX.FILL) }
        row("Class source directory:") { cell(classSourceDirectoryCombo).align(AlignX.FILL) }
        row { cell(replaceExistingFilesCheck) }
    }

    /**
     * Initializes the dialog by populating the source directory combo box.
     *
     * Retrieves the previously selected source directory for new mixins from the module state
     * and populates the combo box with all available source folders from the module.
     * If a previous selection exists, it is pre-selected in the combo box.
     */
    init {
        val newClassesSourceDirectory = TapestryModuleSupportLoader.getInstance(module).state.newMixinsClassesSourceDirectory

        for (sourceFolder in ModuleRootManager.getInstance(module).contentEntries[0].sourceFolders) {
            val folderWrapper = RootFolderWrapper(sourceFolder)
            classSourceDirectoryCombo.addItem(folderWrapper)
            if (folderWrapper.toString() == newClassesSourceDirectory) classSourceDirectoryCombo.selectedItem = folderWrapper
        }
    }

    /**
     * Returns the mixin name entered by the user in the name field.
     */
    val newMixinName: String get() = nameField.text

    /**
     * Returns whether the user has chosen to replace existing files.
     *
     * @return true if existing files should be replaced, false otherwise
     */
    val isReplaceExistingFiles: Boolean get() = replaceExistingFilesCheck.isSelected

    /**
     * Returns the selected source directory for the mixin class.
     *
     * @return the virtual file representing the selected source directory
     */
    val classSourceDirectory: VirtualFile get() = (classSourceDirectoryCombo.selectedItem as RootFolderWrapper).folder

    /**
     * Returns the name field component for setting preferred focus in the dialog.
     *
     * @return the JComponent representing the name text field
     */
    val nameComponent: JComponent get() = nameField
}
