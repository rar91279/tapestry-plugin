package com.github.rar91279.plugin.tapestry.tests.util

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.util.ComponentUtils
import com.github.rar91279.plugin.tapestry.intellij.core.resource.xml.IntellijXmlTag
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.tests.core.EmptyFixtureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class TapestryUtilsTest : EmptyFixtureSpec({

    "isComponentTag_is_tag" {
        runReadAction {
            val tapestryTagMock = mockk<XmlTag>(relaxed = true)
            every { tapestryTagMock.namespace } returns TapestryConstants.TEMPLATE_NAMESPACE

            ComponentUtils.isComponentTag(IntellijXmlTag(tapestryTagMock)) shouldBe true

            val attributeMock = mockk<XmlAttribute>(relaxed = true)
            every { attributeMock.localName } returns "att1"
            every { attributeMock.namespace } returns TapestryConstants.TEMPLATE_NAMESPACE

            every { tapestryTagMock.namespace } returns ""
            every { tapestryTagMock.attributes } returns arrayOf(attributeMock)

            ComponentUtils.isComponentTag(IntellijXmlTag(tapestryTagMock)) shouldBe true
        }
    }

    "isComponentTag_is_not_tag" {
        runReadAction {
            val tapestryTagMock = mockk<XmlTag>(relaxed = true)

            val attributeMock = mockk<XmlAttribute>(relaxed = true)
            every { attributeMock.localName } returns "att1"
            every { attributeMock.namespace } returns ""

            every { tapestryTagMock.namespace } returns ""
            every { tapestryTagMock.attributes } returns arrayOf(attributeMock)

            ComponentUtils.isComponentTag(IntellijXmlTag(tapestryTagMock)) shouldBe false
        }
    }

    "getComponentIdentifier_not_component_tag" {
        runReadAction {
            val tapestryTagMock = mockk<XmlTag>(relaxed = true)
            every { tapestryTagMock.namespace } returns ""
            every { tapestryTagMock.getAttribute(any(), any()) } returns null

            TapestryUtils.getComponentIdentifier(tapestryTagMock) shouldBe null
        }
    }
})
