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

    /**
     * A list of locations that should get a runtime check, given by the
     * TreePath to the location within the AST, as well as the specific
     * value tree that should be tested.
     */
    private Map<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>> runtimeCheckLocations;

    /**
     * A list of locations already added for checks to ensure no duplicates.
     */
    private Set<Tree> existingCheckedValues;

    /**
     * A flag to indicate if we are processing a method which is synthetic,
     * that we generated for type safety.  We don't want to do any dynamic checks
     * in these methods as we are already peforming dynamic checks in the right places.
     */
    private boolean inSyntheticMethod = false;

    /**
     * This field stores the postfix to apply to save methods, and all
     * checked method calls.
     */
    protected final String safeMethodNamePostfix = "_$safe";

    /**
     * The postfix to use for the runtime method selector to determine whether to
     * invoke the safe version or the unsafe version of a method.
     */
    protected final String maybeMethodNamePostfix = "_$maybe";

    /**
     * The name used on all constructor methods.
     */
    protected final String constructorName = "<init>";

    /**
     * Thie class of the dummy marker class used to indicate safe constructors.
     */
    protected final Class<SafeConstructorMarkerDummy> constructorMarkerClass =
	SafeConstructorMarkerDummy.class;

    /**
     * Keep the current class we're processing so that we can look up if a given
     * method has a safe version or not.
     */
    private ClassTree currentClassTree = null;

    public GradualNullnessVisitor(BaseTypeChecker checker, boolean useFbc) {
	super(checker, useFbc);
	runtimeCheckLocations =
	    new HashMap<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>>();
	existingCheckedValues = new HashSet<Tree>();
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
	    // System.err.println("Putting check location: " + getCurrentPath());
	    // System.err.println("Putting value Tree: " + valueTree);
	    // Thread.dumpStack();
	    existingCheckedValues.add(valueTree);
	    runtimeCheckLocations.put(getCurrentPath(),
				  new SimpleEntry<Tree, AnnotatedTypeMirror>(valueTree, varType));
	}

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

    @Override
    public Void visitClass(ClassTree node, Void p) {
	ClassTree prevCurrentClassTree = this.currentClassTree;
	this.currentClassTree = node;

	Void val = super.visitClass(node, p);

	this.currentClassTree = prevCurrentClassTree;
	return val;
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
	if (node.getName().toString().endsWith(this.maybeMethodNamePostfix) ||
	    this.currentClassTreeContainsSafeVersionOf(node) ||
	    this.currentClassTreeContainsSafeVersionOfConstructor(node)) {
	    // System.out.println("Method: " + node.getName() + " is synthetic");
	    this.inSyntheticMethod = true;
	} else {
	    // System.out.println("Method: " + node.getName() + " is not synthetic");
	    this.inSyntheticMethod = false;
	}

	return super.visitMethod(node, p);
    }

    private boolean currentClassTreeContainsSafeVersionOfConstructor(MethodTree node) {
	if (!node.getName().toString().equals(this.constructorName)) {
	    return false;
	}

	for (Tree member : this.currentClassTree.getMembers()) {
	    if (member instanceof MethodTree) {
		MethodTree methodMember = (MethodTree) member;
		if (methodMember.getName().toString().equals(this.constructorName)) {
		    boolean allParamsMatch =
			node.getParameters().size() == methodMember.getParameters().size() - 1;

		    if (!allParamsMatch) { continue; }

		    for (int i = 0; i < node.getParameters().size(); i++) {
			String paramType =
			    methodMember.getParameters().get(i + 1).getType().toString();
			allParamsMatch &=
			    node.getParameters().get(i).getType().toString().equals(paramType);
		    }

		    String firstParam = methodMember.getParameters().get(0).getType().toString();
		    if (allParamsMatch &&
			firstParam.equals(this.constructorMarkerClass.getTypeName())) {
			return true;
		    }
		}
	    }
	}

	return false;
    }

    private boolean currentClassTreeContainsSafeVersionOf(MethodTree node) {
	String newName = node.getName() + this.safeMethodNamePostfix;

	// System.out.println("Checking for safe version of: " + node.getName());
	for (Tree member : this.currentClassTree.getMembers()) {
	    if (member instanceof MethodTree) {
		MethodTree methodMember = (MethodTree) member;
		// System.out.println("Checking : " + methodMember.getName());
		// System.out.println("Newname: " + newName);

		if (methodMember.getName().toString().equals(newName)) {
		    boolean allParamsMatch =
			node.getParameters().size() == methodMember.getParameters().size();
		    // System.out.println("params: " + node.getParameters());
		    // System.out.println("Otherparams: " + methodMember.getParameters());
		    if (!allParamsMatch) { continue; }

		    for (int i = 0; i < node.getParameters().size(); i++) {
			String paramType = methodMember.getParameters().get(i).getType().toString();
			allParamsMatch &=
			    node.getParameters().get(i).getType().toString().equals(paramType);
		    }

		    if (allParamsMatch) {
			// System.out.println("Matching all params");
			return true;
		    } else {
			// System.out.println("Doesn't match all params");
		    }
		}
	    }
	}

	return false;
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
