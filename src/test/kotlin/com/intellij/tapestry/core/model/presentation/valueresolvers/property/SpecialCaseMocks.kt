package com.intellij.tapestry.core.model.presentation.valueresolvers.property

import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.core.java.IJavaClassType
import com.intellij.tapestry.core.java.IJavaTypeFinder
import io.mockk.every
import io.mockk.mockk

/**
 * Mock fixtures shared by the special-case resolver specs. Replaces the old
 * easymock `AbstractSpecialCaseTest` base class — mockk needs no replay/reset,
 * so stubs are set up once and answer every call.
 */
class SpecialCaseMocks {
    val javaTypeFinder: IJavaTypeFinder = mockk()
    val tapestryProject: TapestryProject = mockk {
        every { javaTypeFinder } returns this@SpecialCaseMocks.javaTypeFinder
    }

    fun expectToFindType(type: String, returnValue: IJavaClassType) {
        every { javaTypeFinder.findType(type, true) } returns returnValue
    }
}
