package com.github.rar91279.plugin.tapestry.core.model.ioc

import com.intellij.psi.PsiClass

/**
 * A Tapestry IoC service.
 */
class Service(
    val id: String,
    val scope: String,
    val isEagerLoad: Boolean,
    val serviceClass: PsiClass?,
) {

    override fun toString(): String = id
}
