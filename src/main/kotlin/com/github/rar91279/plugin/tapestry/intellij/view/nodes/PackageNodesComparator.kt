package com.github.rar91279.plugin.tapestry.intellij.view.nodes

object PackageNodesComparator : Comparator<TapestryNode> {

    override fun compare(a: TapestryNode, b: TapestryNode): Int {
        val aIsPackage = a is DirectoryNode
        val bIsPackage = b is DirectoryNode
        return when {
            aIsPackage && bIsPackage -> a.getPresentableText().compareTo(b.getPresentableText())
            aIsPackage -> -1
            bIsPackage -> 1
            else -> a.getPresentableText().compareTo(b.getPresentableText())
        }
    }
}
