package com.github.rar91279.plugin.tapestry.core.util

import com.intellij.openapi.util.text.StringUtil
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiTypes

/**
 * Utility methods for manipulating classes.
 */
object ClassUtils {

    /**
     * Finds every property declared in a class and its super classes.
     *
     * @return the property name mapped to the place in the code where that property is bound to.
     */
    fun getClassProperties(javaClassType: PsiClass?): Map<String, Any> {
        if (javaClassType == null) return HashMap()

        val properties = HashMap<String, Any>()

        for (method in javaClassType.publicMethods(true)) {
            val methodName = method.name
            val returnType = method.returnType ?: continue

            val propertyName = when {
                methodName.startsWith("get") -> methodName.removePrefix("get")
                methodName.startsWith("is") && returnType == PsiTypes.booleanType() -> methodName.removePrefix("is")
                else -> continue
            }

            if (propertyName.isNotEmpty()) {
                properties[StringUtil.decapitalize(propertyName)] = method
            }
        }

        for ((fieldName, field) in javaClassType.tapestryFields(true)) {
            val annotation = field.getAnnotation(TapestryConstants.PROPERTY_ANNOTATION) ?: continue

            if (annotation.attributeValues("read").firstOrNull() == "false") continue

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
