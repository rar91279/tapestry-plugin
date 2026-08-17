package com.github.rar91279.plugin.tapestry.intellij

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.model.presentation.findAssets
import com.github.rar91279.plugin.tapestry.core.model.presentation.findJavaScriptModule
import com.github.rar91279.plugin.tapestry.core.model.presentation.looksLikeAssetPath
import com.github.rar91279.plugin.tapestry.core.util.PathUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryModuleClasses
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiStatement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType

/**
 * A JavaScript stack an element imports with `@Import(stack = …)`.
 *
 * @param name the stack name, as written in the annotation.
 * @param declaration what the stack is: the class the contribution names, or — when the contribution assembles
 *                    the stack inline from `StackExtension`s, so there is no class — the name literal itself,
 *                    which at least lands on the contribution line.
 * @param assets the stylesheets and libraries the stack bundles, including those of the stacks it includes.
 */
class JavaScriptStack(val name: String, val declaration: PsiElement, val assets: List<PsiFile>)

/**
 * Resolves stack names to their contribution and contents.
 *
 * A stack is runtime configuration, not a declaration the PSI models, so this reads the contribution the way
 * a developer does: find the module class that mentions the name in a `JavaScriptStack` contribution, then
 * take the asset paths written near it and in the stack class it names.
 *
 * ponytail: literal-scanning heuristic. It sees paths written as string literals — the overwhelmingly common
 * case — and not ones computed at runtime or pulled from a symbol. Widen only if real stacks turn up that it
 * misses; a resolver that actually evaluated the contributions would be a Tapestry interpreter.
 */
class JavaScriptStackResolver(private val module: Module, private val project: TapestryProject) {

    private val moduleClasses by lazy { TapestryModuleClasses.of(module) }

    fun resolve(names: List<String>): List<JavaScriptStack> {
        val resolved = LinkedHashMap<String, JavaScriptStack>()
        names.forEach { collect(it, resolved) }
        return resolved.values.toList()
    }

    /**
     * Every stack the project itself contributes, by name.
     *
     * Library and framework contributions are left out: their stacks are real, but their files sit in jars, so
     * a listing of them would be a list of names with nothing under them.
     */
    fun resolveAll(): List<JavaScriptStack> {
        val fileIndex = ProjectFileIndex.getInstance(module.project)

        val own = contributions
            .filterValues { literal ->
                literal.containingFile?.virtualFile?.let { fileIndex.isInSourceContent(it) } == true
            }
            .keys
            .sorted()

        return resolve(own).filter { it.name in own }
    }

    /** Resolves [name] into [into], recursing into the stacks it includes. Cycles stop at [into]. */
    private fun collect(name: String, into: MutableMap<String, JavaScriptStack>) {
        if (name.isEmpty() || into.containsKey(name)) return

        val contribution = findDeclaration(name) ?: return
        val stackClass = stackClassNear(contribution)

        // The class is what the stack *is* — `addInstance("leaflet", LeafletStack.class)` names it, and that
        // is where a developer wants to land, not on the registration line.
        val declaration: PsiElement = stackClass ?: contribution

        // Registered before recursing: a stack that includes itself, directly or through another, must not
        // send this into an endless descent.
        into[name] = JavaScriptStack(name, declaration, emptyList())

        val literals = literalsDeclaring(contribution, stackClass)
        val strings = literals.mapNotNull { it.value as? String }.distinct()
        val fromPackage = declaration.packageName()

        // A string in a stack class is one of three things, and which one is only visible from what it
        // resolves to: an asset path, the name of another stack, or the name of a JavaScript module. Anything
        // that resolves to nothing was none of them.
        val assets = strings
            .flatMap {
                if (looksLikeAssetPath(it)) project.findAssets(it, fromPackage)
                else project.findJavaScriptModule(it)
            }
            .distinct()

        into[name] = JavaScriptStack(name, declaration, assets)

        strings.filter { contributions.containsKey(it) }.distinct().forEach { collect(it, into) }
    }

