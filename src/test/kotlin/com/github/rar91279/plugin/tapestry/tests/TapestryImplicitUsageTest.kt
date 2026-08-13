package com.github.rar91279.plugin.tapestry.tests

import com.intellij.codeInspection.deadCode.UnusedDeclarationInspection
import junit.framework.Assert

/** Fields the framework assigns must not be reported as "never assigned". */
class TapestryImplicitUsageTest : TapestryBaseTestCase() {

    override fun getBasePath(): String = "highlighting/"

    fun testInjectedFieldsAreImplicitlyAssigned() {
        myFixture.enableInspections(UnusedDeclarationInspection())
        val file = myFixture.addFileToProject(
            PAGES_PACKAGE_PATH + "Injected.java",
            """
            package com.testapp.pages;

            import org.apache.tapestry5.annotations.InjectPage;
            import org.apache.tapestry5.annotations.Property;
            import org.apache.tapestry5.ioc.annotations.Inject;
            import org.apache.tapestry5.ioc.annotations.InjectService;

            public class Injected {
                @InjectPage private Injected page;
                @InjectService("Foo") private Runnable service;
                @Inject private Runnable injected;
                @Property private String property;

                public Object get() { return new Object[]{page, service, injected, property}; }
            }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(file.virtualFile)

        val complaints = myFixture.doHighlighting().mapNotNull { it.description }.filter { "never assigned" in it }
        Assert.assertEquals(emptyList<String>(), complaints)
    }
}
