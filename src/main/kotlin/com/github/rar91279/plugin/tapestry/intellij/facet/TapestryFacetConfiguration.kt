package com.github.rar91279.plugin.tapestry.intellij.facet

import com.intellij.facet.FacetConfiguration
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.openapi.util.JDOMExternalizer
import com.github.rar91279.plugin.tapestry.intellij.facet.ui.FacetEditor
import org.jdom.Element

class TapestryFacetConfiguration : FacetConfiguration {

    var filterName: String? = null
    var applicationPackage: String? = null
    var version: TapestryVersion? = null

    override fun createEditorTabs(
        editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager
    ): Array<FacetEditorTab> = arrayOf(FacetEditor(editorContext.facet as TapestryFacet, this))

    override fun readExternal(element: Element) {
        filterName = JDOMExternalizer.readString(element, "filterName")
        applicationPackage = JDOMExternalizer.readString(element, "applicationPackage")
        version = TapestryVersion.fromString(JDOMExternalizer.readString(element, "version"))
    }

    override fun writeExternal(element: Element) {
        JDOMExternalizer.write(element, "filterName", filterName)
        JDOMExternalizer.write(element, "applicationPackage", applicationPackage)
        JDOMExternalizer.write(element, "version", version?.toString())
    }
}
