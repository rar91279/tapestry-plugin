package com.github.rar91279.plugin.tapestry.core.mocks

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import io.mockk.every
import io.mockk.mockk

/**
 * Mocked PSI XML elements, stubbed to the handful of members the Tapestry model actually reads.
 *
 * These replace the hand-written `XmlTagMock`/`XmlAttributeMock`, which existed only to implement the
 * plugin's own `XmlTag`/`XmlAttribute` interfaces — field-for-field copies of the PSI types. `mockk` mocks
 * the PSI interfaces directly without needing a running IDE, so the core specs still run in the fast
 * `kotest` task.
 */
fun xmlAttributeMock(localName: String, value: String? = null, namespace: String = ""): XmlAttribute {
    val attribute = mockk<XmlAttribute>(relaxed = true)
    every { attribute.name } returns localName
    every { attribute.localName } returns localName
    every { attribute.value } returns value
    every { attribute.namespace } returns namespace
    return attribute
}

fun xmlTagMock(localName: String, namespace: String = "", vararg attributes: XmlAttribute): XmlTag {
    val tag = mockk<XmlTag>(relaxed = true)
    every { tag.name } returns localName
    every { tag.localName } returns localName
    every { tag.namespace } returns namespace
    every { tag.attributes } returns arrayOf(*attributes)
    return tag
}

/**
 * A mocked PSI [com.intellij.psi.PsiFile] standing in for a Tapestry resource (template, message catalog).
 *
 * Replaces the hand-written `TestableResource`, which implemented the plugin's own `IResource` interface and
 * resolved a real file off the test classpath just to answer "does this exist?". The model now asks
 * `PsiFile.isValid` instead, and [timeStamp] backs the file-modification cache keys.
 */
fun psiFileMock(
    name: String,
    valid: Boolean = true,
    timeStamp: Long = 0,
): com.intellij.psi.PsiFile {
    val virtualFile = mockk<com.intellij.openapi.vfs.VirtualFile>(relaxed = true)
    every { virtualFile.timeStamp } returns timeStamp
    every { virtualFile.path } returns "/web/$name"
    every { virtualFile.extension } returns name.substringAfterLast('.', "")
    every { virtualFile.nameWithoutExtension } returns name.substringBeforeLast('.')

    val psiFile = mockk<com.intellij.psi.PsiFile>(relaxed = true)
    every { psiFile.name } returns name
    every { psiFile.isValid } returns valid
    every { psiFile.virtualFile } returns virtualFile
    return psiFile
}

/**
 * A mocked PSI annotation, stubbed down to what `attributeValues()` reads: the qualified name and the
 * declared attribute list. Each attribute is given a single literal value, or an array initializer of
 * literals when several are passed.
 */
fun psiAnnotationMock(qualifiedName: String, vararg attributes: Pair<String, List<String>>): PsiAnnotation {
    val pairs = attributes.map { (name, values) ->
        val pair = mockk<PsiNameValuePair>(relaxed = true)
        every { pair.attributeName } returns name
        every { pair.value } returns if (values.size == 1) literalMock(values[0]) else arrayInitializerMock(values)
        pair
    }

    val parameterList = mockk<PsiAnnotationParameterList>(relaxed = true)
    every { parameterList.attributes } returns pairs.toTypedArray()

    val annotation = mockk<PsiAnnotation>(relaxed = true)
    every { annotation.qualifiedName } returns qualifiedName
    every { annotation.isValid } returns true
    every { annotation.parameterList } returns parameterList
    return annotation
}

private fun literalMock(value: String): PsiLiteralExpression {
    val literal = mockk<PsiLiteralExpression>(relaxed = true)
    every { literal.value } returns value
    return literal
}

private fun arrayInitializerMock(values: List<String>): PsiArrayInitializerMemberValue {
    val initializer = mockk<PsiArrayInitializerMemberValue>(relaxed = true)
    every { initializer.initializers } returns values.map { literalMock(it) }.toTypedArray()
    return initializer
}

/**
 * A mocked [PsiClass], stubbed to the members the Tapestry model reads off a component/page class.
 *
 * Two details are worth knowing about:
 *  * `project` is wired to a stub [SmartPointerManager], because [PresentationLibraryElement] holds its
 *    class through a smart pointer and would otherwise need a running IDE to construct.
 *  * `constructors`/`superClass` are stubbed explicitly: `PsiUtil.hasDefaultConstructor` walks both, and a
 *    relaxed mock would hand it an endless chain of super classes.
 */
fun psiClassMock(
    qualifiedName: String? = null,
    isPublic: Boolean = false,
    hasDefaultConstructor: Boolean = true,
    containingFile: PsiFile? = null,
    javadoc: String? = null,
): PsiClass {
    val psiClass = mockk<PsiClass>(relaxed = true)

    every { psiClass.qualifiedName } returns qualifiedName
    every { psiClass.name } returns qualifiedName?.substringAfterLast('.')
    every { psiClass.isValid } returns true
    every { psiClass.isEnum } returns false
    every { psiClass.isInterface } returns false
    every { psiClass.hasModifierProperty(PsiModifier.PUBLIC) } returns isPublic
    every { psiClass.containingFile } returns containingFile
    every { psiClass.docComment } returns javadoc?.let(::docCommentMock)
    every { psiClass.navigationElement } returns psiClass
    every { psiClass.superClass } returns null
    every { psiClass.constructors } returns
        if (hasDefaultConstructor) emptyArray() else arrayOf(psiMethodMock("<init>", parameterCount = 1))
    every { psiClass.project } returns projectMockWithSmartPointers()

    return psiClass.stubFields().stubMethods().stubAnnotations()
}

