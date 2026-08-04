package com.github.rar91279.plugin.tapestry.tests.core.java

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.search.GlobalSearchScope
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.core.resource.IntellijResource
import com.github.rar91279.plugin.tapestry.intellij.core.resource.IntellijResourceFinder
import com.github.rar91279.plugin.tapestry.tests.core.JavaModuleFixtureSpec
import com.github.rar91279.plugin.tapestry.tests.mocks.PsiFileMock
import com.github.rar91279.plugin.tapestry.tests.mocks.VirtualFileMock
import io.kotest.matchers.shouldBe

class IntellijJavaClassTypeTest : JavaModuleFixtureSpec({

    fun classTypeOf(fqn: String): IntellijJavaClassType {
        val psiClass = javaFacade().findClass(fqn, GlobalSearchScope.moduleRuntimeScope(module, false))!!
        return IntellijJavaClassType(module, psiClass.containingFile)
    }

    "getFullyQualifiedName" {
        runReadAction {
            val intellijJavaClassType = classTypeOf("com.app.util.Class1")
            intellijJavaClassType.fullyQualifiedName shouldBe "com.app.util.Class1"
            intellijJavaClassType.psiClass!!.qualifiedName shouldBe "com.app.util.Class1"
        }
    }

    "getName" {
        runReadAction {
            classTypeOf("com.app.util.Class1").name shouldBe "Class1"
        }
    }

    "getFile" {
        runReadAction {
            classTypeOf("com.app.util.Class1").file!!.name shouldBe "Class1.java"
        }
    }

    "isInterface_true" {
        runReadAction {
            classTypeOf("com.app.util.Interface1").isInterface shouldBe true
        }
    }

    "isInterface_false" {
        runReadAction {
            classTypeOf("com.app.util.Class1").isInterface shouldBe false
        }
    }

    "isPublic_true" {
        runReadAction {
            classTypeOf("com.app.util.Class1").isPublic shouldBe true
        }
    }

    "isPublic_false" {
        runReadAction {
            classTypeOf("com.app.util.Class5").isPublic shouldBe false
        }
    }

    "hasDefaultConstructor_true" {
        runReadAction {
            classTypeOf("com.app.util.Class1").hasDefaultConstructor() shouldBe true
        }
    }

    "hasDefaultConstructor_false" {
        runReadAction {
            classTypeOf("com.app.util.Class2").hasDefaultConstructor() shouldBe false
        }
    }

    "getPublicMethods_no_public_methods" {
        runReadAction {
            classTypeOf("com.app.util.Class3").getPublicMethods(true).size shouldBe 0
        }
    }

    "getPublicMethods_with_public_methods" {
        runReadAction {
            val intellijJavaClassType = classTypeOf("com.app.util.Class1")
            intellijJavaClassType.getPublicMethods(true).size shouldBe 4
            intellijJavaClassType.getPublicMethods(false).size shouldBe 3
        }
    }

    "getAllMethods_no_methods" {
        runReadAction {
            classTypeOf("com.app.util.Class3").getAllMethods(true).size shouldBe 2
        }
    }

    "getAllMethods_with_methods" {
        runReadAction {
            val intellijJavaClassType = classTypeOf("com.app.util.Class1")
            intellijJavaClassType.getAllMethods(true).size shouldBe 6
            intellijJavaClassType.getAllMethods(false).size shouldBe 5
        }
    }

    "findMethods" {
        runReadAction {
            classTypeOf("com.app.util.Class1").findPublicMethods("[a-z]*[0-9]").size shouldBe 3
        }
    }

    "getAnnotations_no_annotations" {
        runReadAction {
            classTypeOf("com.app.util.Class3").annotations.size shouldBe 0

            val resourceFinder = IntellijResourceFinder(module)
            val notJavaClassType = IntellijJavaClassType(
                module,
                (resourceFinder.findClasspathResource("/com/app/util/Home.tml", false).toTypedArray()[0] as IntellijResource).psiFile
            )
            notJavaClassType.annotations.size shouldBe 0
        }
    }

    "getAnnotations_with_annotations" {
        runReadAction {
            classTypeOf("com.app.util.Class1").annotations.size shouldBe 2
        }
    }

    "getFields" {
        runReadAction {
            val intellijJavaClassType = classTypeOf("com.app.util.Class1")
            intellijJavaClassType.getFields(true).size shouldBe 4
            intellijJavaClassType.getFields(false).size shouldBe 3

            val resourceFinder = IntellijResourceFinder(module)
            val notJavaClassType = IntellijJavaClassType(
                module,
                (resourceFinder.findClasspathResource("/com/app/util/Home.tml", false).toTypedArray()[0] as IntellijResource).psiFile
            )
            notJavaClassType.getFields(true).size shouldBe 0
        }
    }

    "getDocumentation_no_documentation" {
        runReadAction {
            classTypeOf("com.app.util.Class1").documentation!!.isEmpty() shouldBe true

            val resourceFinder = IntellijResourceFinder(module)
            val notJavaClassType = IntellijJavaClassType(
                module,
                (resourceFinder.findClasspathResource("/com/app/util/Home.tml", false).toTypedArray()[0] as IntellijResource).psiFile
            )
            notJavaClassType.documentation!!.length shouldBe 0
        }
    }

    "getDocumentation_with_documentation" {
        runReadAction {
            classTypeOf("com.app.util.Class2").documentation shouldBe " class2. docs."
        }
    }

    "getFile_file_doesn_exist" {
        runReadAction {
            val psiFileMock = PsiFileMock().setVirtualFile(VirtualFileMock().setUrl("file:///doesnt.exist"))
            val classType = IntellijJavaClassType(module, psiFileMock)
            classType.file shouldBe null
        }
    }
})
