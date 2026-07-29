package com.intellij.tapestry.intellij;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.tapestry.core.TapestryConstants;
import com.intellij.tapestry.core.TapestryProject;
import com.intellij.tapestry.intellij.core.java.IntellijJavaClassType;
import com.intellij.tapestry.intellij.util.TapestryUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class TapestryImplicitUsageProvider implements ImplicitUsageProvider {

  private static final String ANNOTATIONS = "org.apache.tapestry5.annotations.";
  private static final String IOC_ANNOTATIONS = "org.apache.tapestry5.ioc.annotations.";

  /** Render-phase and page-lifecycle method annotations invoked by the framework on pages/components/mixins. */
  private static final List<String> LIFECYCLE_ANNOTATIONS = List.of(
    ANNOTATIONS + "SetupRender", ANNOTATIONS + "BeginRender", ANNOTATIONS + "BeforeRenderTemplate",
    ANNOTATIONS + "BeginRenderBody", ANNOTATIONS + "AfterRenderBody", ANNOTATIONS + "AfterRenderTemplate",
    ANNOTATIONS + "AfterRender", ANNOTATIONS + "CleanupRender",
    ANNOTATIONS + "PageLoaded", ANNOTATIONS + "PageAttached", ANNOTATIONS + "PageDetached");

  /** Render-phase methods are also recognized by name (annotation optional in Tapestry). */
  private static final Set<String> RENDER_PHASE_NAMES = Set.of(
    "setupRender", "beginRender", "beforeRenderTemplate", "beginRenderBody",
    "afterRenderBody", "afterRenderTemplate", "afterRender", "cleanupRender");

  /** Fields set (and read) by the framework, so never "unused" or "unassigned". */
  private static final List<String> FIELD_ANNOTATIONS = List.of(
    TapestryConstants.CORE_INJECT_ANNOTATION, IOC_ANNOTATIONS + "Inject",
    TapestryConstants.COMPONENT_ANNOTATION, TapestryConstants.PROPERTY_ANNOTATION,
    TapestryConstants.INJECT_PAGE_ANNOTATION, TapestryConstants.MIXIN_ANNOTATION,
    ANNOTATIONS + "Parameter", ANNOTATIONS + "InjectComponent", ANNOTATIONS + "InjectContainer",
    ANNOTATIONS + "Environmental", ANNOTATIONS + "Persist", ANNOTATIONS + "PageActivationContext",
    ANNOTATIONS + "SessionState", ANNOTATIONS + "ApplicationState", ANNOTATIONS + "SessionAttribute",
    ANNOTATIONS + "ActivationRequestParameter");

  /** IoC module method annotations (invoked reflectively by the registry). */
  private static final List<String> IOC_METHOD_ANNOTATIONS = List.of(
    IOC_ANNOTATIONS + "Contribute", IOC_ANNOTATIONS + "Startup", IOC_ANNOTATIONS + "Advise");

  /** Category of a framework-invoked method, used to pick a gutter icon. */
  public enum FrameworkMethodKind { EVENT, LIFECYCLE, IOC }

  /** The annotation FQNs that define a given method kind (for tooltip javadoc lookup). */
  public static List<String> annotationsForKind(@NotNull FrameworkMethodKind kind) {
    return switch (kind) {
      case EVENT -> List.of(TapestryConstants.EVENT_ANNOTATION);
      case LIFECYCLE -> LIFECYCLE_ANNOTATIONS;
      case IOC -> IOC_METHOD_ANNOTATIONS;
    };
  }

  @Override
  public boolean isImplicitUsage(@NotNull PsiElement element) {
    if (element instanceof PsiMethod) return frameworkMethodKind((PsiMethod)element) != null;
    if (element instanceof PsiField) return isFrameworkField((PsiField)element);
    return false;
  }

  @Override
  public boolean isImplicitRead(@NotNull PsiElement element) {
    return false;
  }

  @Override
  public boolean isImplicitWrite(@NotNull PsiElement element) {
    return element instanceof PsiField && isFrameworkField((PsiField)element);
  }

  private static boolean isFrameworkField(PsiField field) {
    return AnnotationUtil.isAnnotated(field, FIELD_ANNOTATIONS, 0);
  }

  /**
   * Classifies a framework-invoked method: an event handler / render-phase / page-lifecycle method on
   * a page/component/mixin class, or an IoC contribution method on a {@code *Module} class.
   *
   * @return the category, or {@code null} if the method is not framework-invoked.
   */
  public static FrameworkMethodKind frameworkMethodKind(@NotNull PsiMethod method) {
    PsiClass psiClass = method.getContainingClass();
    if (psiClass == null) return null;

    if (isInTapestryElementClass(psiClass)) {
      if (isEventHandlerName(method.getName())
          || AnnotationUtil.isAnnotated(method, TapestryConstants.EVENT_ANNOTATION, 0)) {
        return FrameworkMethodKind.EVENT;
      }
      if (RENDER_PHASE_NAMES.contains(method.getName())
          || AnnotationUtil.isAnnotated(method, LIFECYCLE_ANNOTATIONS, 0)) {
        return FrameworkMethodKind.LIFECYCLE;
      }
    }
    if (isTapestryModuleClass(psiClass)
        && (isIocMethodName(method.getName()) || AnnotationUtil.isAnnotated(method, IOC_METHOD_ANNOTATIONS, 0))) {
      return FrameworkMethodKind.IOC;
    }
    return null;
  }

  // Tapestry event handlers by convention: "on" + capitalized event name, e.g. onActivate, onValidateFromForm.
  private static boolean isEventHandlerName(String name) {
    return name.length() > 2 && name.startsWith("on") && Character.isUpperCase(name.charAt(2));
  }

  // IoC module methods invoked reflectively by the service registry.
  private static boolean isIocMethodName(String name) {
    return name.equals(TapestryConstants.SERVICE_AUTOBUILDER_METHOD_NAME) // "bind"
           || name.startsWith(TapestryConstants.SERVICE_BUILDER_METHOD_PREFIX) // "build"
           || name.startsWith("contribute") || name.startsWith("decorate") || name.startsWith("advise");
  }

  private static boolean isInTapestryElementClass(PsiClass psiClass) {
    Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
    if (module == null) return false;
    TapestryProject project = TapestryModuleSupportLoader.getTapestryProject(module);
    if (project == null) return false;
    if (project.findElement(new IntellijJavaClassType(module, psiClass.getContainingFile())) != null) return true;
    // Mixins are looked up by name, not by class, so check the mixins package directly.
    String fqn = psiClass.getQualifiedName();
    String mixinsPackage = project.getMixinsRootPackage();
    return fqn != null && mixinsPackage != null && fqn.startsWith(mixinsPackage + ".");
  }

  private static boolean isTapestryModuleClass(PsiClass psiClass) {
    String name = psiClass.getName();
    if (name == null || !name.endsWith("Module")) return false;
    Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
    return module != null && TapestryUtils.isTapestryModule(module);
  }
}
