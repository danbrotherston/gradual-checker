package org.checkerframework.checker.gradualnullness;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;

public class ReplacingTreeTranslator extends GeneralTreeTranslator {
    private final Map<JCTree, JCTree> replacementMap;

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
