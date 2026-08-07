package com.github.rar91279.plugin.tapestry.intellij.facet

enum class TapestryVersion(private val version: String) {

    TAPESTRY_5_3_6("5.3.6");

    override fun toString(): String = version

    companion object {

        fun fromString(name: String?): TapestryVersion? = entries.firstOrNull { it.toString() == name }
    }
}
