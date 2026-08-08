package com.github.rar91279.plugin.tapestry.intellij.lang.reference

import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.lang.properties.references.PropertyReference
import com.intellij.openapi.diagnostic.ControlFlowException
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
import com.github.rar91279.plugin.tapestry.intellij.lang.descriptor.TapestryXmlExtension
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.psi.TmlFile
import com.intellij.util.ProcessingContext

/**
 * Contributes the references of a Tapestry template: component types and ids, page names,
 * parameter values and link targets.
 *
 * This contributor registers several reference providers that enable navigation and code insight
 * for various Tapestry-specific attributes and elements in template files (.tml).
 */
class TapestryReferenceContributor : PsiReferenceContributor() {
    /**
     * Key used to store and retrieve XmlTag instances in the processing context during pattern matching.
     */
    private val TAG_KEY: Key<XmlTag> = Key.create("TAG_KEY")

    /**
     * Pattern condition that accepts only XML elements contained within Tapestry template files (.tml).
     */
    private val TAPESTRY_FILE_CONDITION = object : PatternCondition<XmlElement>("tapestryFileCondition") {
        override fun accepts(element: XmlElement, context: ProcessingContext?): Boolean =
            element.containingFile is TmlFile
    }


    /**
     * Registers all reference providers for Tapestry template elements.
     *
     * @param registrar the reference registrar to which providers are registered
     */
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val tapestryTemplateNamespaces = TapestryXmlExtension.tapestryTemplateNamespaces()

