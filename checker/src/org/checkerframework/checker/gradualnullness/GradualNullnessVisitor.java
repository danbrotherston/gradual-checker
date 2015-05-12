package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

import org.checkerframework.checker.nullness.AbstractNullnessChecker;
import org.checkerframework.checker.nullness.NullnessVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The visitor for the gradual nullness type-system.
 */
public class GradualNullnessVisitor extends NullnessVisitor {

    /**
     * A list of locations that should get a runtime check, given by the
     * TreePath to the location within the AST, as well as the specific
     * value tree that should be tested.
     */
    private Map<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>> runtimeCheckLocations;

    public GradualNullnessVisitor(BaseTypeChecker checker, boolean useFbc) {
	super(checker, useFbc);
	runtimeCheckLocations =
	    new HashMap<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>>();
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
	// Record this location to insert a runtime check.
	runtimeCheckLocations.put(getCurrentPath(),
				  new SimpleEntry<Tree, AnnotatedTypeMirror>(valueTree, varType));

	// Consistency check for nullness should always return true, any
	// dynamic types are consistent with all other types.
	return true;
    }

    /**
     * Gets the list of locations where a runtime check should be placed.
     */
    public Map<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>> getRuntimeCheckLocations() {
	return Collections.unmodifiableMap(runtimeCheckLocations);
    }
}
