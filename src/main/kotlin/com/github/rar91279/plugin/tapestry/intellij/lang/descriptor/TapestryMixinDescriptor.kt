package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.psi.PsiElement

/** Descriptor of the `mixin` attribute. */
class TapestryMixinDescriptor : BasicTapestryAttributeDescriptor() {

    override fun getDeclaration(): PsiElement? = null

    override fun getName(): String = "mixin"
}
