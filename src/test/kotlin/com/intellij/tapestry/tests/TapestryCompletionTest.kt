/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.tapestry.tests

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.javaee.ExternalResourceManagerEx
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.ArrayUtil.mergeArrays
import com.intellij.xml.util.XmlUtil
import junit.framework.Assert

/**
 * @author Alexey Chmutov
 */
open class TapestryCompletionTest : TapestryBaseTestCase() {

    fun testTagNameInHtmlParent() {
        initByComponent()
        doTestBasicCompletionVariants(*mergeArrays(CORE_5_1_0_5_TAG_NAMES, "head", "body", getElementTagName()))
    }

    fun testTagNameInTmlParent() {
        CamelHumpMatcher.forceStartMatching(myFixture.testRootDisposable)
        initByComponent()
        addComponentToProject("subpackage.Count")
        doTestBasicCompletionVariants(
            *mergeArrays(
                CORE_5_1_0_5_TAG_NAMES, "base", "link", "meta", "noscript", "p:clientId",
                "p:element", "p:mixins", "rdf:RDF", "script", "style", "title", "template", "t:subpackage.count",
                getElementTagName()
            )
        )
    }

    fun testAttrNameInHtmlParent() {
        CamelHumpMatcher.forceStartMatching(myFixture.testRootDisposable)
        val manager = ExternalResourceManagerEx.getInstanceEx()
        val doctype = manager.getDefaultHtmlDoctype(myFixture.project)
        manager.setDefaultHtmlDoctype(XmlUtil.XHTML_URI, myFixture.project)
        try {
            initByComponent()
            doTestBasicCompletionVariants(
                "accesskey", "charset", "class", "coords", "dir", "href", "hreflang", "id", "lang", "name", "onblur",
                "onclick", "ondblclick", "onfocus", "onkeydown", "onkeypress", "onkeyup", "onmousedown", "onmousemove",
                "onmouseout", "onmouseover", "onmouseup", "rel", "rev", "shape", "style", "tabindex", "target", "title",
                "type", "t:type", "t:id"
            )
        } finally {
            manager.setDefaultHtmlDoctype(doctype, myFixture.project)
        }
    }

    fun testAttrNameInHtmlParent1() {
        CamelHumpMatcher.forceStartMatching(myFixture.testRootDisposable)
        initByComponent()
        doTestBasicCompletionVariants("t:id", "t:type", "tabindex", "target", "title", "translate", "type", "typeof")
    }

    fun testAttrNameInTmlParent() {
        val manager = ExternalResourceManagerEx.getInstanceEx()
        val doctype = manager.getDefaultHtmlDoctype(myFixture.project)
        manager.setDefaultHtmlDoctype(XmlUtil.XHTML_URI, myFixture.project)
        try {
            initByComponent()
            addComponentToProject("Count")
            doTestBasicCompletionVariants(
                "class", "dir", "end", "id", "lang", "mixins", "onclick", "ondblclick", "onkeydown", "onkeypress",
                "onkeyup",
                "onmousedown", "onmousemove", "onmouseout", "onmouseover", "onmouseup", "start", "style", "title",
                "value", "t:id"
            )
        } finally {
            manager.setDefaultHtmlDoctype(doctype, myFixture.project)
        }
    }

    fun testAttrNameInTmlParent1() {
        CamelHumpMatcher.forceStartMatching(myFixture.testRootDisposable)
        initByComponent()
        addComponentToProject("Count")
        doTestBasicCompletionVariants("class", "content", "contenteditable")
    }

    fun testRootTagName() {
        val manager = ExternalResourceManagerEx.getInstanceEx()
        val doctype = manager.getDefaultHtmlDoctype(myFixture.project)
        manager.setDefaultHtmlDoctype(XmlUtil.XHTML_URI, myFixture.project)
        try {
            initByComponent()
            doTestBasicCompletionVariants(*mergeArrays(HTML_AND_CORE_5_1_0_5_TAG_NAMES_AND_PROLOG, getElementTagName()))
        } finally {
            manager.setDefaultHtmlDoctype(doctype, myFixture.project)
        }
    }

