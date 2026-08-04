package com.github.rar91279.plugin.tapestry.intellij

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils

private const val ANNOTATIONS = "org.apache.tapestry5.annotations."
private const val IOC_ANNOTATIONS = "org.apache.tapestry5.ioc.annotations."

/** Render-phase and page-lifecycle method annotations invoked by the framework on pages/components/mixins. */
private val LIFECYCLE_ANNOTATIONS = listOf(
    ANNOTATIONS + "SetupRender", ANNOTATIONS + "BeginRender", ANNOTATIONS + "BeforeRenderTemplate",
    ANNOTATIONS + "BeginRenderBody", ANNOTATIONS + "AfterRenderBody", ANNOTATIONS + "AfterRenderTemplate",
    ANNOTATIONS + "AfterRender", ANNOTATIONS + "CleanupRender",
    ANNOTATIONS + "PageLoaded", ANNOTATIONS + "PageAttached", ANNOTATIONS + "PageDetached")

/** Render-phase methods are also recognized by name (annotation optional in Tapestry). */
private val RENDER_PHASE_NAMES = setOf(
    "setupRender", "beginRender", "beforeRenderTemplate", "beginRenderBody",
    "afterRenderBody", "afterRenderTemplate", "afterRender", "cleanupRender")

/** Fields set (and read) by the framework, so never "unused" or "unassigned". */
private val FIELD_ANNOTATIONS = listOf(
    TapestryConstants.CORE_INJECT_ANNOTATION, IOC_ANNOTATIONS + "Inject",
    TapestryConstants.COMPONENT_ANNOTATION, TapestryConstants.PROPERTY_ANNOTATION,
    TapestryConstants.INJECT_PAGE_ANNOTATION, TapestryConstants.MIXIN_ANNOTATION,
    ANNOTATIONS + "Parameter", ANNOTATIONS + "InjectComponent", ANNOTATIONS + "InjectContainer",
    ANNOTATIONS + "Environmental", ANNOTATIONS + "Persist", ANNOTATIONS + "PageActivationContext",
    ANNOTATIONS + "SessionState", ANNOTATIONS + "ApplicationState", ANNOTATIONS + "SessionAttribute",
    ANNOTATIONS + "ActivationRequestParameter")

/** IoC module method annotations (invoked reflectively by the registry). */
private val IOC_METHOD_ANNOTATIONS = listOf(
    IOC_ANNOTATIONS + "Contribute", IOC_ANNOTATIONS + "Startup", IOC_ANNOTATIONS + "Advise")

/**
 * Category of a framework-invoked method, used to pick a gutter icon. [annotations] are the FQNs that
 * define the kind, used for the tooltip javadoc lookup.
 */
enum class FrameworkMethodKind(val annotations: List<String>) {
    EVENT(listOf(TapestryConstants.EVENT_ANNOTATION)),
    LIFECYCLE(LIFECYCLE_ANNOTATIONS),
    IOC(IOC_METHOD_ANNOTATIONS)
}

class TapestryImplicitUsageProvider : ImplicitUsageProvider {

    override fun isImplicitUsage(element: PsiElement) = when (element) {
        is PsiMethod -> frameworkMethodKind(element) != null
        is PsiField -> isFrameworkField(element)
        else -> false
    }

    override fun isImplicitRead(element: PsiElement) = false

    override fun isImplicitWrite(element: PsiElement) = element is PsiField && isFrameworkField(element)

    private fun isFrameworkField(field: PsiField) = AnnotationUtil.isAnnotated(field, FIELD_ANNOTATIONS, 0)
}

/**
 * Classifies a framework-invoked method: an event handler / render-phase / page-lifecycle method on
 * a page/component/mixin class, or an IoC contribution method on a `*Module` class.
 *
 * @return the category, or null if the method is not framework-invoked.
 */
fun frameworkMethodKind(method: PsiMethod): FrameworkMethodKind? {
    val psiClass = method.containingClass ?: return null

    if (isInTapestryElementClass(psiClass)) {
        if (isEventHandlerName(method.name) || AnnotationUtil.isAnnotated(method, TapestryConstants.EVENT_ANNOTATION, 0)) {
            return FrameworkMethodKind.EVENT
        }
        if (method.name in RENDER_PHASE_NAMES || AnnotationUtil.isAnnotated(method, LIFECYCLE_ANNOTATIONS, 0)) {
            return FrameworkMethodKind.LIFECYCLE
        }
    }
    if (isTapestryModuleClass(psiClass)
        && (isIocMethodName(method.name) || AnnotationUtil.isAnnotated(method, IOC_METHOD_ANNOTATIONS, 0))
    ) {
        return FrameworkMethodKind.IOC
    }
    return null
}

// Tapestry event handlers by convention: "on" + capitalized event name, e.g. onActivate, onValidateFromForm.
private fun isEventHandlerName(name: String) =
    name.length > 2 && name.startsWith("on") && name[2].isUpperCase()

// IoC module methods invoked reflectively by the service registry.
private fun isIocMethodName(name: String) =
    name == TapestryConstants.SERVICE_AUTOBUILDER_METHOD_NAME // "bind"
            || name.startsWith(TapestryConstants.SERVICE_BUILDER_METHOD_PREFIX) // "build"
            || name.startsWith("contribute") || name.startsWith("decorate") || name.startsWith("advise")

private fun isInTapestryElementClass(psiClass: PsiClass): Boolean {
    val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return false
    val project = TapestryModuleSupportLoader.getTapestryProject(module) ?: return false
    if (project.findElement(IntellijJavaClassType(module, psiClass.containingFile)) != null) return true
    // Mixins are looked up by name, not by class, so check the mixins package directly.
    val fqn = psiClass.qualifiedName ?: return false
    val mixinsPackage = project.mixinsRootPackage ?: return false
    return fqn.startsWith("$mixinsPackage.")
}

private fun isTapestryModuleClass(psiClass: PsiClass): Boolean {
    if (psiClass.name?.endsWith("Module") != true) return false
    val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return false
    return TapestryUtils.isTapestryModule(module)
}
