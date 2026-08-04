package com.github.rar91279.plugin.tapestry.intellij

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils

internal class TapestryModuleRootListener : ModuleRootListener {

    override fun rootsChanged(event: ModuleRootEvent) {
        for (module in ModuleManager.getInstance(event.source as Project).modules) {
            if (!TapestryUtils.isTapestryModule(module)) continue
            TapestryModuleSupportLoader.getTapestryProject(module)?.eventsManager?.modelChanged()
        }
    }
}