    fun testTagNameWithinTmlRootTag() {
        val manager = ExternalResourceManagerEx.getInstanceEx()
        val doctype = manager.getDefaultHtmlDoctype(myFixture.project)
        manager.setDefaultHtmlDoctype(XmlUtil.XHTML_URI, myFixture.project)
        try {
            initByComponent()
            doTestBasicCompletionVariants(*mergeArrays(HTML_AND_CORE_5_1_0_5_TAG_NAMES, getElementTagName()))
        } finally {
            manager.setDefaultHtmlDoctype(doctype, myFixture.project)
        }
    }

    fun testTagNameWithoutHtmlContext() {
        val manager = ExternalResourceManagerEx.getInstanceEx()
        val doctype = manager.getDefaultHtmlDoctype(myFixture.project)
        manager.setDefaultHtmlDoctype(XmlUtil.XHTML_URI, myFixture.project)
        try {
            initByComponent()
            doTestBasicCompletionVariants(*mergeArrays(HTML_AND_CORE_5_1_0_5_TAG_NAMES, getElementTagName()))
        } finally {
            manager.setDefaultHtmlDoctype(doctype, myFixture.project)
        }
    }

    fun testInvalidTagName() {
        initByComponent()
        UsefulTestCase.assertEmpty(myFixture.complete(CompletionType.BASIC))
        UsefulTestCase.assertEmpty(myFixture.lookupElementStrings.orEmpty())
    }

    fun testTypeAttrValue() {
        initByComponent()
        addComponentToProject("mycomps.Count")
        doTestBasicCompletionVariants(*mergeArrays(CORE_5_1_0_5_ELEMENT_NAMES, getLowerCaseElementName(), "mycomps/count"))
    }

    fun testPageAttrValue() {
        addPageToProject("StartPage")
        initByComponent()
        doTestBasicCompletionVariants(*mergeArrays(CORE_5_1_0_5_PAGE_NAMES, "startpage"))
    }

    open fun testIdAttrValue() {
        addComponentToProject("Count")
        initByComponent()
        doTestBasicCompletionVariants("index", "link2", "link3")
    }

    fun testAttrValueWithPropPrefix() {
        addComponentToProject("Count")
        initByComponent()
        doTestBasicCompletionVariants("prop:strProp.chars", "prop:strProp.bytes", "prop:strProp.empty", "prop:strProp.blank")
    }

    fun testTapestryAttrValue() {
        addComponentToProject("Count")
        initByComponent()
        doTestBasicCompletionVariants("dateProp", "strProp", "intFieldProp")
    }

    fun testTapestryMixinAttr() {
        addComponentToProject("Count")
        addMixinToProject("FooMixin")
        initByComponent()
        doTestBasicCompletionVariants("bar", "end", "mixins", "start", "t:id", "value")
    }

    fun testTapestryMixinTag() {
        addComponentToProject("Count")
        addMixinToProject("FooMixin")
        initByComponent()
        doTestBasicCompletionVariants(true, "p:bar")
    }

    fun testTagNameWithDoctypePresent() {
        initByComponent()
        doTestBasicCompletionVariants(*mergeArrays(CORE_5_1_0_5_TAG_NAMES, "body", "head", getElementTagName()))
    }

    fun testTagNameWithDoctypeAndExplicitHtmlNSPresent() {
        initByComponent()
        doTestBasicCompletionVariants(*mergeArrays(CORE_5_1_0_5_TAG_NAMES, "body", "head", getElementTagName()))
    }

    fun testTelSecondSegmentAfterProp() {
        initByComponent()
        doTestBasicCompletionVariants(
            "after", "before", "class", "clone", "compareTo", "date", "day", "equals", "getClass",
            "getDate", "getDay", "getHours", "getMinutes", "getMonth", "getSeconds", "getTime", "getTimezoneOffset",
            "getYear", "hashCode", "hours", "minutes", "month", "seconds", "setDate", "setHours", "setMinutes",
            "setMonth", "setSeconds", "setTime", "setYear", "time", "timezoneOffset", "toGMTString", "toInstant",
            "toLocaleString", "toString", "year"
        )
    }

    fun testTelFirstSegment() {
        initByComponent()
        doTestBasicCompletionVariants(
            "class", "currentTime", "equals", "getClass", "getCurrentTime", "getSomeProp", "hashCode", "setSomeProp",
            "someProp", "toString"
        )
    }

