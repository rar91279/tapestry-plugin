package com.github.rar91279.plugin.tapestry.core.util

import com.github.rar91279.plugin.tapestry.core.mocks.psiAnnotationMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiClassMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiFieldMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiMethodMock
import com.github.rar91279.plugin.tapestry.core.mocks.stubFields
import com.github.rar91279.plugin.tapestry.core.mocks.stubMethods
import com.intellij.psi.PsiTypes
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ClassUtilsTest : FreeSpec({

    "getClassProperties" - {
        "null" {
            ClassUtils.getClassProperties(null).size shouldBe 0
        }

        "no_properties" {
            // a class with no methods
            ClassUtils.getClassProperties(psiClassMock()).size shouldBe 0

            // a class with no getter methods.
            val noGetterMethodsClassMock = psiClassMock().stubMethods(
                psiMethodMock("setProperty"),
                psiMethodMock("getProperty2"),
                psiMethodMock("get", returnType = PsiTypes.charType())
            )

            ClassUtils.getClassProperties(noGetterMethodsClassMock).size shouldBe 0
        }

        "with_properties" {
            val getterMethodsClassMock = psiClassMock().stubMethods(
                psiMethodMock("getProperty1", returnType = PsiTypes.booleanType()),
                psiMethodMock("isProperty2", returnType = PsiTypes.booleanType()),
                psiMethodMock("isProperty3", returnType = PsiTypes.shortType()),
                psiMethodMock("getPropertyProp2", returnType = PsiTypes.booleanType())
            )

            val properties = ClassUtils.getClassProperties(getterMethodsClassMock)

            properties.size shouldBe 3
            properties["propertyProp2"] shouldNotBe null
        }

        "with_annotated_properties" {
            val annotatedField = psiFieldMock(
                "_myProp", annotations = listOf(psiAnnotationMock("org.apache.tapestry5.annotations.Property"))
            )
            val notAnnotatedField = psiFieldMock("MyField")

            val classMock = psiClassMock().stubFields(annotatedField, notAnnotatedField)

            val properties = ClassUtils.getClassProperties(classMock)

            properties.size shouldBe 1
            properties["myProp"] shouldNotBe null
        }
    }

    "getName" {
        ClassUtils.getName("_field1") shouldBe "field1"
        ClassUtils.getName("\$field1") shouldBe "field1"
        ClassUtils.getName("field1") shouldBe "field1"
    }
})
