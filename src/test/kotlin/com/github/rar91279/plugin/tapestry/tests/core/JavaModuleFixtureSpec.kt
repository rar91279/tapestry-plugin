package com.github.rar91279.plugin.tapestry.tests.core

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
 * Kotest replacement for the old TestNG `BaseTestCase`: boots a real IntelliJ IDEA
 * test fixture with a Java module (src/test/javaModule) once per spec.
 *
 * This base class provides:
 * - A fully initialized IntelliJ project fixture with a Java module
 * - Access to PSI infrastructure (JavaPsiFacade, search scopes)
 * - A test library (library1 with dep1.jar) pre-configured
 * - Optional JDK support when -Djdk.home system property is set
 *
 * **Important**: Test bodies that access PSI elements must wrap all PSI access
 * in `com.intellij.openapi.application.readActionBlocking` to comply with
 * IntelliJ's threading model and avoid read access exceptions.
 *
 * The fixture is set up once before all tests in the spec and torn down after
 * all tests complete. Setup and teardown operations are executed on the EDT
 * (Event Dispatch Thread) as required by IntelliJ Platform.
 *
 * @param body the DSL block for defining tests within this spec
 */
abstract class JavaModuleFixtureSpec(body: JavaModuleFixtureSpec.() -> Unit) : FreeSpec() {
    /**
     * The IntelliJ project test fixture providing access to the test project and module.
     *
     * This property is initialized during `beforeSpec` and should not be accessed
     * before the spec setup completes. It provides access to the test project,
     * module, and other IDE components needed for testing.
     */
    lateinit var fixture: IdeaProjectTestFixture
        private set

    /**
     * Provides access to the Java module configured in this test fixture.
     *
     * The module contains the test sources from src/test/javaModule and
     * has library1 (with dep1.jar) configured as a dependency.
     */
    val module get() = fixture.module

    /**
     * Returns the JavaPsiFacade instance for the test project.
     *
     * Use this to access PSI-related services such as finding classes by qualified name,
     * resolving references, or working with Java elements. All PSI access must be
     * performed within a read action.
     *
     * @return the JavaPsiFacade instance for this test project
     */
    fun javaFacade(): JavaPsiFacade = JavaPsiFacade.getInstance(fixture.project)

    /**
     * Returns a GlobalSearchScope covering all files in the test project.
     *
     * Use this scope when searching for classes, files, or other elements across
     * the entire test project without restrictions.
     *
     * @return a search scope encompassing all project files
     */
    fun allScope(): GlobalSearchScope = GlobalSearchScope.allScope(fixture.project)

    /**
     * Indicates whether a JDK is available for tests requiring java.lang.* resolution.
     *
     * A JDK is only added when the -Djdk.home system property is set. Tests that need
     * to resolve standard Java library classes (e.g., java.lang.String, java.util.List)
     * must check this flag and conditionally enable themselves.
     *
     * When running from the IDE, the JDK is typically available. In headless CI environments,
     * tests requiring JDK resolution may be skipped unless -Djdk.home is explicitly provided.
     *
     * Usage example:
     * ```kotlin
     * "test requiring JDK".config(enabled = jdkAvailable) {
     *     // test code that resolves java.lang.* classes
     * }
     * ```
     *
     * Note: The IDE's bundled JBR is intentionally not used to avoid triggering
     * JavaScript/Vue plugin initialization issues in the headless test environment.
     */
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
 * Kotest replacement for `BaseTestCase`'s empty fixture: boots a minimal IntelliJ IDEA
 * project (without any modules) once per spec.
 *
 * This base class is designed for tests that only require the IntelliJ Platform
 * to be running but do not need a fully configured module with sources, libraries,
 * or other project structure. It provides a lightweight testing environment suitable
 * for testing platform-level features, services, or utilities.
 *
 * The fixture is set up once before all tests in the spec and torn down after
 * all tests complete. Setup and teardown operations are executed on the EDT
 * (Event Dispatch Thread) as required by IntelliJ Platform.
 *
 * Use this class instead of JavaModuleFixtureSpec when your tests do not require
 * PSI infrastructure, source roots, or module dependencies.
 *
 * @param body the DSL block for defining tests within this spec
 */
abstract class EmptyFixtureSpec(body: EmptyFixtureSpec.() -> Unit) : FreeSpec() {
    /**
     * The minimal IntelliJ project test fixture providing access to the IDE platform.
     *
     * This property is initialized during `beforeSpec` and should not be accessed
     * before the spec setup completes. It provides access to the test project
     * and platform services but does not include any module configuration.
     */
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
