/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.github.rar91279.plugin.tapestry.tests

import com.intellij.testFramework.builders.JavaModuleFixtureBuilder

/**
 * @author Alexey Chmutov
 */
class TapestryCompletionGroovyTest : TapestryCompletionTest() {

    override fun getComponentClassExtension(): String = Util.DOT_GROOVY

    override fun getExistingComponentClassFileName(): String? {
        val fileName = super.getExistingComponentClassFileName()
        return fileName ?: checkTestDataFileExists(getElementName() + super.getComponentClassExtension())
    }

    override fun testIdAttrValue() {
        addComponentToProject("Count")
        initByComponent()
        doTestBasicCompletionVariants("link2", "link3")
    }

    override fun addTapestryLibraries(moduleBuilder: JavaModuleFixtureBuilder<*>) {
        super.addTapestryLibraries(moduleBuilder)
        if (ourTestsWithExtraLibraryComponents.contains(getTestName(false))) {
            moduleBuilder.addLibraryJars("tapestry_5.1.0.5_additional", Util.getCommonTestDataPath() + "libs", "tapestry-upload-5.1.0.5.jar")
        }
    }

    fun testCompleteComponentFromLibrary() {
        addComponentToProject("Count3")
        initByComponent()
        doTestBasicCompletionVariants("wf.upload", "addrowlink", "gridrows", "outputraw", "passwordfield", "removerowlink")
    }

    companion object {
        private val ourTestsWithExtraLibraryComponents: Set<String> = setOf("CompleteComponentFromLibrary")
    }
}
