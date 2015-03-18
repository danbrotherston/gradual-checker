package org.checkerframework.checker.gradualnullness;

import org.checkerframework.checker.nullness.NullnessAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.Dynamic;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationUtils;

public class GradualNullnessAnnotatedTypeFactory extends NullnessAnnotatedTypeFactory {
    public GradualNullnessAnnotatedTypeFactory(BaseTypeChecker checker, boolean useFbc) {
	super(checker, useFbc);
    }

    @Override
    protected void addUntypedDefaultsToQualifierDefaults(QualifierDefaults defs) {
	defs.addUntypedDefault(AnnotationUtils.fromClass(elements, Dynamic.class),
			       DefaultLocation.RETURNS);
    }
}
