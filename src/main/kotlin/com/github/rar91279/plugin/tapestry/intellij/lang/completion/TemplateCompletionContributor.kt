package com.github.rar91279.plugin.tapestry.intellij.lang.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlToken
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryParameter
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverChain
import com.github.rar91279.plugin.tapestry.core.util.ClassUtils
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.lang.descriptor.TapestryXmlExtension
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.intellij.util.ProcessingContext
import java.util.Locale
import java.util.Scanner

/**
 * Completes the component parameter values in a Tapestry template.
 */
class TemplateCompletionContributor : CompletionContributor() {

    init {
        extend(null, psiElement(), object : CompletionProvider<CompletionParameters>() {

            override fun addCompletions(
                parameters: CompletionParameters,
                context: ProcessingContext,
                result: CompletionResultSet
            ) {
                val psiElement = parameters.position
                if (psiElement !is LeafPsiElement) return
                if (psiElement.prevSibling?.text == ".") return

                val module = ModuleUtilCore.findModuleForPsiElement(psiElement)
                // if this isn't a Tapestry module don't do anything
                if (!TapestryUtils.isTapestryModule(module)) return

                if (psiElement !is XmlToken || psiElement.tokenType.toString() != "XML_ATTRIBUTE_VALUE_TOKEN") return

                // The selected attribute and tag
                val attribute = PsiTreeUtil.getParentOfType(psiElement, XmlAttribute::class.java) ?: return
                val tag = PsiTreeUtil.getParentOfType(psiElement, XmlTag::class.java) ?: return

                // Completion of type, id and mixins attributes is handled elsewhere
                if (TapestryXmlExtension.isTapestryTemplateNamespace(attribute.namespace) &&
                    attribute.localName in setOf("type", "id", "mixins")
                ) {
                    return
                }

                // Try to match the tag to a component
                val component = TapestryUtils.getTypeOfTag(tag) ?: return

                val tapestryProject = TapestryModuleSupportLoader.getTapestryProject(module) ?: return
                val element = tapestryProject.findElementByTemplate(parameters.originalFile) ?: return
                val elementClass = element.elementClass as? IntellijJavaClassType ?: return

                for (parameter in component.parameters.values) {
                    if (!parameter.name.equals(attribute.localName, ignoreCase = true)) continue

                    if (completeParameterValue(result, tapestryProject, module!!, elementClass, parameter, tag, attribute)) return
                }
            }

            /**
             * Adds the completions for a single parameter.
             *
             * @return `true` when this parameter fully handled the completion.
             */
            private fun completeParameterValue(
                result: CompletionResultSet,
                tapestryProject: TapestryProject,
                module: Module,
                elementClass: IntellijJavaClassType,
                parameter: TapestryParameter,
                tag: XmlTag,
                attribute: XmlAttribute
            ): Boolean {
                val attributeValue = tag.getAttributeValue(attribute.localName)

                if (attributeValue != null) {
                    // Completion of all attribute values that start with "prop:"
                    if (attributeValue == "prop:IntellijIdeaRulezzz ") {
                        addVariants(result, ClassUtils.getClassProperties(elementClass).keys.map { "prop:$it" })
                        return true
                    }

                    // Completion of composed properties
                    if (attributeValue.contains(".") &&
                        !attributeValue.contains("..") &&
                        (parameter.defaultPrefix == "prop" || attributeValue.startsWith("prop:"))
                    ) {
                        val qualifier = qualifierOf(attributeValue)

                        val resolvedValue = try {
                            ValueResolverChain.resolve(tapestryProject, elementClass, qualifier, parameter.defaultPrefix)
                        }
                        catch (ex: Exception) {
                            logger.error(ex)
                            return true
                        }

                        val resolvedClass = resolvedValue?.type as? IntellijJavaClassType
                        val resolvedFile = resolvedClass?.psiClass?.containingFile
                        if (resolvedFile != null) {
                            val qualifierClass = IntellijJavaClassType(module, resolvedFile)

                            addVariants(
                                result,
                                ClassUtils.getClassProperties(qualifierClass).keys.map { qualifier + it }
                            )
                            return true
                        }
                    }
                }

                // Completion of a boolean parameter
                val parameterTypeName = parameter.parameterField.type?.name
                if (parameterTypeName?.lowercase(Locale.getDefault()) == "boolean") {
                    val attributes = ClassUtils.getClassProperties(elementClass).keys.toMutableSet()
                    attributes.add("literal:true")
                    attributes.add("literal:false")

                    addVariants(result, attributes)
                    return true
                }

                // Completion of all attributes whose default prefix is "prop"
                if (parameter.defaultPrefix == "prop") {
                    addVariants(result, ClassUtils.getClassProperties(elementClass).keys)
                }

                return false
            }

            /** The property expression up to (and including) the last dot the caret sits behind. */
            private fun qualifierOf(attributeValue: String): String {
                if (!attributeValue.contains("." + CompletionInitializationContext.DUMMY_IDENTIFIER)) return attributeValue

                val words = StringBuilder()
                Scanner(attributeValue).use { scan ->
                    var word = ""
                    while (scan.hasNext() && !word.contains(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED)) {
                        word = scan.next()
                        words.append(word)
                    }
                }

                return words.toString().replaceFirst(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED, "")
            }

            private fun addVariants(result: CompletionResultSet, returnedProperties: Collection<String>) {
                for (property in returnedProperties) {
                    result.addElement(LookupElementBuilder.create(property).withCaseSensitivity(false))
                }
            }
        })
    }

    private companion object {
        val logger = Logger.getInstance(TemplateCompletionContributor::class.java)
    }
}
