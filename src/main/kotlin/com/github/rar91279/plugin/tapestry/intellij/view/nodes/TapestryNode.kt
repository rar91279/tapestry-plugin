package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.module.Module
import com.intellij.ui.treeStructure.SimpleNode

/**
 * Base class for all Tapestry related nodes.
 */
abstract class TapestryNode(val module: Module) : SimpleNode(module.project) {

    // Named distinctly from "element": a Kotlin property literally called `element` would synthesize
    // a getElement() accessor that collides with SimpleNode's own (see getValue() below for why that
    // matters).
    private var domainValue: Any? = null
    private var presentation: ItemPresentation? = null

    /** Initializes the node. */
    fun init(id: Any, presentation: ItemPresentation) {
        domainValue = id
        this.presentation = presentation
        icon = presentation.getIcon(false)
    }

    override fun getChildren(): Array<SimpleNode> = NO_CHILDREN

    /**
     * The domain object this node represents (a `Module`, `PsiDirectory`,
     * `PresentationLibraryElement`, ...). Named distinctly from [getElement]: that method is owned
     * by [SimpleNode] and must keep returning `this` — the platform's `StructureTreeModel` calls
     * `NodeDescriptor.getElement()` and feeds the result straight back into
     * `SimpleTreeStructure.getChildElements`, which casts it to `SimpleNode`. Overriding
     * `getElement()` to return the domain object instead (as this class used to) breaks that cast
     * as soon as the async tree model tries to expand the node.
     */
    open fun getValue(): Any? = domainValue

    fun getPresentableText(): String = presentation!!.presentableText!!

    override fun doUpdate(presentation: PresentationData) {
        this.presentation = updatePresentation(this.presentation!!)
        presentation.setIcon(this.presentation!!.getIcon(false))
        presentation.presentableText = this.presentation!!.presentableText
    }

    /** Override this to change presentation data. */
    protected open fun updatePresentation(presentation: ItemPresentation): ItemPresentation = presentation
}
