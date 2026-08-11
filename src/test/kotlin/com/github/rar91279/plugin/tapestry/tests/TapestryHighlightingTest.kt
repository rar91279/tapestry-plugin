package com.github.rar91279.plugin.tapestry.tests

import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter.EXTENSION_POINT_NAME
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.htmlInspections.RequiredAttributesInspection
import com.intellij.codeInspection.xml.DeprecatedClassUsageInspection
import com.intellij.lang.properties.codeInspection.unused.UnusedPropertyInspection
import com.intellij.openapi.editor.XmlHighlighterColors
import com.intellij.openapi.vfs.VirtualFile
import com.github.rar91279.plugin.tapestry.intellij.inspections.PublicInstrumentedFieldInspection
import com.github.rar91279.plugin.tapestry.intellij.inspections.TelReferencesInspection
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.util.containers.ContainerUtil
import com.intellij.xml.util.CheckTagEmptyBodyInspection

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

    fun testPropertyParameter() {
        addAbstractComponentToProject("RibbonBase")
        addComponentToProject("ui.ribbon.Ribbon")
        doTest(false)
    }

    // A library whose mapping is contributed from a Kotlin module class, which the Java stub indexes miss.
    fun testKotlinLibraryMapping() {
        myFixture.addFileToProject(
            "com/testlib/security/components/HasPermission.java",
            "package com.testlib.security.components; public class HasPermission {}"
        )
        myFixture.addFileToProject(
            "com/testapp/services/SecurityModule.kt",
            """
            package com.testapp.services

            import org.apache.tapestry5.ioc.Configuration
            import org.apache.tapestry5.services.LibraryMapping

            object SecurityModule {
                fun contributeComponentClassResolver(configuration: Configuration<LibraryMapping>) {
                    configuration.add(LibraryMapping("security", "com.testlib.security"))
                }
            }
            """.trimIndent()
        )
        doTest(false)
    }

    fun testPublicInstrumentedField() {
        myFixture.enableInspections(PublicInstrumentedFieldInspection())
        initByComponent(false)
        myFixture.checkHighlighting(true, false, true)
    }

    fun testEmptyTagBody() {
        // "XML tag has empty body" is suppressed in templates, see TmlInspectionSuppressor
        doTest(false, CheckTagEmptyBodyInspection())
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
