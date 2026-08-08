package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType

/** The state shared by the resolvers of a single resolution run. */
class ValueResolverContext(
    val project: TapestryProject,
    val contextClass: PsiClass?,
    val value: String?,
    val defaultPrefix: String?
) {

    var resultType: PsiType? = null
    var resultCodeBind: Any? = null
}
