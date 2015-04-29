package org.checkerframework.checker.gradualnullness;

import org.checkerframework.checker.nullness.NullnessAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.Dynamic;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
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
    protected MultiGraphFactory createQualifierHierarchyFactory() {
	return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
	return new NullnessQualifierHierarchy(factory, (Object []) null);
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
