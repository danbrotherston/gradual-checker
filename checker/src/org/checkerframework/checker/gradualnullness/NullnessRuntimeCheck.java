package org.checkerframework.checker.gradualnullness;

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
	System.out.println("Running type check on value: " + value);
	System.out.println("And comparing with type: " + type);
	if (value == null) {
	    return type.contains("Nullable");
	} else {
	    return true;
	}
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
}
