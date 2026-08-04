package com.github.rar91279.plugin.tapestry.intellij

import com.intellij.javaee.ResourceRegistrar
import com.intellij.javaee.StandardResourceProvider
import com.github.rar91279.plugin.tapestry.core.TapestryConstants

class TapestryResourceProvider : StandardResourceProvider {

    override fun registerResources(registrar: ResourceRegistrar) {
        registrar.addIgnoredResource(TapestryConstants.PARAMETERS_NAMESPACE)
        registrar.addStdResource(TapestryConstants.TEMPLATE_NAMESPACE, "/META-INF/tapestry_5_1_0.xsd", javaClass)
        registrar.addStdResource(TapestryConstants.TEMPLATE_NAMESPACE2, "/META-INF/tapestry_5_0_0.xsd", javaClass)
        registrar.addStdResource(TapestryConstants.TEMPLATE_NAMESPACE3, "/META-INF/tapestry_5_3.xsd", javaClass)
        registrar.addStdResource(TapestryConstants.TEMPLATE_NAMESPACE4, "/META-INF/tapestry_5_4.xsd", javaClass)
    }
}
