package com.github.rar91279.plugin.tapestry.tests

import com.intellij.codeInsight.hint.api.impls.XmlParameterInfoHandler
import com.intellij.javaee.ExternalResourceManagerEx
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import com.intellij.testFramework.utils.parameterInfo.MockCreateParameterInfoContext
import com.intellij.testFramework.utils.parameterInfo.MockParameterInfoUIContext
import com.intellij.xml.XmlElementDescriptor
import com.intellij.xml.util.XmlUtil
import junit.framework.Assert

/**
 * @author Alexey.Chmutov
 */
class TapestryParamInfoTest : TapestryBaseTestCase() {
    fun testTmlTagAttrs() {
        addComponentToProject("Count")
        doTest()
    }

    fun testHtmlTagAttrs() {
        val manager = ExternalResourceManagerEx.getInstanceEx()
        val doctype = manager.getDefaultHtmlDoctype(myFixture.project)
        manager.setDefaultHtmlDoctype(XmlUtil.XHTML_URI, myFixture.project)
        try {
            addComponentToProject("Count")
            doTest()
        } finally {
            manager.setDefaultHtmlDoctype(doctype, myFixture.project)
        }
    }

    private fun doTest() {
        initByComponent()

        val handler = XmlParameterInfoHandler()
        val createContext = MockCreateParameterInfoContext(myFixture.editor, myFixture.file)
        val tag = handler.findElementForParameterInfo(createContext)
        Assert.assertNotNull(tag)
        handler.showParameterInfo(tag!!, createContext)
        val items = createContext.itemsToShow
        Assert.assertNotNull(items)
        Assert.assertTrue(items!!.isNotEmpty())
        val descriptor = items[0] as XmlElementDescriptor
        val context = MockParameterInfoUIContext<PsiElement>(tag)
        handler.updateUI(descriptor, context)
    }

    override fun getBasePath(): String = "parameterInfo/"
}
