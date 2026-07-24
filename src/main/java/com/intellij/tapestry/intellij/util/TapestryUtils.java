package com.intellij.tapestry.intellij.util;

import com.intellij.facet.FacetManager;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.xml.*;
import com.intellij.tapestry.core.TapestryConstants;
import com.intellij.tapestry.core.TapestryProject;
import com.intellij.tapestry.core.java.IJavaAnnotation;
import com.intellij.tapestry.core.java.IJavaField;
import com.intellij.tapestry.core.model.presentation.InjectedElement;
import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement;
import com.intellij.tapestry.core.model.presentation.TapestryComponent;
import com.intellij.tapestry.core.model.presentation.TemplateElement;
import com.intellij.tapestry.core.util.PathUtils;
import com.intellij.tapestry.intellij.TapestryModuleSupportLoader;
import com.intellij.tapestry.intellij.core.java.IntellijJavaClassType;
import com.intellij.tapestry.intellij.facet.TapestryFacetType;
import com.intellij.tapestry.intellij.lang.descriptor.TapestryXmlExtension;
import com.intellij.tapestry.lang.TmlFileType;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods related to Tapestry.
 */
public final class TapestryUtils {

  private static final Logger _logger = Logger.getInstance(TapestryUtils.class.getName());

  /**
   * Manifest attribute a Tapestry module declares to advertise its IoC module classes.
   */
  private static final String TAPESTRY_MODULE_CLASSES = "Tapestry-Module-Classes";

  /** Matches the pom.xml {@code <Tapestry-Module-Classes>...</Tapestry-Module-Classes>} manifest entry. */
  private static final Pattern POM_MODULE_CLASSES =
    Pattern.compile("<" + TAPESTRY_MODULE_CLASSES + ">(.*?)</" + TAPESTRY_MODULE_CLASSES + ">", Pattern.DOTALL);

  /**
   * Checks if a module is a Tapestry module.
   * <p>
   * A module counts as Tapestry if it has an explicit Tapestry facet, or if it declares
   * {@code Tapestry-Module-Classes} itself &mdash; in its own {@code pom.xml}
   * (maven-jar-plugin {@code manifestEntries}) or its own {@code META-INF/MANIFEST.MF}.
   * Dependency jars are deliberately not scanned, so merely depending on Tapestry
   * (whose own jars carry that attribute) does not flag a module.
   *
   * @param module the module to check.
   * @return {@code true} if the module is a Tapestry module, {@code false} otherwise.
   */
  public static boolean isTapestryModule(Module module) {
    if (module == null) {
      return false;
    }
    if (!FacetManager.getInstance(module).getFacetsByType(TapestryFacetType.ID).isEmpty()) {
      return true;
    }
    return !getDeclaredModuleClasses(module).isEmpty();
  }

  /**
   * Application root package derived from a facet-less module's declared {@code Tapestry-Module-Classes},
   * e.g. {@code de.betterbits.comp.security.services.SecurityModule} &rarr; {@code de.betterbits.comp.security}.
   * By convention the module class lives in {@code <root>.services}, so the trailing {@code .services}
   * segment is dropped. Returns {@code null} if the module declares no module class.
   */
  @Nullable
  public static String getModuleClassesRootPackage(Module module) {
    for (String moduleClass : getDeclaredModuleClasses(module)) {
      String pkg = rootPackageForModuleClass(moduleClass);
      if (pkg != null) {
        return pkg;
      }
    }
    return null;
  }

  /**
   * The application root package a Tapestry module class implies, e.g.
   * {@code de.betterbits.comp.security.services.SecurityModule} &rarr; {@code de.betterbits.comp.security}.
   * By convention the module class lives in {@code <root>.services}, so a trailing {@code .services}
   * segment is dropped. Returns {@code null} for a module class with no usable package.
   */
  @Nullable
  public static String rootPackageForModuleClass(String moduleClassFqn) {
    String pkg = StringUtil.trimEnd(StringUtil.getPackageName(moduleClassFqn), ".services");
    return pkg.isEmpty() ? null : pkg;
  }

  /** A Tapestry module contributed by a classpath library jar. {@code mavenInfo} is the library's coordinates. */
  public record LibraryModule(String mavenInfo, String moduleClass) {}

