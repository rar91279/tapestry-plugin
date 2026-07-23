package com.intellij.tapestry.core.util

import com.intellij.tapestry.core.java.IJavaField
import com.intellij.tapestry.core.java.IJavaMethod
import com.intellij.tapestry.core.mocks.JavaAnnotationMock
import com.intellij.tapestry.core.mocks.JavaClassTypeMock
import com.intellij.tapestry.core.mocks.JavaFieldMock
import com.intellij.tapestry.core.mocks.JavaPrimitiveTypeMock
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk

class ClassUtilsTest : FreeSpec({

    "constructor is callable" {
        ClassUtils()
    }

    "getClassProperties" - {
        "null" {
            ClassUtils.getClassProperties(null).size shouldBe 0
        }

        "no_properties" {
            // a class with no methods
            val noMethodsClassMock = JavaClassTypeMock()
            ClassUtils.getClassProperties(noMethodsClassMock).size shouldBe 0

            // a class with no getter methods.
            val notGetterMethodsMock = mockk<IJavaMethod>(relaxed = true)
            every { notGetterMethodsMock.name } returns "setProperty"
            every { notGetterMethodsMock.returnType } returns null

            val notGetterMethodsMock2 = mockk<IJavaMethod>(relaxed = true)
            every { notGetterMethodsMock2.name } returns "getProperty2"
            every { notGetterMethodsMock2.returnType } returns null

            val notGetterMethodsMock3 = mockk<IJavaMethod>(relaxed = true)
            every { notGetterMethodsMock3.name } returns "get"
            every { notGetterMethodsMock3.returnType } returns JavaPrimitiveTypeMock("char")

            val noGetterMethodsClassMock = JavaClassTypeMock()
            noGetterMethodsClassMock.addPublicMethod(notGetterMethodsMock)
                .addPublicMethod(notGetterMethodsMock2)
                .addPublicMethod(notGetterMethodsMock3)

            ClassUtils.getClassProperties(noGetterMethodsClassMock).size shouldBe 0
        }

        "with_properties" {
            val getterMethodMock = mockk<IJavaMethod>(relaxed = true)
            every { getterMethodMock.name } returns "getProperty1"
            every { getterMethodMock.returnType } returns JavaPrimitiveTypeMock("boolean")

            val getterMethod2Mock = mockk<IJavaMethod>(relaxed = true)
            every { getterMethod2Mock.name } returns "getPropertyProp2"
            every { getterMethod2Mock.returnType } returns JavaPrimitiveTypeMock("boolean")

            val isMethodMock = mockk<IJavaMethod>(relaxed = true)
            every { isMethodMock.name } returns "isProperty2"
            every { isMethodMock.returnType } returns JavaPrimitiveTypeMock("boolean")

            val isMethodNotBooleanMock = mockk<IJavaMethod>(relaxed = true)
            every { isMethodNotBooleanMock.name } returns "isProperty3"
            every { isMethodNotBooleanMock.returnType } returns JavaPrimitiveTypeMock("short")

            val getterMethodsClassMock = JavaClassTypeMock()
            getterMethodsClassMock.addPublicMethod(getterMethodMock)
                .addPublicMethod(isMethodMock)
                .addPublicMethod(isMethodNotBooleanMock)
                .addPublicMethod(getterMethod2Mock)

            val properties = ClassUtils.getClassProperties(getterMethodsClassMock)

            properties.size shouldBe 3
            properties["propertyProp2"] shouldNotBe null
        }

        "with_annotated_properties" {
            val annotatedField: IJavaField = JavaFieldMock("_myProp", true)
                .addAnnotation(JavaAnnotationMock("org.apache.tapestry5.annotations.Property"))

            val notAnnotatedField: IJavaField = JavaFieldMock("MyField", true)

            val classMock = JavaClassTypeMock()
            classMock.addField(annotatedField).addField(notAnnotatedField)

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
