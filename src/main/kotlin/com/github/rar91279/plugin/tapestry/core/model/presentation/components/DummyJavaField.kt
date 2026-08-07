package com.github.rar91279.plugin.tapestry.core.model.presentation.components

import com.github.rar91279.plugin.tapestry.core.java.IJavaAnnotation
import com.github.rar91279.plugin.tapestry.core.java.IJavaField
import com.github.rar91279.plugin.tapestry.core.java.IJavaType

/**
 * A dummy java field, backing a [DummyTapestryParameter].
 */
internal class DummyJavaField(override val name: String, override val type: IJavaType?) : IJavaField {

    override val isPrivate: Boolean get() = true

    override val annotations: Map<String, IJavaAnnotation> get() = emptyMap()

    override val documentation: String get() = ""

    override val stringRepresentation: String get() = ""

    override val isValid: Boolean get() = true
}
