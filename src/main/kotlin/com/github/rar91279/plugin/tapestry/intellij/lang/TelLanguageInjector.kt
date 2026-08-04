package com.github.rar91279.plugin.tapestry.intellij.lang

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.github.rar91279.plugin.tapestry.intellij.lang.descriptor.TapestryAttributeDescriptor
import com.github.rar91279.plugin.tapestry.intellij.lang.descriptor.TapestryXmlExtension
import com.github.rar91279.plugin.tapestry.lang.TelLanguage

/**
 * Injects the Tapestry Expression Language into the template attributes that hold a property expression.
 */
class TelLanguageInjector : MultiHostInjector {

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val attr = context.parent as? XmlAttribute ?: return
        if (attr.localName == "type" || attr.localName == "id") return

        val parent = attr.parent ?: return
        if (!TapestryXmlExtension.isTapestryTemplateNamespace(attr.namespace) &&
            !TapestryXmlExtension.isTapestryTemplateNamespace(parent.namespace)
        ) {
            return
        }

        if (attr.textContains('\n')) return

        val value = attr.value ?: return
        if (value.any { it in "\${}/\\" }) return

        val explicitProp = value.startsWith(PROP_PREFIX)
        var range = attr.valueTextRange

        if (explicitProp) {
            if (range.length >= PROP_PREFIX.length) {
                range = TextRange(range.startOffset + PROP_PREFIX.length, range.endOffset)
            }
        }
        else {
            val descriptor = attr.descriptor as? TapestryAttributeDescriptor ?: return
            val prefix = descriptor.defaultPrefix
            if (prefix != null && prefix != "prop") return
        }

        registrar.startInjecting(TelLanguage.INSTANCE)
            .addPlace("\${", "}", context as PsiLanguageInjectionHost, range)
            .doneInjecting()
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> = listOf(XmlAttributeValue::class.java)

    private companion object {
        const val PROP_PREFIX = "prop:"
    }
}
