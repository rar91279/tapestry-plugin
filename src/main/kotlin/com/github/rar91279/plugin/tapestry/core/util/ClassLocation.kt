package com.github.rar91279.plugin.tapestry.core.util

import java.net.URL

/** A resource found on the classpath: its simple name (no extension) and the URL to read it from. */
data class ClassLocation(val className: String, val url: URL)
