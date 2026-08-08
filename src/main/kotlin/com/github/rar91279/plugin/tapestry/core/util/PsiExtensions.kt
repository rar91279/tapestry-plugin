package com.github.rar91279.plugin.tapestry.core.util

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.util.Key
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiField
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTypesUtil

/**
 * The PSI accessors the Tapestry model needs that are not a plain property on the PSI type.
 *
 * These are what is left of the `IJava*` portability layer: the handful of places where the old wrappers
 * did something more than delegate. Everything else the model asks of a class, field, method or annotation
 * is now read off the PSI type directly.
 */

/**
 * The values an annotation *declares* for the given attribute, empty when it declares none.
 *
 * Only constant-foldable values are returned: literals, and references to fields initialised with a
 * literal. Anything else (a class literal, a nested annotation, an expression) yields an empty string in
 * the array form and nothing in the single form — the Tapestry model only ever reads string attributes.
 *
 * An unnamed value, `@Component("id")`, is reported under `"value"`, the name PSI gives it.
 */
fun PsiAnnotation.attributeValues(name: String): List<String> =
    parameterList.attributes.firstOrNull { it.attributeName == name }?.value.stringValues()

private fun PsiAnnotationMemberValue?.stringValues(): List<String> = when (this) {
    null -> emptyList()
    is PsiArrayInitializerMemberValue -> initializers.map { it.constantString().orEmpty() }
    else -> listOfNotNull(constantString())
}

private fun PsiAnnotationMemberValue.constantString(): String? = when (this) {
    is PsiLiteralExpression -> value?.toString()
    is PsiReferenceExpression -> ((resolve() as? PsiField)?.initializer as? PsiLiteralExpression)?.value?.toString()
    else -> null
}

/**
 * The class fields by name.
 *
 * @param fromSuper whether fields inherited from super classes are included.
 */
fun PsiClass.tapestryFields(fromSuper: Boolean): Map<String, PsiField> {
    val classFields = try {
        if (fromSuper) allFields else fields
    }
    catch (ex: PsiInvalidElementAccessException) {
        // thrown if the class is invalid, should ignore and return an empty Map
        return emptyMap()
    }

    return classFields.associateBy { it.name }
}

/**
 * The public methods of the class, excluding those declared on `java.lang.Object`.
 *
 * @param fromSuper whether methods inherited from super classes are included.
 */
fun PsiClass.publicMethods(fromSuper: Boolean): List<PsiMethod> =
    tapestryMethods(fromSuper).filter { it.modifierList.hasExplicitModifier(PsiModifier.PUBLIC) }

/**
 * All methods of the class, excluding those declared on `java.lang.Object`.
 *
 * @param fromSuper whether methods inherited from super classes are included.
 */
fun PsiClass.tapestryMethods(fromSuper: Boolean): List<PsiMethod> =
    (if (fromSuper) allMethods else methods).filter {
        it.containingClass?.qualifiedName != CommonClassNames.JAVA_LANG_OBJECT
    }

/** The public methods, super classes included, whose name matches [methodNameRegExp] in full. */
fun PsiClass.findPublicMethods(methodNameRegExp: String): List<PsiMethod> {
    val nameRegex = Regex(methodNameRegExp)

    return publicMethods(true).filter { nameRegex.matches(it.name) }
}

/**
 * Whether the component class accepts informal parameters.
 *
 * Cached against [PsiModificationTracker.MODIFICATION_COUNT]. The wrapper this replaces cached the answer
 * in a plain field with no invalidation at all, so adding or removing the annotation left a stale verdict
 * behind for the lifetime of the model object.
 */
fun PsiClass.supportsInformalParameters(): Boolean =
    CachedValuesManager.getCachedValue(this, INFORMAL_PARAMETERS_KEY) {
        CachedValueProvider.Result.create(
            AnnotationUtil.isAnnotated(this, INFORMAL_PARAMETERS_ANNOTATION, AnnotationUtil.CHECK_HIERARCHY),
            PsiModificationTracker.MODIFICATION_COUNT
        )
    }

private const val INFORMAL_PARAMETERS_ANNOTATION = "org.apache.tapestry5.annotations.SupportsInformalParameters"

private val INFORMAL_PARAMETERS_KEY =
    Key.create<CachedValue<Boolean>>("tapestry.supportsInformalParameters")

/** The type denoting this class. */
fun PsiClass.classType(): PsiClassType = PsiTypesUtil.getClassType(this)

/**
 * The javadoc description of this element, empty when it has none. Falls back to the navigation
 * element, whose language may be one without javadoc (e.g. a Kotlin `KtProperty`).
 */
fun PsiDocCommentOwner.javadocDescription(): String {
    val docComment = docComment ?: (navigationElement as? PsiDocCommentOwner)?.docComment ?: return ""

    return docComment.descriptionElements
        .filter { it !is PsiWhiteSpace }
        .joinToString("") { it.text }
}

/**
 * A bare type variable (`T`) read as `java.lang.Object`.
 *
 * Tapestry coerces parameter values at runtime, so a parameter declared `T` accepts anything. The deleted
 * `IJava*` layer folded this in when it converted a `PsiType`, and without it every value bound to a
 * generic component parameter is flagged as an impossible coercion.
 *
 * Deliberately `Object` rather than `TypeConversionUtil.erasure`, which yields the *bound*: a
 * `T extends Number` parameter would erase to `Number`, which is not a key in the coercion table, and the
 * false error would come straight back for bounded generics.
 */
fun PsiType.erasedIfTypeVariable(): PsiType {
    val typeParameter = (this as? PsiClassType)?.resolve() as? PsiTypeParameter ?: return this

    return PsiType.getJavaLangObject(typeParameter.manager, resolveScope)
}
