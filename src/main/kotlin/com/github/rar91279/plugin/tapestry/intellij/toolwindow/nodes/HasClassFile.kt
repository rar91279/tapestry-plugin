package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement

/** Nothing to show for an element whose class file is gone. */
internal val PresentationLibraryElement.hasClassFile: Boolean
    get() = elementClass.file != null
