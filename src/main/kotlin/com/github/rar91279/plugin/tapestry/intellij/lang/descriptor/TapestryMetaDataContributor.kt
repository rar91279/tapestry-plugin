package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.psi.filters.position.RootTagFilter
import com.intellij.psi.filters.position.TargetNamespaceFilter
import com.intellij.psi.meta.MetaDataContributor
import com.intellij.psi.meta.MetaDataRegistrar
import com.github.rar91279.plugin.tapestry.core.TapestryConstants

/** Registers the namespace descriptors of the Tapestry namespaces. */
class TapestryMetaDataContributor : MetaDataContributor {

    override fun contributeMetaData(registrar: MetaDataRegistrar) {
        registrar.registerMetaData(
            RootTagFilter(TargetNamespaceFilter(TapestryXmlExtension.tapestryTemplateNamespaces())),
            TapestryNamespaceDescriptor::class.java
        )
        registrar.registerMetaData(
            RootTagFilter(TargetNamespaceFilter(TapestryConstants.PARAMETERS_NAMESPACE)),
            TapestryParametersNamespaceDescriptor::class.java
        )
    }
}
