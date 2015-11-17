package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * @author danbrotherston
 *
 * This is a specialization of the replacing tree translator that takes a list of
 * runtime check locations (as values which need to tested) and replaces their
 * enclosing statements with runtime checks.
 */
public class RuntimeCheckInserterTreeTranslator extends GeneralTreeTranslator {

    /**
     * Builder to create runtime checks for the locations which need a check.
     */
    RuntimeCheckBuilder checkBuilder = null;
    
    /**
     * Locations which need runtime checks added.
     */
    Map<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>>
	replacementLocations = null;

    /**
     * Stack keeping track of our position within the tree, allowing us to get
     * a node's parent node.
     */
    Stack<JCTree> treeStack = new Stack<JCTree>();

    /**
     * Map of trees to types that need to be checked at runtime.
     */
    Map<Tree, AnnotatedTypeMirror> replacementMap = null;

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
    public RuntimeCheckInserterTreeTranslator(GradualNullnessChecker c,
					      ProcessingEnvironment env,
					      TreePath p,
					      Map<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>>
					      replacementLocations,
					      RuntimeCheckBuilder checkBuilder) {
	super(c, env, p);
	this.checkBuilder = checkBuilder;
	this.replacementLocations = replacementLocations;
	this.replacementMap = new HashMap<Tree, AnnotatedTypeMirror>();
	for (Map.Entry<Tree, AnnotatedTypeMirror> entry : replacementLocations.values()) {
	    this.replacementMap.put(entry.getKey(), entry.getValue());
	}
    }

    @Override
    public void aboutToVisitTree(JCTree that) {
	treeStack.push(that);
    }

    @Override
    public void visitTree(JCTree that) {
	if (this.replacementMap.containsKey(that)) {
	    // Do something.
	}
	treeStack.pop();
    }
}
