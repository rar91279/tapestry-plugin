package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.mocks.psiClassMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiClassTypeMock
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import io.mockk.every
import io.mockk.mockk

/**
 * Mock fixtures shared by the special-case resolver specs. Replaces the old
 * easymock `AbstractSpecialCaseTest` base class — mockk needs no replay/reset,
 * so stubs are set up once and answer every call.
 */
class SpecialCaseMocks {

    val tapestryProject: TapestryProject = mockk(relaxed = true)

    init {
        every { tapestryProject.classTypeOf(any()) } answers { psiClassTypeMock(firstArg()) }
    }

    /** Makes the project resolve [fullyQualifiedName], and returns the class it resolves to. */
    fun expectToFindType(fullyQualifiedName: String): PsiClass {
        val psiClass = psiClassMock(fullyQualifiedName)
        every { tapestryProject.findType(fullyQualifiedName, true) } returns psiClass
        every { tapestryProject.findClassType(fullyQualifiedName) } returns psiClassTypeMock(psiClass)
        return psiClass
    }
}

/** The qualified name of the class this type denotes, `null` if it is not a class type. */
val PsiType?.resolvedName: String?
    get() = (this as? PsiClassType)?.resolve()?.qualifiedName
