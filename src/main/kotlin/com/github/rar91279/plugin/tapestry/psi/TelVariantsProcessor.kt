package com.github.rar91279.plugin.tapestry.psi

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier.FINAL
import com.intellij.psi.PsiModifier.PRIVATE
import com.intellij.psi.PsiModifier.PUBLIC
import com.intellij.psi.PsiModifier.STATIC
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.beanProperties.BeanProperty
import com.intellij.psi.resolve.JavaMethodResolveHelper
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.PropertyUtilBase
import com.github.rar91279.plugin.tapestry.core.TapestryConstants

/**
 * Collects the TEL-visible members of a class: the `@Property` fields with their generated accessors,
 * the bean properties and the public methods.
 */
internal abstract class TelVariantsProcessor<T>(
    parent: PsiElement,
    private val referenceName: String?,
    private val allowStatic: Boolean
) : PsiScopeProcessor {

    private val result = LinkedHashSet<T>()
    private val forCompletion = referenceName == null
    private val methodCall = parent is TelMethodCallExpression
    private val propertyAccessors: JavaMethodResolveHelper?
    private val methods: JavaMethodResolveHelper?

    init {
        if (methodCall && !forCompletion) {
            val parameterTypes = (parent as TelMethodCallExpression).argumentTypes
            methods = JavaMethodResolveHelper(parent, parent.containingFile, parameterTypes)
            propertyAccessors = null
        }
        else {
            methods = if (forCompletion) JavaMethodResolveHelper(parent, parent.containingFile, null) else null
            propertyAccessors = JavaMethodResolveHelper(parent, parent.containingFile, null)
        }
    }

    override fun execute(element: PsiElement, state: ResolveState): Boolean {
        if (element !is PsiNamedElement) return true
        if (element.name.isNullOrEmpty()) return true
        if (element is PsiClass) return true

        if (element is PsiMethod) {
            if (!processMethod(element, state)) return true
        }
        else if (element is PsiField) {
            if (!processField(element, state)) return true
        }

        return forCompletion || result.size != 1
    }

    protected abstract fun createResult(element: PsiNamedElement, validResult: Boolean): T?

    fun getVariants(array: Array<T>): Array<T> {
        propertyAccessors?.methods?.forEach { methodCandidateInfo ->
            val property = BeanProperty.createBeanProperty(methodCandidateInfo.method)
            if (property != null) {
                createResult(property.psiElement, true)?.let { result.add(it) }
            }
        }

        methods?.methods?.forEach { methodCandidateInfo ->
            val validResult = methods.resolveError == JavaMethodResolveHelper.ErrorType.NONE
            createResult(methodCandidateInfo.method, validResult)?.let { result.add(it) }
        }

        return result.toArray(array)
    }

    /** @return `false` when the method isn't TEL-visible. */
    private fun processMethod(method: PsiMethod, state: ResolveState): Boolean {
        if (!method.hasModifierProperty(PUBLIC) ||
            method.isConstructor ||
            (!allowStatic && method.hasModifierProperty(STATIC)) ||
            method.name in INSECURE_OBJECT_METHODS
        ) {
            return false
        }

        if (!methodCall &&
            propertyAccessors != null &&
            PropertyUtilBase.isSimplePropertyGetter(method) &&
            (referenceName == null || referenceName.equals(PropertyUtilBase.getPropertyName(method), ignoreCase = true))
        ) {
            propertyAccessors.addMethod(method, state.get(PsiSubstitutor.KEY), false)
        }

        if (forCompletion || (methodCall && referenceName.equals(method.name, ignoreCase = true))) {
            methods?.addMethod(method, state.get(PsiSubstitutor.KEY), false)
        }

        return true
    }

    /** @return `false` when the field isn't TEL-visible. */
    private fun processField(field: PsiField, state: ResolveState): Boolean {
        val modifierList = field.modifierList ?: return false
        if (!field.hasModifierProperty(PRIVATE) || field.hasModifierProperty(STATIC)) return false

        val isProperty = modifierList.annotations.any {
            it.qualifiedName == TapestryConstants.PROPERTY_ANNOTATION
        }
        if (!isProperty) return false

        if (forCompletion || (!methodCall && referenceName.equals(field.name, ignoreCase = true))) {
            createResult(field, true)?.let { result.add(it) }
        }

        val getterName = PropertyUtilBase.suggestGetterName(field)
        if (forCompletion || (methodCall && referenceName.equals(getterName, ignoreCase = true))) {
            methods?.addMethod(TapestryAccessorMethod(field, true, getterName), state.get(PsiSubstitutor.KEY), false)
        }

        val setterName = PropertyUtilBase.suggestSetterName(field)
        if (!field.hasModifierProperty(FINAL) &&
            (forCompletion || (methodCall && referenceName.equals(setterName, ignoreCase = true)))
        ) {
            methods?.addMethod(TapestryAccessorMethod(field, false, setterName), state.get(PsiSubstitutor.KEY), false)
        }

        return true
    }

    private companion object {
        val INSECURE_OBJECT_METHODS = setOf("wait", "notify", "notifyAll")
    }
}
