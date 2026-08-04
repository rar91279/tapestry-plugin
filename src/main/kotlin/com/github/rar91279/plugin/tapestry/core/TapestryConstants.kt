package com.github.rar91279.plugin.tapestry.core

object TapestryConstants {

    /** The Tapestry namespaces used in templates. */
    const val TEMPLATE_NAMESPACE: String = "http://tapestry.apache.org/schema/tapestry_5_1_0.xsd"
    const val TEMPLATE_NAMESPACE2: String = "http://tapestry.apache.org/schema/tapestry_5_0_0.xsd"
    const val TEMPLATE_NAMESPACE3: String = "http://tapestry.apache.org/schema/tapestry_5_3.xsd"
    const val TEMPLATE_NAMESPACE4: String = "http://tapestry.apache.org/schema/tapestry_5_4.xsd"

    /** The Tapestry namespace used for parameters. */
    const val PARAMETERS_NAMESPACE: String = "tapestry:parameter"

    /** Service builder method name prefix. */
    const val SERVICE_BUILDER_METHOD_PREFIX: String = "build"

    /** Service autobuilder method name. */
    const val SERVICE_AUTOBUILDER_METHOD_NAME: String = "bind"

    /** Regexp of a service builder method. */
    const val SERVICE_BUILDER_METHOD_REGEXP: String = SERVICE_BUILDER_METHOD_PREFIX + "[\\w\$]*"

    /** Names of the file templates for new Tapestry elements. */
    const val MODULE_BUILDER_CLASS_TEMPLATE_NAME: String = "Tapestry Ioc Module Builder Class.java"
    const val COMPONENT_CLASS_TEMPLATE_NAME: String = "Tapestry Component Class.java"
    const val COMPONENT_TEMPLATE_TEMPLATE_NAME: String = "Tapestry Component Template.html"
    const val PAGE_CLASS_TEMPLATE_NAME: String = "Tapestry Page Class.java"
    const val PAGE_TEMPLATE_TEMPLATE_NAME: String = "Tapestry Page Template.html"
    const val MIXIN_CLASS_TEMPLATE_NAME: String = "Tapestry Mixin Class.java"
    const val START_PAGE_TEMPLATE_TEMPLATE_NAME: String = "Tapestry Start Page Template.html"
    const val START_PAGE_CLASS_TEMPLATE_NAME: String = "Tapestry Start Page Class.java"
    const val POM_TEMPLATE_NAME: String = "Tapestry Project Pom.xml"

    /** Base packages for the Tapestry elements. */
    const val PAGES_PACKAGE: String = "pages"
    const val COMPONENTS_PACKAGE: String = "components"
    const val MIXINS_PACKAGE: String = "mixins"
    const val SERVICES_PACKAGE: String = "services"
    const val BASE_PACKAGE: String = "base"

    @JvmField
    val ELEMENT_PACKAGES: Array<String> = arrayOf(PAGES_PACKAGE, COMPONENTS_PACKAGE, BASE_PACKAGE, MIXINS_PACKAGE)

    /** The suffix of the module builder class. */
    const val MODULE_BUILDER_SUFIX: String = "Module"

    /** The extension of template files. */
    const val TEMPLATE_FILE_EXTENSION: String = "tml"

    /** The extension of property files. */
    const val PROPERTIES_FILE_EXTENSION: String = ".properties"

    /** The base package for the Tapestry core library. */
    const val CORE_LIBRARY_PACKAGE: String = "org.apache.tapestry5.corelib"

    /** The base package for the Tapestry ioc library. */
    const val IOC_LIBRARY_PACKAGE: String = "org.apache.tapestry5.ioc"

    /** Tapestry core annotations. */
    const val CORE_INJECT_ANNOTATION: String = "org.apache.tapestry5.annotations.Inject"
    const val PROPERTY_ANNOTATION: String = "org.apache.tapestry5.annotations.Property"
    const val COMPONENT_ANNOTATION: String = "org.apache.tapestry5.annotations.Component"
    const val EVENT_ANNOTATION: String = "org.apache.tapestry5.annotations.OnEvent"
    const val INJECT_PAGE_ANNOTATION: String = "org.apache.tapestry5.annotations.InjectPage"
    const val MIXIN_ANNOTATION: String = "org.apache.tapestry5.annotations.Mixin"

    /** Provided module id. */
    const val BUILTIN_MODULE_ID: String = "tapestry.ioc"

    /** The Tapestry filter class. */
    const val FILTER_CLASS: String = "org.apache.tapestry5.TapestryFilter"

    /** The Tapestry Home page. */
    const val HOME_PAGE: String = "Start"

    /** The prefix of the default parameter methods. */
    const val DEFAULT_PARAMETER_METHOD_PREFIX: String = "default"

    const val EL_LANGUAGE: String = "TML"
}
