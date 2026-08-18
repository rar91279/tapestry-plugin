package com.github.rar91279.plugin.tapestry.tests

import com.github.rar91279.plugin.tapestry.intellij.view.NodeNavigation
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.ModuleNode
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.TapestryNode
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.TapestryViewOptions
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.ui.treeStructure.SimpleNode
import org.junit.Assert


/**
 * The Tapestry view pane's tree: Tapestry entities only, under the fixed categories of a module.
 *
 * Asserts the node layer rather than the pane, which is why the nodes take their options as a parameter — a
 * `ProjectView` (and so `TapestryProjectViewPane.getInstance`) does not exist in a fixture.
 */
class TapestryProjectViewTest : TapestryBaseTestCase() {

    override fun getBasePath(): String = "highlighting/"

    private fun initApplication() {
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

        // Elements: one page at the root, one in a subpackage, a component and a mixin.
        addElement(PAGES_PACKAGE_PATH, "com.testapp.pages", "Index")
        addElement(PAGES_PACKAGE_PATH + "admin/", "com.testapp.pages.admin", "Login")
        addElement(COMPONENTS_PACKAGE_PATH, "com.testapp.components", "Layout")
        addElement(MIXINS_PACKAGE_PATH, "com.testapp.mixins", "Autocomplete")

        // A plain class in the pages package: not a Tapestry element, so it must not show up anywhere.
        myFixture.addFileToProject(
            PAGES_PACKAGE_PATH + "PageHelper.java",
            "package com.testapp.pages; public class PageHelper { private PageHelper() {} }"
        )

        // Assets on the roots, one of them a JavaScript module.
        myFixture.addFileToProject("META-INF/assets/css/site.css", "body { margin: 0 }")
        myFixture.addFileToProject("META-INF/assets/js/site.js", "console.log('site')")
        myFixture.addFileToProject("META-INF/modules/BB/widget.js", "define([], function() {})")

        // A stack, contributed by the application's own module class.
        myFixture.addFileToProject(
            "com/testapp/stacks/WidgetStack.java",
            """
            package com.testapp.stacks;

            import org.apache.tapestry5.services.javascript.JavaScriptStack;

            public class WidgetStack implements JavaScriptStack {
                private static final String[] CSS = { "css/site.css" };
                private static final String[] JS = { "js/site.js" };
                private static final String[] MODULES = { "BB/widget" };
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/testapp/services/AppModule.java",
            """
            package com.testapp.services;

            import com.testapp.stacks.WidgetStack;
            import org.apache.tapestry5.ioc.MappedConfiguration;
            import org.apache.tapestry5.services.javascript.JavaScriptStack;

            public class AppModule {
                public static Runnable buildBackgroundJob() { return null; }

                public static void contributeJavaScriptStackSource(
                        MappedConfiguration<String, JavaScriptStack> configuration) {
                    configuration.addInstance("widgets", WidgetStack.class);
                }
            }
            """.trimIndent()
        )
    }

    private fun addElement(path: String, packageName: String, name: String) {
        myFixture.addFileToProject("$path$name.java", "package $packageName; public class $name {}")
        myFixture.addFileToProject("$path$name.tml", "<html><body>$name</body></html>")
    }

    /**
     * The tree under a module node, as indented lines, so one assertion covers the whole shape.
     *
     * Reads the node's own text rather than `SimpleNode.presentation`, which the platform only fills in during
     * its update cycle — outside a running tree it is empty.
     */
    private fun tree(node: SimpleNode, depth: Int = 0): List<String> =
        listOf("  ".repeat(depth) + (node as? TapestryNode)?.getPresentableText()) +
                node.children.flatMap { tree(it, depth + 1) }

    fun testTreeShowsTapestryEntitiesOnly() {
        initApplication()

        val module = myModule!!
        val lines = tree(ModuleNode(module, options(showLibraries = false)))

         Assert.assertEquals(
            listOf(
                module.name,
                "  Services",
                "    BackgroundJob",
                "  Pages",
                "    admin",
                "      Login",
                "    Index",
                "  Components",
                "    Layout",
                "  Mixins",
                "    Autocomplete",
                "  Assets",
                "    StyleSheets",
                "      css",
                "        site.css",
                "    Javascripts",
                "      js",
                "        site.js",
                "    Modules",
                "      BB",
                "        widget.js",
                "    JavaScriptStacks",
                "      widgets",
                "        Css",
                "          site.css",
                "        JS",
                "          site.js",
                "        Modules",
                "          widget.js"
            ),
            // Note what is absent: `PageHelper`, the plain class sitting in the pages package, and every
            // directory that holds no element.
            lines
        )
    }

    /**
     * Navigation comes from the list elements only: every leaf opens something, and nothing that expands does.
     *
     * Both halves matter. A leaf that opens nothing is dead on click — one new node kind whose value is neither
     * an element nor PSI is enough to reintroduce that. A container that opens something is the opposite
     * surprise: clicking a category or a folder would open a file instead of expanding.
     */
    fun testNavigationComesFromLeavesOnly() {
        initApplication()

        val root = ModuleNode(myModule!!, options(showLibraries = false, showElementFiles = true))

        val deadLeaves = nodes(root).filter { it.children.isEmpty() }
            .filter { NodeNavigation.navigatableOf(it) == null }
            .map { it.getPresentableText() }

        val navigableContainers = nodes(root).filter { it.children.isNotEmpty() }
            .filter { NodeNavigation.navigatableOf(it) != null }
            .map { it.getPresentableText() }

        Assert.assertEquals("leaves that open nothing", emptyList<String>(), deadLeaves)
        Assert.assertEquals("containers that open something", emptyList<String>(), navigableContainers)

        // Nor through the PSI element: the platform derives a Navigatable from that key, and a directory
        // navigates to its package in the Project view — clicking a folder must not leave this view.
        val directoryNodes = nodes(root)
            .filter { it.getValue() is PsiDirectory }
            .filter { NodeNavigation.psiElementOf(it.getValue()) != null }
            .map { it.getPresentableText() }

        Assert.assertEquals("folders offered as a PSI element", emptyList<String>(), directoryNodes)
    }

    /**
     * A module that only provides IoC services shows only Services.
     *
     * No empty Pages, Components, Mixins or Assets: *New > Page/Component/Mixin* is enabled on the module node,
     * so nothing is lost by leaving out a category with nothing in it.
     */
    fun testEmptyCategoriesAreLeftOut() {
        myFixture.addFileToProject(
            "com/testapp/services/AppModule.java",
            """
            package com.testapp.services;

            public class AppModule {
                public static Runnable buildBackgroundJob() { return null; }
            }
            """.trimIndent()
        )

        val lines = tree(ModuleNode(myModule!!, options(showLibraries = false)))

        Assert.assertEquals(listOf(myModule!!.name, "  Services", "    BackgroundJob"), lines)
    }

    /** With the element files hidden, an element is a leaf — and still opens its class. */
    fun testElementOpensItsClassWhenFilesAreHidden() {
        initApplication()

        val index = nodes(ModuleNode(myModule!!, options(showLibraries = false)))
            .single { it.getPresentableText() == "Index" }

        Assert.assertEquals("Index.java", NodeNavigation.navigatableOf(index)?.let { (it as PsiFile).name })
    }

    /**
     * A double-click on anything with children toggles its subtree.
     *
     * The platform asks the node this only once it knows the node is not a leaf, so a container answering
     * "navigate" is answering for a click that has nowhere to go: the edit-source handler consumes it and
     * nothing happens at all, leaving the row expander as the only way to open Services, Pages or Components.
     */
    fun testContainersExpandOnDoubleClick() {
        initApplication()

        val root = ModuleNode(myModule!!, options(showLibraries = false, showElementFiles = true))

        val stuck = nodes(root).filter { it.children.isNotEmpty() }
            .filterNot { it.expandOnDoubleClick() }
            .map { it.getPresentableText() }

        Assert.assertEquals("containers that do not expand on double-click", emptyList<String>(), stuck)
    }

    private fun nodes(node: SimpleNode): List<TapestryNode> =
        listOfNotNull(node as? TapestryNode) + node.children.flatMap { nodes(it) }

    /** With element files on, an element expands to the files it is made of. */
    fun testElementFilesAreShownWhenToggledOn() {
        initApplication()

        val lines = tree(ModuleNode(myModule!!, options(showLibraries = false, showElementFiles = true)))

        Assert.assertTrue(
            "expected Index.tml under the page, got: $lines",
            lines.any { it.trimStart() == "Index.tml" }
        )
        Assert.assertTrue(
            "expected the page class under the page, got: $lines",
            lines.any { it.trimStart() == "Index.java" }
        )
    }

    private fun options(showLibraries: Boolean, showElementFiles: Boolean = false) =
        object : TapestryViewOptions {
            override val showLibraries = showLibraries
            override val showElementFiles = showElementFiles
        }
}
