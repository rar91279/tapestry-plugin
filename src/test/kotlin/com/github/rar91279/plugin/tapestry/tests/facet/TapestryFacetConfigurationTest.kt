package com.github.rar91279.plugin.tapestry.tests.facet

import com.github.rar91279.plugin.tapestry.intellij.facet.TapestryFacetConfiguration
import com.github.rar91279.plugin.tapestry.intellij.facet.TapestryVersion
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.jdom.Element

class TapestryFacetConfigurationTest : FreeSpec({

    "roundTrip" {
        val saved = TapestryFacetConfiguration().apply {
            filterName = "app"
            applicationPackage = "com.app"
            version = TapestryVersion.TAPESTRY_5_3_6
        }.state

        val loaded = TapestryFacetConfiguration().apply { loadState(saved) }

        loaded.filterName shouldBe "app"
        loaded.applicationPackage shouldBe "com.app"
        loaded.version shouldBe TapestryVersion.TAPESTRY_5_3_6
    }

    // The stored layout is the one earlier plugin versions wrote via JDOMExternalizer.
    "loadsLegacyLayout" {
        val legacy = Element("configuration").apply {
            addContent(Element("setting").setAttribute("name", "filterName").setAttribute("value", "app"))
            addContent(Element("setting").setAttribute("name", "applicationPackage").setAttribute("value", "com.app"))
            addContent(Element("setting").setAttribute("name", "version").setAttribute("value", "5.3.6"))
        }

        val loaded = TapestryFacetConfiguration().apply { loadState(legacy) }

        loaded.filterName shouldBe "app"
        loaded.applicationPackage shouldBe "com.app"
        loaded.version shouldBe TapestryVersion.TAPESTRY_5_3_6
    }

    "unsetValuesAreOmitted" {
        TapestryFacetConfiguration().state.children.size shouldBe 0
    }
})
