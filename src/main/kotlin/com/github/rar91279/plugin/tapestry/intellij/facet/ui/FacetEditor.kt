package com.github.rar91279.plugin.tapestry.intellij.facet.ui

import com.intellij.facet.Facet
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.openapi.util.text.StringUtil
import com.github.rar91279.plugin.tapestry.TapestryBundle
import com.github.rar91279.plugin.tapestry.intellij.facet.AddTapestrySupportUtil
import com.github.rar91279.plugin.tapestry.intellij.facet.TapestryFacet
import com.github.rar91279.plugin.tapestry.intellij.facet.TapestryFacetConfiguration
import com.github.rar91279.plugin.tapestry.intellij.facet.TapestryVersion
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * The "Tapestry" facet settings tab.
 */
class FacetEditor(facet: TapestryFacet, private val configuration: TapestryFacetConfiguration) : FacetEditorTab() {

    private val filterNameField = JBTextField()
    private val applicationPackageField = JBTextField()

    private val mainPanel by lazy {
        panel {
            row {
                text(
                    "Tapestry is an open-source framework for creating dynamic, robust, " +
                    "highly scalable web applications in Java."
                )
            }
            row {
                browserLink("More about Tapestry", "https://tapestry.apache.org")
            }
            group("Application Configuration") {
                row("Filter Name:") {
                    cell(filterNameField).columns(COLUMNS_MEDIUM)
                }
                row("Application Package:") {
                    cell(applicationPackageField).columns(COLUMNS_MEDIUM)
                }
            }
        }
    }

    init {
        if (configuration.filterName == null) {
            configuration.filterName = StringUtil.toLowerCase(facet.module.name)
        }

        reset()
    }

    override fun getDisplayName(): String = TapestryBundle.message("configurable.FacetEditor.display.name")

    override fun createComponent(): JComponent = mainPanel

    override fun onFacetInitialized(facet: Facet<*>) {
        if (configuration.version == null) configuration.version = TapestryVersion.TAPESTRY_5_3_6

        AddTapestrySupportUtil.addSupportInWriteCommandAction(facet.module, configuration, false)
    }

    override fun isModified(): Boolean =
        filterNameField.text != configuration.filterName || applicationPackageField.text != configuration.applicationPackage

    override fun apply() {
        configuration.filterName = filterNameField.text
        configuration.applicationPackage = applicationPackageField.text
    }

    override fun reset() {
        filterNameField.text = configuration.filterName.orEmpty()
        applicationPackageField.text = configuration.applicationPackage.orEmpty()
    }
}
