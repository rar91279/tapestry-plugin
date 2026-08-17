package com.github.rar91279.plugin.tapestry.intellij

import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.core.util.LocalizationUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.intellij.navigation.GotoRelatedItem
import com.intellij.navigation.GotoRelatedProvider
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.parentOfType

/**
 * Hops between the files that make up one Tapestry element: the page/component class, its templates, its
 * message catalogs and the assets it imports. Reachable through *Navigate | Related Symbol* (`Ctrl+Alt+Home`).
 *
 * Anchors on the class and on the template, and back from a message catalog through the class named after it.
 * An asset is not an anchor — the same stylesheet is imported by any number of elements, so there is no single
 * element to navigate back to.
 *
 * Stylesheets and scripts are listed as separate groups, and each `@Import(stack = …)` as a group of its own
 * holding the contribution declaring it and the files it bundles.
 */
class TapestryGotoRelatedProvider : GotoRelatedProvider() {

    override fun getItems(psiElement: PsiElement): List<GotoRelatedItem> {
        // The related-items collection has been seen passing elements from a superseded view provider, whose
        // file access throws. Nothing to navigate from either way.
        if (!psiElement.isValid) return emptyList()

        val file = psiElement.containingFile ?: return emptyList()
        val project = TapestryUtils.getTapestryProject(file) ?: return emptyList()
        val element = findElement(file, project) ?: return emptyList()

        val stacks = stacksOf(file, project, element)

        val related = ArrayList<Pair<PsiElement, String>>()
        element.elementClass?.let { related.add(it to CLASS_GROUP) }
        element.template.forEach { related.add(it to TEMPLATE_GROUP) }
        element.messageCatalog.forEach { related.add(it to MESSAGES_GROUP) }
        element.assets.forEach { related.add(it to it.assetGroup()) }

        for (stack in stacks) {
            val group = stack.group()
            related.add(stack.declaration to group)
            stack.assets.forEach { related.add(it to group) }
        }

        val current = file.originalFile.virtualFile

        // Assets a stack pulls out of a webjar are left out: they are files inside a jar, and listing them
        // without being able to open them was only noise.
        return related
            .filter { (target, _) -> target.containingFile?.virtualFile != current }
            // Per group, so one file can appear under both a stack and a direct import, and so two stacks
            // contributed in the same module class both keep their declaration item.
            .distinctBy { (target, group) -> target.containingFile?.virtualFile to group }
            .map { (target, group) -> relatedItem(target, group) }
    }

    /**
     * A file or a class presents itself; a stack contribution is a string literal, which has no name of its
     * own, so it is labelled with the method it sits in — navigation still lands on the literal.
     */
    private fun relatedItem(target: PsiElement, group: String): GotoRelatedItem =
        if (target is PsiFile || target is PsiNamedElement) GotoRelatedItem(target, group)
        else object : GotoRelatedItem(target, group) {
            override fun getCustomName(): String = target.parentOfType<PsiMethod>()
                ?.let { "${it.containingClass?.name}.${it.name}" }
                ?: target.containingFile.name
        }

    /** The stacks the element imports, with the ones they include in turn. */
    private fun stacksOf(
        file: PsiFile, project: TapestryProject, element: PresentationLibraryElement
    ): List<JavaScriptStack> {
        val stackNames = element.javaScriptStackNames
        if (stackNames.isEmpty()) return emptyList()

        val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return emptyList()

        return JavaScriptStackResolver(module, project).resolve(stackNames)
    }

    /** Each stack gets a group of its own: what it is, and everything it bundles. */
    private fun JavaScriptStack.group(): String = STACK_GROUP_PREFIX + name

    /** Stylesheets and scripts are listed apart: a page's css and its js are looked for separately. */
    private fun PsiFile.assetGroup(): String = when (virtualFile?.extension?.lowercase()) {
        "css", "less", "scss" -> STYLESHEETS_GROUP
        "js", "mjs", "coffee" -> JAVASCRIPT_GROUP
        else -> ASSETS_GROUP
    }

    private fun findElement(file: PsiFile, project: TapestryProject): PresentationLibraryElement? {
        project.findElementByTemplate(file)?.let { return it }

        for (psiClass in (file as? PsiClassOwner)?.classes.orEmpty()) {
            project.findElement(psiClass)?.let { return it }
        }

        if (!file.name.endsWith(TapestryConstants.PROPERTIES_FILE_EXTENSION)) return null

        // A message catalog sits next to its class, under the same name minus the localization suffix.
        val directory = file.containingDirectory ?: return null
        val packageName = JavaDirectoryService.getInstance().getPackage(directory)?.qualifiedName.orEmpty()
        val className = LocalizationUtils.unlocalizeFileName(file.name)
            .removeSuffix(TapestryConstants.PROPERTIES_FILE_EXTENSION)
        val fqn = if (packageName.isEmpty()) className else "$packageName.$className"

        val psiClass = JavaPsiFacade.getInstance(file.project).findClass(fqn, file.resolveScope) ?: return null

        return project.findElement(psiClass)
    }

    companion object {

        /**
         * The popup shows groups in `String.compareTo` order — `NavigationUtil.collectRelatedItems` sorts the
         * items by group name and nothing else — so the reading order has to be encoded in the names.
         *
         * Each name carries as many zero-width spaces as its position. The prefix doesn't render, and a
         * shorter prefix always sorts first: at the position where the prefixes differ, one name has a letter
         * and the other a zero-width space, and every letter sorts below U+200B.
         *
         * ponytail: a sort key smuggled into a display string, because the platform offers no other handle on
         * group order. If the zero-width spaces ever render as boxes, fall back to leading real spaces —
         * visible indentation, same ordering, reversed (a space sorts *below* letters).
         */
        private fun ordered(position: Int, name: String) = ZERO_WIDTH_SPACE.repeat(position) + name

        /** Strips the ordering prefix off a group name. */
        fun groupName(group: String?): String = group?.replace(ZERO_WIDTH_SPACE, "").orEmpty()

        /** U+200B. Invisible on purpose — [ordered] explains what it is doing here. */
        private const val ZERO_WIDTH_SPACE = "​"

        private val CLASS_GROUP = ordered(0, "Class")
        private val TEMPLATE_GROUP = ordered(1, "Template")
        private val MESSAGES_GROUP = ordered(2, "Messages")
        private val STYLESHEETS_GROUP = ordered(3, "Stylesheets")
        private val JAVASCRIPT_GROUP = ordered(4, "Javascript")
        private val ASSETS_GROUP = ordered(5, "Assets")
        private val STACK_GROUP_PREFIX = ordered(6, "Stack: ")
    }
}
