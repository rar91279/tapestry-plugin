package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaType
import com.github.rar91279.plugin.tapestry.core.util.chain.Context

/** The state shared by the resolvers of a single resolution run. */
class ValueResolverContext(
    val project: TapestryProject,
    val contextClass: IJavaClassType?,
    val value: String?,
    val defaultPrefix: String?
) : Context {

    var resultType: IJavaType? = null
    var resultCodeBind: Any? = null
}
