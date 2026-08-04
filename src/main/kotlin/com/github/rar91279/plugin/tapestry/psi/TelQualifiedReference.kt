package com.github.rar91279.plugin.tapestry.psi

import com.intellij.codeInsight.completion.util.SimpleMethodCallLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.ResolveResult
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.beanProperties.BeanProperty
import com.intellij.psi.impl.beanProperties.BeanPropertyElement
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.TypeConversionUtil
import com.github.rar91279.plugin.tapestry.TapestryBundle
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.lang.TelLanguage

/**
 * A reference from a TEL property/method name to the Java member it denotes.
 */
abstract class TelQualifiedReference(private val myElement: TelReferenceQualifier) : PsiPolyVariantReference {

    override fun getElement(): TelExpression = myElement

    override fun getCanonicalText(): String = myElement.text

    override fun isSoft(): Boolean = true

    override fun bindToElement(element: PsiElement): PsiElement {
        if (isReferenceTo(element)) return myElement

        if (element is PsiNamedElement) return handleElementRename(element.name.orEmpty())

        return myElement
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        val manager = myElement.manager

        for (result in multiResolve(false)) {
            val target = result.element

            if (manager.areElementsEquivalent(element, target)) return true
            if (target is BeanPropertyElement && manager.areElementsEquivalent(element, target.method)) return true
            if (target is TapestryAccessorMethod && manager.areElementsEquivalent(element, target.property)) return true
        }

        return false
    }

    final override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        ResolveCache.getInstance(myElement.project).resolveWithCaching(this, MY_RESOLVER, true, false)

    final override fun resolve(): PsiElement? {
        val results = multiResolve(false)

        return if (results.size == 1) results[0].element else null
    }

    abstract fun getReferenceQualifier(): TelReferenceQualifier?

    abstract fun getReferenceName(): String?

    override fun getVariants(): Array<Any> {
        val processor = object : TelVariantsProcessor<PsiNamedElement>(
            myElement.parent, null, getReferenceQualifier() == null
        ) {
            override fun createResult(element: PsiNamedElement, validResult: Boolean): PsiNamedElement = element
        }
        processVariantsInner(processor, ResolveState.initial())

        return processor.getVariants(PsiNamedElement.EMPTY_ARRAY).map { element -> lookupElementFor(element) }.toTypedArray()
    }

    fun isQualifierResolved(): Boolean {
        val qualifier = getReferenceQualifier() ?: return true
        val reference = qualifier.reference

        return reference == null || reference.resolve() != null
    }

    fun getPsiType(): PsiType? = when (val element = resolve()) {
        is PsiMethod -> getSubstitutedType(element, element.returnType)
        is BeanProperty -> getSubstitutedType(element.method, element.propertyType)
        is PsiField -> element.type
        else -> null
    }

    fun getUnresolvedMessage(resolvedWithError: Boolean): String {
        val referenceName = getReferenceName()
        val typeName = TelPsiUtil.getPresentableText(getQualifierClassType())
        val elementParent = myElement.parent

        if (elementParent !is TelMethodCallExpression) {
            return TapestryBundle.message("error.cannot.resolve.property", referenceName.orEmpty(), typeName)
        }
        if (!resolvedWithError) {
            return TapestryBundle.message("error.cannot.resolve.method", referenceName.orEmpty(), typeName)
        }

        val argumentTypes = elementParent.argumentTypes.joinToString(", ") { TelPsiUtil.getPresentableText(it) }

        return TapestryBundle.message("error.no.applicable.method", referenceName.orEmpty(), typeName, "($argumentTypes)")
    }

    private fun resolveInner(): Array<ResolveResult> {
        val referenceName = getReferenceName() ?: return ResolveResult.EMPTY_ARRAY

        val processor = object : TelVariantsProcessor<ResolveResult>(
            myElement.parent, referenceName, getReferenceQualifier() == null
        ) {
            override fun createResult(element: PsiNamedElement, validResult: Boolean): ResolveResult {
                val target = if (element is BeanPropertyElement) element.method else element

                return PsiElementResolveResult(target, validResult)
            }
        }
        processVariantsInner(processor, ResolveState.initial())

        return processor.getVariants(ResolveResult.EMPTY_ARRAY)
    }

    private fun processVariantsInner(processor: PsiScopeProcessor, state: ResolveState) {
        val qualifier = getReferenceQualifier()

        if (qualifier == null) {
            val psiClass = getPsiClassTypeForContainingTmlFile()?.psiClass ?: return
            psiClass.processDeclarations(processor, ResolveState.initial(), null, myElement)
            return
        }

        val type = qualifier.getPsiType()
        if (type is PsiClassType) {
            val psiClass = PsiUtil.resolveClassInType(type)
            if (psiClass != null && !psiClass.processDeclarations(processor, ResolveState.initial(), null, myElement)) {
                return
            }
        }

        val reference = qualifier.reference
        if (reference is TelQualifiedReference) {
            reference.resolve()?.processDeclarations(processor, state, null, myElement)
        }
    }

    private fun getPsiClassTypeForContainingTmlFile(): IntellijJavaClassType? {
        var file: PsiFile = myElement.containingFile

        if (file.language === TelLanguage.INSTANCE) {
            file = InjectedLanguageManager.getInstance(file.project).getInjectionHost(file)?.containingFile ?: return null
        }

        val project = TapestryUtils.getTapestryProject(file) ?: return null
        val libraryElement = project.findElementByTemplate(file) ?: return null

        return libraryElement.elementClass as? IntellijJavaClassType
    }

    private fun getSubstitutedType(method: PsiMethod?, result: PsiType?): PsiType? {
        if (result !is PsiClassType || method == null) return result

        val qualifierType = getQualifierClassType()
        if (qualifierType !is PsiClassType) return result

        val containingClass = method.containingClass ?: return result

        return getSuperClassSubstitutor(containingClass, qualifierType).substitute(result)
    }

    private fun getQualifierClassType(): PsiType? {
        getReferenceQualifier()?.let { return it.getPsiType() }

        return getPsiClassTypeForContainingTmlFile()?.underlyingObject as? PsiClassType
    }

    private fun lookupElementFor(element: PsiNamedElement): LookupElement {
        if (element is PsiMethod) return SimpleMethodCallLookupElement(element)

        val name = element.name!!
        val lookupElement = LookupElementBuilder.create(element, name).withLookupString(name)

        if (element is PsiField) return lookupElement.withTypeText(element.type.presentableText)
        if (element is BeanPropertyElement) {
            element.propertyType?.let { return lookupElement.withTypeText(it.presentableText) }
        }

        return lookupElement
    }

    companion object {

        private val MY_RESOLVER = ResolveCache.PolyVariantResolver<TelQualifiedReference> { expression, _ ->
            expression.resolveInner()
        }

        @JvmStatic
        fun getSuperClassSubstitutor(superClass: PsiClass, classType: PsiClassType): PsiSubstitutor {
            val classResolveResult = classType.resolveGenerics()
            val resolved = classResolveResult.element ?: return PsiSubstitutor.EMPTY

            return TypeConversionUtil.getSuperClassSubstitutor(superClass, resolved, classResolveResult.substitutor)
        }
    }
}
