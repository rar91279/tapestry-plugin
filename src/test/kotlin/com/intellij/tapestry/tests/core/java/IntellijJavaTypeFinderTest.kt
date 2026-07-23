package com.intellij.tapestry.tests.core.java

import com.intellij.openapi.application.runReadAction
import com.intellij.tapestry.intellij.core.java.IntellijJavaTypeFinder
import com.intellij.tapestry.tests.core.JavaModuleFixtureSpec
import io.kotest.matchers.shouldBe

class IntellijJavaTypeFinderTest : JavaModuleFixtureSpec({

    fun finder() = IntellijJavaTypeFinder(module)

    "findType_no_dependencies" {
        runReadAction {
            finder().findType("com.app.util.Class1", false)!!.fullyQualifiedName shouldBe "com.app.util.Class1"
            finder().findType("com.app.dep.Dep1", false) shouldBe null
        }
    }

    "findType_with_dependencies" {
        runReadAction {
            finder().findType("com.app.util.Class1", true)!!.fullyQualifiedName shouldBe "com.app.util.Class1"
            finder().findType("com.app.dep.Dep1", true)!!.fullyQualifiedName shouldBe "com.app.dep.Dep1"
        }
    }

    "findTypesInPackage_no_dependencies" {
        runReadAction {
            finder().findTypesInPackage("com.app.util", false).size shouldBe 6
            finder().findTypesInPackage("com.app.dep", false).size shouldBe 0
        }
    }

    "findTypesInPackage_with_dependencies" {
        runReadAction {
            finder().findTypesInPackage("com.app.util", true).size shouldBe 6
            finder().findTypesInPackage("com.app.dep", true).size shouldBe 2
        }
    }

    "findTypesInPackageRecursively_no_dependencies" {
        runReadAction {
            finder().findTypesInPackageRecursively("com.app.util", false).size shouldBe 7
            finder().findTypesInPackageRecursively("com.app.dep", false).size shouldBe 0
        }
    }

    "findTypesInPackageRecursively_with_dependencies" {
        runReadAction {
            finder().findTypesInPackageRecursively("com.app.util", true).size shouldBe 7
            finder().findTypesInPackageRecursively("com.app.dep", true).size shouldBe 3
        }
    }
})
