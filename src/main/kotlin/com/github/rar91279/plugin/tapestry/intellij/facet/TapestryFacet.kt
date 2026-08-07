package com.github.rar91279.plugin.tapestry.intellij.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetManager
import com.intellij.facet.FacetType
import com.intellij.openapi.module.Module

/**
 * The Tapestry support facet.
 */
class TapestryFacet(
    facetType: FacetType<*, *>,
    module: Module,
    name: String,
    configuration: TapestryFacetConfiguration,
    underlyingFacet: Facet<*>?
) : Facet<TapestryFacetConfiguration>(facetType, module, name, configuration, underlyingFacet) {

    companion object {

        fun findFacetConfiguration(module: Module): TapestryFacetConfiguration? =
            FacetManager.getInstance(module).getFacetByType(TapestryFacetType.ID)?.configuration
    }
}
