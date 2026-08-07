package com.github.rar91279.plugin.tapestry.intellij.util

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiTypes

/**
 * JavaBean naming rules, as Tapestry applies them to property accessors.
 */
object TapestryPropertyNamingUtil {

    fun isWaitOrNotifyOfObject(method: PsiMethod): Boolean {
        if (method.containingClass?.qualifiedName != CommonClassNames.JAVA_LANG_OBJECT) return false

        return method.name in setOf("wait", "notify", "notifyAll")
    }

    fun isPropertyGetter(method: PsiMethod): Boolean {
        if (method.isConstructor) return false

        val methodName = method.name
        val returnType = method.returnType

        when {
            methodName.startsWith("get") && methodName.length > "get".length ->
                if (returnType == null || returnType == PsiTypes.voidType()) return false

            methodName.startsWith("is") ->
                if (returnType != PsiTypes.booleanType()) return false

            else -> return false
        }

        return method.parameterList.parametersCount == 0
    }

    fun isPropertySetter(method: PsiMethod): Boolean {
        if (method.isConstructor) return false

        return method.name.startsWith("set") &&
               method.name.length > "set".length &&
               method.parameterList.parametersCount == 1 &&
               (method.returnType == null || method.returnType == PsiTypes.voidType())
    }

    fun isPropertyAccessor(method: PsiMethod): Boolean = isPropertyGetter(method) || isPropertySetter(method)

    fun getPropertyNameFromAccessor(accessor: PsiMethod): String? = when {
        isPropertySetter(accessor) -> propertyNameFromSetter(accessor)
        isPropertyGetter(accessor) -> propertyNameFromGetter(accessor)
        else -> null
    }

    fun findPropertyGetter(aClass: PsiClass?, propertyName: String): PsiMethod? =
        findPropertyAccessor(aClass, propertyName) { if (isPropertyGetter(it)) propertyNameFromGetter(it) else null }

    fun findPropertySetter(aClass: PsiClass?, propertyName: String): PsiMethod? =
        findPropertyAccessor(aClass, propertyName) { if (isPropertySetter(it)) propertyNameFromSetter(it) else null }

    private fun propertyNameFromGetter(getter: PsiMethod): String =
        getter.name.substring(if (getter.name.startsWith("get")) "get".length else "is".length)

    private fun propertyNameFromSetter(setter: PsiMethod): String = setter.name.substring("set".length)

    private fun findPropertyAccessor(
        aClass: PsiClass?,
        propertyName: String,
        extractPropertyName: (PsiMethod) -> String?
    ): PsiMethod? =
        aClass?.allMethods?.firstOrNull { propertyName.equals(extractPropertyName(it), ignoreCase = true) }
}
