package com.intellij.tapestry.tests.core

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory
import com.intellij.testFramework.runInEdtAndWait
import io.kotest.core.spec.style.FreeSpec
import java.io.File

/**
 * kotest replacement for the old TestNG `BaseTestCase`: boots a real IDE
 * `javaModule` fixture (src/test/javaModule) once per spec. Test bodies that
 * touch PSI must wrap access in `com.intellij.openapi.application.runReadAction`.
 */
abstract class JavaModuleFixtureSpec(body: JavaModuleFixtureSpec.() -> Unit) : FreeSpec() {
    lateinit var fixture: IdeaProjectTestFixture
        private set

    val module get() = fixture.module
    fun javaFacade(): JavaPsiFacade = JavaPsiFacade.getInstance(fixture.project)
    fun allScope(): GlobalSearchScope = GlobalSearchScope.allScope(fixture.project)

    // A JDK is only added when -Djdk.home is set (see beforeSpec). Tests that resolve java.lang.*
    // FQNs must gate on this, else they fail headless. Run from the IDE (or with -Djdk.home) to
    // exercise them. Usage: `"name".config(enabled = jdkAvailable) { ... }`.
    val jdkAvailable: Boolean = System.getProperty("jdk.home") != null

    init {
        beforeSpec {
            val builder = JavaTestFixtureFactory.createFixtureBuilder(javaClass.simpleName)
            val javaBuilder = builder.addModule(JavaModuleFixtureBuilder::class.java)
            javaBuilder.addContentRoot(File("").absoluteFile.toString() + "/src/test/javaModule")
            javaBuilder.addSourceRoot("src")
            // Optional JDK: honoured only if -Djdk.home is passed. The IDE's bundled JBR
            // must NOT be used here — indexing its bundled JS resources trips a JS/Vue
            // plugin init bug in this headless sandbox. Tests needing java.lang.* FQN
            // resolution require a real -Djdk.home (as when run from the IDE).
            System.getProperty("jdk.home")?.let { javaBuilder.addJdk(it) }
            javaBuilder.addLibrary("library1")
            javaBuilder.addLibraryJars("library1", "", File("").absoluteFile.toString() + "/src/test/javaModule/lib/dep1.jar")
            fixture = builder.fixture
            // IntelliJ fixture set-up/tear-down must run on the EDT.
            runInEdtAndWait { fixture.setUp() }
        }
        afterSpec {
            runInEdtAndWait { fixture.tearDown() }
        }
        body()
    }
}

/**
 * kotest replacement for `BaseTestCase`'s empty fixture: boots a bare IDE project
 * (no module) once per spec, for tests that only need the platform running.
 */
abstract class EmptyFixtureSpec(body: EmptyFixtureSpec.() -> Unit) : FreeSpec() {
    lateinit var fixture: IdeaProjectTestFixture
        private set

    init {
        beforeSpec {
            fixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(javaClass.simpleName).fixture
            runInEdtAndWait { fixture.setUp() }
        }
        afterSpec {
            runInEdtAndWait { fixture.tearDown() }
        }
        body()
    }
}
