package com.github.rar91279.plugin.tapestry.tests.core.java

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiImportList
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaTypeCreator

class IntellijJavaTypeCreatorDummy : IntellijJavaTypeCreator {

    private var _controlMock: IntellijJavaTypeCreator? = null

    constructor(module: Module) : super(module)

    constructor(module: Module, controlMock: IntellijJavaTypeCreator) : super(module) {
        _controlMock = controlMock
    }

    override fun addImport(importList: PsiImportList, aClass: PsiClass) {
        _controlMock!!.addImport(importList, aClass)
    }
}
