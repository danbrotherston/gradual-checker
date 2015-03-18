package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A type qualifier representing a dynamic runtime value.  A type system
 * can support this type qualifier and then override defaults for
 * untyped code to allow it to specify dynamic as the return type from
 * untyped function calls.
 */
@Documented
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@TypeQualifier
public @interface Dynamic {
}
