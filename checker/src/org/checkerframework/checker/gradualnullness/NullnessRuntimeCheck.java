package org.checkerframework.checker.gradualnullness;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class NullnessRuntimeCheck {
    public static boolean runtimeCheck(Object value, AnnotatedTypeMirror type) {
	System.out.println("Running type check on value: " + value.toString());
	return false;
    }

    public static void runtimeFailure(Object value, AnnotatedTypeMirror type) {
	System.out.println("Typecheck failure on value: " + value.toString());
    }
}
