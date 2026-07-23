package com.intellij.tapestry.core.model.externalizable.totemplatechain;

import com.intellij.tapestry.core.util.chain.Command;
import com.intellij.tapestry.core.util.chain.Context;

/**
 * Base class for all to template externalizers.
 */
public class ToTemplateExternalizer implements Command {

    private ExternalizeToTemplateContext _context;

    @Override
    public boolean execute(Context context) throws Exception {
        if (!(context instanceof ExternalizeToTemplateContext))
            return false;

        _context = (ExternalizeToTemplateContext) context;

        return true;
    }

    ExternalizeToTemplateContext getContext() {
        return _context;
    }
}
