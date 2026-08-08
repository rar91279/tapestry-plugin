package com.github.rar91279.plugin.tapestry.tests.core

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiImportList
import com.github.rar91279.plugin.tapestry.intellij.util.JavaTypeCreator

/** A [JavaTypeCreator] that routes [addImport] to a mock, so the spec can verify it was called. */
class JavaTypeCreatorDummy(module: Module, private val controlMock: JavaTypeCreator) : JavaTypeCreator(module) {

    override fun addImport(importList: PsiImportList, aClass: PsiClass) {
        controlMock.addImport(importList, aClass)
    }
}
