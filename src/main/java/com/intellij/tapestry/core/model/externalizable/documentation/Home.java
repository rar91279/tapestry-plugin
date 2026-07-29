package com.intellij.tapestry.core.model.externalizable.documentation;

import com.intellij.tapestry.core.model.externalizable.ExternalizableToDocumentation;
import com.intellij.tapestry.core.model.externalizable.documentation.generationchain.DocumentationGenerator;

import java.util.List;

/**
 * Utility class used to generate the documentation home.
 */
public class Home implements ExternalizableToDocumentation {

    private final List<ModuleDoc> _modules;

    public Home(List<ModuleDoc> modules) {
        _modules = modules;
    }

    @Override
    public String getDocumentation() throws Exception {
        return DocumentationGenerator.generate(this);
    }

    public List<ModuleDoc> getModules() {
        return _modules;
    }

    /** A project module and the Tapestry IoC services it contributes (empty for non-Tapestry modules). */
    public static class ModuleDoc {

        private final String _name;
        private final boolean _tapestry;
        private final List<ServiceDoc> _services;

        public ModuleDoc(String name, boolean tapestry, List<ServiceDoc> services) {
            _name = name;
            _tapestry = tapestry;
            _services = services;
        }

        public String getName() {
            return _name;
        }

        public boolean isTapestry() {
            return _tapestry;
        }

        public List<ServiceDoc> getServices() {
            return _services;
        }
    }

    /** A Tapestry IoC service and its documentation. */
    public static class ServiceDoc {

        private final String _id;
        private final String _className;
        private final String _scope;
        private final boolean _eagerLoad;
        private final String _description;

        public ServiceDoc(String id, String className, String scope, boolean eagerLoad, String description) {
            _id = id;
            _className = className;
            _scope = scope;
            _eagerLoad = eagerLoad;
            _description = description;
        }

        public String getId() {
            return _id;
        }

        public String getClassName() {
            return _className;
        }

        public String getScope() {
            return _scope;
        }

        public boolean isEagerLoad() {
            return _eagerLoad;
        }

        public String getDescription() {
            return _description;
        }
    }
}
