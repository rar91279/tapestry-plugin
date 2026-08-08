package com.github.rar91279.plugin.tapestry.intellij

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TapestryModuleRootListener(private val project: Project) : ModuleRootListener {

    /**
     * `rootsChanged` is delivered on the write thread, and it is exactly the event that invalidates the
     * `isTapestryModule` caches — so doing the work inline meant re-reading every module's pom.xml and
     * every dependency jar's `MANIFEST.MF` synchronously, freezing the IDE for seconds after each Maven
     * or Gradle reimport.
     *
     * The scan now runs in a background read action; only the listener notification, which reaches Swing
     * components ([com.github.rar91279.plugin.tapestry.intellij.view.TapestryProjectViewPane.reload] and
     * the tool window), is handed back to the EDT.
     */
    override fun rootsChanged(event: ModuleRootEvent) {
        project.tapestryScope.launch {
            val tapestryProjects = readAction {
                ModuleManager.getInstance(project).modules.mapNotNull { module ->
                    ProgressManager.checkCanceled()
                    if (module.isDisposed || !TapestryUtils.isTapestryModule(module)) null
                    else TapestryModuleSupportLoader.getTapestryProject(module)
                }
            }

            if (tapestryProjects.isEmpty()) return@launch

            withContext(Dispatchers.EDT) {
                tapestryProjects.forEach(TapestryProject::notifyModelChanged)
            }
        }
    }
}

private fun TapestryProject.notifyModelChanged() = eventsManager.modelChanged()
