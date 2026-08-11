package com.github.rar91279.plugin.tapestry.core

import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Caches the library prefix to package mappings declared with `new LibraryMapping(...)`.
 */
@VisibleForTesting
object MappingDataCache {

    private const val TAPESTRY_MAPPING_FQN = "org.apache.tapestry5.services.LibraryMapping"

    fun getMappingData(file: PsiFile): Map<String, String> =
        CachedValuesManager.getProjectPsiDependentCache(file, ::computeMappingData)

    private fun computeMappingData(file: PsiFile): Map<String, String> {
        // UAST, not Java PSI: module classes are just as often Kotlin, and `new LibraryMapping(...)` and
        // `LibraryMapping(...)` are the same call expression through it.
        val uFile = file.sourceFile().toUElementOfType<UFile>() ?: return emptyMap()
        val result = HashMap<String, String>()

        uFile.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (node.kind == UastCallKind.CONSTRUCTOR_CALL && node.valueArgumentCount == 2 &&
                    node.resolve()?.containingClass?.qualifiedName == TAPESTRY_MAPPING_FQN
                ) {
                    val prefix = node.valueArguments[0].evaluateString()
                    val packageName = node.valueArguments[1].evaluateString()

                    if (prefix != null && packageName != null) {
                        result[prefix] = packageName
                    }
                }

                return false
            }
        })

        return result
    }

    /** The source file behind a compiled one, or the file itself. */
    private fun PsiFile.sourceFile(): PsiFile {
        if (this !is PsiCompiledElement) return this

        val navigationElement = navigationElement
        if (navigationElement !== this && navigationElement is PsiFile) return navigationElement

        return mirror as? PsiFile ?: this
    }
}
