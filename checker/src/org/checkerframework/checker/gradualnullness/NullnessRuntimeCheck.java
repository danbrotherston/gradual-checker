package org.checkerframework.checker.gradualnullness;

import org.checkerframework.checker.nullness.qual.PolyNull;

/**
 * @author danbrotherston
 *
 * This is the actual runtime check inserted into the target program.
 * These methods are static, with no inheritance to improve the chances that
 * the method dispatch can be optimized fully by the compiler.  Since these
 * occur at every typed/untyped boundary, they should be as efficient and
 * fast as possible.  The types of the arguments should match the required
 * inserted check, but the names are configured in the runtime class.
 */
public class NullnessRuntimeCheck {
    /**
     * This method is called to actually check a runtime value.
     *
     * @param value The actual value of the variable at runtime.
     * @param type The string representation of the type of the variable at
     *             compile time.
     * @return True if the type is compatible with the value, false otherwise.
     */
    public static boolean runtimeCheck(Object value, String type) {
	System.err.println("Nullness Check Value: " + value + " type: " + type);
	Thread.dumpStack();
	return true;
	/*	if (value == null) {
	    return type.contains("Nullable");
	} else {
	    return true;
	    }*/
    }

    /**
     * This method is called in the event the above function ever returns
     * false.  The author can decide what to do here, log an error, throw
     * an error, or even depend on an environment variable or configuration
     * option.  It shouldn't be called frequently.
     *
     * @param value The runtime value which caused the runtime type check failure.
     * @param type The compile time value which the above type is not compatible
     *             with.
     */
    public static void runtimeFailure(Object value, String type) {
	System.out.println("Typecheck failure on value: " + value);
	throw new RuntimeException("Type Error");
    }

    /**
     * This method performs a runtime check on a value and either throws
     * an error or returns the value.  It is used to check the parameters
     * to functions because it can be used without creating a temporary variable
     * but only works in this context because the value won't have to be
     * evaluated multiple times.  In the eventual AST tree, the compiler will
     * be responsible for casting the return type to the proper type to ensure
     * that the correct method is called.
     *
     * @param value The actual value of the argument at runtime.
     * @param type The compile time type which the above value must be compatible
     *             with.
     *
     * @return The provided value, always, unless an error is thrown.
     */
    public static @PolyNull Object runtimeCheckArgument(@PolyNull Object value, String type) {
	if (!runtimeCheck(value, type)) {
	    runtimeFailure(value, type);
	}

	return value;
    }
}
