package com.github.rar91279.plugin.tapestry.intellij.view

import com.intellij.ide.SelectInContext
import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem

class TapestryProjectSelectInTarget(project: Project) : ProjectViewSelectInTarget(project) {

    override fun toString(): String = "Tapestry Project View"

    override fun canSelect(psiFileSystemItem: PsiFileSystemItem): Boolean {
        if (!super.canSelect(psiFileSystemItem)) return false
        return TapestryProjectViewPane.getInstance(myProject).canSelect()
    }

    override fun getMinorViewId(): String = TapestryProjectViewPane.getInstance(myProject).id

    override fun getWeight(): Float = 5f

    override fun isSubIdSelectable(subId: String, context: SelectInContext): Boolean = true
}
