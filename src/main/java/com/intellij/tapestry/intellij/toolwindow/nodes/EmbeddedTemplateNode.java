package com.intellij.tapestry.intellij.toolwindow.nodes;

import com.intellij.tapestry.core.model.presentation.*;
import com.intellij.tapestry.core.resource.IResource;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;

public class EmbeddedTemplateNode extends DefaultMutableTreeNode {

    private transient IResource _resource;
    private final String _label;
    private final List<InjectedElement> _embeddedTemplateNodes = new ArrayList<>();

    public EmbeddedTemplateNode(IResource resource, PresentationLibraryElement element) {
        super(resource);

        _resource = resource;
        // Precompute the label here (read action); toString() runs on the EDT during rendering.
        _label = resource.getName().split("." + resource.getExtension())[0];

        if (element.getElementClass().getFile() == null) {
            return;
        }

        switch (element.getElementType()) {
            case COMPONENT:
                addChildElements((TapestryComponent) element, resource.getName());
                break;
            case PAGE:
                addChildElements((Page) element, resource.getName());
                break;
            case MIXIN:
                addChildElements((Mixin) element, resource.getName());
                break;
        }
    }

    private void addChildElements(TapestryComponent component, String resource) {
        for (TemplateElement embedded : component.getEmbeddedComponentsTemplate()) {
            if (embedded.getTemplate().equals(resource)) {
                add(new EmbeddedComponentNode(embedded.getElement()));
                _embeddedTemplateNodes.add(embedded.getElement());
            }
        }
    }

    private void addChildElements(Page page, String resource) {
        for (TemplateElement embedded : page.getEmbeddedComponentsTemplate()) {
            if (embedded.getTemplate().equals(resource)) {
                add(new EmbeddedComponentNode(embedded.getElement()));
                _embeddedTemplateNodes.add(embedded.getElement());
            }
        }
    }

    private void addChildElements(Mixin mixin, String resource) {
        for (TemplateElement embedded : mixin.getEmbeddedComponentsTemplate()) {
            if (embedded.getTemplate().equals(resource)) {
                add(new EmbeddedComponentNode(embedded.getElement()));
                _embeddedTemplateNodes.add(embedded.getElement());
            }
        }
    }

    public List<InjectedElement> getEmbeddedTemplateNodes() {
        return _embeddedTemplateNodes;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return _label;
    }
}
