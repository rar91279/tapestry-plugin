package com.intellij.tapestry.intellij.view.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiDirectory;
import com.intellij.tapestry.core.model.TapestryLibrary;
import icons.TapestryIcons;

public class ComponentsNode extends PackageNode {

    public ComponentsNode(TapestryLibrary library, PsiDirectory psiDirectory, Module module) {
        super(library, psiDirectory, module);

        init(psiDirectory, new PresentationData(psiDirectory.getName(), psiDirectory.getName(),
                                                TapestryIcons.Components, null));
    }

    public ComponentsNode(PsiDirectory psiDirectory, Module module) {
        super(psiDirectory, module);

        init(psiDirectory, new PresentationData(psiDirectory.getName(), psiDirectory.getName(),
                                                TapestryIcons.Components, null));
    }
}
