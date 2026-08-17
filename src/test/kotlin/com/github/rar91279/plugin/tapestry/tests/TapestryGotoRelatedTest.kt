package com.github.rar91279.plugin.tapestry.tests

import com.github.rar91279.plugin.tapestry.intellij.TapestryGotoRelatedProvider
import com.intellij.navigation.GotoRelatedItem
import com.intellij.navigation.GotoRelatedProvider
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import org.junit.Assert

/**
 * *Navigate | Related Symbol* over a page bundle: class, template, message catalog and imported assets.
 *
 * Goes through the extension point rather than the provider class, so a missing `plugin.xml` registration
 * fails here too.
 */
class TapestryGotoRelatedTest : TapestryBaseTestCase() {

    override fun getBasePath(): String = "highlighting/"

    private val GOTO_RELATED_PROVIDER_EP =
        ExtensionPointName<GotoRelatedProvider>("com.intellij.gotoRelatedProvider")

    private fun initPageBundle() {
        // Tapestry 5.1 (the version the test libraries pin) has no @Import yet.
        myFixture.addFileToProject(
            "org/apache/tapestry5/annotations/Import.java",
            """
            package org.apache.tapestry5.annotations;

            public @interface Import {
                String[] library() default {};
                String[] stylesheet() default {};
            }
            """.trimIndent()
        )

        // Two layouts at once: next to the class, and under the 5.4 asset root.
        myFixture.addFileToProject(PAGES_PACKAGE_PATH + "css/start.css", "body { margin: 0 }")
        myFixture.addFileToProject("META-INF/assets/css/pages/theme.css", "body { color: red }")
        myFixture.addFileToProject(PAGES_PACKAGE_PATH + "js/start.js", "console.log('start')")
        myFixture.addFileToProject(PAGES_PACKAGE_PATH + "images/logo.txt", "logo")
        myFixture.addFileToProject(PAGES_PACKAGE_PATH + "Start.properties", "greeting=hello")
        myFixture.addFileToProject(PAGES_PACKAGE_PATH + "Start.tml", "<html><body>Start</body></html>")
        myFixture.addFileToProject(
            PAGES_PACKAGE_PATH + "Start.java",
            """
            package com.testapp.pages;

            import org.apache.tapestry5.Asset;
            import org.apache.tapestry5.annotations.Import;
            import org.apache.tapestry5.annotations.Path;
            import org.apache.tapestry5.ioc.annotations.Inject;

            @Import(
                stylesheet = {"css/start.css", "css/pages/theme.css"},
                library = {"js/start.js", "webjars:jquery:jquery.js"}
            )
            public class Start {
                @Inject @Path("images/logo.txt") private Asset logo;

                public Asset getLogo() { return logo; }
            }
            """.trimIndent()
        )
    }

