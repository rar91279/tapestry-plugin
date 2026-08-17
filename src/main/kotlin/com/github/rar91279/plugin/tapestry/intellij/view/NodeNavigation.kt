package com.github.rar91279.plugin.tapestry.intellij.view

import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.TapestryNode
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement

/**
 * What a node in the Tapestry view opens, and which PSI element it stands for.
 *
 * Kept apart from the pane so it can be exercised without a `ProjectView`: every node kind the tree grows has
 * to be answered here, and a kind that isn't is a node that does nothing when you click it.
 */
object NodeNavigation {

    /**
     * What opening a node means.
     *
     * A node that has children is a container: clicking it expands it, and opening a file behind it as well
     * would be a surprise — that goes for the categories, the subpackage folders, and for an element whose
     * files are listed underneath. Only the leaves open something, which is also what keeps the behaviour
     * consistent when the element files are hidden: the element is the leaf then, and opens its class.
     */
    fun navigatableOf(node: TapestryNode?): Navigatable? {
        if (node == null || node.children.isNotEmpty()) return null

        return when (val value = node.getValue()) {
            is PresentationLibraryElement -> value.elementClass?.containingFile
            // A directory is a place, not a thing to open — an empty category or folder stays inert.
            is PsiDirectory -> null
            is Navigatable -> value
            else -> null
        }
    }

    /**
     * The PSI behind a node's value, for the actions that work on a selected node rather than open it.
     *
     * Deliberately not a directory. The platform derives a `Navigatable` from this key when none is offered
     * directly, and navigating a directory means "select this package in the Project view" — so exposing it
     * made a double-click on a category or folder jump out of the Tapestry view entirely.
     */
    fun psiElementOf(value: Any?): PsiElement? = when (value) {
        is PresentationLibraryElement -> value.elementClass
        is PsiDirectory -> null
        is PsiElement -> value
        else -> null
    }
}
