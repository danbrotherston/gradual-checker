package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to the declaration of a type qualifier specifies that
 * the given annotation should be the default for a particular location,
 * only when the symbol is from untyped code, and only when the
 * conservative untyped checking is turned on.
 *
 * TODO: Document use relative to the other annotations.
 * This qualifier is for type system developers, not end-users.
 *
 * @see DefaultLocation
 * @see DefaultQualifier
 * @see DefaultQualifierInUntyped
 * @see ImplicitFor
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DefaultForInUntyped {
    /**
     * @return the locations to which the annotation should be applied
     */
    DefaultLocation[] value();
}
