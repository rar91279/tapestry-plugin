package com.github.rar91279.plugin.tapestry.intellij.view.nodes

/**
 * The view's toolbar toggles, as the nodes see them.
 *
 * Read through an interface rather than off the pane directly for two reasons: the pane resolves via
 * `ProjectView` and so only exists when the UI does — which would make the node layer untestable — and the
 * values have to be read *as children are built*, not captured when the tree was created, or a toggle would
 * only take effect after a restart.
 */
interface TapestryViewOptions {

    /** Whether the Tapestry libraries the module depends on are listed. */
    val showLibraries: Boolean

    /** Whether an element expands to the files it is made of: class, templates, catalogs, assets. */
    val showElementFiles: Boolean

    companion object {

        /** Everything shown — what a tree built outside the pane gets. */
        val DEFAULT: TapestryViewOptions = object : TapestryViewOptions {
            override val showLibraries = true
            override val showElementFiles = true
        }
    }
}
