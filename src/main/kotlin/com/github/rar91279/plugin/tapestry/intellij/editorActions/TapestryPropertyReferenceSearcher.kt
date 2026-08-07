package com.github.rar91279.plugin.tapestry.intellij.editorActions

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.github.rar91279.plugin.tapestry.lang.TmlFileType
import com.intellij.util.Processor

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
