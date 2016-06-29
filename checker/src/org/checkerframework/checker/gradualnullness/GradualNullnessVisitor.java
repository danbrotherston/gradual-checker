package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

import org.checkerframework.checker.nullness.AbstractNullnessChecker;
import org.checkerframework.checker.nullness.NullnessVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.Dynamic;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

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

    @Override
    protected boolean dynamicCheck(AnnotatedTypeMirror valueType,
				   AnnotatedTypeMirror varType,
				   Tree valueTree) {
	// System.out.println("ValueType: " + valueType + " and tree: " + valueTree);
	// System.out.println("VarType: " + varType);

	// Record this location to insert a runtime check.
	if (!this.inSyntheticMethod && !this.existingCheckedValues.contains(valueTree)) {
	    //System.err.println("Putting check location: " + getCurrentPath());
	    //System.err.println("Putting value Tree: " + valueTree);
	    //System.err.println("Class: " + valueTree.getClass());
	    //System.err.println("VarClass: " + valueTree.getClass());
	    // Thread.dumpStack();
	    if (this.inMethod) {
		if (varType.hasAnnotation(NONNULL)) {
		    //System.err.println("Adding check location");
		    existingCheckedValues.add(valueTree);
		    runtimeCheckLocations.put(getCurrentPath(),
	              new SimpleEntry<Tree, AnnotatedTypeMirror>(valueTree, varType));
		}
	    }
	}

	// Consistency check for nullness should always return true, any
	// dynamic types are consistent with all other types.
	return true;
    }

    @Override
    protected void checkForNullability(ExpressionTree tree, String errMsg) {
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(tree);
	AnnotatedTypeMirror targetType =
	    AnnotatedTypeMirror.createType(type.getUnderlyingType(), atypeFactory, false);
	targetType.addAnnotation(NONNULL);

        AnnotationMirror dyn = AnnotationUtils.fromClass(elements, Dynamic.class);

	boolean success = false;
	if (AnnotatedTypes.containsModifier(type, dyn) && !this.inSyntheticMethod) {
	    success = dynamicCheck(type, targetType, tree);
	} else {
	    success = type.hasEffectiveAnnotation(NONNULL);
	}

	if (!success) {	
	    checker.report(Result.failure(errMsg, tree), tree);
	}
    }
}
