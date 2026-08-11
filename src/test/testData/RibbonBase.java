package com.testapp.base;

import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;

public abstract class RibbonBase
{
    @Property
    @Parameter
    protected String container;
}
