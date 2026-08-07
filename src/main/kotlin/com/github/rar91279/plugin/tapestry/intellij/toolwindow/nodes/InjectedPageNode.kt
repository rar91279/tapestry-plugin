package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.InjectedElement

/** Injected pages have no element id (that's a component concept), so fall back to the page name. */
class InjectedPageNode(injected: InjectedElement) :
    InjectedElementNode(
        injected,
        injected.elementId?.takeIf { it.isNotEmpty() } ?: injected.element?.name ?: ""
    )
