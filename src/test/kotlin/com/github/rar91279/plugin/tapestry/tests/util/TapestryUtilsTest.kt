package com.github.rar91279.plugin.tapestry.tests.util

import com.intellij.openapi.application.readActionBlocking
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.util.ComponentUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.tests.core.EmptyFixtureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

/**
 * Test suite for Tapestry utility functions.
 *
 * This test class verifies the behavior of component tag identification and component
 * identifier extraction utilities used throughout the Tapestry plugin. Tests use mocked
 * XML tags and attributes to verify correct behavior without requiring actual PSI elements.
 */
class TapestryUtilsTest : EmptyFixtureSpec({

    /**
     * Verifies that tags are correctly identified as Tapestry component tags.
     *
     * Tests two scenarios:
     * 1. A tag with the Tapestry template namespace is recognized as a component tag
     * 2. A tag with Tapestry namespace attributes is recognized as a component tag
     */
    "isComponentTag_is_tag" {
        readActionBlocking {
            val tapestryTagMock = mockk<XmlTag>(relaxed = true)
            every { tapestryTagMock.namespace } returns TapestryConstants.TEMPLATE_NAMESPACE

            ComponentUtils.isComponentTag(tapestryTagMock) shouldBe true

            val attributeMock = mockk<XmlAttribute>(relaxed = true)
            every { attributeMock.localName } returns "att1"
            every { attributeMock.namespace } returns TapestryConstants.TEMPLATE_NAMESPACE

            every { tapestryTagMock.namespace } returns ""
            every { tapestryTagMock.attributes } returns arrayOf(attributeMock)

            ComponentUtils.isComponentTag(tapestryTagMock) shouldBe true
        }
    }

    /**
     * Verifies that non-Tapestry tags are correctly identified as not being component tags.
     *
     * Tests that a tag without the Tapestry namespace and without Tapestry namespace
     * attributes is not recognized as a component tag.
     */
    "isComponentTag_is_not_tag" {
        readActionBlocking {
            val tapestryTagMock = mockk<XmlTag>(relaxed = true)

            val attributeMock = mockk<XmlAttribute>(relaxed = true)
            every { attributeMock.localName } returns "att1"
            every { attributeMock.namespace } returns ""

            every { tapestryTagMock.namespace } returns ""
            every { tapestryTagMock.attributes } returns arrayOf(attributeMock)

            ComponentUtils.isComponentTag(tapestryTagMock) shouldBe false
        }
    }

    /**
     * Verifies that getComponentIdentifier returns null for non-component tags.
     *
     * Tests that attempting to extract a component identifier from a tag that is not
     * a Tapestry component tag returns null rather than throwing an exception or
     * returning an invalid value.
     */
    "getComponentIdentifier_not_component_tag" {
        readActionBlocking {
            val tapestryTagMock = mockk<XmlTag>(relaxed = true)
            every { tapestryTagMock.namespace } returns ""
            every { tapestryTagMock.getAttribute(any(), any()) } returns null

            TapestryUtils.getComponentIdentifier(tapestryTagMock) shouldBe null
        }
    }
})
