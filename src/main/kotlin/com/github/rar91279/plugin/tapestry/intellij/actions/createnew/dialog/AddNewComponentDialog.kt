package com.github.rar91279.plugin.tapestry.intellij.actions.createnew.dialog

import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.intellij.ide.setToolTipText
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapperPeer
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.jps.model.java.JavaResourceRootType
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * Dialog panel for creating a new Tapestry component or page.
 *
 * This dialog provides a user interface for configuring and creating new Tapestry components or pages.
 * It allows users to:
 * - Specify the component/page name with support for path-based naming (e.g., "public/Login")
 * - Select source directories for both the Java class file and the template file
 * - Choose whether to replace existing files if they already exist
 * - Optionally skip template creation (class-only mode)
 *
 * The dialog remembers the user's previous source directory selections by storing them in the
 * module's Tapestry support state. It also applies conventions, such as defaulting the template
 * directory to the classpath resource root (typically `src/main/resources`) when no previous
 * selection exists.
 *
 * @param module the IntelliJ IDEA module where the component or page will be created
 * @param selectedPackage the initially selected package name to populate in the name field
 * @param isPage true if creating a page, false if creating a component (affects default directories and available options)
 */
class AddNewComponentDialog(module: Module, selectedPackage: String, isPage: Boolean) {

    /**
     * Text field for entering the component or page name.
     *
     * The name field supports path-based naming using forward slashes (e.g., "public/Login" creates
     * a component named "Login" in the "public" subpackage). A tooltip is automatically displayed
     * to inform users about this feature. The field is initialized with the [selectedPackage] value
     * and configured to not have initial text selection when the dialog opens.
     */
    private val nameField = JTextField(selectedPackage).apply {
        setToolTipText(HtmlChunk.text("You can use \"/\" to add a path to the name. (ex: \"public/Login\")"))
        putClientProperty(DialogWrapperPeer.HAVE_INITIAL_SELECTION, false)
    }

    /**
     * Combo box for selecting the source directory where the Java class file will be created.
     *
     * This combo box is populated with all source folders from the module's content entries.
     * If the user previously selected a class source directory (stored in module state), that
     * directory is pre-selected in the combo box.
     */
    private val classSourceDirectoryCombo = ComboBox<RootFolderWrapper>()

    /**
     * Combo box for selecting the source directory where the template file will be created.
     *
     * This combo box is populated with source folders and web roots from the module. For pages,
     * web roots are also included as valid template locations. The combo box pre-selects either
     * the user's previously chosen template directory (from module state) or defaults to the
     * resource root directory following Tapestry conventions.
     */
    private val templateSourceDirectoryCombo = ComboBox<RootFolderWrapper>()

    /**
     * Checkbox to enable replacing existing class and template files.
     *
     * When checked, if the generated class or template files already exist in the target location,
     * they will be overwritten. When unchecked, the creation operation will fail if files already exist.
     * A tooltip explains this behavior to the user.
     */
    private val replaceExistingFilesCheck = JCheckBox("Replace existing files").apply {
        setToolTipText(HtmlChunk.text("If the generated class or template exists should they be replaced ?"))
    }

    /**
     * Checkbox to skip template creation and create only the Java class file.
     *
     * When checked, only the component or page class will be created without an accompanying template file.
     * This also disables the template source directory combo box. This option is useful for creating
     * components that render their content programmatically without a template.
     */
    private val createTemplateCheck = JCheckBox("Do not create template")

    /**
     * The main panel containing all dialog UI components.
     *
     * This panel is built using the IntelliJ Platform UI DSL and follows the IntelliJ Platform
     * UI Guidelines. It contains:
     * - A labeled row for the component/page name text field
     * - A labeled row for the class source directory combo box
     * - A labeled row for the template source directory combo box
     * - A row with both the "Replace existing files" and "Do not create template" checkboxes
     *
     * All input fields are aligned to fill the available horizontal space.
     */
    val contentPane: JPanel = panel {
        row("Name:") { cell(nameField).align(AlignX.FILL) }
        row("Class source directory:") { cell(classSourceDirectoryCombo).align(AlignX.FILL) }
        row("Template source directory:") { cell(templateSourceDirectoryCombo).align(AlignX.FILL) }
        row {
            cell(replaceExistingFilesCheck)
            cell(createTemplateCheck)
        }
    }

    /**
     * Initializes the dialog by populating source directory combo boxes and restoring previous selections.
     *
     * This initialization block:
     * 1. Retrieves the previously selected source directories from the module's Tapestry support state,
     *    distinguishing between page and component directories based on [isPage]
     * 2. Populates the class source directory combo box with all source folders from the module's
     *    first content entry
     * 3. Populates the template source directory combo box with source folders and (for pages) web roots
     * 4. Pre-selects the user's previously chosen directories if they match any available options
     * 5. Applies Tapestry conventions by defaulting the template directory to the resource root
     *    (`src/main/resources`) if no previous selection exists
     * 6. Sets up an action listener on the "Do not create template" checkbox to enable/disable
     *    the template source directory combo box
     *
     * The resource root is identified as any source folder with type [JavaResourceRootType.RESOURCE].
     */
    init {
        val state = TapestryModuleSupportLoader.getInstance(module).state
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

        IdeaUtils.findWebRoots(module).map {
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

    /**
     * The component or page name entered by the user.
     *
     * This value may include forward slashes to specify a path-based naming structure
     * (e.g., "public/Login" for a component in the "public" subpackage).
     */
    val newComponentName: String get() = nameField.text

    /**
     * Whether existing files should be replaced if they already exist.
     *
     * Returns `true` if the "Replace existing files" checkbox is checked, `false` otherwise.
     */
    val isReplaceExistingFiles: Boolean get() = replaceExistingFilesCheck.isSelected

    /**
     * Whether template creation should be skipped (class-only mode).
     *
     * Returns `true` if the "Do not create template" checkbox is checked, `false` otherwise.
     */
    val isNotCreatingTemplate: Boolean get() = createTemplateCheck.isSelected

    /**
     * The virtual file representing the selected template source directory.
     *
     * This directory will be used as the target location for creating the template file,
     * unless template creation is skipped via [isNotCreatingTemplate].
     *
     * @throws ClassCastException if the selected item is not a [RootFolderWrapper]
     */
    val templateSourceDirectory: VirtualFile get() = (templateSourceDirectoryCombo.selectedItem as RootFolderWrapper).folder

    /**
     * The virtual file representing the selected class source directory.
     *
     * This directory will be used as the target location for creating the Java class file.
     *
     * @throws ClassCastException if the selected item is not a [RootFolderWrapper]
     */
    val classSourceDirectory: VirtualFile get() = (classSourceDirectoryCombo.selectedItem as RootFolderWrapper).folder

    /**
     * The name text field component, used for setting initial focus in the dialog.
     *
     * This component should be passed to [com.intellij.openapi.ui.DialogBuilder.setPreferredFocusComponent]
     * to ensure the name field receives focus when the dialog opens.
     */
    val nameComponent: JComponent get() = nameField

    /**
     * Enables or disables the template source directory combo box based on template creation settings.
     *
     * When template creation is disabled (class-only mode), the template source directory combo box
     * is disabled since no template will be created. When template creation is enabled, the combo box
     * is enabled to allow directory selection.
     *
     * This method is invoked automatically when the "Do not create template" checkbox state changes.
     *
     * @param enabled true to enable the template directory selection, false to disable it
     */
    fun setNotCreatingTemplate(enabled: Boolean) {
        templateSourceDirectoryCombo.isEnabled = enabled
    }
}
