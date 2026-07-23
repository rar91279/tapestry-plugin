package com.intellij.tapestry.core.ioc

import com.intellij.tapestry.core.mocks.JavaClassTypeMock
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ServiceBindingTest : FreeSpec({

    "getters_setters" {
        val classTypeMock = JavaClassTypeMock("MyClass")

        val serviceBinding = ServiceBinding()
        serviceBinding.serviceClass = classTypeMock
        serviceBinding.isEagerLoad = true
        serviceBinding.scope = "myscope"
        serviceBinding.id = "myid"

        serviceBinding.serviceClass.name shouldBe "MyClass"
        serviceBinding.id shouldBe "myid"
        serviceBinding.scope shouldBe "myscope"
        serviceBinding.isEagerLoad shouldBe true
    }
})
