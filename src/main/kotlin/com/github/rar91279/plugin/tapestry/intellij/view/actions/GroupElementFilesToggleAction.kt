package com.github.rar91279.plugin.tapestry.intellij.view.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.ToggleAction
import icons.TapestryIcons

abstract class GroupElementFilesToggleAction :
    ToggleAction("Group Element Files", "Group Element Files Like it's Class and Template in a Parent Node", TapestryIcons.GroupElementFiles) {

    /** Implementations only read a plain flag off the view pane. */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
