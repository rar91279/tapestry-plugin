package com.github.rar91279.plugin.tapestry.tests

import com.intellij.codeInspection.deadCode.UnusedDeclarationInspection
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import junit.framework.Assert

/**
 * A `@Property` field referenced from the template: what the IDE has to know about it.
 *
 * The template reference resolves to the field, so everything that counts references — Find Usages, the usage
 * inlay, *Show Usages* on the declaration — has to see the template too, and the field must not be reported as
 * unused.
 */
class TapestryPropertyUsagesTest : TapestryBaseTestCase() {

    override fun getBasePath(): String = "highlighting/"

    private fun initPage() {
        myFixture.addFileToProject(
            "org/apache/tapestry5/annotations/Property.java",
            "package org.apache.tapestry5.annotations; public @interface Property {}"
        )
        myFixture.addFileToProject(
            PAGES_PACKAGE_PATH + "Start.tml",
            """
            <html xmlns:t="http://tapestry.apache.org/schema/tapestry_5_4.xsd">
                <body class="${'$'}{greeting}">${'$'}{greeting}</body>
            </html>
            """.trimIndent()
        )
        myFixture.addFileToProject(
            PAGES_PACKAGE_PATH + "Start.java",
            """
            package com.testapp.pages;

            import org.apache.tapestry5.annotations.Property;

            public class Start {
                @Property private String greeting;
            }
            """.trimIndent()
        )
    }

    private fun propertyField(): PsiField {
        val file = myFixture.findFileInTempDir(PAGES_PACKAGE_PATH + "Start.java")
        myFixture.configureFromExistingVirtualFile(file)

        val psiClass = JavaPsiFacade.getInstance(myFixture.project)
            .findClass("com.testapp.pages.Start", GlobalSearchScope.allScope(myFixture.project))

        return psiClass!!.fields.single()
    }

    /** Find Usages, the usage inlay and *Show Usages* all come from this search. */
    fun testFieldReferenceSearchFindsTheTemplateUsage() {
        initPage()

        val references: Collection<PsiReference> =
            ReferencesSearch.search(propertyField(), GlobalSearchScope.projectScope(myFixture.project), false).findAll()

        // Both usages: the one in an attribute value and the one in the element body.
        Assert.assertEquals(
            "template references to the field, got ${references.map { it.element.containingFile.name }}",
            2,
            references.size
        )
    }

    /** The framework assigns and reads the field, so neither half of the unused report applies. */
    fun testPropertyFieldIsNotReportedUnused() {
        initPage()

        myFixture.enableInspections(UnusedDeclarationInspection())
        myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir(PAGES_PACKAGE_PATH + "Start.java"))

        val complaints = myFixture.doHighlighting().mapNotNull { it.description }
            .filter { "never" in it || "unused" in it }

        Assert.assertEquals(emptyList<String>(), complaints)
    }
}
