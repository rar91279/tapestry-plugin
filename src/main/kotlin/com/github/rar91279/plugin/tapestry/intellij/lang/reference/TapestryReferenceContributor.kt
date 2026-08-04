package com.github.rar91279.plugin.tapestry.intellij.lang.reference

import com.intellij.lang.properties.references.PropertyReference
import com.intellij.openapi.paths.PathReferenceManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlElement
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryParameter
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverChain
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaField
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaMethod
import com.github.rar91279.plugin.tapestry.intellij.core.resource.IntellijResource
import com.github.rar91279.plugin.tapestry.intellij.lang.descriptor.TapestryXmlExtension
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.psi.TmlFile
import com.intellij.util.ProcessingContext

/**
 * Contributes the references of a Tapestry template: component types and ids, page names,
 * parameter values and link targets.
 */
class TapestryReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val tapestryTemplateNamespaces = TapestryXmlExtension.tapestryTemplateNamespaces()

        registerTypeAttrValueReferenceProvider(registrar, tapestryTemplateNamespaces)
        registerIdAttrValueReferenceProvider(registrar, tapestryTemplateNamespaces)
        registerAttrValueReferenceProvider(registrar)
        registerLinkHrefReference(registrar)
    }

    private fun registerLinkHrefReference(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            XmlPatterns.xmlAttributeValue("href").inside(XmlPatterns.xmlTag().withName("link")).with(TAPESTRY_FILE_CONDITION),
            referenceProvider { element, _ ->
                PathReferenceManager.getInstance().createReferences(element, true, false, true)
            }
        )
    }

    private fun registerTypeAttrValueReferenceProvider(registrar: PsiReferenceRegistrar, namespaces: Array<String>) {
        registrar.registerReferenceProvider(
            XmlPatterns.xmlAttributeValue("type").withNamespace(*namespaces).with(TAPESTRY_FILE_CONDITION),
            referenceProvider { element, _ ->
                val typeAttrValue = element as XmlAttributeValue
                getReferenceToComponentClass(typeAttrValue, ElementManipulators.getValueTextRange(typeAttrValue))
            }
        )

        registrar.registerReferenceProvider(
            XmlPatterns.xmlAttributeValue("alt").with(TAPESTRY_FILE_CONDITION),
            referenceProvider { element, _ ->
                val value = StringUtil.stripQuotesAroundValue(element.text)
                val prefix = "message:"

                if (value.startsWith(prefix)) {
                    val key = value.substring(prefix.length)
                    val valueStart = prefix.length + 1

                    arrayOf(PropertyReference(key, element, null, true, TextRange(valueStart, valueStart + key.length)))
                }
                else PsiReference.EMPTY_ARRAY
            }
        )
    }

    private fun registerIdAttrValueReferenceProvider(registrar: PsiReferenceRegistrar, namespaces: Array<String>) {
        registrar.registerReferenceProvider(
            XmlPatterns.xmlAttributeValue("id").withNamespace(*namespaces).with(TAPESTRY_FILE_CONDITION),
            referenceProvider { element, _ ->
                val idAttrValue = element as XmlAttributeValue
                val identifier = TapestryUtils.getComponentIdentifier(parentTag(idAttrValue))
                val valueTextRange = ElementManipulators.getValueTextRange(idAttrValue)

                if (identifier === idAttrValue.parent) getReferenceToEmbeddedComponent(idAttrValue, valueTextRange)
                else getReferenceByComponentId(idAttrValue, valueTextRange)
            }
        )
    }

    private fun registerAttrValueReferenceProvider(registrar: PsiReferenceRegistrar) {
        val tapestryTagCondition = object : PatternCondition<XmlTag>("tapestryTagCondition") {
            override fun accepts(tag: XmlTag, context: ProcessingContext?): Boolean =
                tag.containingFile is TmlFile && TapestryUtils.getTypeOfTag(tag) != null
        }

        val tapestryAttributeValuePattern = XmlPatterns.xmlAttributeValue()
            .withSuperParent(2, XmlPatterns.xmlTag().with(tapestryTagCondition).save(TAG_KEY))

        registrar.registerReferenceProvider(
            tapestryAttributeValuePattern,
            referenceProvider { element, context ->
                val attrValue = element as XmlAttributeValue
                val tag = context.get(TAG_KEY)

                val component = TapestryUtils.getTypeOfTag(tag)
                val localName = (attrValue.parent as? XmlAttribute)?.localName
                val parameter = component?.parameters?.get(localName)

                when {
                    parameter == null -> PsiReference.EMPTY_ARRAY
                    localName == "page" -> getReferenceToPage(component, attrValue)
                    else -> getAttrValueReference(attrValue, component.project, parameter)
                }
            }
        )
    }

    private fun getAttrValueReference(
        attrValue: XmlAttributeValue,
        project: TapestryProject,
        parameter: TapestryParameter
    ): Array<PsiReference> {
        val element = project.findElementByTemplate(attrValue.containingFile) ?: return PsiReference.EMPTY_ARRAY
        val elementClass = element.elementClass as? IntellijJavaClassType ?: return PsiReference.EMPTY_ARRAY

        val resolvedValue = try {
            ValueResolverChain.getInstance().resolve(project, elementClass, attrValue.value, parameter.defaultPrefix)
        }
        catch (ex: Exception) {
            return PsiReference.EMPTY_ARRAY
        } ?: return PsiReference.EMPTY_ARRAY

        return when (val codeBind = resolvedValue.codeBind) {
            is IntellijJavaMethod -> arrayOf(PsiAttributeValueReference(attrValue, codeBind.psiMethod))
            is IntellijJavaField -> arrayOf(PsiAttributeValueReference(attrValue, codeBind.psiField))
            else -> PsiReference.EMPTY_ARRAY
        }
    }

    private fun getReferenceToComponentClass(attributeValue: XmlAttributeValue, range: TextRange?): Array<PsiReference> {
        if (range == null) return PsiReference.EMPTY_ARRAY

        val tag = parentTag(attributeValue) ?: return PsiReference.EMPTY_ARRAY
        val elementClass = TapestryUtils.getTypeOfTag(tag)?.elementClass as? IntellijJavaClassType

        return arrayOf(object : TapestryPsiReferenceBase<PsiElement>(attributeValue, range) {

            override fun resolve(): PsiElement? = elementClass?.psiClass

            override fun getVariants(): Array<Any> =
                TapestryModuleSupportLoader.getTapestryProject(tag)?.availableComponentNames?.toList()?.toTypedArray() ?: emptyArray()
        })
    }

    private fun getReferenceToEmbeddedComponent(attr: XmlAttributeValue, range: TextRange?): Array<PsiReference> {
        if (range == null) return PsiReference.EMPTY_ARRAY

        val tag = parentTag(attr) ?: return PsiReference.EMPTY_ARRAY
        val field = TapestryUtils.findIdentifyingField(tag) as? IntellijJavaField

        return arrayOf(object : TapestryPsiReferenceBase<PsiElement>(attr, range) {

            override fun resolve(): PsiElement? = field?.psiField

            override fun getVariants(): Array<Any> = TapestryUtils.getEmbeddedComponentIds(tag).toTypedArray()
        })
    }

    private fun getReferenceByComponentId(attrValue: XmlAttributeValue, range: TextRange?): Array<PsiReference> {
        if (range == null) return PsiReference.EMPTY_ARRAY

        return arrayOf(object : TapestryPsiReferenceBase<PsiElement>(attrValue, range) {

            override fun resolve(): PsiElement = attrValue
        })
    }

    private fun getReferenceToPage(component: TapestryComponent, pageAttrValue: XmlAttributeValue): Array<PsiReference> {
        val range = ElementManipulators.getValueTextRange(pageAttrValue) ?: return PsiReference.EMPTY_ARRAY
        val page = component.project.findPage(pageAttrValue.value)

        return arrayOf(object : TapestryPsiReferenceBase<PsiElement>(pageAttrValue, range) {

            override fun resolve(): PsiElement? =
                (page?.template?.firstOrNull() as? IntellijResource)?.psiFile

            override fun getVariants(): Array<Any> = component.project.availablePageNames.toList().toTypedArray()
        })
    }

    private fun parentTag(value: XmlAttributeValue): XmlTag? = (value.parent as? XmlAttribute)?.parent

    /** A [PsiReferenceProvider] built from the given reference factory. */
    private fun referenceProvider(
        references: (element: PsiElement, context: ProcessingContext) -> Array<PsiReference>
    ): PsiReferenceProvider = object : PsiReferenceProvider() {

        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> =
            references(element, context)
    }

    private companion object {

        val TAG_KEY: Key<XmlTag> = Key.create("TAG_KEY")

        val TAPESTRY_FILE_CONDITION = object : PatternCondition<XmlElement>("tapestryFileCondition") {
            override fun accepts(element: XmlElement, context: ProcessingContext?): Boolean =
                element.containingFile is TmlFile
        }
    }
}
