package com.github.rar91279.plugin.tapestry.intellij.facet

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.maven.MavenConfiguration
import com.github.rar91279.plugin.tapestry.core.maven.MavenUtils
import java.io.IOException

/**
 * Adds Tapestry support (currently: a generated pom.xml) to a module.
 */
object AddTapestrySupportUtil {

    private val logger = Logger.getInstance(AddTapestrySupportUtil::class.java)

    fun addSupportInWriteCommandAction(
        module: Module,
        configuration: TapestryFacetConfiguration,
        generatePom: Boolean
    ) {
        if (configuration.applicationPackage == null) return

        WriteCommandAction.writeCommandAction(module.project).run<RuntimeException> {
            try {
                if (generatePom) generatePom(module, configuration)
            }
            catch (ex: Exception) {
                logger.error(ex)
            }
        }
    }

    @Throws(IOException::class)
    private fun generatePom(module: Module, configuration: TapestryFacetConfiguration) {
        val rootModel = ModuleRootManager.getInstance(module)
        val contentRoots = rootModel.contentRoots

        if (contentRoots.isEmpty()) {
            logger.warn("Couldn't generate pom because it wasn't possible to determine the module content root.")
            return
        }
        if (rootModel.sourceRoots.isEmpty()) {
            logger.warn("Coulnd't generate startup application because it wasn't possible to determine module source root")
            return
        }

        val modulePath = contentRoots[0].path

        try {
            val template = javaClass.getResourceAsStream("/fileTemplates/j2ee/${TapestryConstants.POM_TEMPLATE_NAME}.ft")
            val pomText = FileUtil.loadTextAndClose(template)
                .replace("\${GROUP}", configuration.applicationPackage.orEmpty())
                .replace("\${ARTIFACT}", configuration.filterName.orEmpty())
                .replace("\${NAME}", module.name)
                .replace("\${SOURCE_PATH}", rootModel.sourceRoots[0].path.substring(modulePath.length + 1))
                .replace("\${TAPESTRY_VERSION}", configuration.version.toString())

            val moduleDirectory = VirtualFileManager.getInstance().findFileByUrl("file://$modulePath")
            val psiDirectory = moduleDirectory?.let { PsiManager.getInstance(module.project).findDirectory(it) }

            if (psiDirectory != null) {
                val pomFile = psiDirectory.findFile("pom.xml") ?: psiDirectory.createFile("pom.xml")
                VfsUtil.saveText(pomFile.virtualFile, pomText)
            }
        }
        catch (ex: Exception) {
            logger.error(ex)
        }

        MavenUtils.createMavenSupport(
            modulePath,
            MavenConfiguration(
                false, false, null, null, null,
                configuration.applicationPackage, configuration.filterName, "1.0-SNAPSHOT", emptyList()
            ),
            configuration.version.toString()
        )
    }
}
