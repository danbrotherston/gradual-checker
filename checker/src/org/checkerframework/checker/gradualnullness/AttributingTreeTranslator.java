package org.checkerframework.checker.gradualnullness;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;

import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;

public class AttributingTreeTranslator extends GeneralTreeTranslator {
    private final Map<JCTree, JCTree> attributionMap;

    public AttributingTreeTranslator(GradualNullnessChecker c,
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
		this.attribute((JCTree.JCStatement) original,
			       (JCTree.JCStatement) that);
	    }
	}
    }
}
