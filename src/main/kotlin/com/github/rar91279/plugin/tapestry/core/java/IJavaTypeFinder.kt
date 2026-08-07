package com.github.rar91279.plugin.tapestry.core.java

import com.github.rar91279.plugin.tapestry.core.ioc.IServiceBindingDiscoverer

/**
 * Searches for JAVA types in the project.
 */
interface IJavaTypeFinder {

    /**
     * @return the type with the given fully qualified name, `null` if none is found.
     */
    fun findType(fullyQualifiedName: String, includeDependencies: Boolean): IJavaClassType?

    /**
     * @return all the JAVA types declared in the given package.
     */
    fun findTypesInPackage(packageName: String, includeDependencies: Boolean): Collection<IJavaClassType>

    /**
     * @return all the JAVA types declared in the given package and it's sub-packages.
     */
    fun findTypesInPackageRecursively(basePackageName: String, includeDependencies: Boolean): Collection<IJavaClassType>

    val serviceBindingDiscoverer: IServiceBindingDiscoverer?
}
