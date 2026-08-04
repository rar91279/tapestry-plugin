package com.intellij.tapestry.tests

import com.intellij.facet.FacetManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.tapestry.intellij.TapestryModuleSupportLoader
import com.intellij.tapestry.intellij.facet.TapestryFacet
import com.intellij.tapestry.intellij.facet.TapestryFacetType
import com.intellij.tapestry.intellij.util.TapestryUtils
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory
import com.intellij.testFramework.fixtures.TestFixtureBuilder
import junit.framework.Assert
import java.io.File

/**
 * @author Alexey Chmutov
 */
abstract class TapestryBaseTestCase : UsefulTestCase() {

    protected val TEST_APPLICATION_PACKAGE = "com.testapp"
    protected val COMPONENTS = "components"
    protected val ABSTRACT_COMPONENTS = "base"
    protected val PAGES = "pages"
    protected val MIXINS = "mixins"
    protected val COMPONENTS_PACKAGE_PATH = TEST_APPLICATION_PACKAGE.replace('.', '/') + "/" + COMPONENTS + "/"
    protected val ABSTRACT_COMPONENTS_PACKAGE_PATH = TEST_APPLICATION_PACKAGE.replace('.', '/') + "/" + ABSTRACT_COMPONENTS + "/"
    protected val MIXINS_PACKAGE_PATH = TEST_APPLICATION_PACKAGE.replace('.', '/') + "/" + MIXINS + "/"
    protected val PAGES_PACKAGE_PATH = TEST_APPLICATION_PACKAGE.replace('.', '/') + "/" + PAGES + "/"

    protected abstract fun getBasePath(): String

    protected fun getTestDataPath(): String = Util.getCommonTestDataPath() + getBasePath()

    protected lateinit var myFixture: CodeInsightTestFixture
    protected var myModule: Module? = null

    protected open fun getModuleFixtureBuilderClass(): Class<out JavaModuleFixtureBuilder<*>> =
        JavaModuleFixtureBuilder::class.java

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        val projectBuilder: TestFixtureBuilder<IdeaProjectTestFixture> = JavaTestFixtureFactory.createFixtureBuilder(name)
        val moduleBuilder = projectBuilder.addModule(getModuleFixtureBuilderClass())
        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(projectBuilder.fixture)
        myFixture.testDataPath = getTestDataPath()
        // Allow the whole project dir: covers src/test/testData/libs jars and the
        // .intellijPlatform sandbox (kotlin-stdlib etc.) pulled in when resolving .kt pages.
        VfsRootAccess.allowRootAccess(testRootDisposable, File("").absoluteFile.path)
        configureModule(moduleBuilder)

        myFixture.setUp()
        myModule = moduleBuilder.fixture.module

