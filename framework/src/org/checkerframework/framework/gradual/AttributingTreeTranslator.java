package org.checkerframework.framework.gradual;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;

import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * @author danbrotherston
 *
 * This tree translator class will traverse a tree and correctly annotate a list
 * of JCTree nodes.  The translator iterates the tree, depth first, and upon
 * encountering a node in the given set, it will annotate it.
 *
 * The list of nodes is actually a mapping.  The key should be the actual node in
 * the tree, and the the value should be the previous node value, if the node
 * has been mutated, or the block in which the new node is added.
 */
public class AttributingTreeTranslator<Checker extends BaseTypeChecker>
        extends GeneralTreeTranslator<Checker> {
    private final Map<JCTree, JCTree> attributionMap;

    /**
     * @constructor
     * @param c The actual checker which is invoking this translator.
     * @param env The annotation processing environment from which this is called.
     * @param p The tree path to the compilation unit this is invoked on.
     * @param attributionMap The set of nodes to annotate, as described in the class
     *                       description.
     */
    public AttributingTreeTranslator(Checker c,
				     ProcessingEnvironment env,
				     TreePath p,
				     Map<JCTree, JCTree> attributionMap) {

	super(c, env, p);

	this.attributionMap = attributionMap;
    }

    @Override
    public void visitTree(JCTree that) {
	if (attributionMap.containsKey(that)) {
	    JCTree original = attributionMap.get(that);
	    if (that instanceof JCTree.JCExpression &&
		original instanceof JCTree.JCExpression) {
		this.attribute((JCTree.JCExpression) original,
			       (JCTree.JCExpression) that);
	    } else if (that instanceof JCTree.JCStatement &&
		       original instanceof JCTree.JCStatement) {
		System.err.println("Attrib orig: " + original + 
				   "replacement: " + that);
		this.attribute((JCTree.JCStatement) original,
			       (JCTree.JCStatement) that);
	    }
	}
    }
}
