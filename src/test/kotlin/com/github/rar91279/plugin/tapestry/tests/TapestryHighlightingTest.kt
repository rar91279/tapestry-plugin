package com.github.rar91279.plugin.tapestry.tests

import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter.EXTENSION_POINT_NAME
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.htmlInspections.RequiredAttributesInspection
import com.intellij.codeInspection.xml.DeprecatedClassUsageInspection
import com.intellij.lang.properties.codeInspection.unused.UnusedPropertyInspection
import com.intellij.openapi.editor.XmlHighlighterColors
import com.intellij.openapi.vfs.VirtualFile
import com.github.rar91279.plugin.tapestry.intellij.inspections.TelReferencesInspection
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.util.containers.ContainerUtil

/**
 * @author Alexey Chmutov
 */
class TapestryHighlightingTest : TapestryBaseTestCase() {

    override fun getBasePath(): String {
        return "highlighting/"
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        suppressXmlNSAnnotator()
    }

    private fun suppressXmlNSAnnotator() {
        val filter = HighlightInfoFilter { info, _ -> info.forcedTextAttributesKey != XmlHighlighterColors.XML_NS_PREFIX }
        EXTENSION_POINT_NAME.point.registerExtension(filter, myFixture.testRootDisposable)
    }

    fun testTmlTagNameUsingSubpackage() {
        addComponentToProject("other.Count")
        ExpectedHighlightingData.expectedDuplicatedHighlighting { doTest(true) }
    }

    fun testTmlAttrName() {
        addComponentToProject("Count")
        ExpectedHighlightingData.expectedDuplicatedHighlighting { doTest(true) }
    }

    fun testTmlAttrNameInHtmlTag() {
        ExpectedHighlightingData.expectedDuplicatedHighlighting { doTest(true, DeprecatedClassUsageInspection()) }
    }

    fun testHtml5() {
        doTest(true)
    }

    fun testUnknownTypeOfTag() {
        addComponentToProject("Count")
        doTest(false)
    }

    fun testAttrNameWithUnknownPrefixInHtmlTag() {
        addComponentToProject("Count")
        ExpectedHighlightingData.expectedDuplicatedHighlighting { doTest(true) }
    }

    fun testTmlAttrNameWithPrefix() {
        addComponentToProject("Count")
        addComponentToProject("Count2")
        doTest(false, RequiredAttributesInspection(), TelReferencesInspection())
    }

    fun testNonPropBindingPrefix() {
        doTest(true)
    }

    fun testTelPropertiesAndAccessors() {
        doTest(true, TelReferencesInspection())
    }

    fun testTelPropertiesAndAccessors2() {
        doTest(true, TelReferencesInspection())
    }

    fun testHtmlTagNameInHtmlParentTag() {
        addComponentToProject("Count")
        ExpectedHighlightingData.expectedDuplicatedHighlighting { doTest(true) }
    }

    fun testHtmlTagNameInHtmlParentTagError() {
        addComponentToProject("Count")
        ExpectedHighlightingData.expectedDuplicatedHighlighting { doTest(true) }
    }

    fun testTmlIfWithElse() {
        addComponentToProject("If")
        addComponentToProject("TestComp")
        ExpectedHighlightingData.expectedDuplicatedHighlighting { doTest(true) }
    }

    fun testAbstractComponent() {
        addAbstractComponentToProject("AbstractComponent")
        val tmlName = getElementTemplateFileName()
        val templateFile = myFixture.copyFileToProject(tmlName, ABSTRACT_COMPONENTS_PACKAGE_PATH + tmlName)
        myFixture.configureFromExistingVirtualFile(templateFile)
        myFixture.enableInspections(TelReferencesInspection())
        myFixture.testHighlighting(true, true, true, templateFile)
    }

    fun testComponentFromJar() {
        doTest(false)
    }

    fun testLibraryMapping() {
        addComponentToProject("Count3")
        doTest(false)
    }

    fun testNewSchema() {
        ExpectedHighlightingData.expectedDuplicatedHighlighting { doTest(true) }
    }

    fun testPropertyReferences() {
        myFixture.enableInspections(UnusedPropertyInspection())
        myFixture.testHighlighting(true, true, true, getTestName(false) + ".properties", getTestName(false) + ".tml")
    }

    fun testSchema() {
        doTest(false)
    }

    override fun addTapestryLibraries(moduleBuilder: JavaModuleFixtureBuilder<*>) {
        super.addTapestryLibraries(moduleBuilder)
        if (ourTestsWithExtraLibraryComponents.contains(getTestName(false))) {
            moduleBuilder.addLibraryJars("tapestry_5.1.0.5_additional", Util.getCommonTestDataPath() + "libs", "tapestry-upload-5.1.0.5.jar")
        }
    }

    protected fun doTest(checkInfos: Boolean, vararg tools: LocalInspectionTool) {
        val templateFile = initByComponent(true)
        myFixture.enableInspections(*tools)
        myFixture.testHighlighting(true, checkInfos, true, templateFile)
    }

    companion object {
        private val ourTestsWithExtraLibraryComponents: Set<String> =
            ContainerUtil.newHashSet("ComponentFromJar", "LibraryMapping")
    }
}
