package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.intellij.psi.PsiClass
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.intellij.psi.PsiFile

/**
 * A Tapestry mixin.
 */
class Mixin internal constructor(
    library: TapestryLibrary?,
    componentClass: PsiClass,
    project: TapestryProject
) : ParameterReceiverElement(library, componentClass, project) {

    override fun allowsTemplate(): Boolean = false

    override val template: Array<PsiFile> get() = emptyArray()

    override val messageCatalog: Array<PsiFile> get() = emptyArray()
}
