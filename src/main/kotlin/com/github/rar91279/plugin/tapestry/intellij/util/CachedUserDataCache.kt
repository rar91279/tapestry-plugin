package com.github.rar91279.plugin.tapestry.intellij.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

/**
 * A value computed per owner (module, project, ...) and cached on it until its dependencies change.
 */
abstract class CachedUserDataCache<T, Owner : UserDataHolder>(keyName: String) {

    // Was based on com.intellij.openapi.util.UserDataCache, which casts the owner to UserDataHolderEx.
    // Since 2026.2 a Module (ModuleBridgeImpl) is no longer a UserDataHolderEx, so that cast throws.
    // CachedValuesManager.getCachedValue works on any plain UserDataHolder (Module, Project, ...).
    private val key: Key<CachedValue<T>> = Key.create(keyName)

    protected abstract fun computeValue(owner: Owner): T?

    protected open fun getDependencies(owner: Owner): Array<Any> = arrayOf(owner)

    protected abstract fun getProject(projectOwner: Owner): Project

    fun get(owner: Owner): T =
        CachedValuesManager.getManager(getProject(owner)).getCachedValue(
            owner, key,
            { CachedValueProvider.Result.create(computeValue(owner), *getDependencies(owner)) },
            false
        )
}
