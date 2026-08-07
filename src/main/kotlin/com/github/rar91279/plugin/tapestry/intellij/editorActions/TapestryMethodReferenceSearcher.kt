package com.github.rar91279.plugin.tapestry.intellij.editorActions

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryPropertyNamingUtil
import com.github.rar91279.plugin.tapestry.lang.TmlFileType
import com.intellij.util.Processor

/**
 * Finds the template references to a property accessor, by its method and its property name.
 */
class TapestryMethodReferenceSearcher : QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters>(true) {

    override fun processQuery(
        parameters: MethodReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {
        val method = parameters.method
        val propName = TapestryPropertyNamingUtil.getPropertyNameFromAccessor(method)
        val searchScope = parameters.effectiveSearchScope.restrictedToTemplates()

        if (!StringUtil.isEmpty(propName)) {
            parameters.optimizer.searchWord(propName!!, searchScope, UsageSearchContext.IN_FOREIGN_LANGUAGES, false, method)
        }
        parameters.optimizer.searchWord(method.name, searchScope, UsageSearchContext.IN_FOREIGN_LANGUAGES, false, method)
    }
}

/** The given scope, narrowed to Tapestry templates when it is a global one. */
internal fun SearchScope.restrictedToTemplates(): SearchScope =
    if (this is GlobalSearchScope) GlobalSearchScope.getScopeRestrictedByFileTypes(this, TmlFileType) else this
