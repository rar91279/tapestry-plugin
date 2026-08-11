package com.testapp.components;

import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;

public class PublicInstrumentedField
{
    @Property
    public String <error descr="A field annotated with @Property must be instrumented, and may not be public">badProperty</error>;

    @Parameter
    public String <error descr="A field annotated with @Parameter must be instrumented, and may not be public">badParameter</error>;

    @Persist
    public String <error descr="A field annotated with @Persist must be instrumented, and may not be public">badPersist</error>;

    @Component
    public String <error descr="A field annotated with @Component must be instrumented, and may not be public">badComponent</error>;

    @Inject
    public String <error descr="A field annotated with @Inject must be instrumented, and may not be public">badInject</error>;

    @Property
    protected String okProtected;

    @Parameter
    private String okPrivate;

    public String plainField;
}
