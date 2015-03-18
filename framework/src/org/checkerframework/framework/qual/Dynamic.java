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
 *
 * This annotation applies to every type qualifier hierarchy for which no
 * explicit qualifier is written and which supports the <tt>Dynamic</tt>
 * qualifier.  {@see PolyAll} and {@see PolymorphicQualifier} for more
 * information.
 *
 * To support <tt>Dynamic</tt> in a type system simply add it to the list of
 * {@see TypeQualifiers}, and implement the required methods for Dynamic
 * checking.
 */
@Documented
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@TypeQualifier
@PolymorphicQualifier
public @interface Dynamic {
}
