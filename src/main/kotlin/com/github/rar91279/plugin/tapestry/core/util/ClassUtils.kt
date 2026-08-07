package com.github.rar91279.plugin.tapestry.core.util

import com.intellij.openapi.util.text.StringUtil
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType

/**
 * Utility methods for manipulating classes.
 */
object ClassUtils {

    fun instanceOf(items: Array<Any>, aClass: Class<*>): Boolean = items.all { aClass.isInstance(it) }

    /**
     * Finds every property declared in a class and its super classes.
     *
     * @return the property name mapped to the place in the code where that property is bound to.
     */
    fun getClassProperties(javaClassType: IJavaClassType?): Map<String, Any> {
        if (javaClassType == null) return HashMap()

        val properties = HashMap<String, Any>()

        for (method in javaClassType.getPublicMethods(true)) {
            val methodName = method.name ?: continue
            val returnType = method.returnType ?: continue

            val propertyName = when {
                methodName.startsWith("get") -> methodName.removePrefix("get")
                methodName.startsWith("is") && returnType.name == "boolean" -> methodName.removePrefix("is")
                else -> continue
            }

            if (StringUtil.isNotEmpty(propertyName)) {
                properties[StringUtil.decapitalize(propertyName)] = method
            }
        }

        for ((fieldName, field) in javaClassType.getFields(true)) {
            val annotation = field.annotations[TapestryConstants.PROPERTY_ANNOTATION] ?: continue

            if (annotation.parameters["read"]?.get(0) == "false") continue

            properties[getName(fieldName)] = field
        }

        return properties
    }

    /**
     * Computes the name of a field without any leading `$` and `_` characters.
     */
    fun getName(name: String): String =
        if (name.startsWith("$") || name.startsWith("_")) name.substring(1) else name
}
