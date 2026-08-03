package com.intellij.tapestry.intellij

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.java.ultimate.icons.JavaUltimateIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.tapestry.core.TapestryConstants
import com.intellij.tapestry.intellij.util.TapestryUtils
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getContainingUMethod
import org.jetbrains.uast.getUParentForIdentifier
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor

private const val INJECT_SERVICE_ANNOTATION = "org.apache.tapestry5.ioc.annotations.InjectService"

/**
 * Annotations marking an injection point. Tapestry 5.9 honours the JSR-330 `@Inject` (both namespaces)
 * next to its own, and `@InjectService` injects by id.
 */
private val INJECT_ANNOTATIONS = setOf(
    TapestryConstants.CORE_INJECT_ANNOTATION, "org.apache.tapestry5.ioc.annotations.Inject",
    "jakarta.inject.Inject", "javax.inject.Inject", INJECT_SERVICE_ANNOTATION)

/** Annotations that name the wanted service explicitly, disambiguating same-type services. */
private val QUALIFIER_ANNOTATIONS = setOf(
    "jakarta.inject.Named", "javax.inject.Named",
    "org.apache.tapestry5.ioc.annotations.Service", INJECT_SERVICE_ANNOTATION)

private const val SERVICE_ID_ANNOTATION = "org.apache.tapestry5.ioc.annotations.ServiceId"
private const val WITH_ID_METHOD = "withId"
private const val GET_SIMPLE_NAME_METHOD = "getSimpleName"

/** A place a service of the injected type comes from: a `build*` method or a `bind(...)` call. */
private class ServiceSource(val id: String, val element: PsiElement)

/**
 * Gutter icons for both ends of a Tapestry IoC injection:
 *  * an injected field or constructor parameter → the `build*` methods and `bind(...)` calls providing it,
 *  * a service `build*` method → the injection points it feeds.
 *
 * Works on Java and Kotlin sources alike, hence UAST.
 */
class TapestryInjectedBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        // Anchor on the name identifier of the declaration (a leaf), per platform guidance.
        val declaration = getUParentForIdentifier(element) ?: return

        injectionPoint(declaration)?.let { point ->
            val sources = serviceSources(point, element)
            if (sources.isNotEmpty()) {
                result.add(
                    NavigationGutterIconBuilder.create(JavaUltimateIcons.Cdi.Gutter.ShowAutowiredCandidates)
                        .setTargets(sources.map { it.element })
                        .setTooltipText("Tapestry IoC service source")
                        .setPopupTitle("Service Sources")
                        .createLineMarkerInfo(element))
            }
            return
        }

        (declaration as? UMethod)?.let { addInjectionPointsMarker(it, element, result) }
        (declaration as? UCallExpression)?.let { addBoundServiceMarker(it, element, result) }
    }

    /**
     * The reverse direction for autobuilt services: from a `bind(Service.class, ...)` call — anchored on
     * the call itself, so the icon sits on that line — to the injection points it feeds.
     */
    private fun addBoundServiceMarker(
        call: UCallExpression,
        anchor: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (call.methodName != TapestryConstants.SERVICE_AUTOBUILDER_METHOD_NAME) return
        val binder = call.getContainingUMethod()?.javaPsi ?: return
        if (frameworkMethodKind(binder) != FrameworkMethodKind.IOC) return

        val boundType = (call.valueArguments.firstOrNull() as? UClassLiteralExpression)?.type ?: return
        val serviceClass = PsiUtil.resolveClassInType(boundType) ?: return
        val module = ModuleUtilCore.findModuleForPsiElement(anchor) ?: return
        val points = injectionPoints(serviceClass, module)
        if (points.isEmpty()) return

        result.add(
            NavigationGutterIconBuilder.create(JavaUltimateIcons.Cdi.Gutter.ShowAutowiredDependencies)
                .setTargets(points)
                .setTooltipText("Tapestry injection points")
                .setPopupTitle("Injected At")
                .createLineMarkerInfo(anchor))
    }

    /** The reverse direction: from a service `build*` method to the fields and parameters it feeds. */
    private fun addInjectionPointsMarker(
        method: UMethod,
        anchor: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val psiMethod = method.javaPsi
        if (!psiMethod.name.startsWith(TapestryConstants.SERVICE_BUILDER_METHOD_PREFIX)) return
        if (frameworkMethodKind(psiMethod) != FrameworkMethodKind.IOC) return

        val serviceClass = PsiUtil.resolveClassInType(psiMethod.returnType) ?: return
        val module = ModuleUtilCore.findModuleForPsiElement(anchor) ?: return
        val points = injectionPoints(serviceClass, module)
        if (points.isEmpty()) return

        result.add(
            NavigationGutterIconBuilder.create(JavaUltimateIcons.Cdi.Gutter.ShowAutowiredDependencies)
                .setTargets(points)
                .setTooltipText("Tapestry injection points")
                .setPopupTitle("Injected At")
                .createLineMarkerInfo(anchor))
    }

    /** Every `@Inject` field and constructor parameter of the service's type, anywhere it can be seen. */
    private fun injectionPoints(serviceClass: PsiClass, module: Module): List<PsiElement> {
        // ponytail: walks all references to the service type; fine for a slow line marker, but a very
        // widely used type pays for it — narrow the scope if that ever bites.
        val scope = GlobalSearchScope.moduleWithDependentsScope(module)
        return ReferencesSearch.search(serviceClass, scope).asIterable()
            .mapNotNull { enclosingVariable(it.element) }
            .filter { injectionPoint(it) != null && PsiUtil.resolveClassInType(it.type) == serviceClass }
            .mapNotNull { it.sourcePsi ?: it.javaPsi }
            .distinct()
    }

    /** The field or parameter a reference sits in, in any JVM language. */
    private fun enclosingVariable(element: PsiElement): UVariable? {
        var current: PsiElement? = element
        repeat(5) {
            current?.toUElementOfType<UVariable>()?.let { return it }
            current = current?.parent ?: return null
        }
        return null
    }

    /**
     * An injected `@Inject` field, or a constructor parameter — Tapestry injects those by type, without
     * any annotation. Null for anything else.
     */
    private fun injectionPoint(declaration: UElement?): UVariable? = when {
        declaration is UField && declaration.uAnnotations.any { it.qualifiedName in INJECT_ANNOTATIONS } -> declaration
        declaration is UParameter && (declaration.uastParent as? UMethod)?.isConstructor == true -> declaration
        else -> null
    }

    /**
     * Every source of a service fitting the injection point's type. When it names the service it wants
     * (`@Named`, `@Service`, `@InjectService`), only that id is kept — unless nothing matches, in which
     * case all candidates stay rather than leaving the injection point looking unprovided.
     */
    private fun serviceSources(injectionPoint: UVariable, anchor: PsiElement): List<ServiceSource> {
        val fieldType = injectionPoint.type as? PsiClassType ?: return emptyList()
        val module = ModuleUtilCore.findModuleForPsiElement(anchor) ?: return emptyList()
        if (!TapestryUtils.isTapestryModule(module)) return emptyList()
        val moduleClasses = moduleClasses(module)

        val sources = (buildMethodSources(module, moduleClasses, fieldType) +
                moduleClasses.flatMap { bindSources(it, fieldType) })
            .distinctBy { it.element }

        val wantedId = requestedId(injectionPoint) ?: return sources
        return sources.filter { it.id.equals(wantedId, ignoreCase = true) }.ifEmpty { sources }
    }

    /** The service id the injection point asks for by name, if any. */
    private fun requestedId(injectionPoint: UVariable): String? = injectionPoint.uAnnotations
        .firstOrNull { it.qualifiedName in QUALIFIER_ANNOTATIONS }
        ?.findAttributeValue("value")?.evaluate() as? String

    /**
     * The `build*` methods of the IoC module classes on the field's classpath whose return type fits the
     * field. Their service id is `@ServiceId` if present, else the method name minus `build`.
     */
    private fun buildMethodSources(
        module: Module, moduleClasses: List<PsiClass>, fieldType: PsiClassType
    ): List<ServiceSource> {
        val candidates = moduleClasses.flatMap { it.allMethods.asList() } +
                byConventionalName(module, fieldType)

        return candidates.distinct()
            .filter { it.hasModifierProperty(PsiModifier.PUBLIC) && it.name.startsWith(TapestryConstants.SERVICE_BUILDER_METHOD_PREFIX) }
            .filter { fits(fieldType, it.returnType) }
            .map { ServiceSource(buildMethodId(it), it) }
    }

    private fun buildMethodId(method: PsiMethod): String {
        AnnotationUtil.findAnnotation(method, SERVICE_ID_ANNOTATION)
            ?.let { AnnotationUtil.getStringAttributeValue(it, "value") }
            ?.let { return it }
        // A method called plainly `build` takes its id from the service type (as Tapestry does).
        return method.name.removePrefix(TapestryConstants.SERVICE_BUILDER_METHOD_PREFIX)
            .ifEmpty { typeName(method.returnType) }
    }

    /**
     * Services bound in the module's `bind(ServiceBinder)` method: each `bind(Service.class, Impl.class)`
     * call whose service type fits the field, with the id from a chained `withId(...)` when given.
     */
    private fun bindSources(moduleClass: PsiClass, fieldType: PsiClassType): List<ServiceSource> {
        val binder = moduleClass.findMethodsByName(TapestryConstants.SERVICE_AUTOBUILDER_METHOD_NAME, false)
            .firstOrNull() ?: return emptyList()
        val body = binder.toUElementOfType<UMethod>()?.uastBody ?: return emptyList()

        val sources = mutableListOf<ServiceSource>()
        body.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (node.methodName != TapestryConstants.SERVICE_AUTOBUILDER_METHOD_NAME) return false
                val boundType = (node.valueArguments.firstOrNull() as? UClassLiteralExpression)?.type
                val source = node.sourcePsi
                if (boundType != null && source != null && fits(fieldType, boundType)) {
                    sources.add(ServiceSource(explicitId(node) ?: typeName(boundType), source))
                }
                return false
            }
        })
        return sources
    }

    /** The id of a `bind(...)` call given as `.withId(...)` somewhere in the chained call. */
    private fun explicitId(bindCall: UCallExpression): String? {
        var parent = bindCall.uastParent
        repeat(5) {
            val call = parent as? UCallExpression
            if (call != null && call.methodName == WITH_ID_METHOD) {
                return idOf(call.valueArguments.firstOrNull())
            }
            parent = parent?.uastParent ?: return null
        }
        return null
    }

    /**
     * The id an argument denotes: a constant string, or `Impl.class.getSimpleName()` — the idiom that
     * keeps the id in sync with the implementation class.
     */
    private fun idOf(argument: UExpression?): String? {
        (argument?.evaluate() as? String)?.let { return it }
        val call = argument as? UCallExpression ?: return null
        if (call.methodName != GET_SIMPLE_NAME_METHOD) return null
        return typeName((call.receiver as? UClassLiteralExpression)?.type)
    }

    private fun typeName(type: PsiType?): String = PsiUtil.resolveClassInType(type)?.name.orEmpty()

    /**
     * Whether a provided service can be injected here. Compared on erasures: services are bound and built
     * as raw types (`bind(SimpleSyncActionApplier.class, ...)`) while injection points name a parameterized
     * one — Tapestry matches by service id, not by type argument, so the plugin follows suit.
     */
    private fun fits(injected: PsiType, provided: PsiType?): Boolean {
        val target = TypeConversionUtil.erasure(injected) ?: return false
        val source = provided?.let { TypeConversionUtil.erasure(it) } ?: return false
        return target.isAssignableFrom(source)
    }

    /**
     * `build<Type>` methods of any `*Module` class on the classpath, found by name through the index.
     * Catches module classes that neither a manifest nor the `services` package convention reveals.
     */
    private fun byConventionalName(module: Module, fieldType: PsiClassType): List<PsiMethod> {
        val typeName = fieldType.resolve()?.name ?: return emptyList()
        val scope = module.getModuleWithDependenciesAndLibrariesScope(false)
        return PsiShortNamesCache.getInstance(module.project)
            .getMethodsByName(TapestryConstants.SERVICE_BUILDER_METHOD_PREFIX + typeName, scope)
            .filter { it.containingClass?.name?.endsWith(TapestryConstants.MODULE_BUILDER_SUFIX) == true }
    }

    /**
     * The IoC module classes visible from a module: those advertised by a `Tapestry-Module-Classes`
     * manifest (so library services like `Request` resolve too), plus — since an application usually
     * declares none — the `*Module` classes in its `<applicationRootPackage>.services` package.
     */
    private fun moduleClasses(module: Module): List<PsiClass> {
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

        return declared.mapNotNull { facade.findClass(it, scope) } + conventional
    }
}
