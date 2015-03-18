package org.checkerframework.checker.gradualnullness;

import org.checkerframework.checker.nullness.AbstractNullnessChecker;
import org.checkerframework.checker.nullness.NullnessVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * The visitor for the gradual nullness type-system.
 */
public class GradualNullnessVisitor extends NullnessVisitor {
    public GradualNullnessVisitor(BaseTypeChecker checker, boolean useFbc) {
	super(checker, useFbc);
    }

    @Override
    public GradualNullnessAnnotatedTypeFactory createTypeFactory() {
	// See base class implementation for pain.
	return new GradualNullnessAnnotatedTypeFactory(checker,
            ((AbstractNullnessChecker)checker).useFbc);
    }
}
