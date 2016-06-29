package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

import org.checkerframework.framework.gradual.*;
import org.checkerframework.checker.nullness.AbstractNullnessFbcChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.qual.Dynamic;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.javacutil.ErrorReporter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A checker extending the Nullness checker which provides the dynamic
 * qualifier to allow gradual type checking.  This checker overrides
 * the normal defaults and uses dynamic any time untyped code is returning
 * a value.  It then performs runtime checks to ensure validity.
 */
@TypeQualifiers({ Dynamic.class })
@StubFiles("../nullness/astubs/gnu-getopt.astub")
public class GradualNullnessChecker extends AbstractNullnessFbcChecker {
    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
	return new GradualNullnessVisitor(this, true);
    }

    @Override
    public void init(ProcessingEnvironment env) {
	runtimeCheckMethodName =
  	 "org.checkerframework.checker.gradualnullness.NullnessRuntimeCheck.runtimeCheckArgument";

	super.init(env);
	this.trees = Trees.instance(env);

	/*	this.runtimeCheckMethod =
	    (NullnessRuntimeCheck.class).getDeclaredMethod("runtimeCheck",
							   Object.class,
							   String.class);

	this.runtimeCheckFailureMethod =
	    (NullnessRuntimeCheck.class).getDeclaredMethod("runtimeFailure",
							   Object.class,
							   String.class);
	this.runtimeCheckClass = NullnessRuntimeCheck.class;*/
    }

    @Override
    public boolean isGradualTypeSystem() {
	return true;
    }


}
