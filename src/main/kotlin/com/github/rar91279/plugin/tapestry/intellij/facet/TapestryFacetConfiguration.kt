package com.github.rar91279.plugin.tapestry.intellij.facet

import com.intellij.facet.FacetConfiguration
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.openapi.components.PersistentStateComponent
import com.github.rar91279.plugin.tapestry.intellij.facet.ui.FacetEditor
import org.jdom.Element

/**
 * Persisted through [PersistentStateComponent], the replacement for `FacetConfiguration`'s deprecated
 * `readExternal`/`writeExternal`. The state is the raw [Element] rather than a serialized bean so the
 * stored layout stays the `<setting name=".." value=".."/>` one earlier plugin versions wrote — facets
 * configured before this change keep loading.
 */
class TapestryFacetConfiguration : FacetConfiguration, PersistentStateComponent<Element> {

    var filterName: String? = null
    var applicationPackage: String? = null
    var version: TapestryVersion? = null

    override fun createEditorTabs(
        editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager
    ): Array<FacetEditorTab> = arrayOf(FacetEditor(editorContext.facet as TapestryFacet, this))

    override fun getState(): Element = Element("configuration").apply {
        writeSetting("filterName", filterName)
        writeSetting("applicationPackage", applicationPackage)
        writeSetting("version", version?.toString())
    }

    override fun loadState(state: Element) {
        filterName = state.readSetting("filterName")
        applicationPackage = state.readSetting("applicationPackage")
        version = TapestryVersion.fromString(state.readSetting("version"))
    }
}

private fun Element.writeSetting(name: String, value: String?) {
    if (value != null) addContent(Element("setting").setAttribute("name", name).setAttribute("value", value))
}

private fun Element.readSetting(name: String): String? =
    getChildren("setting").firstOrNull { it.getAttributeValue("name") == name }?.getAttributeValue("value")
