package com.github.rar91279.plugin.tapestry.intellij.util

import com.github.rar91279.plugin.tapestry.core.util.attributeValues
import com.intellij.facet.FacetManager
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlElement
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.github.rar91279.plugin.tapestry.core.util.tapestryFields
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.core.util.PathUtils
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.facet.TapestryFacetType
import com.github.rar91279.plugin.tapestry.intellij.lang.descriptor.TapestryXmlExtension
import com.github.rar91279.plugin.tapestry.lang.TmlFileType
import com.intellij.util.IncorrectOperationException
import java.io.IOException
import java.util.jar.Manifest

/**
 * Utility methods related to Tapestry.
 */
object TapestryUtils {

    private val logger = Logger.getInstance(TapestryUtils::class.java)

    /** Manifest attribute a Tapestry module declares to advertise its IoC module classes. */
    private const val TAPESTRY_MODULE_CLASSES = "Tapestry-Module-Classes"

    /** Matches the pom.xml `<Tapestry-Module-Classes>...</Tapestry-Module-Classes>` manifest entry. */
    private val POM_MODULE_CLASSES =
        Regex("<$TAPESTRY_MODULE_CLASSES>(.*?)</$TAPESTRY_MODULE_CLASSES>", RegexOption.DOT_MATCHES_ALL)

    /**
     * Checks if a module is a Tapestry module.
     *
     * A module counts as Tapestry if it has an explicit Tapestry facet, or if it declares
     * `Tapestry-Module-Classes` itself — in its own `pom.xml` (maven-jar-plugin `manifestEntries`)
     * or its own `META-INF/MANIFEST.MF`. Dependency jars are deliberately not scanned, so merely
     * depending on Tapestry (whose own jars carry that attribute) does not flag a module.
     */
    fun isTapestryModule(module: Module?): Boolean {
        if (module == null) return false
        if (FacetManager.getInstance(module).getFacetsByType(TapestryFacetType.ID).isNotEmpty()) return true

        return getDeclaredModuleClasses(module).isNotEmpty()
    }

    /**
     * Application root package derived from a facet-less module's declared `Tapestry-Module-Classes`,
     * e.g. `de.betterbits.comp.security.services.SecurityModule` → `de.betterbits.comp.security`.
     * By convention the module class lives in `<root>.services`, so the trailing `.services`
     * segment is dropped. Returns `null` if the module declares no module class.
     */
    fun getModuleClassesRootPackage(module: Module): String? =
        getDeclaredModuleClasses(module).firstNotNullOfOrNull { rootPackageForModuleClass(it) }

    /**
     * The application root package a Tapestry module class implies, e.g.
     * `de.betterbits.comp.security.services.SecurityModule` → `de.betterbits.comp.security`.
     * Returns `null` for a module class with no usable package.
     */
    fun rootPackageForModuleClass(moduleClassFqn: String): String? =
        StringUtil.trimEnd(StringUtil.getPackageName(moduleClassFqn), ".services").ifEmpty { null }

    /** A Tapestry module contributed by a classpath library jar. [mavenInfo] is the library's coordinates. */
    data class LibraryModule(val mavenInfo: String, val moduleClass: String)

    /**
     * Tapestry modules contributed by the module's library-classpath jars — libraries whose
     * `META-INF/MANIFEST.MF` declares `Tapestry-Module-Classes`. Unlike [getDeclaredModuleClasses],
     * this looks only at dependency libraries. Cached per module.
     */
    fun getClasspathLibraryModules(module: Module): List<LibraryModule> =
        CachedValuesManager.getManager(module.project).getCachedValue(module) {
            val result = ArrayList<LibraryModule>()
            // ponytail: mounts + reads MANIFEST.MF of every library jar on first call; cached thereafter.
            OrderEnumerator.orderEntries(module).librariesOnly().forEachLibrary { library ->
                val mavenInfo = StringUtil.trimStart(StringUtil.notNullize(library.name), "Maven: ")
                for (root in library.getFiles(OrderRootType.CLASSES)) {
                    readModuleClasses(root.findFileByRelativePath("META-INF/MANIFEST.MF"), true)
                        .forEach { result.add(LibraryModule(mavenInfo, it)) }
                }
                true
            }
            CachedValueProvider.Result.create(result, ProjectRootManager.getInstance(module.project))
        }

