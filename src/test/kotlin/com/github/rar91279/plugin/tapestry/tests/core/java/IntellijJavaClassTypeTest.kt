package com.github.rar91279.plugin.tapestry.tests.core.java

import com.intellij.psi.search.GlobalSearchScope
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.core.resource.IntellijResource
import com.github.rar91279.plugin.tapestry.intellij.core.resource.IntellijResourceFinder
import com.github.rar91279.plugin.tapestry.tests.core.JavaModuleFixtureSpec
import com.github.rar91279.plugin.tapestry.tests.mocks.PsiFileMock
import com.github.rar91279.plugin.tapestry.tests.mocks.VirtualFileMock
import com.intellij.openapi.application.readActionBlocking
import io.kotest.matchers.shouldBe

/**
 * Test suite for [IntellijJavaClassType] functionality.
 *
 * This test class verifies the correct behavior of IntellijJavaClassType wrapper
 * for IntelliJ PSI Java classes, including property access, method retrieval,
 * annotation handling, and documentation extraction.
 */
class IntellijJavaClassTypeTest : JavaModuleFixtureSpec({

    /**
     * Helper function to create an [IntellijJavaClassType] instance for a given fully qualified class name.
     *
     * @param fqn the fully qualified name of the class to wrap
     * @return an IntellijJavaClassType instance wrapping the found PSI class
     */
    fun classTypeOf(fqn: String): IntellijJavaClassType {
        val psiClass = javaFacade().findClass(fqn, GlobalSearchScope.moduleRuntimeScope(module, false))!!
        return IntellijJavaClassType(module, psiClass.containingFile)
    }

    /**
     * Tests that the fully qualified name is correctly retrieved from a Java class.
     */
    "getFullyQualifiedName" {
        readActionBlocking {
            val intellijJavaClassType = classTypeOf("com.app.util.Class1")
            intellijJavaClassType.fullyQualifiedName shouldBe "com.app.util.Class1"
            intellijJavaClassType.psiClass!!.qualifiedName shouldBe "com.app.util.Class1"
        }
    }

    /**
     * Tests that the simple class name is correctly retrieved from a Java class.
     */
    "getName" {
        readActionBlocking {
            classTypeOf("com.app.util.Class1").name shouldBe "Class1"
        }
    }

    /**
     * Tests that the file containing the Java class is correctly retrieved.
     */
    "getFile" {
        readActionBlocking {
            classTypeOf("com.app.util.Class1").file!!.name shouldBe "Class1.java"
        }
    }

    /**
     * Tests that an interface is correctly identified as an interface.
     */
    "isInterface_true" {
        readActionBlocking {
            classTypeOf("com.app.util.Interface1").isInterface shouldBe true
        }
    }

    /**
     * Tests that a class is correctly identified as not being an interface.
     */
    "isInterface_false" {
        readActionBlocking {
            classTypeOf("com.app.util.Class1").isInterface shouldBe false
        }
    }

    /**
     * Tests that a public class is correctly identified as public.
     */
    "isPublic_true" {
        readActionBlocking {
            classTypeOf("com.app.util.Class1").isPublic shouldBe true
        }
    }

    /**
     * Tests that a non-public class is correctly identified as not public.
     */
    "isPublic_false" {
        readActionBlocking {
            classTypeOf("com.app.util.Class5").isPublic shouldBe false
        }
    }

    /**
     * Tests that a class with a default constructor is correctly identified.
     */
    "hasDefaultConstructor_true" {
        readActionBlocking {
            classTypeOf("com.app.util.Class1").hasDefaultConstructor() shouldBe true
        }
    }

    /**
     * Tests that a class without a default constructor is correctly identified.
     */
    "hasDefaultConstructor_false" {
        readActionBlocking {
            classTypeOf("com.app.util.Class2").hasDefaultConstructor() shouldBe false
        }
    }

    /**
     * Tests that a class with no public methods returns an empty list.
     */
    "getPublicMethods_no_public_methods" {
        readActionBlocking {
            classTypeOf("com.app.util.Class3").getPublicMethods(true).size shouldBe 0
        }
    }

    /**
     * Tests that public methods are correctly retrieved, both including and excluding inherited methods.
     */
    "getPublicMethods_with_public_methods" {
        readActionBlocking {
            val intellijJavaClassType = classTypeOf("com.app.util.Class1")
            intellijJavaClassType.getPublicMethods(true).size shouldBe 4
            intellijJavaClassType.getPublicMethods(false).size shouldBe 3
        }
    }

    /**
     * Tests that all methods (including inherited ones) are retrieved even for classes with no declared methods.
     */
    "getAllMethods_no_methods" {
        readActionBlocking {
            classTypeOf("com.app.util.Class3").getAllMethods(true).size shouldBe 2
        }
    }

    /**
     * Tests that all methods are correctly retrieved, both including and excluding inherited methods.
     */
    "getAllMethods_with_methods" {
        readActionBlocking {
            val intellijJavaClassType = classTypeOf("com.app.util.Class1")
            intellijJavaClassType.getAllMethods(true).size shouldBe 6
            intellijJavaClassType.getAllMethods(false).size shouldBe 5
        }
    }

    /**
     * Tests that public methods can be found using a regular expression pattern.
     */
    "findMethods" {
        readActionBlocking {
            classTypeOf("com.app.util.Class1").findPublicMethods("[a-z]*[0-9]").size shouldBe 3
        }
    }

    /**
     * Tests that classes without annotations return an empty annotations list,
     * including non-Java files.
     */
    "getAnnotations_no_annotations" {
        readActionBlocking {
            classTypeOf("com.app.util.Class3").annotations.size shouldBe 0

            val resourceFinder = IntellijResourceFinder(module)
            val notJavaClassType = IntellijJavaClassType(
                module,
                (resourceFinder.findClasspathResource("/com/app/util/Home.tml", false).toTypedArray()[0] as IntellijResource).psiFile
            )
            notJavaClassType.annotations.size shouldBe 0
        }
    }

    /**
     * Tests that class annotations are correctly retrieved.
     */
    "getAnnotations_with_annotations" {
        readActionBlocking {
            classTypeOf("com.app.util.Class1").annotations.size shouldBe 2
        }
    }

    /**
     * Tests that class fields are correctly retrieved, both including and excluding inherited fields,
     * and that non-Java files return no fields.
     */
    "getFields" {
        readActionBlocking {
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

    /**
     * Tests that classes without documentation return empty documentation strings,
     * including non-Java files.
     */
    "getDocumentation_no_documentation" {
        readActionBlocking {
            classTypeOf("com.app.util.Class1").documentation!!.isEmpty() shouldBe true

            val resourceFinder = IntellijResourceFinder(module)
            val notJavaClassType = IntellijJavaClassType(
                module,
                (resourceFinder.findClasspathResource("/com/app/util/Home.tml", false).toTypedArray()[0] as IntellijResource).psiFile
            )
            notJavaClassType.documentation!!.length shouldBe 0
        }
    }

    /**
     * Tests that class documentation is correctly extracted.
     */
    "getDocumentation_with_documentation" {
        readActionBlocking {
            classTypeOf("com.app.util.Class2").documentation shouldBe " class2. docs."
        }
    }

    /**
     * Tests that non-existent files return null when attempting to retrieve the file.
     */
    "getFile_file_doesn_exist" {
        readActionBlocking {
            val psiFileMock = PsiFileMock().setVirtualFile(VirtualFileMock().setUrl("file:///doesnt.exist"))
            val classType = IntellijJavaClassType(module, psiFileMock)
            classType.file shouldBe null
        }
    }
})
