package com.github.rar91279.plugin.tapestry.intellij.core.java

import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
import com.github.rar91279.plugin.tapestry.core.ioc.IServiceBindingDiscoverer
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaTypeFinder

/** [IJavaTypeFinder] that resolves types in a module's scope. */
class IntellijJavaTypeFinder(private val module: Module) : IJavaTypeFinder {

    override fun findType(fullyQualifiedName: String, includeDependencies: Boolean): IJavaClassType? {
        val psiClass = JavaPsiFacade.getInstance(module.project)
            .findClass(fullyQualifiedName, scope(includeDependencies)) ?: return null

        return IntellijJavaClassType(module, psiClass.containingFile)
    }

    override fun findTypesInPackage(packageName: String, includeDependencies: Boolean): Collection<IJavaClassType> =
        findPackage(packageName)?.getClasses(scope(includeDependencies))
            ?.map { IntellijJavaClassType(module, it.containingFile) }
            ?: emptyList()

    override fun findTypesInPackageRecursively(basePackageName: String, includeDependencies: Boolean): Collection<IJavaClassType> {
        val psiPackage = findPackage(basePackageName) ?: return emptyList()
        val scope = scope(includeDependencies)

        val types = mutableListOf<IJavaClassType>()
        psiPackage.getClasses(scope).forEach { types.add(IntellijJavaClassType(module, it.containingFile)) }
        for (subPackage in psiPackage.getSubPackages(scope)) {
            types.addAll(findTypesInPackageRecursively(subPackage.qualifiedName, includeDependencies))
        }

        return types
    }

    /** Service binding discovery is not implemented for the IDE model. */
    override val serviceBindingDiscoverer: IServiceBindingDiscoverer?
        get() = null

    private fun findPackage(packageName: String): PsiPackage? =
        JavaPsiFacade.getInstance(module.project).findPackage(packageName)

    private fun scope(includeDependencies: Boolean): GlobalSearchScope =
        if (includeDependencies) GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
        else GlobalSearchScope.moduleScope(module)
}
