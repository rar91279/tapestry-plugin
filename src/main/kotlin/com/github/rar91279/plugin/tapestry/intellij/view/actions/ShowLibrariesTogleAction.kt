package com.github.rar91279.plugin.tapestry.intellij.view.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.ToggleAction

abstract class ShowLibrariesTogleAction :
    ToggleAction("Show Libraries", "Show/Hide Tapestry Libraries", AllIcons.Nodes.PpLibFolder) {

    /** Implementations only read a plain flag off the view pane. */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
