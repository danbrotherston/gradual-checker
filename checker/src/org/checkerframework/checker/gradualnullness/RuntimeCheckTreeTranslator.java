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

import javax.annotation.processing.ProcessingEnvironment;

/**
 * @author danbrotherston
 *
 * This is a specialization of the replacing tree translator that takes a list of
 * runtime check locations (as values which need to tested) and replaces their
 * enclosing statements with runtime checks.
 */
public class RuntimeCheckTreeTranslator extends ReplacingTreeTranslator {
    // I hate you Java.
    static private Map<JCTree, JCTree> leftToAttributeStatic;
    private Map<JCTree, JCTree> leftToAttribute;

    /**
     * Get a list of trees which need to be attributed after running this translator.
     * @returns A list of nodes modified by this translator.
     */
    public Map<JCTree, JCTree> getUnattributedTrees() {
	return Collections.unmodifiableMap(leftToAttribute);
    }

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
    public RuntimeCheckTreeTranslator(GradualNullnessChecker c,
				      ProcessingEnvironment env,
				      TreePath p,
				      Map<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>>
				          replacementLocations,
				      RuntimeCheckBuilder checkBuilder) {
	// Since this must occur in a method which we can call here to satisfy Java we
	// have to do some finagling.
	super(c, env, p,
	      RuntimeCheckTreeTranslator.buildReplacementMap(checkBuilder, p,
							     replacementLocations));

	// We assign this class variable from a static variable set in the initializer
	// directly above.  This must be syncronous, if multiple classes are constructed
	// at the same time, they can stomp this value.
	this.leftToAttribute = RuntimeCheckTreeTranslator.leftToAttributeStatic;
    }

    /**
     * Build the map of replacements.  This is a static method so it can be called in the constructor
     * before the super constructor has been invoked.
     *
     * @params As described in the constructor.
     * @return A mapping from tree nodes to their replacements with runtime checks inserted.
     */
    static private Map<JCTree, JCTree> buildReplacementMap(RuntimeCheckBuilder checkBuilder,
							   TreePath p,
							   Map<TreePath,
							       Map.Entry<Tree, AnnotatedTypeMirror>>
							   replacementLocations) {
	// Initialize this to a new static empty hashmap, so that this is separate from other
	// invocations of the constructor.  This must be done syncronously.
	leftToAttributeStatic = new HashMap<JCTree, JCTree>();
	Map<JCTree, JCTree> replacementMap = new HashMap<JCTree, JCTree>();
	
	// First we iterate over all locations where runtime checks should be inserted.
 	for (Map.Entry<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>> location :
		 replacementLocations.entrySet()) {

	    // The first step is to find the enclosing statement of the location where the
	    // check must be inserted.  Since the check is a statement (an if statement, and
	    // variable declaration in a block specifically), we must insert it in sequence
	    // the statement containing the unchecked value.  To do this, iterate to the values
	    // parent until we get a statement.  This must terminate since every expression is
	    // contained within a statement.
	    TreePath statementToReplace = location.getKey();
	    // System.out.println("First statement: " + statementToReplace.getLeaf());
	    // System.out.println("Given tree: " + location.getValue().getKey());
	    while (!(statementToReplace.getLeaf() instanceof StatementTree)
		   && statementToReplace != null) {
		statementToReplace = statementToReplace.getParentPath();
	    }

	    // System.out.println("replacing: " + statementToReplace.getLeaf());

	    // If the statement is a VarDecl we must treat it specially to preserve scope.
	    if (statementToReplace.getLeaf() instanceof JCTree.JCVariableDecl) {
		// Build a runtime check.  VarDecls must always be contained within a JCBlock
		// for valid java programs so we know its parent will be a JCBlock
		Map.Entry<JCTree, JCTree> check =
		    checkBuilder.buildVarDeclRuntimeCheck((JCTree.JCExpression)
							  location.getValue().getKey(),
							  (JCTree.JCVariableDecl)
							  statementToReplace.getLeaf(),
							  (JCTree.JCBlock)
							  statementToReplace.getParentPath().getLeaf(),
							  statementToReplace,
							  location.getValue().getValue(),
							  location.getKey());

		// Put it in the replacement map.
		replacementMap.put((JCTree) (statementToReplace.getParentPath().getLeaf()),
				   check.getKey());

		// Also insert the odes in the list of nodes to annotate.  Insert both the check,
		// and the value statement.
		leftToAttributeStatic.put(check.getValue(), (JCTree) (location.getValue().getKey()));
		leftToAttributeStatic.put(check.getKey(),
					  (JCTree) (statementToReplace.getParentPath().getLeaf()));
	    } else {
		// Now build a runtime check with this information.
		Map.Entry<JCTree, JCTree> check =
		    checkBuilder.buildRuntimeCheck((JCTree.JCExpression) location.getValue().getKey(),
						   (JCTree.JCStatement) statementToReplace.getLeaf(),
						   statementToReplace,
						   location.getValue().getValue(),
						   location.getKey());

		// Put it in the replacement map.
		replacementMap.put((JCTree) (statementToReplace.getLeaf()), check.getKey());

		// Also insert the resulting nodes in the list of nodes to annotate.  Insert both the
		// check, and the value statement since the value statement is modified.
		leftToAttributeStatic.put(check.getValue(), (JCTree) (location.getValue().getKey()));
		leftToAttributeStatic.put(check.getKey(), (JCTree) (statementToReplace.getLeaf()));
	    }
	    // System.out.println("Statement to replace is now: " + statementToReplace.getLeaf());
	}

	return replacementMap;
    }
}
