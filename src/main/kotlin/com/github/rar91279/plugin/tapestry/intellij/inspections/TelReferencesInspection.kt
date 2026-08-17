package com.github.rar91279.plugin.tapestry.intellij.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.codeInspection.ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.jvm.JvmModifier
import com.intellij.lang.jvm.actions.CreatePropertyActionGroup
import com.intellij.lang.jvm.actions.CreateReadOnlyPropertyActionGroup
import com.intellij.lang.jvm.actions.CreateWriteOnlyPropertyActionGroup
import com.intellij.lang.jvm.actions.JvmGroupIntentionAction
import com.intellij.lang.jvm.actions.annotationRequest
import com.intellij.lang.jvm.actions.createAddFieldActions
import com.intellij.lang.jvm.actions.createMethodActions
import com.intellij.lang.jvm.actions.expectedTypes
import com.intellij.lang.jvm.actions.fieldRequest
import com.intellij.lang.jvm.actions.methodRequest
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJvmSubstitutor
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.util.PropertyUtilBase
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.github.rar91279.plugin.tapestry.TapestryBundle
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.psi.TelMethodCallExpression
import com.github.rar91279.plugin.tapestry.psi.TelQualifiedReference
import com.github.rar91279.plugin.tapestry.psi.TelReferenceExpression

/**
 * Reports the TEL references that don't resolve.
 */
class TelReferencesInspection : TapestryInspectionBase() {

    override fun registerProblems(element: PsiElement, holder: ProblemsHolder) {
        if (element !is TelReferenceExpression) return

        val ref = element.reference
        if (!ref.isQualifierResolved()) return

        val results = ref.multiResolve(false)
        val resolvedWithError = results.isNotEmpty() && !results[0].isValidResult

        if (resolvedWithError || results.isEmpty()) { // can not check ref.resolve() for null here as we can have 2 results
            val fixes = if (resolvedWithError) emptyList() else createAccessorFixes(element, ref)

            holder.registerProblemForReference(
                ref,
                if (resolvedWithError) GENERIC_ERROR_OR_WARNING else LIKE_UNKNOWN_SYMBOL,
                ref.getUnresolvedMessage(resolvedWithError),
                *fixes.toTypedArray()
            )
        }
    }

    override fun getShortName(): String = "TelReferencesInspection"

    /**
     * "Create @Property field 'foo'" and "Create method 'getFoo()'" for an unresolved property. Delegated to the JVM
     * actions so that Kotlin element classes work too. The platform's own field+accessors property variants are
     * dropped: in a template, a property is `@Property private Foo foo`, not a JavaBean pair.
     */
    private fun createAccessorFixes(element: TelReferenceExpression, ref: TelQualifiedReference): List<LocalQuickFix> {
        if (element.parent is TelMethodCallExpression) return emptyList()

        val propertyName = ref.getReferenceName()?.ifEmpty { null } ?: return emptyList()
        val targetClass = ref.getQualifierClass() ?: return emptyList()
        val project = targetClass.project
        if (!PsiManager.getInstance(project).isInProject(targetClass)) return emptyList()

        val type =
            if (isBooleanContext(element)) PsiTypes.booleanType()
            else PsiType.getJavaLangObject(targetClass.manager, targetClass.resolveScope)
        val file = element.containingFile

        val fieldRequest = fieldRequest(
            propertyName, listOf(annotationRequest(TapestryConstants.PROPERTY_ANNOTATION)), listOf(JvmModifier.PRIVATE),
            expectedTypes(type), PsiJvmSubstitutor(project, PsiSubstitutor.EMPTY), null, false
        )
        val fieldFixes = IntentionWrapper.wrapToQuickFixes(createAddFieldActions(targetClass, fieldRequest), file)
            .map { RenamedFix(it, TapestryBundle.message("quickfix.create.property.field", propertyName)) }

        val methodRequest = methodRequest(
            project, PropertyUtilBase.suggestGetterName(propertyName, type), listOf(JvmModifier.PUBLIC), type
        )
        val methodActions = createMethodActions(targetClass, methodRequest)
            .filterNot { (it as? JvmGroupIntentionAction)?.actionGroup in BEAN_PROPERTY_GROUPS }

        return fieldFixes + IntentionWrapper.wrapToQuickFixes(methodActions, file)
    }

    /** `true` when the expression sits in the `test` parameter of `t:if`/`t:unless`, explicit or instrumented. */
    private fun isBooleanContext(element: TelReferenceExpression): Boolean {
        val host = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: element
        val attribute = PsiTreeUtil.getParentOfType(host, XmlAttribute::class.java, false) ?: return false
        if (attribute.localName != "test") return false

        val tag = attribute.parent ?: return false

        return tag.localName in CONDITIONAL_TAGS ||
               TapestryUtils.getTapestryAttribute(tag, "type")?.value in CONDITIONAL_TAGS
    }

    /** Only there to give the generic "Create field 'foo'" action a name that says the field is a `@Property`. */
    private class RenamedFix(delegate: LocalQuickFix, private val name: String) : LocalQuickFix by delegate {
        override fun getName(): String = name
    }

    private companion object {
        val CONDITIONAL_TAGS = setOf("if", "unless")
        val BEAN_PROPERTY_GROUPS =
            setOf(CreatePropertyActionGroup, CreateReadOnlyPropertyActionGroup, CreateWriteOnlyPropertyActionGroup)
    }
}