    /**
     * A page importing a stack, the module class contributing it, and the two stack classes — the outer one
     * including the inner one, whose assets load with it.
     */
    private fun initStackBundle() {
        myFixture.addFileToProject(
            "org/apache/tapestry5/annotations/Import.java",
            """
            package org.apache.tapestry5.annotations;

            public @interface Import {
                String[] library() default {};
                String[] stylesheet() default {};
                String[] stack() default {};
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "org/apache/tapestry5/services/javascript/JavaScriptStack.java",
            "package org.apache.tapestry5.services.javascript; public interface JavaScriptStack {}"
        )
        myFixture.addFileToProject(
            "org/apache/tapestry5/ioc/MappedConfiguration.java",
            """
            package org.apache.tapestry5.ioc;

            public interface MappedConfiguration<K, V> {
                void addInstance(K key, Class<? extends V> clazz);
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("META-INF/assets/css/scheduler.css", "body { margin: 0 }")
        myFixture.addFileToProject("META-INF/assets/js/scheduler.js", "console.log('scheduler')")
        myFixture.addFileToProject("META-INF/assets/js/leaflet.js", "console.log('leaflet')")
        // A JavaScript module is named, not pathed: it lives under the module root.
        myFixture.addFileToProject("META-INF/modules/BB/Scheduler/toolbar.js", "define([], function() {})")

        // A base class holding the arrays, as real stacks are written: the concrete stacks name their
        // libraries, stylesheets and dependencies in constants and hand them to `super(…)`, so no method of
        // the stack class itself mentions any of them.
        myFixture.addFileToProject(
            "com/testapp/stacks/AbstractStack.java",
            """
            package com.testapp.stacks;

            import java.util.Arrays;
            import java.util.List;
            import org.apache.tapestry5.services.javascript.JavaScriptStack;

            public abstract class AbstractStack implements JavaScriptStack {
                private final String[] jsLibs;
                private final String[] cssStylesheets;
                private final String[] stacks;
                private final String[] modules;

                protected AbstractStack(String[] jsLibs, String[] cssStylesheets, String[] stacks,
                        String[] modules) {
                    this.jsLibs = jsLibs;
                    this.cssStylesheets = cssStylesheets;
                    this.stacks = stacks == null ? new String[0] : stacks;
                    this.modules = modules == null ? new String[0] : modules;
                }

                public List<String> getStacks() { return Arrays.asList(stacks); }
                public List<String> getStylesheets() { return Arrays.asList(cssStylesheets); }
                public List<String> getLibraries() { return Arrays.asList(jsLibs); }
                public List<String> getModules() { return Arrays.asList(modules); }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/testapp/stacks/SchedulerStack.java",
            """
            package com.testapp.stacks;

            public class SchedulerStack extends AbstractStack {

                private static final String[] JS_LIBS = { "js/scheduler.js" };

                private static final String[] CSS_STYLES = {
                        "webjars:dhtmlx-scheduler:dhtmlxscheduler.css",
                        "css/scheduler.css"
                };

                private static final String[] STACKS = { "leaflet" };

                private static final String[] MODULES = { "BB/Scheduler/toolbar" };

                public SchedulerStack() {
                    super(JS_LIBS, CSS_STYLES, STACKS, MODULES);
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/testapp/stacks/LeafletStack.java",
            """
            package com.testapp.stacks;

            public class LeafletStack extends AbstractStack {
                private static final String[] JS_LIBS = { "js/leaflet.js" };

                public LeafletStack() {
                    super(JS_LIBS, null, null, null);
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/testapp/services/AppModule.java",
            """
            package com.testapp.services;

            import com.testapp.stacks.LeafletStack;
            import com.testapp.stacks.SchedulerStack;
            import org.apache.tapestry5.ioc.MappedConfiguration;
            import org.apache.tapestry5.services.javascript.JavaScriptStack;

            public class AppModule {
                public static void contributeJavaScriptStackSource(
                        MappedConfiguration<String, JavaScriptStack> configuration) {
                    configuration.addInstance("bb-scheduler", SchedulerStack.class);
                    configuration.addInstance("leaflet", LeafletStack.class);
                }
            }
            """.trimIndent()
        )

        myFixture.addFileToProject(PAGES_PACKAGE_PATH + "Board.tml", "<html><body>Board</body></html>")
        myFixture.addFileToProject(
            PAGES_PACKAGE_PATH + "Board.java",
            """
            package com.testapp.pages;

            import org.apache.tapestry5.annotations.Import;

            @Import(stack = "bb-scheduler")
            public class Board {
            }
            """.trimIndent()
        )
    }

    /**
     * The related items the platform offers for the file currently open in the editor, as "group: name", **in
     * the order the popup shows them**: sorted by raw group name, the way `NavigationUtil.collectRelatedItems`
     * does it. The invisible ordering prefix is stripped from the reported group, so the expectations below
     * read as the user sees them and still fail if the order drifts.
     */
    private fun relatedItems(path: String): List<String> {
        val file = myFixture.findFileInTempDir(path)
        myFixture.configureFromExistingVirtualFile(file)

        return GOTO_RELATED_PROVIDER_EP.extensionList
            .flatMap { it.getItems(myFixture.file) }
            .sortedBy { it.group }
            .map { item: GotoRelatedItem ->
                val element = item.element
                val name = item.customName
                    ?: (element as? PsiFile)?.name
                    ?: (element as? PsiNamedElement)?.name
                    ?: element?.text
                "${TapestryGotoRelatedProvider.groupName(item.group)}: $name"
            }
    }

    /** Groups appear in reading order: class, template, messages, stylesheets, javascript, other assets. */
    fun testRelatedItemsFromTemplate() {
        initPageBundle()

        Assert.assertEquals(
            listOf(
                "Class: Start",
                "Messages: Start.properties",
                "Stylesheets: start.css",
                "Stylesheets: theme.css",
                "Javascript: start.js",
                "Assets: logo.txt"
            ),
            relatedItems(PAGES_PACKAGE_PATH + "Start.tml")
        )
    }

    fun testRelatedItemsFromClassAndCatalog() {
        initPageBundle()

        // The class the caret sits in is filtered out; everything else in the bundle is offered.
        Assert.assertEquals(
            listOf(
                "Template: Start.tml",
                "Messages: Start.properties",
                "Stylesheets: start.css",
                "Stylesheets: theme.css",
                "Javascript: start.js",
                "Assets: logo.txt"
            ),
            relatedItems(PAGES_PACKAGE_PATH + "Start.java")
        )

        // And back from the message catalog, which is only linked to its element by name.
        Assert.assertEquals(
            listOf(
                "Class: Start",
                "Template: Start.tml",
                "Stylesheets: start.css",
                "Stylesheets: theme.css",
                "Javascript: start.js",
                "Assets: logo.txt"
            ),
            relatedItems(PAGES_PACKAGE_PATH + "Start.properties")
        )
    }

    /**
     * `@Import(stack = …)`: the stack class the contribution names, its files, and those of the stack it
     * includes. Stacks come last, after the element's own files.
     */
    fun testRelatedItemsFromStackImport() {
        initStackBundle()

        Assert.assertEquals(
            listOf(
                "Template: Board.tml",
                "Stack: bb-scheduler: SchedulerStack",
                // Within a stack, the files keep the order the class declares them — the order the stack
                // loads them in.
                "Stack: bb-scheduler: scheduler.js",
                "Stack: bb-scheduler: scheduler.css",
                // From MODULES, resolved under META-INF/modules.
                "Stack: bb-scheduler: toolbar.js",
                // CSS_STYLES also holds a `webjars:` entry, which is left out: it is a file inside a jar.
                "Stack: leaflet: LeafletStack",
                "Stack: leaflet: leaflet.js"
            ),
            relatedItems(PAGES_PACKAGE_PATH + "Board.java")
        )
    }
}
