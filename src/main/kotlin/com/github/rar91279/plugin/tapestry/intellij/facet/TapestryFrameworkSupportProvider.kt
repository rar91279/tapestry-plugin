package com.github.rar91279.plugin.tapestry.intellij.facet

import com.intellij.CommonBundle
import com.intellij.facet.ui.FacetBasedFrameworkSupportProvider
import com.intellij.ide.util.frameworkSupport.FrameworkRole
import com.intellij.ide.util.frameworkSupport.FrameworkVersion
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.javaee.framework.JavaeeProjectCategory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.github.rar91279.plugin.tapestry.intellij.facet.ui.NewFacetDialog
import com.github.rar91279.plugin.tapestry.intellij.util.Validators

class TapestryFrameworkSupportProvider : FacetBasedFrameworkSupportProvider<TapestryFacet>(TapestryFacetType.getInstance()) {

    override fun setupConfiguration(
        tapestryFacet: TapestryFacet,
        modifiableRootModel: ModifiableRootModel,
        version: FrameworkVersion
    ) {
        setupConfiguration(tapestryFacet.configuration, tapestryFacet.module, TapestryVersion.fromString(version.versionName))
    }

    override fun getVersions(): List<FrameworkVersion> = TapestryVersion.entries.map { FrameworkVersion(it.toString()) }

    override fun isEnabledForModuleBuilder(builder: ModuleBuilder): Boolean = false

    override fun getTitle(): String = "Tapestry"

    override fun onFacetCreated(facet: TapestryFacet, rootModel: ModifiableRootModel, version: FrameworkVersion) {
        val project = facet.module.project

        // The facet is created while the project is still being set up, so the dialog is deferred to a
        // later event (StartupManager.runAfterOpened, used before, is internal API).
        ApplicationManager.getApplication().invokeLater(
            {
                if (project.isDisposed) return@invokeLater

                val configuration = facet.configuration
                val newFacetDialog = NewFacetDialog(configuration)
                val builder = DialogBuilder(project)

                builder.removeAllActions()
                builder.addOkAction()
                builder.setCenterPanel(newFacetDialog.mainPanel)
                builder.setTitle("New Tapestry Support")

                builder.setOkOperation {
                    if (!Validators.isValidPackageName(newFacetDialog.applicationPackage)) {
                        Messages.showErrorDialog("Invalid package!", CommonBundle.getErrorTitle())
                        return@setOkOperation
                    }
                    configuration.filterName = newFacetDialog.filterName
                    configuration.applicationPackage = newFacetDialog.applicationPackage

                    builder.window.dispose()
                }
                builder.showModal(true)

                AddTapestrySupportUtil.addSupportInWriteCommandAction(
                    rootModel.module, configuration, newFacetDialog.shouldGeneratePom()
                )
            },
            ModalityState.nonModal()
        )
    }

    override fun getRoles(): Array<FrameworkRole> = arrayOf(JavaeeProjectCategory.ROLE)

    companion object {

        fun setupConfiguration(conf: TapestryFacetConfiguration, module: Module, version: TapestryVersion?) {
            conf.version = version
            if (conf.filterName.isNullOrEmpty()) conf.filterName = module.name.lowercase()
            if (conf.applicationPackage.isNullOrEmpty()) conf.applicationPackage = "com.app"
        }
    }
}
