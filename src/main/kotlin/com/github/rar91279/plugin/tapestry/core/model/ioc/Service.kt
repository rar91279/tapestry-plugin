package com.github.rar91279.plugin.tapestry.core.model.ioc

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

/**
 * A Tapestry IoC service.
 *
 * @param declaration the `build*` method declaring it, which is where the service is actually written — the
 *                    service class is usually an interface shared by many implementations.
 */
class Service(
    val id: String,
    val scope: String,
    val isEagerLoad: Boolean,
    val serviceClass: PsiClass?,
    val declaration: PsiMethod? = null,
) {

    override fun toString(): String = id
}