        registerTypeAttrValueReferenceProvider(registrar, tapestryTemplateNamespaces)
        registerIdAttrValueReferenceProvider(registrar, tapestryTemplateNamespaces)
        registerAttrValueReferenceProvider(registrar)
        registerLinkHrefReference(registrar)
    }

    /**
     * Registers a reference provider for href attributes in link tags, enabling file path references.
     *
     * @param registrar the reference registrar to which the provider is registered
     */
    private fun registerLinkHrefReference(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            XmlPatterns.xmlAttributeValue("href").inside(XmlPatterns.xmlTag().withName("link")).with(TAPESTRY_FILE_CONDITION),
            referenceProvider { element, _ ->
                PathReferenceManager.getInstance().createReferences(element, true, false, true)
            }
        )
    }

    /**
     * Registers reference providers for type attributes and alt attributes with message: prefix.
     *
     * Handles references to component class definitions and property file message keys.
     *
     * @param registrar the reference registrar to which providers are registered
     * @param namespaces the array of Tapestry namespace URIs to match
     */
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

    /**
     * Registers a reference provider for id attributes, enabling navigation to embedded component fields.
     *
     * @param registrar the reference registrar to which the provider is registered
     * @param namespaces the array of Tapestry namespace URIs to match
     */
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

    /**
     * Registers a reference provider for parameter value attributes in Tapestry component tags.
     *
     * Resolves parameter values to Java fields, methods, or page templates based on parameter type and prefix.
     *
     * @param registrar the reference registrar to which the provider is registered
     */
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

    /**
     * Creates references from parameter value attributes to their Java bindings (fields or methods).
     *
     * Uses the value resolver chain to resolve the attribute value according to the parameter's default prefix.
     *
     * @param attrValue the XML attribute value element
     * @param project the Tapestry project context
     * @param parameter the parameter definition for this attribute
     * @return an array of references to the resolved Java elements, or empty if resolution fails
     */
    private fun getAttrValueReference(
        attrValue: XmlAttributeValue,
        project: TapestryProject,
        parameter: TapestryParameter
    ): Array<PsiReference> {
        val element = project.findElementByTemplate(attrValue.containingFile) ?: return PsiReference.EMPTY_ARRAY
        val elementClass = element.elementClass ?: return PsiReference.EMPTY_ARRAY

        val resolvedValue = try {
            ValueResolverChain.resolve(project, elementClass, attrValue.value, parameter.defaultPrefix)
        }
        catch (e: Exception) {
            // Reference resolution runs under highlighting; swallowing a cancellation here would make the
            // whole pass ignore it.
            if (e is ControlFlowException) throw e
            return PsiReference.EMPTY_ARRAY
        } ?: return PsiReference.EMPTY_ARRAY

        return when (val codeBind = resolvedValue.codeBind) {
            is PsiMethod -> arrayOf(PsiAttributeValueReference(attrValue, codeBind))
            is PsiField -> arrayOf(PsiAttributeValueReference(attrValue, codeBind))
            else -> PsiReference.EMPTY_ARRAY
        }
    }

    /**
     * Creates a reference from a type attribute value to the component's Java class.
     *
     * Provides code completion with available component names from the current Tapestry project.
     *
     * @param attributeValue the XML attribute value element
     * @param range the text range within the element for the reference
     * @return an array containing the reference, or empty if range is null or component cannot be resolved
     */
    private fun getReferenceToComponentClass(attributeValue: XmlAttributeValue, range: TextRange?): Array<PsiReference> {
        if (range == null) return PsiReference.EMPTY_ARRAY

        val tag = parentTag(attributeValue) ?: return PsiReference.EMPTY_ARRAY
        val elementClass = TapestryUtils.getTypeOfTag(tag)?.elementClass

        return arrayOf(object : TapestryPsiReferenceBase<PsiElement>(attributeValue, range) {

            override fun resolve(): PsiElement? = elementClass

            override fun getVariants(): Array<Any> =
                TapestryModuleSupportLoader.getTapestryProject(tag)?.availableComponentNames?.toList()?.toTypedArray() ?: emptyArray()
        })
    }

    /**
     * Creates a reference from an id attribute value to the embedded component's Java field.
     *
     * Provides code completion with all embedded component IDs defined in the current component.
     *
     * @param attr the XML attribute value element
     * @param range the text range within the element for the reference
     * @return an array containing the reference, or empty if range is null or field cannot be found
     */
    private fun getReferenceToEmbeddedComponent(attr: XmlAttributeValue, range: TextRange?): Array<PsiReference> {
        if (range == null) return PsiReference.EMPTY_ARRAY

        val tag = parentTag(attr) ?: return PsiReference.EMPTY_ARRAY
        val field = TapestryUtils.findIdentifyingField(tag)

        return arrayOf(object : TapestryPsiReferenceBase<PsiElement>(attr, range) {

            override fun resolve(): PsiElement? = field

            override fun getVariants(): Array<Any> = TapestryUtils.getEmbeddedComponentIds(tag).toTypedArray()
        })
    }

    /**
     * Creates a self-reference for a component ID attribute value.
     *
     * Used when the ID doesn't correspond to an embedded component field.
     *
     * @param attrValue the XML attribute value element
     * @param range the text range within the element for the reference
     * @return an array containing the self-reference, or empty if range is null
     */
    private fun getReferenceByComponentId(attrValue: XmlAttributeValue, range: TextRange?): Array<PsiReference> {
        if (range == null) return PsiReference.EMPTY_ARRAY

        return arrayOf(object : TapestryPsiReferenceBase<PsiElement>(attrValue, range) {

            override fun resolve(): PsiElement = attrValue
        })
    }

    /**
     * Creates a reference from a page attribute value to the corresponding page's template file.
     *
     * Provides code completion with all available page names in the current Tapestry project.
     *
     * @param component the component containing the page parameter
     * @param pageAttrValue the XML attribute value element containing the page name
     * @return an array containing the reference to the page template file
     */
    private fun getReferenceToPage(component: TapestryComponent, pageAttrValue: XmlAttributeValue): Array<PsiReference> {
        val range = ElementManipulators.getValueTextRange(pageAttrValue)
        val page = component.project.findPage(pageAttrValue.value)

        return arrayOf(object : TapestryPsiReferenceBase<PsiElement>(pageAttrValue, range) {

            override fun resolve(): PsiElement? =
                page?.template?.firstOrNull()

            override fun getVariants(): Array<Any> = component.project.availablePageNames.toList().toTypedArray()
        })
    }

    /**
     * Extracts the parent XmlTag from an XML attribute value.
     *
     * @param value the XML attribute value element
     * @return the parent XmlTag, or null if the value is not inside a tag
     */
    private fun parentTag(value: XmlAttributeValue): XmlTag? = (value.parent as? XmlAttribute)?.parent

    /**
     * Creates a [PsiReferenceProvider] from a lambda that produces references for an element.
     *
     * @param references the function that creates references given a PSI element and processing context
     * @return a PsiReferenceProvider that delegates to the given function
     */
    private fun referenceProvider(
        references: (element: PsiElement, context: ProcessingContext) -> Array<PsiReference>
    ): PsiReferenceProvider = object : PsiReferenceProvider() {

        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> =
            references(element, context)
    }
}
