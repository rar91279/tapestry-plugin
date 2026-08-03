package com.intellij.tapestry.intellij

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlElement
import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.intellij.core.java.IntellijJavaTypeCreator
import com.intellij.tapestry.intellij.core.java.IntellijJavaTypeFinder
import com.intellij.tapestry.intellij.core.resource.IntellijResourceFinder
import com.intellij.tapestry.lang.TmlFileType

@State(name = "Loomy")
class TapestryModuleSupportLoader(module: Module) :
    PersistentStateComponent<TapestryModuleSupportLoader.ModuleConfiguration> {

    val tapestryProject: TapestryProject = TapestryProject(
        module,
        IntellijResourceFinder(module),
        IntellijJavaTypeFinder(module),
        IntellijJavaTypeCreator(module)
    )

    private var configuration = ModuleConfiguration()

    override fun getState() = configuration

    override fun loadState(state: ModuleConfiguration) {
        configuration = state
    }

    /** The directories last used to create new elements, remembered per module. */
    class ModuleConfiguration {
        var newPagesTemplatesSourceDirectory: String? = null
        var newPagesClassesSourceDirectory: String? = null
        var newComponentsTemplatesSourceDirectory: String? = null
        var newComponentsClassesSourceDirectory: String? = null
        var newMixinsClassesSourceDirectory: String? = null
    }

    companion object {

        @JvmStatic
        fun getInstance(module: Module): TapestryModuleSupportLoader =
            module.getService(TapestryModuleSupportLoader::class.java)

        @JvmStatic
        fun getInstance(element: PsiElement): TapestryModuleSupportLoader? {
            val file = element.containingFile ?: return null
            if (file.fileType !is TmlFileType) return null
            val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
            return getInstance(module)
        }

        /** The Tapestry project associated with a module, or null if there is none. */
        @JvmStatic
        fun getTapestryProject(module: Module?): TapestryProject? =
            if (module == null || module.isDisposed) null else getInstance(module).tapestryProject

        @JvmStatic
        fun getTapestryProject(element: XmlElement): TapestryProject? = getInstance(element)?.tapestryProject
    }
}
