package com.github.rar91279.plugin.tapestry.tests.core.java

import com.intellij.openapi.application.readActionBlocking
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaTypeFinder
import com.github.rar91279.plugin.tapestry.tests.core.JavaModuleFixtureSpec
import io.kotest.matchers.shouldBe

/**
 * Test suite for [IntellijJavaTypeFinder] functionality.
 *
 * This test class verifies the behavior of the IntellijJavaTypeFinder in locating Java types
 * within the module and its dependencies. The tests cover three main operations:
 * - Finding a single type by fully qualified name
 * - Finding all types in a specific package (non-recursive)
 * - Finding all types in a package hierarchy (recursive)
 *
 * Each operation is tested in two scenarios:
 * - Without dependencies: Only types from the module itself are considered
 * - With dependencies: Types from both the module and its dependencies are considered
 *
 * The test uses a fixture module containing test classes in the "com.app.util" package
 * and dependency classes in the "com.app.dep" package.
 */
class IntellijJavaTypeFinderTest : JavaModuleFixtureSpec({

    /**
     * Creates an instance of IntellijJavaTypeFinder for the test module.
     *
     * @return a new IntellijJavaTypeFinder instance configured for the fixture module
     */
    fun finder() = IntellijJavaTypeFinder(module)

    /**
     * Verifies that findType() correctly finds types in the module but ignores dependencies
     * when the includeDependencies parameter is false.
     *
     * Expected behavior:
     * - Successfully finds "com.app.util.Class1" from the module
     * - Returns null for "com.app.dep.Dep1" which exists only in dependencies
     */
    "findType_no_dependencies" {
        readActionBlocking {
            finder().findType("com.app.util.Class1", false)!!.fullyQualifiedName shouldBe "com.app.util.Class1"
            finder().findType("com.app.dep.Dep1", false) shouldBe null
        }
    }

    /**
     * Verifies that findType() finds types from both the module and its dependencies
     * when the includeDependencies parameter is true.
     *
     * Expected behavior:
     * - Successfully finds "com.app.util.Class1" from the module
     * - Successfully finds "com.app.dep.Dep1" from dependencies
     */
    "findType_with_dependencies" {
        readActionBlocking {
            finder().findType("com.app.util.Class1", true)!!.fullyQualifiedName shouldBe "com.app.util.Class1"
            finder().findType("com.app.dep.Dep1", true)!!.fullyQualifiedName shouldBe "com.app.dep.Dep1"
        }
    }

    /**
     * Verifies that findTypesInPackage() returns only types from the module's package
     * when the includeDependencies parameter is false.
     *
     * Expected behavior:
     * - Finds 6 types in "com.app.util" package from the module
     * - Finds 0 types in "com.app.dep" package (dependency package is ignored)
     */
    "findTypesInPackage_no_dependencies" {
        readActionBlocking {
            finder().findTypesInPackage("com.app.util", false).size shouldBe 6
            finder().findTypesInPackage("com.app.dep", false).size shouldBe 0
        }
    }

    /**
     * Verifies that findTypesInPackage() returns types from both the module and dependencies
     * when the includeDependencies parameter is true.
     *
     * Expected behavior:
     * - Finds 6 types in "com.app.util" package from the module
     * - Finds 2 types in "com.app.dep" package from dependencies
     */
    "findTypesInPackage_with_dependencies" {
        readActionBlocking {
            finder().findTypesInPackage("com.app.util", true).size shouldBe 6
            finder().findTypesInPackage("com.app.dep", true).size shouldBe 2
        }
    }

    /**
     * Verifies that findTypesInPackageRecursively() searches the package hierarchy
     * but only within the module when the includeDependencies parameter is false.
     *
     * Expected behavior:
     * - Finds 7 types in "com.app.util" and its sub-packages from the module
     * - Finds 0 types in "com.app.dep" hierarchy (dependency packages are ignored)
     */
    "findTypesInPackageRecursively_no_dependencies" {
        readActionBlocking {
            finder().findTypesInPackageRecursively("com.app.util", false).size shouldBe 7
            finder().findTypesInPackageRecursively("com.app.dep", false).size shouldBe 0
        }
    }

    /**
     * Verifies that findTypesInPackageRecursively() searches the package hierarchy
     * across both the module and its dependencies when the includeDependencies parameter is true.
     *
     * Expected behavior:
     * - Finds 7 types in "com.app.util" and its sub-packages from the module
     * - Finds 3 types in "com.app.dep" hierarchy from dependencies
     */
    "findTypesInPackageRecursively_with_dependencies" {
        readActionBlocking {
            finder().findTypesInPackageRecursively("com.app.util", true).size shouldBe 7
            finder().findTypesInPackageRecursively("com.app.dep", true).size shouldBe 3
        }
    }
})
