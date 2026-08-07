package com.github.rar91279.plugin.tapestry.tests.core.resource

import com.intellij.openapi.application.readActionBlocking
import com.github.rar91279.plugin.tapestry.intellij.core.resource.IntellijResourceFinder
import com.github.rar91279.plugin.tapestry.tests.core.JavaModuleFixtureSpec
import io.kotest.matchers.shouldBe

/**
 * Test suite for [IntellijResourceFinder] functionality.
 *
 * This test class verifies the resource finding capabilities of the IntellijResourceFinder,
 * including classpath resource lookup with and without dependencies, and localized resource
 * resolution. All tests are executed within read actions to ensure thread safety when
 * accessing IntelliJ platform APIs.
 *
 * The tests cover:
 * - Finding resources in the module classpath with and without module dependencies
 * - Handling both absolute and relative resource paths
 * - Localized resource variant resolution
 * - Proper handling of non-existent resources
 */
class IntellijResourceFinderTest : JavaModuleFixtureSpec({

    fun finder() = IntellijResourceFinder(module)

    /**
     * Tests classpath resource finding without including module dependencies.
     *
     * Verifies that:
     * - Resources can be found using both absolute paths (starting with "/") and relative paths
     * - Resources from the current module are found correctly
     * - Resources from dependent modules are NOT found when dependencies are excluded
     * - Non-existent resources return an empty collection
     */
    "findClasspathResource_no_dependencies" {
        readActionBlocking {
            val resourceFinder = finder()

            resourceFinder.findClasspathResource("/com/app/util/Home.tml", false).first().name shouldBe "Home.tml"

            resourceFinder.findClasspathResource("com/app/util/Home.tml", false).first().name shouldBe "Home.tml"

            resourceFinder.findClasspathResource("/com/app/dep/Home.tml", false).size shouldBe 0

            resourceFinder.findClasspathResource("com/app/dep/Home1.tml", false).size shouldBe 0

            resourceFinder.findClasspathResource("com/app/dep1/Home1.tml", false).size shouldBe 0
        }
    }

    /**
     * Tests classpath resource finding with module dependencies included.
     *
     * Verifies that:
     * - Resources can be found using both absolute paths (starting with "/") and relative paths
     * - Resources from the current module are found correctly
     * - Resources from dependent modules ARE found when dependencies are included
     * - Non-existent resources return an empty collection even with dependencies enabled
     */
    "findClasspathResource_with_dependencies" {
        readActionBlocking {
            val resourceFinder = finder()

            resourceFinder.findClasspathResource("/com/app/util/Home.tml", true).first().name shouldBe "Home.tml"

            resourceFinder.findClasspathResource("com/app/util/Home.tml", true).first().name shouldBe "Home.tml"

            resourceFinder.findClasspathResource("/com/app/dep/Home.tml", true).first().name shouldBe "Home.tml"

            resourceFinder.findClasspathResource("com/app/dep/Home.tml", true).first().name shouldBe "Home.tml"

            resourceFinder.findClasspathResource("com/app/dep/Home1.tml", true).size shouldBe 0
        }
    }

    /**
     * Tests localized classpath resource finding without including module dependencies.
     *
     * Verifies that:
     * - All localized variants of a resource are found (e.g., Home.tml, Home_en.tml)
     * - Localized resource lookup works with both absolute and relative paths
     * - Only resources from the current module are included (dependencies excluded)
     * - Non-existent resources return an empty collection
     */
    "findLocalizedClasspathResource_no_dependencies" {
        readActionBlocking {
            val resourceFinder = finder()

            resourceFinder.findLocalizedClasspathResource("/com/app/util/Home.tml", false).size shouldBe 2

            resourceFinder.findLocalizedClasspathResource("com/app/util/Home.tml", false).size shouldBe 2

            resourceFinder.findLocalizedClasspathResource("/com/app/dep/Home.tml", false).size shouldBe 0

            resourceFinder.findLocalizedClasspathResource("com/app/dep/Home1.tml", false).size shouldBe 0

            resourceFinder.findClasspathResource("com/app/dep1/Home1.tml", false).size shouldBe 0
        }
    }

    /**
     * Tests localized classpath resource finding with module dependencies included.
     *
     * Verifies that:
     * - All localized variants of a resource are found across the module and its dependencies
     * - Localized resource lookup works with both absolute and relative paths
     * - Resources from both the current module and dependent modules are included
     * - Non-existent resources return an empty collection even with dependencies enabled
     */
    "findLocalizedClasspathResource_with_dependencies" {
        readActionBlocking {
            val resourceFinder = finder()

            resourceFinder.findLocalizedClasspathResource("/com/app/util/Home.tml", true).size shouldBe 2

            resourceFinder.findLocalizedClasspathResource("com/app/util/Home.tml", true).size shouldBe 2

            resourceFinder.findLocalizedClasspathResource("/com/app/dep/Home.tml", true).size shouldBe 2

            resourceFinder.findLocalizedClasspathResource("com/app/dep/Home.tml", true).size shouldBe 2

            resourceFinder.findLocalizedClasspathResource("com/app/dep/Home1.tml", true).size shouldBe 0
        }
    }
})
