package com.github.rar91279.plugin.tapestry.tests.core.resource

import com.intellij.openapi.application.runReadAction
import com.github.rar91279.plugin.tapestry.intellij.core.resource.IntellijResourceFinder
import com.github.rar91279.plugin.tapestry.tests.core.JavaModuleFixtureSpec
import io.kotest.matchers.shouldBe

class IntellijResourceFinderTest : JavaModuleFixtureSpec({

    fun finder() = IntellijResourceFinder(module)

    "findClasspathResource_no_dependencies" {
        runReadAction {
            val resourceFinder = finder()

            resourceFinder.findClasspathResource("/com/app/util/Home.tml", false).first().name shouldBe "Home.tml"

            resourceFinder.findClasspathResource("com/app/util/Home.tml", false).first().name shouldBe "Home.tml"

            resourceFinder.findClasspathResource("/com/app/dep/Home.tml", false).size shouldBe 0

            resourceFinder.findClasspathResource("com/app/dep/Home1.tml", false).size shouldBe 0

            resourceFinder.findClasspathResource("com/app/dep1/Home1.tml", false).size shouldBe 0
        }
    }

    "findClasspathResource_with_dependencies" {
        runReadAction {
            val resourceFinder = finder()

            resourceFinder.findClasspathResource("/com/app/util/Home.tml", true).first().name shouldBe "Home.tml"

            resourceFinder.findClasspathResource("com/app/util/Home.tml", true).first().name shouldBe "Home.tml"

            resourceFinder.findClasspathResource("/com/app/dep/Home.tml", true).first().name shouldBe "Home.tml"

            resourceFinder.findClasspathResource("com/app/dep/Home.tml", true).first().name shouldBe "Home.tml"

            resourceFinder.findClasspathResource("com/app/dep/Home1.tml", true).size shouldBe 0
        }
    }

    "findLocalizedClasspathResource_no_dependencies" {
        runReadAction {
            val resourceFinder = finder()

            resourceFinder.findLocalizedClasspathResource("/com/app/util/Home.tml", false).size shouldBe 2

            resourceFinder.findLocalizedClasspathResource("com/app/util/Home.tml", false).size shouldBe 2

            resourceFinder.findLocalizedClasspathResource("/com/app/dep/Home.tml", false).size shouldBe 0

            resourceFinder.findLocalizedClasspathResource("com/app/dep/Home1.tml", false).size shouldBe 0

            resourceFinder.findClasspathResource("com/app/dep1/Home1.tml", false).size shouldBe 0
        }
    }

    "findLocalizedClasspathResource_with_dependencies" {
        runReadAction {
            val resourceFinder = finder()

            resourceFinder.findLocalizedClasspathResource("/com/app/util/Home.tml", true).size shouldBe 2

            resourceFinder.findLocalizedClasspathResource("com/app/util/Home.tml", true).size shouldBe 2

            resourceFinder.findLocalizedClasspathResource("/com/app/dep/Home.tml", true).size shouldBe 2

            resourceFinder.findLocalizedClasspathResource("com/app/dep/Home.tml", true).size shouldBe 2

            resourceFinder.findLocalizedClasspathResource("com/app/dep/Home1.tml", true).size shouldBe 0
        }
    }

    //@TODO uncomment when http://www.jetbrains.net/jira/browse/IDEA-17361 is fixed
    /*"findContextResource" {
        runReadAction {
            val resourceFinder = finder()

            resourceFinder.findContextResource("/WEB-INF/web.xml").name shouldBe "web.xml"

            resourceFinder.findContextResource("/web.xml") shouldBe null

            resourceFinder.findContextResource("/Page1.tml").name shouldBe "Page1.tml"
        }
    }

    "findLocalizedContextResource" {
        runReadAction {
            val resourceFinder = finder()

            resourceFinder.findLocalizedContextResource("/Page1.tml").size shouldBe 2

            resourceFinder.findLocalizedContextResource("/folder1/Page2.tml").size shouldBe 2
        }
    }*/
})
