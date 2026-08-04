package com.github.rar91279.plugin.tapestry.intellij.facet.ui

import com.github.rar91279.plugin.tapestry.intellij.facet.TapestryFacetConfiguration
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JPanel

/**
 * The panel shown when Tapestry support is added to a module.
 */
class NewFacetDialog(configuration: TapestryFacetConfiguration) {

    private val filterNameField = JBTextField(configuration.filterName ?: "app")
    private val applicationPackageField = JBTextField(configuration.applicationPackage ?: "com.app")
    private val generatePomCheckBox = JBCheckBox("Generate Example Maven2 POM", true)

    val mainPanel: JPanel = panel {
        row("Filter Name:") {
            cell(filterNameField).columns(COLUMNS_MEDIUM)
        }
        row("Application Package:") {
            cell(applicationPackageField).columns(COLUMNS_MEDIUM)
        }
        row {
            cell(generatePomCheckBox)
        }
    }

    val filterName: String
        get() = filterNameField.text

    val applicationPackage: String
        get() = applicationPackageField.text

    fun shouldGeneratePom(): Boolean = generatePomCheckBox.isSelected
}
