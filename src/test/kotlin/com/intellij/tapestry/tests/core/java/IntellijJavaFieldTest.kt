package com.intellij.tapestry.tests.core.java

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.tapestry.core.java.IJavaArrayType
import com.intellij.tapestry.core.java.IJavaPrimitiveType
import com.intellij.tapestry.intellij.core.java.IntellijJavaClassType
import com.intellij.tapestry.intellij.core.java.IntellijJavaField
import com.intellij.tapestry.tests.core.JavaModuleFixtureSpec
import com.intellij.tapestry.tests.mocks.PsiClassTypeMock
import com.intellij.tapestry.tests.mocks.PsiFieldMock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class IntellijJavaFieldTest : JavaModuleFixtureSpec({

    fun class1Field(index: Int) = IntellijJavaField(
        module,
        javaFacade().findClass("com.app.util.Class1", GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module))!!.fields[index]
    )

    "getName" {
        runReadAction {
            val psiField = javaFacade().elementFactory.createField("_fieldName", PsiType.BOOLEAN)
            IntellijJavaField(module, psiField).name shouldBe "_fieldName"
            IntellijJavaField(module, psiField).psiField.name shouldBe "_fieldName"
        }
    }

    "getType_primitive" {
        runReadAction {
            val psiField = javaFacade().elementFactory.createField("_fieldName", PsiType.BOOLEAN)
            (IntellijJavaField(module, psiField).type is IJavaPrimitiveType) shouldBe true
            IntellijJavaField(module, psiField).type!!.name shouldBe "boolean"
        }
    }

    "getType_class" {
        runReadAction {
            val field = class1Field(1)
            (field.type is IntellijJavaClassType) shouldBe true
            (field.type as IntellijJavaClassType).fullyQualifiedName shouldBe "com.app.util.Class1"
        }
    }

    "getType_cant_resolve" {
        runReadAction {
            val field = IntellijJavaField(module, PsiFieldMock().setType(PsiClassTypeMock().setResolve(null)))
            field.type shouldBe null
        }
    }

    "getType_array" {
        runReadAction {
            val field = class1Field(2)
            (field.type is IJavaArrayType) shouldBe true
        }
    }

    "isPrivate" {
        runReadAction {
            val field1 = class1Field(0)
            val field2 = class1Field(1)
            field1.isPrivate shouldBe true
            field2.isPrivate shouldBe false
        }
    }

    "getAnnotations_no_annotations" {
        runReadAction {
            class1Field(2).annotations.size shouldBe 0
        }
    }

    "getAnnotations_with_annotations" {
        runReadAction {
            class1Field(1).annotations.size shouldBe 1
        }
    }

    "getDocumentation_no_documentation" {
        runReadAction {
            class1Field(0).documentation!!.isEmpty() shouldBe true
        }
    }

    "getDocumentation_with_documentation" {
        runReadAction {
            class1Field(1).documentation shouldBe " field2. docs."
        }
    }

    "getStringRepresentation" {
        runReadAction {
            IntellijJavaField(module, javaFacade().elementFactory.createField("_fieldName", PsiType.BOOLEAN))
                .stringRepresentation shouldBe "private boolean _fieldName;"
        }
    }

    "isValid" {
        runReadAction {
            IntellijJavaField(module, PsiFieldMock().setValid(true)).isValid shouldBe true
            IntellijJavaField(module, PsiFieldMock().setValid(false)).isValid shouldBe false
        }
    }

    "testEquals" {
        runReadAction {
            val field1 = IntellijJavaField(module, PsiFieldMock().setMockName("field1"))
            val field2 = IntellijJavaField(module, PsiFieldMock().setMockName("field2"))
            val field3 = IntellijJavaField(module, PsiFieldMock().setMockName("field1"))

            field1.equals(null) shouldBe false
            field1.equals("") shouldBe false
            field2 shouldNotBe field1
            field3 shouldBe field1
        }
    }

    "hashCode" {
        runReadAction {
            val field1 = IntellijJavaField(module, PsiFieldMock().setMockName("field1"))
            "field1".hashCode() shouldBe field1.hashCode()
        }
    }
})
