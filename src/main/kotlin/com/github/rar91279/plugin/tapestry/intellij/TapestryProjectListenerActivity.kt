package com.github.rar91279.plugin.tapestry.intellij

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Instantiates [TapestryPsiTreeChangeListener], which registers itself with the project's `PsiManager`
 * in its constructor and is disposed with the project.
 *
 * A project service is only created on first access, so something has to ask for it — that is all this
 * activity does.
 */
internal class TapestryProjectListenerActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        project.service<TapestryPsiTreeChangeListener>()
    }
}
