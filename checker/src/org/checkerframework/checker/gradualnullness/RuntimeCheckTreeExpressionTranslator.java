package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.processing.ProcessingEnvironment;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.trees.TreeBuilder;

/**
 * @author danbrotherston
 *
 * This is a specialization of the replacing tree translator that takes a list of
 * runtime check locations (as values which need to tested) and replaces their
 * enclosing statements with runtime checks.
 */
public class RuntimeCheckTreeExpressionTranslator extends GeneralTreeTranslator {

    /**
     * Stores the list of locations where we need to perform runtime checks.
     */
    protected final Map<JCTree, AnnotatedTypeMirror> replacementLocations;

    /**
     * This field stores a tree builder used for building trees used in renaming
     * and converting methods.
     */
    protected final TreeBuilder builder;

    /**
     * This field stores the processing environment this translator was
     * constructed with.
     */
    protected final ProcessingEnvironment procEnv;

    /**
     * The type factory to use for manipulating annotated types.
     */
    protected final AnnotatedTypeFactory aTypeFactory;

    /**
     * String references the runtimeCheckArgument function in the NullnessRunctimeCheck
     * class.
     */
    protected final String argumentCheckFunctionName =
	"org.checkerframework.checker.gradualnullness.NullnessRuntimeCheck.runtimeCheckArgument";

    /**
     * @constructor
     * @param c The checker invoking this particular translator.
     * @param env The annotation processing environment this is invoked from.
     * @param replacementLocations All the information needed to build the runtime checks.
     *        The location map is built as such: <TreePath, <Tree, TypeMirror>>
     * Where TreePath is the path within the tree to the value node to replace.
     *       Tree is the actual value node to replace.
     *       AnnotatedTypeMirror is the compile time type this value must conform to at runtime.
     * @param checkBuilder A properly configured RuntimeCheckBuilder to use to build the runtime
     *                     checks.
     */
    public RuntimeCheckTreeExpressionTranslator(GradualNullnessChecker c,
						ProcessingEnvironment env,
						TreePath p,
						Map<TreePath,
						    Map.Entry<Tree, AnnotatedTypeMirror>>
						replacementLocations,
						RuntimeCheckBuilder checkBuilder) {
	// Since this must occur in a method which we can call here to satisfy Java we
	// have to do some finagling.
	super(c, env, p);
	this.builder = new TreeBuilder(env);
	this.aTypeFactory = c.getTypeFactory();
	this.procEnv = env;
	this.replacementLocations = new HashMap<JCTree, AnnotatedTypeMirror>();

	for(Map.Entry<Tree, AnnotatedTypeMirror> entry : replacementLocations.values()) {
	    this.replacementLocations.put((JCTree)(entry.getKey()), entry.getValue());
	}
    }

    @Override
    public void visitTree(JCTree that) {
	if (this.replacementLocations.containsKey(that)) {
	    result = buildTestCall(that);
	    System.err.println("Old: " + that + " new: " + result);
	} else {
	    result = that;
	}
    }

    private JCTree buildTestCall(JCTree that) {
	JCTree.JCExpression checkerFunction = dotsExp(this.argumentCheckFunctionName);
	AnnotatedTypeMirror type = this.replacementLocations.get(that);
	String literalType = type.toString();
	JCTree.JCExpression methodCall = maker.Apply(null, checkerFunction,
						     List.of((JCTree.JCExpression)that,
							     maker.Literal(literalType)));
	JCTree.JCExpression castMethodCall =
	    maker.TypeCast(that.type, methodCall);
							    
	//	attr.attribExpr(methodCall, this.getAttrEnv(that), that.getType());
	this.attribute(castMethodCall,
		       (JCTree.JCExpression)that);
	return castMethodCall;
    }
}