    /**
     * The fully-qualified `Tapestry-Module-Classes` declared by the module itself — in its own
     * `pom.xml` (maven-jar-plugin `manifestEntries`) or its own `META-INF/MANIFEST.MF`.
     * Dependency jars are deliberately not scanned. Cached per module.
     */
    fun getDeclaredModuleClasses(module: Module): List<String> =
        CachedValuesManager.getManager(module.project).getCachedValue(module) {
            val roots = ModuleRootManager.getInstance(module)
            val classes = ArrayList<String>()

            for (content in roots.contentRoots) {
                classes.addAll(readModuleClasses(content.findChild("pom.xml"), false))
            }
            for (source in roots.sourceRoots) {
                classes.addAll(readModuleClasses(source.findFileByRelativePath("META-INF/MANIFEST.MF"), true))
            }

            // ponytail: cache keyed on project roots only; a pom.xml/manifest content edit that adds
            // the attribute refreshes on next reimport/root change, not on the keystroke.
            CachedValueProvider.Result.create(classes, ProjectRootManager.getInstance(module.project))
        }

    private fun readModuleClasses(file: VirtualFile?, manifest: Boolean): List<String> {
        if (file == null) return emptyList()

        return try {
            val raw = if (manifest) {
                file.inputStream.use { Manifest(it).mainAttributes.getValue(TAPESTRY_MODULE_CLASSES) }
            }
            else {
                POM_MODULE_CLASSES.find(VfsUtilCore.loadText(file))?.groupValues?.get(1)
            }

            raw?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        }
        catch (e: IOException) {
            logger.debug("Failed to read ${file.path}", e)
            emptyList()
        }
    }

    /**
     * @return all modules in the given project with Tapestry support.
     */
    fun getAllTapestryModules(project: Project): Array<Module> =
        ModuleManager.getInstance(project).modules.filter { isTapestryModule(it) }.toTypedArray()

    /**
     * @return the element in a Tapestry component tag that identifies the type of component.
     */
    fun getComponentIdentifier(tag: XmlTag?): XmlElement? = when {
        tag == null -> null
        // embedded components using invisible instrumentation
        TapestryXmlExtension.isTapestryTemplateNamespace(tag.namespace) -> IdeaUtils.getNameElement(tag)
        else -> getIdentifyingAttribute(tag)
    }

    fun getIdentifyingAttribute(tag: XmlTag): XmlAttribute? = getTTypeAttribute(tag) ?: getTIdAttribute(tag)

    fun getTIdAttribute(tag: XmlTag): XmlAttribute? =
        tag.getAttribute("id", TapestryXmlExtension.getTapestryNamespace(tag))

    fun getTTypeAttribute(tag: XmlTag): XmlAttribute? =
        tag.getAttribute("type", TapestryXmlExtension.getTapestryNamespace(tag))

    /**
     * Verifies the existence of a parameter declaration in elementClass.
     *
     * @return `true` if the parameter is defined in the class, `false` otherwise.
     */
    fun parameterDefinedInClass(paramName: String, elementClass: PsiClass, tag: XmlTag): Boolean {
        val field = findIdentifyingField(elementClass, tag) ?: return false
        val annotation = field.getAnnotation(TapestryConstants.COMPONENT_ANNOTATION) ?: return false
        val fieldParameters = annotation.attributeValues("parameters")

        return fieldParameters.any { it.split("=").let { parts -> parts.size == 2 && parts[0] == paramName } }
    }

    fun getFieldId(field: PsiField): String? {
        val annotation = field.getAnnotation(TapestryConstants.COMPONENT_ANNOTATION) ?: return null

        return annotation.attributeValues("id").firstOrNull()?.ifEmpty { null } ?: field.name
    }

    fun findIdentifyingField(tag: XmlTag): PsiField? {
        val element = getTapestryProject(tag)?.findElementByTemplate(tag.containingFile) ?: return null

        return findIdentifyingField(element.elementClass ?: return null, tag)
    }