    /**
     * Every stack name contributed anywhere in reach, mapped to the literal that names it.
     *
     * Built in one pass and reused: the alternative is re-scanning every module class per stack name, and a
     * page pulling in a stack that pulls in two more would walk them repeatedly.
     */
    private val contributions: Map<String, PsiLiteralExpression> by lazy {
        val found = HashMap<String, PsiLiteralExpression>()

        for (moduleClass in moduleClasses) {
            for (method in moduleClass.methods.filter { it.contributesJavaScriptStacks() }) {
                for (literal in PsiTreeUtil.findChildrenOfType(method, PsiLiteralExpression::class.java)) {
                    val name = literal.value as? String ?: continue
                    if (name.isNotEmpty() && !looksLikeAssetPath(name)) found.putIfAbsent(name, literal)
                }
            }
        }

        found
    }

    private fun findDeclaration(name: String): PsiLiteralExpression? = contributions[name]

    /**
     * Whether a method contributes JavaScript stacks: `contributeJavaScriptStackSource(…)`, or any method
     * whose `@Contribute` or parameters name a stack type — the 5.4 `StackExtension` style.
     */
    private fun PsiMethod.contributesJavaScriptStacks(): Boolean =
        JAVA_SCRIPT_STACK_TYPE in name ||
                JAVA_SCRIPT_STACK_TYPE in parameterList.text ||
                annotations.any { JAVA_SCRIPT_STACK_TYPE in it.text }

    /**
     * The class implementing the stack, when the contribution names one: `addInstance("x", XStack.class)` or
     * `add("x", new XStack(…))`. A stack assembled inline from `StackExtension`s names none.
     */
    private fun stackClassNear(declaration: PsiElement): PsiClass? {
        val call = declaration.parentOfType<PsiMethodCallExpression>() ?: return null

        return call.argumentList.expressions.firstNotNullOfOrNull { argument ->
            when (argument) {
                is PsiClassObjectAccessExpression -> argument.operand.type.resolveClass()
                is PsiNewExpression -> argument.classReference?.resolve() as? PsiClass
                else -> null
            }
        }
    }

    /**
     * Every string that describes what a stack contains: those written in the stack class, or — for an inline
     * contribution — those in the statement that adds this stack. The statement, not the whole method: one
     * `contributeJavaScriptStackSource` commonly registers several stacks, and each keeps its own contents.
     *
     * A stack states its contents wherever it likes: returned from `getStacks()` and friends, or handed to a
     * base class through the constructor (`super(JS_LIBS, CSS_STYLES, STACKS, MODULES)`), where no method of
     * the class mentions them at all. So the whole class is read, and what each string *is* — asset path,
     * stack name, module name — is decided by what it resolves to.
     */
    private fun literalsDeclaring(contribution: PsiElement, stackClass: PsiClass?): List<PsiLiteralExpression> {
        val scope: PsiElement = stackClass ?: contribution.parentOfType<PsiStatement>() ?: return emptyList()

        return literalsIn(scope)
    }

    /**
     * Every string [scope] holds, following the fields it reads one hop.
     *
     * A stack class keeps its paths and dependencies in constants — `private static final String[] STACKS =
     * {"bb_context_menu"}` — which a method may only pass along by reference, so a method body on its own can
     * hold no literal at all. Reading the referenced field's initializer is what a developer does to follow it.
     */
    private fun literalsIn(scope: PsiElement): List<PsiLiteralExpression> {
        val direct = PsiTreeUtil.findChildrenOfType(scope, PsiLiteralExpression::class.java)

        val fromFields = PsiTreeUtil.findChildrenOfType(scope, PsiReferenceExpression::class.java)
            .mapNotNull { it.resolve() as? PsiField }
            .distinct()
            .mapNotNull { it.initializer }
            .flatMap { PsiTreeUtil.findChildrenOfType(it, PsiLiteralExpression::class.java) }

        return (direct + fromFields).distinct()
    }

    private fun PsiElement.packageName(): String? {
        val containingClass = this as? PsiClass ?: parentOfType<PsiClass>()
        return containingClass?.qualifiedName?.substringBeforeLast(PathUtils.PACKAGE_SEPARATOR, "")
    }

    private fun com.intellij.psi.PsiType.resolveClass(): PsiClass? =
        (this as? com.intellij.psi.PsiClassType)?.resolve()

    private companion object {
        const val JAVA_SCRIPT_STACK_TYPE = "JavaScriptStack"
    }
}
