package org.checkerframework.checker.gradualnullness;

import org.checkerframework.checker.nullness.NullnessAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.Dynamic;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GradualNullnessAnnotatedTypeFactory extends NullnessAnnotatedTypeFactory {
    public GradualNullnessAnnotatedTypeFactory(BaseTypeChecker checker, boolean useFbc) {
	super(checker, useFbc);
    }

    @Override
    protected void addUntypedDefaultsToQualifierDefaults(QualifierDefaults defs) {
	defs.addUntypedDefault(AnnotationUtils.fromClass(elements, Dynamic.class),
			       DefaultLocation.RETURNS);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
	Set<Class<? extends Annotation>> gradualQualifiers = 
	    getSupportedTypeQualifiersFromAnnotation(
                checker.getClass().getAnnotation(TypeQualifiers.class));
	
	Set<Class<? extends Annotation>> nullnessQualifiers =
	    getSupportedTypeQualifiersFromAnnotation(
		checker.getClass().getSuperclass().getAnnotation(TypeQualifiers.class));

	Set<Class<? extends Annotation>> typeQualifiers = new HashSet<Class<? extends Annotation>>();
	typeQualifiers.addAll(gradualQualifiers);
	typeQualifiers.addAll(nullnessQualifiers);
	return Collections.unmodifiableSet(typeQualifiers);
    }
}