/** Re-stubs the fields of a [psiClassMock]; both the declared and the inherited view. */
fun PsiClass.stubFields(vararg fields: PsiField): PsiClass {
    every { this@stubFields.fields } returns arrayOf(*fields)
    every { allFields } returns arrayOf(*fields)
    return this
}

/** Re-stubs the methods of a [psiClassMock]; both the declared and the inherited view. */
fun PsiClass.stubMethods(vararg methods: PsiMethod): PsiClass {
    every { this@stubMethods.methods } returns arrayOf(*methods)
    every { allMethods } returns arrayOf(*methods)
    return this
}

/** Re-stubs the annotations of a [psiClassMock]. */
fun PsiClass.stubAnnotations(vararg annotations: PsiAnnotation): PsiClass {
    every { this@stubAnnotations.annotations } returns arrayOf(*annotations)
    every { getAnnotation(any()) } answers { annotations.firstOrNull { it.qualifiedName == firstArg() } }
    every { hasAnnotation(any()) } answers { annotations.any { it.qualifiedName == firstArg() } }
    return this
}

fun psiFieldMock(
    name: String,
    isPrivate: Boolean = true,
    type: PsiType? = null,
    annotations: List<PsiAnnotation> = emptyList(),
    javadoc: String? = null,
    text: String = "",
): PsiField {
    val field = mockk<PsiField>(relaxed = true)

    every { field.name } returns name
    every { field.isValid } returns true
    every { field.type } returns (type ?: PsiTypes.nullType())
    every { field.text } returns text
    every { field.docComment } returns javadoc?.let(::docCommentMock)
    every { field.navigationElement } returns field
    every { field.hasModifierProperty(PsiModifier.PRIVATE) } returns isPrivate
    every { field.annotations } returns annotations.toTypedArray()
    every { field.getAnnotation(any()) } answers { annotations.firstOrNull { it.qualifiedName == firstArg() } }
    every { field.hasAnnotation(any()) } answers { annotations.any { it.qualifiedName == firstArg() } }

    return field
}

fun psiMethodMock(
    name: String,
    returnType: PsiType? = null,
    parameterCount: Int = 0,
    isPublic: Boolean = true,
    javadoc: String? = null,
    annotations: List<PsiAnnotation> = emptyList(),
): PsiMethod {
    val modifierList = mockk<PsiModifierList>(relaxed = true)
    every { modifierList.hasExplicitModifier(PsiModifier.PUBLIC) } returns isPublic
    every { modifierList.annotations } returns annotations.toTypedArray()

    val parameterList = mockk<PsiParameterList>(relaxed = true)
    every { parameterList.isEmpty } returns (parameterCount == 0)
    every { parameterList.parametersCount } returns parameterCount
    every { parameterList.parameters } returns Array(parameterCount) { mockk<PsiParameter>(relaxed = true) }

    val method = mockk<PsiMethod>(relaxed = true)
    every { method.name } returns name
    every { method.isValid } returns true
    every { method.returnType } returns returnType
    every { method.modifierList } returns modifierList
    every { method.parameterList } returns parameterList
    every { method.containingClass } returns null
    every { method.docComment } returns javadoc?.let(::docCommentMock)
    every { method.navigationElement } returns method
    every { method.annotations } returns annotations.toTypedArray()
    every { method.getAnnotation(any()) } answers { annotations.firstOrNull { it.qualifiedName == firstArg() } }
    every { method.hasAnnotation(any()) } answers { annotations.any { it.qualifiedName == firstArg() } }

    return method
}

/** A [PsiClassType] that resolves to [psiClass]. */
fun psiClassTypeMock(psiClass: PsiClass): PsiClassType {
    val type = mockk<PsiClassType>(relaxed = true)
    every { type.resolve() } returns psiClass
    every { type.canonicalText } returns psiClass.qualifiedName.orEmpty()
    every { type.presentableText } returns psiClass.name.orEmpty()
    return type
}

private fun docCommentMock(text: String): PsiDocComment {
    val element = mockk<PsiElement>(relaxed = true)
    every { element.text } returns text

    val comment = mockk<PsiDocComment>(relaxed = true)
    every { comment.descriptionElements } returns arrayOf(element)
    return comment
}

/**
 * A [Project] whose only wired service is the [SmartPointerManager], handing back a pointer that simply
 * holds on to the element. Enough for the model objects that keep their class behind a smart pointer.
 */
private fun projectMockWithSmartPointers(): Project {
    val pointerManager = mockk<SmartPointerManager>(relaxed = true)
    every { pointerManager.createSmartPsiElementPointer(any<PsiElement>()) } answers {
        val target = firstArg<PsiElement>()
        mockk<SmartPsiElementPointer<PsiElement>>(relaxed = true) { every { element } returns target }
    }

    val project = mockk<Project>(relaxed = true)
    every { project.getService(SmartPointerManager::class.java) } returns pointerManager
    return project
}
