package com.github.rar91279.plugin.tapestry.intellij.editorActions

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
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

/**
 * Finds the template references to a field, by its name and by its accessor names.
 */
class TapestryPropertyReferenceSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {

    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {
        val refElement = queryParameters.elementToSearch
        if (refElement !is PsiField) return

        val scope = queryParameters.effectiveSearchScope
        val searchScope =
            if (scope is GlobalSearchScope) scope.restrictedToTemplates()
            else GlobalSearchScope
                .getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(refElement.getProject()), TmlFileType)
                .intersectWith(scope)

        if (searchScope is GlobalSearchScope && searchScope.project == null) return

        val name = refElement.name
        for (word in listOf(name, "get$name", "set$name")) {
            queryParameters.optimizer.searchWord(word, searchScope, UsageSearchContext.IN_FOREIGN_LANGUAGES, false, refElement)
        }
    }
}

/** The given scope, narrowed to Tapestry templates when it is a global one. */
private fun SearchScope.restrictedToTemplates(): SearchScope =
    if (this is GlobalSearchScope) GlobalSearchScope.getScopeRestrictedByFileTypes(this, TmlFileType) else this
