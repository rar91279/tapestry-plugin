package com.github.rar91279.plugin.tapestry.tests

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import org.junit.Assert

/**
 * `TapestryProject.libraries` is on the hot path: the documentation tab, the dependencies tab and the
 * project-view pane all reach it, concurrently, from background read actions. It must be cached (each
 * miss costs two stub-index queries via `findLibraryMapping`) and it must publish that cache as one
 * consistent snapshot.
 *
 * Uses the JUnit 3 base rather than a kotest spec because the Tapestry facet fixture — an application
 * package with no filter name, which is what exposes the caching bug — only exists here.
 */
class TapestryProjectLibrariesTest : TapestryBaseTestCase() {

    override fun getBasePath(): String = "events/"

    private val tapestryProject: TapestryProject
        get() = TapestryModuleSupportLoader.getTapestryProject(myModule)!!

    /**
     * The facet sets an application package but no filter name. The old cache guard required *both* to be
     * non-empty before it would consider its cached value, so a null filter name meant every single access
     * rebuilt the library list and re-ran both stub-index queries.
     */
    fun testLibrariesAreCachedWhenTheModelHasNotChanged() {
        val project = tapestryProject

        val first = project.libraries
        val second = project.libraries

        Assert.assertSame("libraries must be served from cache when nothing changed", first, second)
    }

    /** A cache hit must not change what is returned. */
    fun testCachedLibrariesHaveTheSameContentAsAFreshComputation() {
        val project = tapestryProject

        val first = project.libraries.map { it.basePackage }
        val second = project.libraries.map { it.basePackage }

        Assert.assertEquals(first, second)
        Assert.assertTrue("the application library should be present", first.isNotEmpty())
    }
}
