package com.github.rar91279.plugin.tapestry.intellij.lang.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement
import com.intellij.psi.XmlRecursiveElementVisitor
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.coercion.TypeCoercionValidator
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryParameter
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.AbstractValueResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverChain
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.lang.TemplateColorSettingsPage
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils

/**
 * Annotates a Tapestry template.
 */
class TemplateTagAnnotator : XmlRecursiveElementVisitor(), Annotator {

    private var annotationHolder: AnnotationHolder? = null

    override fun annotate(psiElement: PsiElement, annotationHolder: AnnotationHolder) {
        this.annotationHolder = annotationHolder

        try {
            psiElement.accept(this)
        }
        finally {
            this.annotationHolder = null
        }
    }

    override fun visitXmlTag(tag: XmlTag) {
        if (TapestryUtils.getComponentIdentifier(tag) != null) {
            // annotate the tag
            annotateTapestryTag(tag)
            TapestryUtils.getIdentifyingAttribute(tag)?.let { annotateTapestryAttribute(it) }

            val component = TapestryUtils.getTypeOfTag(tag)
            if (component != null) {
                val tapestryProject = TapestryModuleSupportLoader.getTapestryProject(tag)
                val element = tapestryProject?.findElementByTemplate(tag.containingFile)
                val elementClass = element?.elementClass as? IntellijJavaClassType

                // annotate the tag parameters
                for (parameter in component.parameters.values) {
                    val attribute = TapestryUtils.getTapestryAttribute(tag, parameter.name) ?: continue

                    annotateTapestryAttribute(attribute)
                    if (elementClass != null && tapestryProject != null) {
                        annotateAttributeValue(tapestryProject, elementClass, parameter, attribute)
                    }
                }
            }
        }

        tag.acceptChildren(this)
    }

    private fun annotateAttributeValue(
        tapestryProject: TapestryProject,
        elementClass: IntellijJavaClassType,
        parameter: TapestryParameter,
        attribute: XmlAttribute
    ) {
        val value = attribute.valueElement ?: return

        val resolvedValue = try {
            ValueResolverChain.getInstance()
                .resolve(tapestryProject, elementClass, attribute.value, parameter.defaultPrefix)
        }
        catch (pce: ProcessCanceledException) {
            throw pce
        }
        catch (ex: Exception) {
            logger.error(ex)
            return
        } ?: return

        val resolvedType = resolvedValue.type ?: return
        val parameterType = parameter.parameterField.type
        val holder = annotationHolder ?: return

        if (!TypeCoercionValidator.canCoerce(
                tapestryProject, resolvedType,
                AbstractValueResolver.getCleanValue(attribute.value), parameterType
            )
        ) {
            holder.newAnnotation(
                HighlightSeverity.ERROR,
                "Can't coerce a ${resolvedType.name} to a ${parameterType?.name ?: "undefined"}"
            ).range(value).create()
        }
    }

    private fun annotateTapestryTag(tag: XmlTag) {
        val holder = annotationHolder ?: return

        IdeaUtils.getNameElement(tag)?.let {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(it).textAttributes(TemplateColorSettingsPage.TAG_NAME).create()
        }

        if (!tag.isEmpty) {
            IdeaUtils.getNameElementClosing(tag)?.let {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(it).textAttributes(TemplateColorSettingsPage.TAG_NAME).create()
            }
        }
    }

    private fun annotateTapestryAttribute(attribute: XmlAttribute) {
        val holder = annotationHolder ?: return

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(attribute.firstChild).textAttributes(TemplateColorSettingsPage.ATTR_NAME).create()
    }

    private companion object {
        val logger = Logger.getInstance(TemplateTagAnnotator::class.java)
    }
}
