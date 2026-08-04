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

    override var fullyQualifiedName: String? = null
    override var isInterface = false
    override var isPublic = false
    override val isEnum = false
    override var superClassType: IJavaClassType? = null
    override var documentation: String? = null
    override var file: IResource? = null
    override val annotations = ArrayList<IJavaAnnotation>()

    private var _defaultConstructor = false
    private val _publicMethods = ArrayList<IJavaMethod>()
    private val _allMethods = ArrayList<IJavaMethod>()
    private val _fields = HashMap<String, IJavaField>()

    constructor(fullyQualifiedName: String?) : this() {
        this.fullyQualifiedName = fullyQualifiedName
    }

    override val name: String?
        get() {
            val fqn = fullyQualifiedName ?: return null
            if (fqn.indexOf('.') == -1) return fqn
            return fqn.substring(fqn.lastIndexOf('.') + 1)
        }

    fun setPublic(aPublic: Boolean): JavaClassTypeMock {
        isPublic = aPublic
        return this
    }

    override fun hasDefaultConstructor(): Boolean = _defaultConstructor

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
        return getPublicMethods(true).filter { pattern.matcher(it.name!!).matches() }
    }

    override fun getFields(fromSuper: Boolean): Map<String, IJavaField> = _fields

    fun addField(field: IJavaField): JavaClassTypeMock {
        _fields[field.name!!] = field
        return this
    }

    override fun supportsInformalParameters(): Boolean = false

    fun setFile(file: IResource?): JavaClassTypeMock {
        this.file = file
        return this
    }

    override fun isAssignableFrom(type: IJavaType?): Boolean = false

    override val underlyingObject: Any get() = fullyQualifiedName!!
}
