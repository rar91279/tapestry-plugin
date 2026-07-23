package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.java.IJavaAnnotation
import com.intellij.tapestry.core.java.IJavaClassType
import com.intellij.tapestry.core.java.IJavaField
import com.intellij.tapestry.core.java.IJavaMethod
import com.intellij.tapestry.core.java.IJavaType
import com.intellij.tapestry.core.resource.IResource
import java.util.regex.Pattern

/**
 * Utility class for easy creation of IJavaClassType mocks.
 */
class JavaClassTypeMock() : IJavaClassType {

    private var _fullyQualifiedName: String? = null
    private var _interface = false
    private var _public = false
    private var _defaultConstructor = false
    private val _publicMethods = ArrayList<IJavaMethod>()
    private val _allMethods = ArrayList<IJavaMethod>()
    private val _annotations = ArrayList<IJavaAnnotation>()
    private val _fields = HashMap<String, IJavaField>()
    private var _documentation: String? = null
    private var _file: IResource? = null
    private var _superClassType: IJavaClassType? = null

    constructor(fullyQualifiedName: String?) : this() {
        _fullyQualifiedName = fullyQualifiedName
    }

    override fun getFullyQualifiedName(): String? = _fullyQualifiedName

    override fun getName(): String? {
        val fqn = _fullyQualifiedName ?: return null
        if (fqn.indexOf('.') == -1) return fqn
        return fqn.substring(fqn.lastIndexOf('.') + 1)
    }

    override fun isInterface(): Boolean = _interface

    fun setInterface(anInterface: Boolean) {
        _interface = anInterface
    }

    override fun isPublic(): Boolean = _public

    override fun isEnum(): Boolean = false

    fun setPublic(aPublic: Boolean): JavaClassTypeMock {
        _public = aPublic
        return this
    }

    override fun hasDefaultConstructor(): Boolean = _defaultConstructor

    override fun getSuperClassType(): IJavaClassType? = _superClassType

    fun setSuperClassType(superClassType: IJavaClassType?) {
        _superClassType = superClassType
    }

    fun setDefaultConstructor(defaultConstructor: Boolean): JavaClassTypeMock {
        _defaultConstructor = defaultConstructor
        return this
    }

    override fun getPublicMethods(fromSuper: Boolean): Collection<IJavaMethod> = _publicMethods

    override fun getAllMethods(fromSuper: Boolean): Collection<IJavaMethod> = _allMethods

    fun addPublicMethod(method: IJavaMethod): JavaClassTypeMock {
        _publicMethods.add(method)
        return this
    }

    override fun findPublicMethods(methodNameRegExp: String): Collection<IJavaMethod> {
        val pattern = Pattern.compile(methodNameRegExp)
        val foundMethods = ArrayList<IJavaMethod>()
        for (method in getPublicMethods(true)) {
            if (pattern.matcher(method.name).matches()) {
                foundMethods.add(method)
            }
        }
        return foundMethods
    }

    override fun getAnnotations(): Collection<IJavaAnnotation> = _annotations

    override fun getFields(fromSuper: Boolean): Map<String, IJavaField> = _fields

    fun addField(field: IJavaField): JavaClassTypeMock {
        _fields[field.name] = field
        return this
    }

    override fun getDocumentation(): String? = _documentation

    fun setDocumentation(documentation: String?) {
        _documentation = documentation
    }

    override fun getFile(): IResource? = _file

    override fun supportsInformalParameters(): Boolean = false

    fun setFile(file: IResource?): JavaClassTypeMock {
        _file = file
        return this
    }

    override fun isAssignableFrom(type: IJavaType?): Boolean = false

    override fun getUnderlyingObject(): Any = _fullyQualifiedName!!
}
