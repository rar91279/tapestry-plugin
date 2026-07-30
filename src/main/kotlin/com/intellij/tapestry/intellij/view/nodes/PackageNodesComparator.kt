package com.intellij.tapestry.intellij.view.nodes

object PackageNodesComparator : Comparator<TapestryNode> {

    override fun compare(a: TapestryNode, b: TapestryNode): Int {
        val aIsPackage = a is PackageNode
        val bIsPackage = b is PackageNode
        return when {
            aIsPackage && bIsPackage -> a.getPresentableText().compareTo(b.getPresentableText())
            aIsPackage -> -1
            bIsPackage -> 1
            else -> a.getPresentableText().compareTo(b.getPresentableText())
        }
    }
}
