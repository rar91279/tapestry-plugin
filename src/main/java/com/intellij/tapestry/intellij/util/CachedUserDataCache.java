package com.intellij.tapestry.intellij.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexey Chmutov
 */
public abstract class CachedUserDataCache<T, Owner extends UserDataHolder> {
  private final Key<CachedValue<T>> myKey;

  public CachedUserDataCache(@NonNls String keyName) {
    // Was based on com.intellij.openapi.util.UserDataCache, which casts the owner to UserDataHolderEx.
    // Since 2026.2 a Module (ModuleBridgeImpl) is no longer a UserDataHolderEx, so that cast throws.
    // CachedValuesManager.getCachedValue works on any plain UserDataHolder (Module, Project, ...).
    myKey = Key.<CachedValue<T>>create(keyName);
  }

  @Nullable
  protected abstract T computeValue(Owner owner);

  protected Object[] getDependencies(Owner owner) {
    return new Object[]{owner};
  }

  protected abstract Project getProject(Owner projectOwner);

  public final T get(Owner owner) {
    return CachedValuesManager.getManager(getProject(owner)).<T>getCachedValue(
      owner, myKey,
      () -> CachedValueProvider.Result.<T>create(computeValue(owner), getDependencies(owner)),
      false);
  }
}
