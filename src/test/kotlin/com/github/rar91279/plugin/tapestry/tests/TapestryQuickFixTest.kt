package com.github.rar91279.plugin.tapestry.tests

import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.intellij.inspections.TelReferencesInspection

/**
 * The fixes offered on an unresolved TEL property reference: a `@Property` field and a getter.
 */
class TapestryQuickFixTest : TapestryBaseTestCase() {

    override fun getBasePath(): String = "quickFix/"

    fun testCreateGetter() {
        val method = applyFix("'getMissingProp'").findMethodsByName("getMissingProp", false).single()

        assertEquals("java.lang.Object", method.returnType!!.canonicalText)
    }

    fun testCreateBooleanGetter() {
        val method = applyFix("'isMissingFlag'").findMethodsByName("isMissingFlag", false).single()

        assertEquals("boolean", method.returnType!!.canonicalText)
    }

    fun testCreatePropertyField() {
        val field = applyFix("@Property field").findFieldByName("missingProp", false)!!

        assertEquals("java.lang.Object", field.type.canonicalText)
        assertTrue(field.hasModifierProperty("private"))
        assertNotNull(field.getAnnotation(TapestryConstants.PROPERTY_ANNOTATION))
    }

    // The JVM actions route lets the Kotlin plugin render the accessor for a Kotlin element class: it offers a
    // Kotlin property instead of a method, and the getter TEL resolves against comes with it.
    fun testCreateGetterKotlin() {
        val method = applyFix("Add 'var'").findMethodsByName("getMissingProp", false).single()

        assertEquals("java.lang.Object", method.returnType!!.canonicalText)
    }

    override fun getComponentClassExtension(): String =
        if (getElementName().endsWith("Kotlin")) Util.DOT_KOTLIN else Util.DOT_JAVA

    /** Applies the single fix whose name contains [fixText] and returns the element class it changed. */
    private fun applyFix(fixText: String): PsiClass {
        TemplateManagerImpl.setTemplateTesting(myFixture.testRootDisposable)
        initByComponent(true)
        myFixture.enableInspections(TelReferencesInspection())

        val fixes = myFixture.getAllQuickFixes()
        // The platform's own "create property" variants generate a JavaBean field+getter+setter, which isn't what
        // a Tapestry template calls a property.
        assertEmpty(fixes.filter { it.text.startsWith("Create property") || it.text.contains("only property") })

        val fix = fixes.firstOrNull { it.text.contains(fixText) }
        assertNotNull("No '$fixText' fix, got: " + fixes.map { it.text }, fix)
        myFixture.launchAction(fix!!)

        // The JVM action leaves a live template on the new member's type; finish it.
        FileEditorManager.getInstance(myFixture.project).selectedTextEditor?.let {
            TemplateManagerImpl.getTemplateState(it)?.gotoEnd(false)
        }

        val elementClass = JavaPsiFacade.getInstance(myFixture.project).findClass(
            "$TEST_APPLICATION_PACKAGE.$COMPONENTS.${getElementName()}", GlobalSearchScope.allScope(myFixture.project)
        )
        assertNotNull(elementClass)

        return elementClass!!
    }
}
