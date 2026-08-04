package com.github.rar91279.plugin.tapestry.psi

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.HierarchicalMethodSignature
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiReferenceList
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.PsiTypeParameterList
import com.intellij.psi.PsiTypes
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.impl.light.LightEmptyImplementsList
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.impl.light.LightVariableBase
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import java.util.Objects

/**
 * The accessor method Tapestry generates for a `@Property` field.
 */
class TapestryAccessorMethod internal constructor(
    val property: PsiField,
    private val getterNotSetter: Boolean,
    private val myName: String
) : LightElement(property.manager, JavaLanguage.INSTANCE), PsiMethod {

    private val myModifierList: PsiModifierList = object : LightModifierList(manager) {

        override fun hasModifierProperty(name: String): Boolean = PsiModifier.PUBLIC == name

        override fun hasExplicitModifier(name: String): Boolean = PsiModifier.PUBLIC == name

        override fun getText(): String = PsiModifier.PUBLIC
    }

    private var myParameterList: LightParameterList? = null

    override fun getNavigationElement(): PsiElement = property

    fun isGetter(): Boolean = getterNotSetter

    override fun getDocComment(): PsiDocComment? = null

    override fun hasTypeParameters(): Boolean = false

    override fun getTypeParameterList(): PsiTypeParameterList? = null

    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    override fun getContainingClass(): PsiClass? = property.containingClass

    override fun getReturnType(): PsiType = if (getterNotSetter) property.type else PsiTypes.voidType()

    override fun getReturnTypeElement(): PsiTypeElement? = property.typeElement

    override fun getParameterList(): PsiParameterList {
        myParameterList?.let { return it }

        return LightParameterList(manager) {
            if (getterNotSetter) EMPTY_PARAMETERS_ARRAY
            else arrayOf(LightParameter(manager, property.name, null, property.type, this))
        }.also { myParameterList = it }
    }

    override fun getNameIdentifier(): PsiIdentifier? = property.nameIdentifier

    override fun getModifierList(): PsiModifierList = myModifierList

    override fun setName(name: String): PsiElement? = null

    override fun getHierarchicalMethodSignature(): HierarchicalMethodSignature =
        PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this)

    override fun toString(): String = "AccessorMethod"

    override fun getThrowsList(): PsiReferenceList = LightEmptyImplementsList(manager)

    override fun getBody(): PsiCodeBlock? = null

    override fun isConstructor(): Boolean = false

    override fun isVarArgs(): Boolean = false

    override fun getSignature(substitutor: PsiSubstitutor): MethodSignature =
        MethodSignatureBackedByPsiMethod.create(this, substitutor)

    override fun findSuperMethods(): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY

    override fun findSuperMethods(checkAccess: Boolean): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY

    override fun findSuperMethods(parentClass: PsiClass?): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY

    override fun findSuperMethodSignaturesIncludingStatic(checkAccess: Boolean): List<MethodSignatureBackedByPsiMethod> =
        emptyList()

    override fun findDeepestSuperMethods(): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY

    override fun findDeepestSuperMethod(): PsiMethod? = null

    override fun hasModifierProperty(name: String): Boolean = modifierList.hasModifierProperty(name)

    override fun isDeprecated(): Boolean = false

    override fun getName(): String = myName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TapestryAccessorMethod) return false

        return getterNotSetter == other.getterNotSetter && property == other.property && myName == other.myName
    }

    override fun hashCode(): Int = Objects.hash(property, getterNotSetter, myName)

    /** A parameter of a generated accessor. */
    class LightParameter(
        manager: PsiManager,
        private val myName: String?,
        nameIdentifier: PsiIdentifier?,
        type: PsiType,
        scope: PsiElement
    ) : LightVariableBase(manager, nameIdentifier, type, false, scope), PsiParameter {

        override fun accept(visitor: PsiElementVisitor) {
            if (visitor is JavaElementVisitor) visitor.visitParameter(this) else visitor.visitElement(this)
        }

        override fun toString(): String = "Light Parameter"

        override fun isVarArgs(): Boolean = false

        override fun getName(): String = StringUtil.notNullize(myName)
    }

    /** The lazily computed parameter list of a generated accessor. */
    class LightParameterList internal constructor(
        manager: PsiManager,
        private val parametersComputation: Computable<Array<LightParameter>>
    ) : LightElement(manager, JavaLanguage.INSTANCE), PsiParameterList {

        private var myParameters: Array<LightParameter>? = null

        override fun accept(visitor: PsiElementVisitor) {
            if (visitor is JavaElementVisitor) visitor.visitParameterList(this) else visitor.visitElement(this)
        }

        override fun getParameters(): Array<PsiParameter> {
            val parameters = myParameters ?: parametersComputation.compute().also { myParameters = it }

            return parameters.toList().toTypedArray()
        }

        override fun getParameterIndex(parameter: PsiParameter): Int {
            LOG.assertTrue(parameter.parent === this)

            return PsiImplUtil.getParameterIndex(parameter, this)
        }

        override fun getParametersCount(): Int = parameters.size

        override fun toString(): String = "Light Parameter List"
    }

    private companion object {
        val LOG = Logger.getInstance(TapestryAccessorMethod::class.java)
        val EMPTY_PARAMETERS_ARRAY = emptyArray<LightParameter>()
    }
}
