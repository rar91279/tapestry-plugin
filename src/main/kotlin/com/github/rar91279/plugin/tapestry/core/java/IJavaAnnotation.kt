package com.github.rar91279.plugin.tapestry.core.java

interface IJavaAnnotation {

    val fullyQualifiedName: String?

    /** Annotation attribute values by name; the default `value` attribute is keyed `null`. */
    val parameters: Map<String?, Array<String>>
}
