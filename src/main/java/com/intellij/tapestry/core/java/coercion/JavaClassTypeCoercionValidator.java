package com.intellij.tapestry.core.java.coercion;

import com.intellij.psi.CommonClassNames;
import com.intellij.tapestry.core.java.IJavaClassType;
import com.intellij.tapestry.core.util.chain.Command;
import com.intellij.tapestry.core.util.chain.Context;

import java.util.*;

public class JavaClassTypeCoercionValidator implements Command {

    /**
     * List of all possible coercions.
     * The key is the type to coerce to, the values are the types that can be coerced to the key type.
     */
    private static final Map<String, List<String>> CLASS_COERCION_MAP = new HashMap<>();

    static {
        CLASS_COERCION_MAP.put(CommonClassNames.JAVA_LANG_STRING, Collections.<String>singletonList(
          CommonClassNames.JAVA_LANG_OBJECT));

        CLASS_COERCION_MAP.put("java.lang.Double", Arrays.<String>asList(
                CommonClassNames.JAVA_LANG_STRING,
                "java.math.BigDecimal",
                "java.lang.Long",
                "java.lang.Float"));

        CLASS_COERCION_MAP.put("java.math.BigDecimal", Collections.<String>singletonList(
          CommonClassNames.JAVA_LANG_STRING
        ));

        CLASS_COERCION_MAP.put("java.math.BigInteger", Collections.<String>singletonList(
          CommonClassNames.JAVA_LANG_STRING
        ));

        CLASS_COERCION_MAP.put("java.lang.Long", Arrays.<String>asList(
                CommonClassNames.JAVA_LANG_STRING,
                "java.lang.Number",
                "org.apache.tapestry5.ioc.util.TimeInterval"
        ));

        CLASS_COERCION_MAP.put("java.lang.Byte", Collections.<String>singletonList(
          "java.lang.Long"
        ));

        CLASS_COERCION_MAP.put("java.lang.Short", Collections.<String>singletonList(
          "java.lang.Long"
        ));

        CLASS_COERCION_MAP.put("java.lang.Integer", Collections.<String>singletonList(
          "java.lang.Long"
        ));

        CLASS_COERCION_MAP.put("java.lang.Float", Collections.<String>singletonList(
          "java.lang.Double"
        ));

        CLASS_COERCION_MAP.put("java.lang.Boolean", Collections.<String>singletonList(
          CommonClassNames.JAVA_LANG_OBJECT
        ));

        CLASS_COERCION_MAP.put(CommonClassNames.JAVA_UTIL_LIST, Collections.<String>singletonList(
          CommonClassNames.JAVA_LANG_OBJECT
        ));

        CLASS_COERCION_MAP.put("org.apache.tapestry5.grid.GridDataSource", Collections.<String>singletonList(
          CommonClassNames.JAVA_UTIL_LIST
        ));

        CLASS_COERCION_MAP.put("org.apache.tapestry5.ioc.util.TimeInterval", Collections.<String>singletonList(
          CommonClassNames.JAVA_LANG_STRING
        ));

        CLASS_COERCION_MAP.put("java.text.DateFormat", Collections.<String>singletonList(
          CommonClassNames.JAVA_LANG_STRING
        ));
    }

    @Override
    public boolean execute(Context context) throws Exception {
        if (!(((CoercionContext) context).getSourceType() instanceof IJavaClassType && ((CoercionContext) context).getTargetType() instanceof IJavaClassType))
            return false;

        List<String> coercions = CLASS_COERCION_MAP.get(((IJavaClassType) ((CoercionContext) context).getTargetType()).getFullyQualifiedName());

        if (coercions == null)
            return false;

        for (String typeName : coercions) {
            IJavaClassType type = ((CoercionContext) context).getProject().getJavaTypeFinder().findType(typeName, true);

            if (type != null && type.isAssignableFrom(((CoercionContext) context).getSourceType())) {
                ((CoercionContext) context).setResult(true);

                return true;
            }
        }

        return true;
    }
}
