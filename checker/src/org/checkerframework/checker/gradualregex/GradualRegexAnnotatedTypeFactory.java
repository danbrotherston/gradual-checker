package org.checkerframework.checker.gradualregex;

import org.checkerframework.checker.regex.classic.RegexClassicAnnotatedTypeFactory;
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

public class GradualRegexAnnotatedTypeFactory extends RegexClassicAnnotatedTypeFactory {
    public GradualRegexAnnotatedTypeFactory(BaseTypeChecker checker) {
	super(checker);
    }

    @Override
    protected boolean addUnannotatedDefaultsToQualifierDefaults(QualifierDefaults defs,
                                                                boolean unused) {
        unused = super.addUnannotatedDefaultsToQualifierDefaults(defs, unused);
	defs.addUnannotatedDefault(AnnotationUtils.fromClass(elements, Dynamic.class),
				   DefaultLocation.RETURNS);
	defs.addUnannotatedDefault(AnnotationUtils.fromClass(elements, Dynamic.class),
				   DefaultLocation.PARAMETERS);
		/*defs.addUnannotatedDefault(AnnotationUtils.fromClass(elements, Dynamic.class),
			       DefaultLocation.UPPER_BOUNDS);
	// defs.addUnannotatedDefault(AnnotationUtils.fromClass(elements, Dynamic.class),
	//		       DefaultLocation.EXPLICIT_UPPER_BOUNDS);*/
	//defs.addUnannotatedDefault(AnnotationUtils.fromClass(elements, Dynamic.class),
	//			   DefaultLocation.FIELD);

	defs.treatAccessibleFieldsAsUnannotated();
	return unused;
    }

    @Override
    protected MultiGraphFactory createQualifierHierarchyFactory() {
	return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
	Set<Class<? extends Annotation>> gradualQualifiers = 
	    getSupportedTypeQualifiersFromAnnotation(
                checker.getClass().getAnnotation(TypeQualifiers.class));
	
	Set<Class<? extends Annotation>> regexQualifiers =
	    getSupportedTypeQualifiersFromAnnotation(
	        checker.getClass().getSuperclass().getAnnotation(TypeQualifiers.class));

	Set<Class<? extends Annotation>> typeQualifiers = new HashSet<Class<? extends Annotation>>();
	typeQualifiers.addAll(gradualQualifiers);
	typeQualifiers.addAll(regexQualifiers);
	return Collections.unmodifiableSet(typeQualifiers);
    }
}
