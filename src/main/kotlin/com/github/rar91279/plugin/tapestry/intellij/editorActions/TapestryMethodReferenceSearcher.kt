package com.github.rar91279.plugin.tapestry.intellij.editorActions

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.util.PropertyUtilBase
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
        val searchScope = parameters.effectiveSearchScope.restrictedToTemplates()

        // The searches below are case-insensitive, so the platform's decapitalised property name ("foo" for
        // getFoo) serves as well as the raw accessor suffix the plugin used to derive itself.
        PropertyUtilBase.getPropertyName(method)
            ?.takeIf { it.isNotEmpty() }
            ?.let { parameters.optimizer.searchWord(it, searchScope, UsageSearchContext.IN_FOREIGN_LANGUAGES, false, method) }

        parameters.optimizer.searchWord(method.name, searchScope, UsageSearchContext.IN_FOREIGN_LANGUAGES, false, method)
    }
}

/** The given scope, narrowed to Tapestry templates when it is a global one. */
internal fun SearchScope.restrictedToTemplates(): SearchScope =
    if (this is GlobalSearchScope) GlobalSearchScope.getScopeRestrictedByFileTypes(this, TmlFileType) else this