  /**
   * Tapestry modules contributed by the module's library-classpath jars &mdash; libraries whose
   * {@code META-INF/MANIFEST.MF} declares {@code Tapestry-Module-Classes}. Unlike
   * {@link #getDeclaredModuleClasses}, this looks only at dependency libraries. Cached per module.
   */
  @NotNull
  public static List<LibraryModule> getClasspathLibraryModules(final Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      List<LibraryModule> result = new ArrayList<>();
      // ponytail: mounts + reads MANIFEST.MF of every library jar on first call; cached thereafter.
      OrderEnumerator.orderEntries(module).librariesOnly().forEachLibrary(library -> {
        String mavenInfo = StringUtil.trimStart(StringUtil.notNullize(library.getName()), "Maven: ");
        for (VirtualFile root : library.getFiles(OrderRootType.CLASSES)) {
          List<String> classes = new ArrayList<>();
          readModuleClasses(root.findFileByRelativePath("META-INF/MANIFEST.MF"), true, classes);
          for (String moduleClass : classes) {
            result.add(new LibraryModule(mavenInfo, moduleClass));
          }
        }
        return true;
      });
      return CachedValueProvider.Result.create(result, ProjectRootManager.getInstance(module.getProject()));
    });
  }

  /**
   * The fully-qualified {@code Tapestry-Module-Classes} declared by the module itself &mdash; in its own
   * {@code pom.xml} (maven-jar-plugin {@code manifestEntries}) or its own {@code META-INF/MANIFEST.MF}.
   * Dependency jars are deliberately not scanned. Cached per module.
   */
  @NotNull
  public static List<String> getDeclaredModuleClasses(final Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      ModuleRootManager roots = ModuleRootManager.getInstance(module);
      List<String> classes = new ArrayList<>();
      for (VirtualFile content : roots.getContentRoots()) {
        readModuleClasses(content.findChild("pom.xml"), false, classes);
      }
      for (VirtualFile source : roots.getSourceRoots()) {
        readModuleClasses(source.findFileByRelativePath("META-INF/MANIFEST.MF"), true, classes);
      }
      // ponytail: cache keyed on project roots only; a pom.xml/manifest content edit that adds
      // the attribute refreshes on next reimport/root change, not on the keystroke.
      return CachedValueProvider.Result.create(classes, ProjectRootManager.getInstance(module.getProject()));
    });
  }

  private static void readModuleClasses(@Nullable VirtualFile file, boolean manifest, List<String> out) {
    if (file == null) {
      return;
    }
    try {
      String raw;
      if (manifest) {
        try (InputStream in = file.getInputStream()) {
          raw = new Manifest(in).getMainAttributes().getValue(TAPESTRY_MODULE_CLASSES);
        }
      }
      else {
        Matcher m = POM_MODULE_CLASSES.matcher(VfsUtilCore.loadText(file));
        raw = m.find() ? m.group(1) : null;
      }
      if (raw == null) {
        return;
      }
      for (String fqn : raw.split(",")) {
        fqn = fqn.trim();
        if (!fqn.isEmpty()) {
          out.add(fqn);
        }
      }
    }
    catch (IOException e) {
      _logger.debug("Failed to read " + file.getPath(), e);
    }
  }

  /**
   * Finds all module with Tapestry support in a project.
   *
   * @param project the project to look for Tapestry modules in.
   * @return all modules in the given project with Tapestry support.
   */
  public static Module[] getAllTapestryModules(Project project) {
    final Module[] modules = ModuleManager.getInstance(project).getModules();
    List<Module> result = new ArrayList<>();

    for (Module module : modules) {
      if (isTapestryModule(module)) {
        result.add(module);
      }
    }

    return result.toArray(Module.EMPTY_ARRAY);
  }

  /**
   * Finds the element in a Tapestry component tag that identifies the type of component.
   *
   * @param tag the component tag.
   * @return the attribute that identifies the type of component.
   */
  @Nullable
  public static XmlElement getComponentIdentifier(@Nullable final XmlTag tag) {
    return tag == null ? null : TapestryXmlExtension.isTapestryTemplateNamespace(tag.getNamespace()) // embedded components
                                ? IdeaUtils.getNameElement(tag) // using invisible instrumentation
                                : getIdentifyingAttribute(tag);
  }

  @Nullable
  public static XmlAttribute getIdentifyingAttribute(@NotNull XmlTag tag) {
    XmlAttribute typeAttribute = getTTypeAttribute(tag);
    return typeAttribute != null ? typeAttribute : getTIdAttribute(tag);
  }

  @Nullable
  public static XmlAttribute getTIdAttribute(XmlTag tag) {
    return tag.getAttribute("id", TapestryXmlExtension.getTapestryNamespace(tag));
  }

  @Nullable
  public static XmlAttribute getTTypeAttribute(XmlTag tag) {
    return tag.getAttribute("type", TapestryXmlExtension.getTapestryNamespace(tag));
  }

  /**
   * Verify the existence of parameter declaration in elementClass
   *
   * @param paramName    the parameter name to check
   * @param elementClass the class to get the fields
   * @param tag          the component to get the parameters
   * @return {@code true} if the parameter is defined in the class, {@code false} otherwise.
   */
  public static boolean parameterDefinedInClass(String paramName, IntellijJavaClassType elementClass, XmlTag tag) {

    IJavaField field = findIdentifyingField(elementClass, tag);
    if (field == null) return false;

    final IJavaAnnotation annotation = field.getAnnotations().get(TapestryConstants.COMPONENT_ANNOTATION);
    String[] fieldParameters = annotation.getParameters().get("parameters");
    if (fieldParameters == null) return false;
    for (String fieldParameter : fieldParameters) {
      final String[] paramNameValue = fieldParameter.split("=");
      if (paramNameValue.length == 2 && paramNameValue[0].equals(paramName)) return true;
    }
    return false;
  }

  @Nullable
  public static String getFieldId(IJavaField field) {
    final IJavaAnnotation annotation = field.getAnnotations().get(TapestryConstants.COMPONENT_ANNOTATION);
    if (annotation == null) return null;
    String[] fieldIds = annotation.getParameters().get("id");
    return fieldIds != null && fieldIds.length > 0 && fieldIds[0] != null && !fieldIds[0].isEmpty() ? fieldIds[0] : field.getName();
  }

  @Nullable
  public static IJavaField findIdentifyingField(XmlTag tag) {
    final TapestryProject tapestryProject = getTapestryProject(tag);
    if (tapestryProject == null) return null;
    PresentationLibraryElement element = tapestryProject.findElementByTemplate(tag.getContainingFile());
    return element != null ? findIdentifyingField((IntellijJavaClassType)element.getElementClass(), tag) : null;
  }

  @NotNull
  public static List<String> getEmbeddedComponentIds(XmlTag tag) {
    final TapestryProject tapestryProject = getTapestryProject(tag);
    if (tapestryProject == null) return Collections.emptyList();
    PresentationLibraryElement element = tapestryProject.findElementByTemplate(tag.getContainingFile());
    if (element == null) return Collections.emptyList();
    List<String> embeddedIds = new ArrayList<>();
    for (TemplateElement injectedElement : element.getEmbeddedComponents()) {
      ContainerUtil.addIfNotNull(embeddedIds, injectedElement.getElement().getElementId());
    }
    return embeddedIds;
  }

  @Nullable
  private static IJavaField findIdentifyingField(IntellijJavaClassType elementClass, XmlTag tag) {
    final String tagId = tag.getAttributeValue("id", TapestryXmlExtension.getTapestryNamespace(tag));
    if (tagId == null) return null;
    for (IJavaField field : elementClass.getFields(false).values()) {
      if (tagId.equals(getFieldId(field))) return field;
    }
    return null;
  }

  @Nullable
  public static TapestryProject getTapestryProject(PsiElement psiElement) {
    Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (module == null) return null;
    return TapestryModuleSupportLoader.getTapestryProject(module);
  }

  @Nullable
  public static XmlAttribute getTapestryAttribute(XmlTag tag, String attrName) {
    XmlAttribute attribute = tag.getAttribute(attrName, TapestryXmlExtension.getTapestryNamespace(tag));
    return attribute != null ? attribute : tag.getAttribute(attrName, "");
  }

  /**
   * Creates a new component.
   *
   * @param module                  the module to create the page in.
   * @param classSourceDirectory    the source root where to create the page class.
   * @param templateSourceDirectory the source root where to create the page template.
   * @param pageName                the page name.
   * @param replaceExistingFiles    should an existing page file be replaced.
   * @throws IllegalStateException if the page file already existed and {@code replaceExistingFiles = false}
   */
  public static void createComponent(Module module,
                                     PsiDirectory classSourceDirectory,
                                     PsiDirectory templateSourceDirectory,
                                     String pageName,
                                     boolean replaceExistingFiles) throws IllegalStateException {
    String errorMsg = "";
    try {
      createClass(classSourceDirectory, TapestryModuleSupportLoader.getTapestryProject(module).getComponentsRootPackage(), pageName,
                  replaceExistingFiles, TapestryConstants.COMPONENT_CLASS_TEMPLATE_NAME);

      if (templateSourceDirectory != null) {
        createTemplate(module, templateSourceDirectory, TapestryModuleSupportLoader.getTapestryProject(module).getComponentsRootPackage(),
                       pageName, replaceExistingFiles, TapestryConstants.COMPONENT_TEMPLATE_TEMPLATE_NAME);
      }
    }
    catch (IncorrectOperationException ex) {
      errorMsg = "An error occurred creating the component!\n\n";

      _logger.error(ex);
    }
    catch (FileAlreadyExistsException ex) {
      errorMsg = "Some component file already exists, the existing version was kept!\n\n";
    }

    if (!errorMsg.isEmpty()) {
      throw new IllegalStateException(errorMsg);
    }
  }

  /**
   * Creates a new page.
   *
   * @param module                  the module to create the page in.
   * @param classSourceDirectory    the source root where to create the page class.
   * @param templateSourceDirectory the source root where to create the page template.
   * @param pageName                the page name.
   * @param replaceExistingFiles    should an existing page file be replaced.
   * @throws IllegalStateException if the page file already existed and {@code replaceExistingFiles = false}
   */
  public static void createPage(Module module,
                                PsiDirectory classSourceDirectory,
                                PsiDirectory templateSourceDirectory,
                                String pageName,
                                boolean replaceExistingFiles) throws IllegalStateException {
    String errorMsg = "";
    try {
      createClass(classSourceDirectory, TapestryModuleSupportLoader.getTapestryProject(module).getPagesRootPackage(), pageName,
                  replaceExistingFiles, TapestryConstants.PAGE_CLASS_TEMPLATE_NAME);

      if (templateSourceDirectory != null) {
        createTemplate(module, templateSourceDirectory, TapestryModuleSupportLoader.getTapestryProject(module).getPagesRootPackage(),
                       pageName, replaceExistingFiles, TapestryConstants.PAGE_TEMPLATE_TEMPLATE_NAME);
      }
    }
    catch (IncorrectOperationException ex) {
      errorMsg = "An error occurred creating the page!\n\n";

      _logger.error(ex);
    }
    catch (FileAlreadyExistsException e) {
      errorMsg = "Some page file already exists, the existing version was kept!\n\n";
    }


    if (!errorMsg.isEmpty()) {
      throw new IllegalStateException(errorMsg);
    }
  }

  /**
   * Creates a new mixin.
   *
   * @param module               the module to create the mixin in.
   * @param classSourceDirectory the source root where to create the mixin class.
   * @param mixinName            the mixin name.
   * @param replaceExistingFiles should an existing mixin file be replaced.
   * @throws IllegalStateException if the mixin file already existed and {@code replaceExistingFiles = false}
   */
  public static void createMixin(Module module, PsiDirectory classSourceDirectory, String mixinName, boolean replaceExistingFiles)
    throws IllegalStateException {
    String errorMsg = "";
    try {
      createClass(classSourceDirectory, TapestryModuleSupportLoader.getTapestryProject(module).getMixinsRootPackage(), mixinName,
                  replaceExistingFiles, TapestryConstants.MIXIN_CLASS_TEMPLATE_NAME);
    }
    catch (IncorrectOperationException ex) {
      errorMsg = "An error occurred creating the mixin!\n\n";

      _logger.error(ex);
    }
    catch (FileAlreadyExistsException e) {
      errorMsg = "Some mixin file already exists, the existing version was kept!\n\n";
    }

    if (!errorMsg.isEmpty()) {
      throw new IllegalStateException(errorMsg);
    }
  }

  /**
   * Builds the component object that corresponds to a HTML tag.
   *
   * @param tag the component tag.
   * @return the component that the given tag represents.
   */
  @Nullable
  public static TapestryComponent getTypeOfTag(XmlTag tag) {
    return CachedValuesManager.<XmlTag, @Nullable TapestryComponent>getProjectPsiDependentCache(tag, _ -> {
      Module module = ModuleUtilCore.findModuleForPsiElement(tag);
      return module == null ? null : getTypeOfTag(module, tag);
    });
  }

  /**
   * Builds the component object that corresponds to a HTML tag.
   *
   * @param module the module to find the component in.
   * @param tag    the component tag.
   * @return the component that the given tag represents.
   */
  @Nullable
  private static TapestryComponent getTypeOfTag(@NotNull Module module, @NotNull XmlTag tag) {
    TapestryProject tapestryProject = TapestryModuleSupportLoader.getTapestryProject(module);
    if (tapestryProject == null) return null;
    XmlElement identifier = getComponentIdentifier(tag);
    if (identifier == null) return null;

    if (identifier instanceof XmlAttribute) {
      final String attrName = ((XmlAttribute)identifier).getLocalName();
      final String attrValue = ((XmlAttribute)identifier).getValue();
      if (attrValue == null) return null;
      if (attrName.equals("type")) {
        return tapestryProject.findComponent(attrValue);
      }
      if (attrName.equals("id")) {
        PresentationLibraryElement element = tapestryProject.findElementByTemplate(tag.getContainingFile());
        if (element != null) {
          for (TemplateElement embeddedComponent : element.getEmbeddedComponents()) {
            final InjectedElement element1 = embeddedComponent.getElement();
            if (attrValue.equals(element1.getElementId())) return (TapestryComponent)element1.getElement();
          }
        }
      }
      return null;
    }
    final String tagLocalName = StringUtil.toLowerCase(tag.getLocalName()).replace('.', '/');
    // element names are delimited by slashes but tag names may not contain slashes
    return tapestryProject.findComponent(tagLocalName);
  }

  /**
   * Finds the Tapestry namespace prefix declared in a template.
   *
   * @param template the template to search for the prefix;
   * @return the Tapestry namespace prefix declared in the given template or {@code null} if none is found.
   */
  @Nullable
  public static String getTapestryNamespacePrefix(XmlFile template) {
    XmlDocument doc = template.getDocument();
    if (doc == null) return null;
    final XmlTag rootTag = doc.getRootTag();
    if (rootTag == null) return null;
    for (XmlAttribute attribute : rootTag.getAttributes()) {
      if (attribute.getName().startsWith("xmlns:") &&
          TapestryXmlExtension.isTapestryTemplateNamespace(attribute.getValue())) {
        return attribute.getName().substring(6);
      }
    }
    return null;
  }

  /**
   * Creates a class.
   *
   * @param sourceDirectory      the source root where to create the class.
   * @param basePackage          the base package to create the class in.
   * @param pageName             the page name.
   * @param replaceExistingFiles should an existing class be replaced.
   * @param templateName         the name of the template to use for the class.
   * @throws FileAlreadyExistsException  if the page class already existed and {@code replaceExistingFiles = false}
   * @throws IncorrectOperationException if an error occurs creating the class.
   */
  private static void createClass(PsiDirectory sourceDirectory,
                                  String basePackage,
                                  String pageName,
                                  boolean replaceExistingFiles,
                                  String templateName) throws FileAlreadyExistsException, IncorrectOperationException {
    PsiDirectory classDirectory =
      IdeaUtils.findOrCreateDirectoryForPackage(sourceDirectory, PathUtils.getFullComponentPackage(basePackage, pageName));

    String fileName = PathUtils.getComponentFileName(pageName);
    PsiFile file = classDirectory.findFile(fileName + ".java");
    if (file != null) {
      if (!replaceExistingFiles) {
        throw new FileAlreadyExistsException();
      }
      else {
        file.delete();
      }
    }
    JavaDirectoryService.getInstance().createClass(classDirectory, PathUtils.getComponentFileName(pageName), templateName);
  }

  /**
   * Creates a template.
   *
   * @param module               the module to create the page template in.
   * @param sourceDirectory      the source root where to create the page template.
   * @param basePackage          the base package to create the template in.
   * @param pageName             the page name.
   * @param replaceExistingFiles should an existing page class be replaced.
   * @param template             the template to use.
   * @throws FileAlreadyExistsException  if the page template already existed and {@code replaceExistingFiles = false}
   * @throws IncorrectOperationException if an error occurs creating the template.
   */
  private static void createTemplate(Module module,
                                     PsiDirectory sourceDirectory,
                                     String basePackage,
                                     String pageName,
                                     boolean replaceExistingFiles,
                                     String template) throws FileAlreadyExistsException, IncorrectOperationException {
    PsiDirectory templateDirectory;
    if (IdeaUtils.isWebRoot(module, sourceDirectory.getVirtualFile())) basePackage = "";
    templateDirectory =
      IdeaUtils.findOrCreateDirectoryForPackage(sourceDirectory, PathUtils.getFullComponentPackage(basePackage, pageName));

    String fileName = PathUtils.getComponentFileName(pageName) + "." + TapestryConstants.TEMPLATE_FILE_EXTENSION;
    final PsiFile psiFile = templateDirectory.findFile(fileName);
    if (psiFile != null) {
      if (!replaceExistingFiles) throw new FileAlreadyExistsException();
      psiFile.delete();
    }

    PsiFile pageTemplate = PsiFileFactory.getInstance(module.getProject())
      .createFileFromText(fileName, TmlFileType.INSTANCE, FileTemplateManager.getInstance(module.getProject()).getInternalTemplate(template).getText());
    templateDirectory.add(pageTemplate);
  }

  static class FileAlreadyExistsException extends Exception {

  }
}
