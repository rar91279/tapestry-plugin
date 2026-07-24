package com.intellij.tapestry.tests

import com.intellij.javaee.ExternalResourceManagerEx
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.css.CssClass
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.tapestry.core.MappingDataCache
import com.intellij.tapestry.core.model.TapestryLibrary
import com.intellij.tapestry.intellij.TapestryModuleSupportLoader
import com.intellij.tapestry.psi.TapestryAccessorMethod
import com.intellij.tapestry.psi.TmlFile
import com.intellij.xml.Html5SchemaProvider
import com.intellij.xml.util.XmlUtil
import junit.framework.Assert
import org.intellij.plugins.relaxNG.compact.RncElementTypes

/**
 * @author Alexey Chmutov
 */
class TapestryResolveTest : TapestryBaseTestCase() {
    override fun getBasePath(): String {
        return "resolve/"
    }

    private var myOldDoctype: String? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        val manager = ExternalResourceManagerEx.getInstanceEx()
        myOldDoctype = manager.getDefaultHtmlDoctype(myFixture.project)
        manager.setDefaultHtmlDoctype(XmlUtil.XHTML_URI, myFixture.project)
    }

    @Throws(Exception::class)
    override fun tearDown() {
        try {
            ExternalResourceManagerEx.getInstanceEx().setDefaultHtmlDoctype(myOldDoctype!!, myFixture.project)
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

    fun testHtmlTagName() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(XmlTag::class.java)
        Assert.assertEquals("xs:element", ref.name)
    }

    fun testHtml5TagName() {
        val manager = ExternalResourceManagerEx.getInstanceEx()
        val doctype = manager.getDefaultHtmlDoctype(myFixture.project)
        manager.setDefaultHtmlDoctype(Html5SchemaProvider.getHtml5SchemaLocation(), myFixture.project)
        try {
            addComponentToProject("Count")
            initByComponent()
            val ref = resolveReferenceAtCaretPosition(PsiElement::class.java).navigationElement
            Assert.assertEquals(RncElementTypes.NAME_CLASS, ref.node.elementType)
            Assert.assertEquals("body", ref.text)
        } finally {
            manager.setDefaultHtmlDoctype(doctype, myFixture.project)
        }
    }

    fun testHtmlAttrName() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(XmlTag::class.java)
        Assert.assertEquals("xs:attribute", ref.name)
    }

    fun testLibTmlTagName() {
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(PsiClass::class.java)
        Assert.assertEquals("org.apache.tapestry5.corelib.components.Any", ref.qualifiedName)
    }

    fun testTmlMapping() {
        val psiFile = myFixture.configureByFile("TmlMapping.java")
        val compute = MappingDataCache.getMappingData(psiFile)
        Assert.assertTrue(compute.containsKey("foo"))
    }

    fun testTmlMapping2() {
        val psiFile = myFixture.configureByFile("TmlMapping2.java")
        val compute = MappingDataCache.getMappingData(psiFile)
        Assert.assertTrue(compute.containsKey("foo"))
    }

    fun testTmlMapping3() {
        myFixture.configureByFile("TmlMapping3.java")
        val moduleSupportLoader = TapestryModuleSupportLoader.getInstance(myModule)
        val libraries = moduleSupportLoader.tapestryProject.libraries
        var libraryOfInterest: TapestryLibrary? = null

        for (library in libraries) {
            if ("dk.nesluop.librarymapping.framework" == library.basePackage) {
                libraryOfInterest = library
                break
            }
        }
        Assert.assertNotNull(libraryOfInterest)
    }

    fun testTmlMixin() {
        addComponentToProject("Count")
        addMixinToProject("FooMixin")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(PsiField::class.java)
        Assert.assertEquals("bar", ref.name)
    }

    fun testTmlParameter() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(PsiField::class.java)
        Assert.assertEquals("start", ref.name)
    }

    fun testTmlTagName() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(PsiClass::class.java)
        Assert.assertEquals("$TEST_APPLICATION_PACKAGE.$COMPONENTS.Count", ref.qualifiedName)
    }

    fun testTmlTagNameUsingSubpackage() {
        addComponentToProject("other.Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(PsiClass::class.java)
        Assert.assertEquals("$TEST_APPLICATION_PACKAGE.$COMPONENTS.other.Count", ref.qualifiedName)
    }

    fun testTmlAttrName() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(PsiField::class.java)
        Assert.assertEquals("end", ref.name)
    }

    fun testTmlAttrNameInHtmlTag() {
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(PsiField::class.java)
        Assert.assertEquals("page", ref.name)
    }

    fun testTmlAttrNameWithoutPrefixInHtmlTag() {
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(PsiField::class.java)
        Assert.assertEquals("page", ref.name)
    }

    fun testTmlAttrNameWithPrefix() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(PsiField::class.java)
        Assert.assertEquals("end", ref.name)
    }

    fun testAttrNameWithUnknownPrefix() {
        addComponentToProject("Count")
        initByComponent()
        checkReferenceAtCaretPositionUnresolved()
    }

    fun testAttrNameWithUnknownPrefixInHtmlTag() {
        addComponentToProject("Count")
        initByComponent()
        checkReferenceAtCaretPositionUnresolved()
    }

    fun testUnknownAttrName() {
        addComponentToProject("Count")
        initByComponent()
        checkReferenceAtCaretPositionUnresolved()
    }

    fun testHtmlTypeAttrName() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(XmlTag::class.java)
        Assert.assertEquals("xs:attribute", ref.name)
    }

    fun testTypeAttrName() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(XmlTag::class.java)
        Assert.assertEquals("button", ref.name)
    }

    fun testTypeAttrValue() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(PsiClass::class.java)
        Assert.assertEquals("$TEST_APPLICATION_PACKAGE.$COMPONENTS.Count", ref.qualifiedName)
    }

    fun testTypeAttrUnknownValue() {
        addComponentToProject("Count")
        initByComponent()
        checkReferenceAtCaretPositionUnresolved()
    }

    fun testHtmlIdAttrName() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(XmlTag::class.java)
        Assert.assertEquals("xs:attribute", ref.name)
    }

    fun testIdAttrName() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(XmlTag::class.java)
        Assert.assertEquals("a", ref.name)
    }

    fun testIdAttrValue() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(PsiField::class.java)
        Assert.assertEquals("index55", ref.name)
    }

    fun testIdAttrValueTypeAttrPresent() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(XmlAttributeValue::class.java)
        Assert.assertEquals("t:id", (ref.parent as XmlAttribute).name)
    }

    fun testIdAttrValueInTmlTag() {
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(XmlAttributeValue::class.java)
        Assert.assertEquals("t:id", (ref.parent as XmlAttribute).name)
    }

    fun testIdAttrValueUnresolved() {
        addComponentToProject("Count")
        initByComponent()
        checkReferenceAtCaretPositionUnresolved()
    }

    fun testPageAttrValue() {
        addPageToProject("StartPage")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(TmlFile::class.java)
        Assert.assertEquals("StartPage.tml", ref.name)
    }

    fun testPageAttrValue2() {
        addElementToProject(PAGES_PACKAGE_PATH, "StartPage2", Util.DOT_GROOVY)
        addElementToProject(PAGES_PACKAGE_PATH, "StartPage2", getTemplateExtension())
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(TmlFile::class.java)
        Assert.assertEquals("StartPage2.tml", ref.name)
    }

    fun testPageAttrValue3() {
        addElementToProject(PAGES_PACKAGE_PATH, "StartPage3", Util.DOT_KOTLIN)
        addElementToProject(PAGES_PACKAGE_PATH, "StartPage3", getTemplateExtension())
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(TmlFile::class.java)
        Assert.assertEquals("StartPage3.tml", ref.name)
    }

    fun testPageAttrValueOfPagelinkTag() {
        addPageToProject("StartPage")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(TmlFile::class.java)
        Assert.assertEquals("StartPage.tml", ref.name)
    }

    fun testPrefixedPageAttrValue() {
        addPageToProject("StartPage")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(TmlFile::class.java)
        Assert.assertEquals("StartPage.tml", ref.name)
    }

    fun testPageAttrValueReferencingToSubpackage() {
        addPageToProject("subpack.StartPage")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(TmlFile::class.java)
        Assert.assertEquals("StartPage.tml", ref.name)
    }

    fun testTapestryAttrValueWithPropPrefix() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(PsiMethod::class.java)
        Assert.assertEquals("getHours", ref.name)
    }

    fun testTapestryAttrValueReferencingToField() {
        addComponentToProject("Count")
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(PsiField::class.java)
        Assert.assertEquals("intFieldProp", ref.name)
    }

    fun testTelSetterByProperty() {
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(TapestryAccessorMethod::class.java)
        Assert.assertEquals("setSomeProp", ref.name)
        val field = assertInstanceOf(ref.navigationElement, PsiField::class.java)
        Assert.assertEquals("someProp", field.name)
    }

    fun testTelPropertyByGetter() {
        initByComponent()
        val ref = resolveReferenceAtCaretPosition(PsiMethod::class.java)
        Assert.assertEquals("getCurrentTime", ref.name)
    }

    fun testCssClass() {
        myFixture.copyFileToProject("CssClass.css", COMPONENTS_PACKAGE_PATH + "CssClass.css")
        initByComponent()
        val cssClass = resolveReferenceAtCaretPosition(CssClass::class.java)
        Assert.assertEquals("cssClassTapestry", cssClass.name)
        Assert.assertEquals("CssClass.css", cssClass.containingFile.name)
    }

    private fun checkReferenceAtCaretPositionUnresolved() {
        val ref = getReferenceAtCaretPosition()
        Assert.assertNotNull(ref)
        val element = ref!!.resolve()
        Assert.assertNull(element.toString(), element)
    }

    private fun <T> resolveReferenceAtCaretPosition(aClass: Class<T>): T {
        return assertInstanceOf(resolveReferenceAtCaretPosition(), aClass)
    }
}
