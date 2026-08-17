package com.github.rar91279.plugin.tapestry.intellij.util

import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.util.PsiUtil

/**
 * Discovery of the Tapestry IoC module classes visible from an IDEA module.
 *
 * Shared by everything that has to read contributions out of module classes — injected-bean line markers,
 * JavaScript stack resolution — because the set is anything but obvious: an application usually declares no
 * modules at all, libraries advertise theirs in a manifest, and the framework hardwires its own.
 */
object TapestryModuleClasses {

    /**
     * The IoC module classes visible from a module: those advertised by a `Tapestry-Module-Classes` manifest
     * (so library services like `Request` resolve too), the `*Module` classes in the application's `services`
     * package, the framework's own, and everything those pull in with `@ImportModule`.
     */
    fun of(module: Module): List<PsiClass> {
        val facade = JavaPsiFacade.getInstance(module.project)
        val scope = module.getModuleWithDependenciesAndLibrariesScope(false)

        val declared = TapestryUtils.getDeclaredModuleClasses(module) +
                TapestryUtils.getClasspathLibraryModules(module).map { it.moduleClass }

        val rootPackage = TapestryModuleSupportLoader.getTapestryProject(module)?.applicationRootPackage
        val conventional = rootPackage
            ?.let { facade.findPackage("$it.${TapestryConstants.SERVICES_PACKAGE}") }
            ?.getClasses(scope)
            ?.filter { it.name?.endsWith(TapestryConstants.MODULE_BUILDER_SUFIX) == true }
            .orEmpty()

        // ponytail: three small package scans per pass; cache alongside getClasspathLibraryModules if it ever
        // shows up in a freeze report.
        val framework = FRAMEWORK_MODULE_PACKAGES
            .mapNotNull { facade.findPackage(it) }
            .flatMap { it.getClasses(scope).asList() }
            .filter { it.name?.endsWith(TapestryConstants.MODULE_BUILDER_SUFIX) == true } +
                listOfNotNull(facade.findClass(LEGACY_CORE_MODULE_CLASS, scope))

        return withImportedModules(declared.mapNotNull { facade.findClass(it, scope) } + conventional + framework)
    }

    /**
     * [roots] plus every module class they pull in with `@ImportModule`, transitively.
     *
     * This is how an application adds a module that is neither named in a manifest nor sitting in
     * `<applicationRootPackage>.services` — typically `@ImportModule(DAOModule.class)` on `AppModule`.
     */
    private fun withImportedModules(roots: List<PsiClass>): List<PsiClass> {
        val seen = LinkedHashSet<PsiClass>()
        val pending = ArrayDeque(roots)

        while (pending.isNotEmpty()) {
            val moduleClass = pending.removeFirst()
            // The visited set also breaks the cycle two modules importing each other would otherwise form.
            if (seen.add(moduleClass)) pending.addAll(importedModules(moduleClass))
        }

        return seen.toList()
    }

    /** The module classes a module class lists in its `@ImportModule`, one or many. */
    private fun importedModules(moduleClass: PsiClass): List<PsiClass> {
        val value = AnnotationUtil.findAnnotation(moduleClass, IMPORT_MODULE_ANNOTATION)
            ?.findAttributeValue("value") ?: return emptyList()

        val classLiterals = if (value is PsiArrayInitializerMemberValue) value.initializers.asList() else listOf(value)

        return classLiterals.mapNotNull {
            PsiUtil.resolveClassInType((it as? PsiClassObjectAccessExpression)?.operand?.type)
        }
    }

    private const val IMPORT_MODULE_ANNOTATION = "org.apache.tapestry5.ioc.annotations.ImportModule"

    /**
     * Packages holding the framework's own IoC modules.
     *
     * A third-party library advertises its modules in the manifest — `tapestry-json` declares
     * `Tapestry-Module-Classes`, and [TapestryUtils.getClasspathLibraryModules] picks it up. `tapestry-core`,
     * `tapestry-ioc` and `tapestry-http` advertise nothing at all (their manifests carry only
     * `Automatic-Module-Name`), because Tapestry hardwires those modules in its own bootstrap. Without this
     * list, every service they `bind(...)` — `SelectModelFactory` and the rest of core — has no findable source.
     */
    private val FRAMEWORK_MODULE_PACKAGES = listOf(
        "org.apache.tapestry5.modules",
        "org.apache.tapestry5.ioc.modules",
        "org.apache.tapestry5.http.modules",
    )

    /** Where tapestry-core kept its module class before the 5.4 move to `org.apache.tapestry5.modules`. */
    private const val LEGACY_CORE_MODULE_CLASS = "org.apache.tapestry5.services.TapestryModule"
}
