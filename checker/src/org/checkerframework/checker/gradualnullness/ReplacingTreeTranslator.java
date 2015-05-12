package org.checkerframework.checker.gradualnullness;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * @author danbrotherston
 *
 * This is a specialized version of the GeneralTreeTranslator that simply replaces
 * a given set of nodes with another set of nodes.  The translator is provided
 * with a mapping of old nodes to new nodes.  When an old node is encountered it
 * is replaced with the associated new node.
 */
public class ReplacingTreeTranslator extends GeneralTreeTranslator {
    private final Map<JCTree, JCTree> replacementMap;

    /**
     * @constructor
     * @param c The checker which is invoking this translator.
     * @param env The annotation processing environment this is invoked from.
     * @param p The tree path of the compilation this is invoked on.
     * @param replacementMap A mapping of old JCTree to new JCTree.  The nodes
     *                       in the key position should return true when
     *                       .equals is called on the node in the actual tree
     *                       that it should replace.
     */
    public ReplacingTreeTranslator(GradualNullnessChecker c,
				   ProcessingEnvironment env,
				   TreePath p,
				   Map<JCTree, JCTree> replacementMap) {

	super(c, env, p);

	this.replacementMap = replacementMap;
    }

    @Override
    public void visitTree(JCTree that) {
	if (replacementMap.containsKey(that)) {
	    result = replacementMap.get(that);
	}
    }
}