    fun getEmbeddedComponentIds(tag: XmlTag): List<String> {
        val element = getTapestryProject(tag)?.findElementByTemplate(tag.containingFile) ?: return emptyList()

        return element.embeddedComponents.mapNotNull { it.element?.elementId }
    }

    private fun findIdentifyingField(elementClass: PsiClass, tag: XmlTag): PsiField? {
        val tagId = tag.getAttributeValue("id", TapestryXmlExtension.getTapestryNamespace(tag)) ?: return null

        return elementClass.tapestryFields(false).values.firstOrNull { tagId == getFieldId(it) }
    }

    fun getTapestryProject(psiElement: PsiElement): TapestryProject? =
        TapestryModuleSupportLoader.getTapestryProject(ModuleUtilCore.findModuleForPsiElement(psiElement))

    fun getTapestryAttribute(tag: XmlTag, attrName: String): XmlAttribute? =
        tag.getAttribute(attrName, TapestryXmlExtension.getTapestryNamespace(tag)) ?: tag.getAttribute(attrName, "")

    /**
     * Creates a new component.
     *
     * @throws IllegalStateException if the component file already existed and `replaceExistingFiles = false`
     */
    @Throws(IllegalStateException::class)
    fun createComponent(
        module: Module,
        classSourceDirectory: PsiDirectory,
        templateSourceDirectory: PsiDirectory?,
        pageName: String,
        replaceExistingFiles: Boolean
    ) {
        val rootPackage = TapestryModuleSupportLoader.getTapestryProject(module)?.componentsRootPackage

        create("component") {
            createClass(
                classSourceDirectory, rootPackage, pageName, replaceExistingFiles,
                TapestryConstants.COMPONENT_CLASS_TEMPLATE_NAME
            )

            if (templateSourceDirectory != null) {
                createTemplate(
                    module, templateSourceDirectory, rootPackage, pageName, replaceExistingFiles,
                    TapestryConstants.COMPONENT_TEMPLATE_TEMPLATE_NAME
                )
            }
        }
    }

    /**
     * Creates a new page.
     *
     * @throws IllegalStateException if the page file already existed and `replaceExistingFiles = false`
     */
    @Throws(IllegalStateException::class)
    fun createPage(
        module: Module,
        classSourceDirectory: PsiDirectory,
        templateSourceDirectory: PsiDirectory?,
        pageName: String,
        replaceExistingFiles: Boolean
    ) {
        val rootPackage = TapestryModuleSupportLoader.getTapestryProject(module)?.pagesRootPackage

        create("page") {
            createClass(
                classSourceDirectory, rootPackage, pageName, replaceExistingFiles,
                TapestryConstants.PAGE_CLASS_TEMPLATE_NAME
            )

            if (templateSourceDirectory != null) {
                createTemplate(
                    module, templateSourceDirectory, rootPackage, pageName, replaceExistingFiles,
                    TapestryConstants.PAGE_TEMPLATE_TEMPLATE_NAME
                )
            }
        }
    }

    /**
     * Creates a new mixin.
     *
     * @throws IllegalStateException if the mixin file already existed and `replaceExistingFiles = false`
     */
    @Throws(IllegalStateException::class)
    fun createMixin(module: Module, classSourceDirectory: PsiDirectory, mixinName: String, replaceExistingFiles: Boolean) {
        val rootPackage = TapestryModuleSupportLoader.getTapestryProject(module)?.mixinsRootPackage

        create("mixin") {
            createClass(
                classSourceDirectory, rootPackage, mixinName, replaceExistingFiles,
                TapestryConstants.MIXIN_CLASS_TEMPLATE_NAME
            )
        }
    }

    /** Runs an element creation, translating its failures into an [IllegalStateException] for the caller to show. */
    private fun create(elementKind: String, creation: () -> Unit) {
        val errorMsg = try {
            creation()
            return
        }
        catch (ex: IncorrectOperationException) {
            logger.error(ex)
            "An error occurred creating the $elementKind!\n\n"
        }
        catch (ex: FileAlreadyExistsException) {
            "Some $elementKind file already exists, the existing version was kept!\n\n"
        }

        throw IllegalStateException(errorMsg)
    }

