package com.github.rar91279.plugin.tapestry.intellij.facet

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
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
        if (configuration.applicationPackage == null || !generatePom) return

        // Loading the template and substituting into it touches no PSI and no VFS, so it is done before the
        // write action rather than under the write lock. Only the file write itself needs the lock.
        val modulePath = pomTargetPath(module) ?: return
        val pomText = try {
            renderPom(module, configuration, modulePath)
        }
        catch (ex: Exception) {
            if (ex is ControlFlowException) throw ex
            logger.warn("Failed to render the pom.xml template for module ${module.name}", ex)
            return
        }

        WriteCommandAction.writeCommandAction(module.project)
            .withName("Add Tapestry Support")
            .run<RuntimeException> {
                try {
                    writePom(module, modulePath, pomText)
                }
                catch (ex: Exception) {
                    if (ex is ControlFlowException) throw ex
                    // Failing to write pom.xml is an environment problem (permissions, read-only file), not
                    // an IDE bug — warn instead of raising a fatal-error balloon.
                    logger.warn("Failed to write pom.xml for module ${module.name}", ex)
                }
            }
    }

    /** The module content root the pom belongs in, or null if the module's roots can't be determined. */
    private fun pomTargetPath(module: Module): String? {
        val rootModel = ModuleRootManager.getInstance(module)

        if (rootModel.contentRoots.isEmpty()) {
            logger.warn("Couldn't generate pom because it wasn't possible to determine the module content root.")
            return null
        }
        if (rootModel.sourceRoots.isEmpty()) {
            logger.warn("Couldn't generate pom because it wasn't possible to determine the module source root.")
            return null
        }

        return rootModel.contentRoots[0].path
    }

    @Throws(IOException::class)
    private fun renderPom(module: Module, configuration: TapestryFacetConfiguration, modulePath: String): String {
        val rootModel = ModuleRootManager.getInstance(module)
        val template = javaClass.getResourceAsStream("/fileTemplates/j2ee/${TapestryConstants.POM_TEMPLATE_NAME}.ft")

        return FileUtil.loadTextAndClose(template)
            .replace("\${GROUP}", configuration.applicationPackage.orEmpty())
            .replace("\${ARTIFACT}", configuration.filterName.orEmpty())
            .replace("\${NAME}", module.name)
            .replace("\${SOURCE_PATH}", rootModel.sourceRoots[0].path.substring(modulePath.length + 1))
            .replace("\${TAPESTRY_VERSION}", configuration.version.toString())
    }

    @Throws(IOException::class)
    private fun writePom(module: Module, modulePath: String, pomText: String) {
        val moduleDirectory = VirtualFileManager.getInstance().findFileByUrl("file://$modulePath") ?: return
        val psiDirectory = PsiManager.getInstance(module.project).findDirectory(moduleDirectory) ?: return

        val pomFile = psiDirectory.findFile("pom.xml") ?: psiDirectory.createFile("pom.xml")
        VfsUtil.saveText(pomFile.virtualFile, pomText)
    }
}
