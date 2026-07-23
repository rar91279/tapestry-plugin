package com.intellij.tapestry.intellij.view.nodes;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiDirectory;
import com.intellij.tapestry.core.model.TapestryLibrary;

public class LibraryNode extends PackageNode {

    public LibraryNode(TapestryLibrary library, PsiDirectory psiDirectory, Module module) {
        super(library, psiDirectory, module);

        init(psiDirectory, new PresentationData(psiDirectory.getName(), psiDirectory.getName(),
                                                AllIcons.Nodes.PpLib, null));
    }
}
