package com.github.rar91279.plugin.tapestry.tests.core.java

import com.intellij.openapi.application.readActionBlocking
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.github.rar91279.plugin.tapestry.core.java.IJavaArrayType
import com.github.rar91279.plugin.tapestry.core.java.IJavaPrimitiveType
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaField
import com.github.rar91279.plugin.tapestry.tests.core.JavaModuleFixtureSpec
import com.github.rar91279.plugin.tapestry.tests.mocks.PsiClassTypeMock
import com.github.rar91279.plugin.tapestry.tests.mocks.PsiFieldMock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe


/**
 * Test suite for [IntellijJavaField] class.
 *
 * This test class validates the functionality of the IntelliJ-based Java field wrapper,
 * testing field name retrieval, type resolution (primitive, class, and array types),
 * visibility modifiers, annotations, documentation extraction, string representation,
 * validity checks, and object equality/hash code implementations.
 */
class IntellijJavaFieldTest : JavaModuleFixtureSpec({

    /**
     * Helper function to retrieve a field from the test Class1 by index.
     *
     * @param index the zero-based index of the field in Class1
     * @return an [IntellijJavaField] wrapper for the field at the specified index
     */
    fun class1Field(index: Int) = IntellijJavaField(
        module,
        javaFacade().findClass("com.app.util.Class1", GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module))!!.fields[index]
    )

    /**
     * Verifies that the field name is correctly retrieved from the underlying PSI field.
     */
    "getName" {
        readActionBlocking {
            val psiField = javaFacade().elementFactory.createField("_fieldName", PsiType.BOOLEAN)
            IntellijJavaField(module, psiField).name shouldBe "_fieldName"
            IntellijJavaField(module, psiField).psiField.name shouldBe "_fieldName"
        }
    }

    /**
     * Verifies that primitive type fields are correctly identified and their type name is retrieved.
     */
    "getType_primitive" {
        readActionBlocking {
            val psiField = javaFacade().elementFactory.createField("_fieldName", PsiType.BOOLEAN)
            (IntellijJavaField(module, psiField).type is IJavaPrimitiveType) shouldBe true
            IntellijJavaField(module, psiField).type!!.name shouldBe "boolean"
        }
    }

    /**
     * Verifies that class type fields are correctly identified and their fully qualified name is retrieved.
     */
    "getType_class" {
        readActionBlocking {
            val field = class1Field(1)
            (field.type is IntellijJavaClassType) shouldBe true
            (field.type as IntellijJavaClassType).fullyQualifiedName shouldBe "com.app.util.Class1"
        }
    }

    /**
     * Verifies that unresolvable field types return null.
     */
    "getType_cant_resolve" {
        readActionBlocking {
            val field = IntellijJavaField(module, PsiFieldMock().setType(PsiClassTypeMock().setResolve(null)))
            field.type shouldBe null
        }
    }

    /**
     * Verifies that array type fields are correctly identified.
     */
    "getType_array" {
        readActionBlocking {
            val field = class1Field(2)
            (field.type is IJavaArrayType) shouldBe true
        }
    }

    /**
     * Verifies that the private visibility modifier is correctly detected for fields.
     */
    "isPrivate" {
        readActionBlocking {
            val field1 = class1Field(0)
            val field2 = class1Field(1)
            field1.isPrivate shouldBe true
            field2.isPrivate shouldBe false
        }
    }

    /**
     * Verifies that fields without annotations return an empty annotation list.
     */
    "getAnnotations_no_annotations" {
        readActionBlocking {
            class1Field(2).annotations.size shouldBe 0
        }
    }

    /**
     * Verifies that fields with annotations return the correct annotation count.
     */
    "getAnnotations_with_annotations" {
        readActionBlocking {
            class1Field(1).annotations.size shouldBe 1
        }
    }

    /**
     * Verifies that fields without JavaDoc documentation return an empty string.
     */
    "getDocumentation_no_documentation" {
        readActionBlocking {
            class1Field(0).documentation!!.isEmpty() shouldBe true
        }
    }

    /**
     * Verifies that fields with JavaDoc documentation return the correct documentation text.
     */
    "getDocumentation_with_documentation" {
        readActionBlocking {
            class1Field(1).documentation shouldBe " field2. docs."
        }
    }

    /**
     * Verifies that the string representation of a field matches the expected Java field declaration.
     */
    "getStringRepresentation" {
        readActionBlocking {
            IntellijJavaField(module, javaFacade().elementFactory.createField("_fieldName", PsiType.BOOLEAN))
                .stringRepresentation shouldBe "private boolean _fieldName;"
        }
    }

    /**
     * Verifies that the validity state of a field is correctly reported based on the underlying PSI element.
     */
    "isValid" {
        readActionBlocking {
            IntellijJavaField(module, PsiFieldMock().setValid(true)).isValid shouldBe true
            IntellijJavaField(module, PsiFieldMock().setValid(false)).isValid shouldBe false
        }
    }

    /**
     * Verifies that the equals method correctly compares fields based on their names.
     */
    "testEquals" {
        readActionBlocking {
            val field1 = IntellijJavaField(module, PsiFieldMock().setMockName("field1"))
            val field2 = IntellijJavaField(module, PsiFieldMock().setMockName("field2"))
            val field3 = IntellijJavaField(module, PsiFieldMock().setMockName("field1"))

            field1.equals(null) shouldBe false
            field1.equals("") shouldBe false
            field2 shouldNotBe field1
            field3 shouldBe field1
        }
    }

    /**
     * Verifies that the hash code is correctly calculated based on the field name.
     */
    "hashCode" {
        readActionBlocking {
            val field1 = IntellijJavaField(module, PsiFieldMock().setMockName("field1"))
            "field1".hashCode() shouldBe field1.hashCode()
        }
    }
})
