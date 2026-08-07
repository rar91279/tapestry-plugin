package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.InjectedElement

class EmbeddedComponentNode(injected: InjectedElement) :
    InjectedElementNode(injected, injected.elementId ?: "")