        createFacet()
    }

    @Throws(Exception::class)
    override fun tearDown() {
        try {
            myFixture.tearDown()
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            myModule = null
            super.tearDown()
        }
    }

    protected fun createFacet(): TapestryFacet {
        return WriteCommandAction.runWriteCommandAction(myFixture.project, Computable {
            val facetType = TapestryFacetType.getInstance()
            val facetManager = FacetManager.getInstance(myModule!!)
            val facet = facetManager.addFacet(facetType, facetType.presentableName, null)
            facet.configuration.applicationPackage = TEST_APPLICATION_PACKAGE
            Assert.assertNotNull(facetManager.getFacetByType(TapestryFacetType.ID))
            Assert.assertTrue("Not Tapestry module", TapestryUtils.isTapestryModule(myModule!!))
            Assert.assertNotNull("No TapestryModuleSupportLoader", TapestryModuleSupportLoader.getInstance(myModule!!))
            val tapestryProject = TapestryModuleSupportLoader.getTapestryProject(myModule)
            Assert.assertNotNull("No TapestryProject", tapestryProject)
            Assert.assertNotNull(tapestryProject!!.applicationRootPackage)
            Assert.assertNotNull(tapestryProject.applicationLibrary)
            facet
        })
    }

    protected open fun configureModule(moduleBuilder: JavaModuleFixtureBuilder<*>) {
        moduleBuilder.addContentRoot(myFixture.tempDirPath)
        moduleBuilder.addSourceRoot("")
        // The legacy MockJdkLevel.jdk15 mock JDK no longer resolves java.lang/java.util members on 2026.2,
        // so EL/property resolution against String/Date breaks. Use the real JDK the tests run on.
        moduleBuilder.addJdk(System.getProperty("java.home"))
        addTapestryLibraries(moduleBuilder)
    }

    protected open fun addTapestryLibraries(moduleBuilder: JavaModuleFixtureBuilder<*>) {
        moduleBuilder.addLibraryJars(
            "tapestry_5.1.0.5", Util.getCommonTestDataPath() + "libs", "antlr-runtime-3.1.1.jar", "commons-codec.jar",
            "javassist.jar", "log4j.jar", "slf4j-api.jar", "slf4j-log4j12.jar", "stax2.jar",
            "tapestry5-annotations.jar", "tapestry-core.jar", "tapestry-ioc.jar", "wstx-asl.jar"
        )
    }

    protected fun getElementTagName(): String = "t:" + getLowerCaseElementName()

    protected fun getLowerCaseElementName(): String = getElementName().lowercase()

    protected fun getElementName(): String = getTestName(false)

    protected fun getElementClassFileName(): String = getElementName() + getComponentClassExtension()

    protected open fun getComponentClassExtension(): String = Util.DOT_JAVA

    protected fun getAuxClassExtension(): String = Util.DOT_JAVA

    protected open fun getTemplateExtension(): String = Util.DOT_TML

    protected fun getElementTemplateFileName(): String = getElementName() + getTemplateExtension()

    protected fun initByComponent() {
        initByComponent(true)
    }

    protected fun initByComponent(configureByTmlNotJava: Boolean): VirtualFile {
        val javaFile = copyOrCreateComponentClassFile()
        val tmlName = getElementTemplateFileName()

        val copyTmlFile = configureByTmlNotJava || File(myFixture.testDataPath + "/" + tmlName).exists()
        val tmlFile = if (copyTmlFile) myFixture.copyFileToProject(tmlName, COMPONENTS_PACKAGE_PATH + tmlName) else null
        val result = if (configureByTmlNotJava) tmlFile!! else javaFile
        myFixture.configureFromExistingVirtualFile(result)
        return result
    }

    protected fun checkResultByFile() {
        val afterFileName = getElementName() + Util.AFTER + getTemplateExtension()
        myFixture.checkResultByFile(afterFileName)
    }

    protected fun getFileByPath(filePath: String): File = File(myFixture.testDataPath + "/" + filePath)

    protected fun copyOrCreateComponentClassFile(): VirtualFile {
        val existingComponentClassFile = getExistingComponentClassFileName()
        val targetPath = COMPONENTS_PACKAGE_PATH + getElementClassFileName()
        val destFile: VirtualFile?
        if (existingComponentClassFile != null) {
            destFile = myFixture.copyFileToProject(existingComponentClassFile, targetPath)
        } else {
            addFileAndAllowTreeAccess(
                targetPath,
                "package " + TEST_APPLICATION_PACKAGE + "." + COMPONENTS + "; public class " + getElementName() + " {}"
            )
            val ioFile = File(myFixture.tempDirPath + "/" + targetPath)
            destFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile)
        }
        Assert.assertNotNull(destFile)
        return destFile!!
    }

    protected open fun getExistingComponentClassFileName(): String? = checkTestDataFileExists(getElementClassFileName())

    protected fun checkTestDataFileExists(fileName: String): String? =
        if (getFileByPath(fileName).exists()) fileName else null

    protected fun addComponentToProject(className: String) {
        addElementToProject(COMPONENTS_PACKAGE_PATH, className, getAuxClassExtension())
    }

    protected fun addAbstractComponentToProject(className: String) {
        addElementToProject(ABSTRACT_COMPONENTS_PACKAGE_PATH, className, getAuxClassExtension())
    }

    protected fun addMixinToProject(className: String) {
        addElementToProject(MIXINS_PACKAGE_PATH, className, getAuxClassExtension())
    }

    protected fun addPageToProject(className: String): VirtualFile {
        addElementToProject(PAGES_PACKAGE_PATH, className, getAuxClassExtension())
        return addElementToProject(PAGES_PACKAGE_PATH, className, getTemplateExtension())
    }

    protected fun addElementToProject(relativePath: String, className: String, ext: String): VirtualFile {
        var relPath = relativePath
        var name = className
        val afterDotIndex = name.lastIndexOf('.')
        val fileText: String
        if (afterDotIndex != -1) { // we want the element to be placed in the subpackage
            val subpackage = name.substring(0, afterDotIndex)
            relPath += subpackage.replace('.', '/') + '/'
            name = name.substring(afterDotIndex + 1)
            var text = Util.getCommonTestDataFileText(name + ext)
            if (text.startsWith("package " + TEST_APPLICATION_PACKAGE)) {
                val toPasteSubpackageIndex = text.indexOf(';')
                text = text.substring(0, toPasteSubpackageIndex) + '.' + subpackage + text.substring(toPasteSubpackageIndex)
            }
            fileText = text
        } else {
            fileText = Util.getCommonTestDataFileText(name + ext)
        }
        return addFileAndAllowTreeAccess(relPath + name + ext, fileText)
    }

    private fun addFileAndAllowTreeAccess(targetPath: String, fileText: String): VirtualFile {
        val file = myFixture.addFileToProject(targetPath, fileText)
        Assert.assertNotNull(file)
        val virtualFile = file.virtualFile
        Assert.assertNotNull(virtualFile)
        return virtualFile!!
    }

    protected fun getReferenceAtCaretPosition(): PsiReference? =
        myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)

    protected fun resolveReferenceAtCaretPosition(): PsiElement {
        val ref = getReferenceAtCaretPosition()
        Assert.assertNotNull("No reference at caret", ref)
        val element = ref!!.resolve()
        Assert.assertNotNull("unresolved reference '" + ref.canonicalText + "'", element)
        return element!!
    }
}