    /**
     * @return the component that the given tag represents.
     */
    fun getTypeOfTag(tag: XmlTag): TapestryComponent? =
        CachedValuesManager.getProjectPsiDependentCache(tag) {
            ModuleUtilCore.findModuleForPsiElement(tag)?.let { getTypeOfTag(it, tag) }
        }

    private fun getTypeOfTag(module: Module, tag: XmlTag): TapestryComponent? {
        val tapestryProject = TapestryModuleSupportLoader.getTapestryProject(module) ?: return null
        val identifier = getComponentIdentifier(tag) ?: return null

        if (identifier is XmlAttribute) {
            val attrValue = identifier.value ?: return null

            if (identifier.localName == "type") return tapestryProject.findComponent(attrValue)

            if (identifier.localName == "id") {
                val element = tapestryProject.findElementByTemplate(tag.containingFile)
                element?.embeddedComponents?.forEach { embedded ->
                    val injected = embedded.element
                    if (attrValue == injected?.elementId) return injected.element as TapestryComponent?
                }
            }
            return null
        }

        // element names are delimited by slashes but tag names may not contain slashes
        return tapestryProject.findComponent(tag.localName.lowercase().replace('.', '/'))
    }

    /**
     * @return the Tapestry namespace prefix declared in the given template, or `null` if none is found.
     */
    fun getTapestryNamespacePrefix(template: XmlFile): String? {
        val rootTag = template.document?.rootTag ?: return null

        return rootTag.attributes
            .firstOrNull { it.name.startsWith("xmlns:") && TapestryXmlExtension.isTapestryTemplateNamespace(it.value) }
            ?.name?.substring("xmlns:".length)
    }

    /**
     * Creates the class of a new element from a file template.
     *
     * @throws FileAlreadyExistsException if the class already existed and `replaceExistingFiles = false`
     * @throws IncorrectOperationException if an error occurs creating the class.
     */
    private fun createClass(
        sourceDirectory: PsiDirectory,
        basePackage: String?,
        pageName: String,
        replaceExistingFiles: Boolean,
        templateName: String
    ) {
        val classDirectory =
            IdeaUtils.findOrCreateDirectoryForPackage(sourceDirectory, PathUtils.getFullComponentPackage(basePackage, pageName))

        val fileName = PathUtils.getLastPathElement(pageName)
        classDirectory.findFile("$fileName.java")?.let {
            if (!replaceExistingFiles) throw FileAlreadyExistsException()
            it.delete()
        }

        JavaDirectoryService.getInstance().createClass(classDirectory, fileName, templateName)
    }

    /**
     * Creates the template of a new element from a file template.
     *
     * @throws FileAlreadyExistsException if the template already existed and `replaceExistingFiles = false`
     * @throws IncorrectOperationException if an error occurs creating the template.
     */
    private fun createTemplate(
        module: Module,
        sourceDirectory: PsiDirectory,
        basePackage: String?,
        pageName: String,
        replaceExistingFiles: Boolean,
        template: String
    ) {
        val packageName = if (IdeaUtils.isWebRoot(module, sourceDirectory.virtualFile)) "" else basePackage
        val templateDirectory =
            IdeaUtils.findOrCreateDirectoryForPackage(sourceDirectory, PathUtils.getFullComponentPackage(packageName, pageName))

        val fileName = PathUtils.getLastPathElement(pageName) + "." + TapestryConstants.TEMPLATE_FILE_EXTENSION
        templateDirectory.findFile(fileName)?.let {
            if (!replaceExistingFiles) throw FileAlreadyExistsException()
            it.delete()
        }

        val pageTemplate = PsiFileFactory.getInstance(module.project).createFileFromText(
            fileName, TmlFileType,
            FileTemplateManager.getInstance(module.project).getInternalTemplate(template).text
        )
        templateDirectory.add(pageTemplate)
    }

    private class FileAlreadyExistsException : Exception()
}
