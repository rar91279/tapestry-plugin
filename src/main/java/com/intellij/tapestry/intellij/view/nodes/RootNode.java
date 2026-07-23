package com.intellij.tapestry.intellij.view.nodes;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.tapestry.intellij.util.TapestryUtils;
import com.intellij.ui.treeStructure.SimpleNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Tapestry view root node.
 */
public class RootNode extends SimpleNode {

    private static final String ID = "ROOT";
    protected Object _myElement;

    public RootNode(@NotNull final Project project) {
        super(project);

        _myElement = ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getElement() {
        return _myElement;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SimpleNode @NotNull [] getChildren() {
        final List<AbstractModuleNode> newNodes = new ArrayList<>();
        final Module[] allTapestryModules = TapestryUtils.getAllTapestryModules(myProject);

        for (final Module module : allTapestryModules)
            newNodes.add(new ModuleNode(module));


        return newNodes.<AbstractModuleNode>toArray(new AbstractModuleNode[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object @NotNull [] getEqualityObjects() {
        return new Object[]{_myElement};
    }
}
