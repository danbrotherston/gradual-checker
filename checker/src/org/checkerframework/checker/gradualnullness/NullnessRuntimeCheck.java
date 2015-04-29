package org.checkerframework.checker.gradualnullness;

public class NullnessRuntimeCheck {
    public static boolean runtimeCheck(Object value, String type) {
	System.out.println("Running type check on value: " + value);
	System.out.println("And comparing with type: " + type);
	return false;
    }

    public static void runtimeFailure(Object value, String type) {
	System.out.println("Typecheck failure on value: " + value);
	throw new RuntimeException("Type Error");
    }
}
