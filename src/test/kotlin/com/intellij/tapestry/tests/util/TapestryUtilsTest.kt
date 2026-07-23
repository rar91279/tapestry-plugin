package com.intellij.tapestry.tests.util

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.intellij.tapestry.core.TapestryConstants
import com.intellij.tapestry.core.util.ComponentUtils
import com.intellij.tapestry.intellij.core.resource.xml.IntellijXmlTag
import com.intellij.tapestry.intellij.util.TapestryUtils
import com.intellij.tapestry.tests.core.EmptyFixtureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class TapestryUtilsTest : EmptyFixtureSpec({

    // preserved from the original @BeforeClass: exercises the default constructor
    TapestryUtils()

    "isComponentTag_is_tag" {
        runReadAction {
            val tapestryTagMock = mockk<XmlTag>(relaxed = true)
            every { tapestryTagMock.namespace } returns TapestryConstants.TEMPLATE_NAMESPACE

            ComponentUtils._isComponentTag(IntellijXmlTag(tapestryTagMock)) shouldBe true

            val attributeMock = mockk<XmlAttribute>(relaxed = true)
            every { attributeMock.localName } returns "att1"
            every { attributeMock.namespace } returns TapestryConstants.TEMPLATE_NAMESPACE

            every { tapestryTagMock.namespace } returns ""
            every { tapestryTagMock.attributes } returns arrayOf(attributeMock)

            ComponentUtils._isComponentTag(IntellijXmlTag(tapestryTagMock)) shouldBe true
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

            ComponentUtils._isComponentTag(IntellijXmlTag(tapestryTagMock)) shouldBe false
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
