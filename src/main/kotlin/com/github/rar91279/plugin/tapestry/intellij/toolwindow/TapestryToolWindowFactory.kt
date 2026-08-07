package com.github.rar91279.plugin.tapestry.intellij.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory

class TapestryToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.isAvailable = true
        val tapestryToolWindow = TapestryToolWindow(project)
        val content = ContentFactory.getInstance().createContent(tapestryToolWindow.mainPanel, "Tapestry", true)
        // Tie the tool window's lifetime to its content, so its listener subscriptions are released with it.
        Disposer.register(content, tapestryToolWindow)
        // ...and don't leave getToolWindow() handing out the disposed instance afterwards.
        Disposer.register(content) { project.putUserData(TAPESTRY_TOOL_WINDOW_KEY, null) }
        toolWindow.contentManager.addContent(content)
        project.putUserData(TAPESTRY_TOOL_WINDOW_KEY, tapestryToolWindow)
    }
}

const val TAPESTRY_TOOLWINDOW_ID = "Tapestry"
private val TAPESTRY_TOOL_WINDOW_KEY = Key.create<TapestryToolWindow>("tapestry.toolWindow")

/** The tool window's content, or null while the tool window has never been opened. */
fun getToolWindow(project: Project): TapestryToolWindow? =
    if (ToolWindowManager.getInstance(project).getToolWindow(TAPESTRY_TOOLWINDOW_ID) == null) null
    else project.getUserData(TAPESTRY_TOOL_WINDOW_KEY)
