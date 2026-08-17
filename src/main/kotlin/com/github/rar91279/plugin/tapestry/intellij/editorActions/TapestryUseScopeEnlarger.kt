package com.github.rar91279.plugin.tapestry.intellij.editorActions

import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.lang.TmlFileType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UseScopeEnlarger

/**
 * Widens the search scope of a page/component/mixin member to the module's templates.
 *
 * Java says a private member can only be referenced inside its own file, and the platform narrows every search
 * accordingly — which is why *Find Usages*, the usage inlay and *Show Usages* all reported nothing for a
 * `@Property` field even though the template plainly references it, and the reference resolved to it. Tapestry
 * breaks that assumption: a template reads private fields and calls private accessors.
 *
 * Only private members are enlarged; anything more visible is already searched project-wide.
 */
class TapestryUseScopeEnlarger : UseScopeEnlarger() {

    override fun getAdditionalUseScope(element: PsiElement): SearchScope? {
        val member = element as? PsiMember ?: return null
        if (!member.hasModifierProperty(PsiModifier.PRIVATE)) return null

        val containingClass = member.containingClass ?: return null
        val module = ModuleUtilCore.findModuleForPsiElement(containingClass) ?: return null
        val project = TapestryModuleSupportLoader.getTapestryProject(module) ?: return null

        if (!containingClass.isTapestryElement(project.applicationRootPackage)) return null

        return GlobalSearchScope.getScopeRestrictedByFileTypes(module.moduleContentScope, TmlFileType)
    }

    /**
     * Whether the class is one a template can bind to, by package.
     *
     * Deliberately a string comparison rather than a model lookup: this runs whenever the platform computes a
     * use scope, and resolving the element model there would put stub-index work on that path.
     */
    private fun PsiClass.isTapestryElement(applicationRootPackage: String?): Boolean {
        val rootPackage = applicationRootPackage ?: return false
        val fqn = qualifiedName ?: return false

        return TapestryConstants.ELEMENT_PACKAGES.any { fqn.startsWith("$rootPackage.$it.") }
    }
}