    fun testTelSetterByProperty() {
        initByComponent()
        myFixture.complete(CompletionType.BASIC)
        checkResultByFile()
    }

    fun testTelPropertyByGetter() {
        CamelHumpMatcher.forceStartMatching(myFixture.testRootDisposable)
        initByComponent()
        myFixture.complete(CompletionType.BASIC)
        checkResultByFile()
    }

    protected fun doTestBasicCompletionVariants(vararg expectedItems: String) {
        doTestBasicCompletionVariants(false, *expectedItems)
    }

    protected fun doTestBasicCompletionVariants(contains: Boolean, vararg expectedItems: String) {
        doTestCompletionVariants(CompletionType.BASIC, contains, *expectedItems)
    }

    protected fun doTestCompletionVariants(type: CompletionType, contains: Boolean, vararg expectedItems: String) {
        val items = myFixture.complete(type)
        Assert.assertNotNull("No lookup was shown, probably there was only one lookup element that was inserted automatically", items)
        if (!contains) {
            UsefulTestCase.assertSameElements(myFixture.lookupElementStrings.orEmpty(), *expectedItems)
            return
        }
        val elements = HashSet(myFixture.lookupElementStrings)
        for (expectedItem in expectedItems) {
            Assert.assertTrue("$expectedItem not found", elements.contains(expectedItem))
        }
    }

    override fun getBasePath(): String = "completion/"

    companion object {
        val CORE_5_1_0_5_PAGE_NAMES = arrayOf("exceptionreport", "propertydisplayblocks", "propertyeditblocks", "servicestatus")
        val CORE_5_1_0_5_SCHEMA_NAMES = arrayOf("t:content", "t:extend", "t:extension-point", "t:remove", "t:replacement")

        val CORE_5_1_0_5_ELEMENT_NAMES = arrayOf(
            "actionlink", "addrowlink", "ajaxformloop", "any", "beandisplay", "beaneditform", "beaneditor", "block", "body", "checkbox",
            "container", "datefield", "delegate", "errors", "eventlink", "exceptiondisplay", "form", "formfragment", "forminjector", "grid",
            "gridcell", "gridcolumns", "gridpager", "gridrows", "hidden", "if", "label", "linksubmit", "loop", "output", "outputraw", "pagelink",
            "palette", "parameter", "passwordfield", "progressivedisplay", "propertydisplay", "propertyeditor", "radio", "radiogroup",
            "removerowlink", "renderobject", "select", "submit", "submitnotifier", "textarea", "textfield", "textoutput", "unless", "zone"
        )

        val CORE_5_1_0_5_TAG_NAMES: Array<String> =
            Array(CORE_5_1_0_5_ELEMENT_NAMES.size) { "t:" + CORE_5_1_0_5_ELEMENT_NAMES[it] } + CORE_5_1_0_5_SCHEMA_NAMES

        val HTML_TAG_NAMES = arrayOf(
            "a", "abbr", "acronym", "address", "applet", "area", "b", "base", "basefont", "bdo", "big", "blockquote", "body", "br", "button",
            "caption", "center", "cite", "code", "col", "colgroup", "dd", "del", "dfn", "dir", "div", "dl", "dt", "em", "embed", "fieldset",
            "font", "form", "h1", "h2", "h3", "h4", "h5", "h6", "head", "hr", "html", "i", "iframe", "img", "input", "ins", "isindex", "kbd",
            "label", "legend", "li", "link", "map", "menu", "meta", "noframes", "noscript", "object", "ol", "optgroup", "option", "p", "param",
            "pre", "q", "s", "samp", "script", "select", "small", "span", "strike", "strong", "style", "sub", "sup", "table", "tbody", "td",
            "textarea", "tfoot", "th", "thead", "title", "tr", "tt", "u", "ul", "var"
        )

        val HTML_AND_CORE_5_1_0_5_TAG_NAMES = mergeArrays(CORE_5_1_0_5_TAG_NAMES, *HTML_TAG_NAMES)

        val HTML_AND_CORE_5_1_0_5_TAG_NAMES_AND_PROLOG =
            mergeArrays(HTML_AND_CORE_5_1_0_5_TAG_NAMES, "?xml version=\"1.0\" encoding=\"\" ?>")
    }
}
