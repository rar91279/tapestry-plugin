package com.github.rar91279.plugin.tapestry.intellij.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetType
import com.intellij.facet.FacetTypeId
import com.intellij.framework.detection.FacetBasedFrameworkDetector
import com.intellij.framework.detection.FileContentPattern
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.patterns.ElementPattern
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.lang.TmlFileType
import com.intellij.util.indexing.FileContent
import icons.TapestryIcons
import javax.swing.Icon

class TapestryFacetType internal constructor() :
    FacetType<TapestryFacet, TapestryFacetConfiguration>(ID, "tapestry", "Tapestry") {

    override fun createDefaultConfiguration(): TapestryFacetConfiguration = TapestryFacetConfiguration()

    override fun createFacet(
        module: Module,
        name: String?,
        configuration: TapestryFacetConfiguration,
        underlyingFacet: Facet<*>?
    ): TapestryFacet = TapestryFacet(this, module, name.orEmpty(), configuration, underlyingFacet)

    override fun isSuitableModuleType(moduleType: ModuleType<*>?): Boolean = moduleType is JavaModuleType

    override fun getIcon(): Icon = TapestryIcons.Tapestry_logo_small

    /** Detects Tapestry support in a module from the presence of .tml templates. */
    class TapestryFrameworkDetector :
        FacetBasedFrameworkDetector<TapestryFacet, TapestryFacetConfiguration>("tapestry") {

        override fun getFacetType(): FacetType<TapestryFacet, TapestryFacetConfiguration> = getInstance()

        override fun getFileType(): FileType = TmlFileType

        override fun createSuitableFilePattern(): ElementPattern<FileContent> = FileContentPattern.fileContent()

        override fun setupFacet(facet: TapestryFacet, model: ModifiableRootModel) {
            val configuration = facet.configuration
            val componentDirectories = TapestryConstants.ELEMENT_PACKAGES.toSet()

            for (file in FileTypeIndex.getFiles(TmlFileType, GlobalSearchScope.moduleScope(facet.module))) {
                val parent = file.parent ?: continue
                if (parent.name !in componentDirectories) continue

                val sourceRootForFile = ProjectRootManager.getInstance(model.project).fileIndex
                    .getSourceRootForFile(parent) ?: continue

                configuration.applicationPackage = VfsUtilCore.getRelativePath(parent.parent, sourceRootForFile, '.')
                break
            }

            TapestryFrameworkSupportProvider.setupConfiguration(
                configuration, facet.module, TapestryVersion.TAPESTRY_5_3_6
            )
        }
    }

    companion object {

        @JvmField
        val ID: FacetTypeId<TapestryFacet> = FacetTypeId("tapestry")

        @JvmStatic
        fun getInstance(): TapestryFacetType = findInstance(TapestryFacetType::class.java)
    }
}
