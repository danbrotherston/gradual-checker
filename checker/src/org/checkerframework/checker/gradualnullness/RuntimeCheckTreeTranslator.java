package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.processing.ProcessingEnvironment;

public class RuntimeCheckTreeTranslator extends ReplacingTreeTranslator {
    // I hate you Java.
    static private Map<JCTree, JCTree> leftToAttributeStatic;
    private Map<JCTree, JCTree> leftToAttribute;

    public Map<JCTree, JCTree> getUnattributedTrees() {
	return Collections.unmodifiableMap(leftToAttribute);
    }

    public RuntimeCheckTreeTranslator(GradualNullnessChecker c,
				      ProcessingEnvironment env,
				      TreePath p,
				      Map<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>>
				          replacementLocations,
				      RuntimeCheckBuilder checkBuilder) {
	super(c, env, p,
	      RuntimeCheckTreeTranslator.buildReplacementMap(checkBuilder, p,
							     replacementLocations));
	this.leftToAttribute = RuntimeCheckTreeTranslator.leftToAttributeStatic;
    }

    static private Map<JCTree, JCTree> buildReplacementMap(RuntimeCheckBuilder checkBuilder,
							   TreePath p,
							   Map<TreePath,
							       Map.Entry<Tree, AnnotatedTypeMirror>>
							   replacementLocations) {
	Map<JCTree, JCTree> replacementMap = new HashMap<JCTree, JCTree>();
	
 	for (Map.Entry<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>> location :
		 replacementLocations.entrySet()) {
	    TreePath statementToReplace = location.getKey();
	    while (!(statementToReplace.getLeaf() instanceof StatementTree)
		   && statementToReplace != null) {
		statementToReplace = statementToReplace.getParentPath();
	    }


	    Map.Entry<JCTree, JCTree> check =
		checkBuilder.buildRuntimeCheck((JCTree.JCExpression) location.getValue().getKey(),
					       (JCTree.JCStatement) statementToReplace.getLeaf(),
					       statementToReplace,
					       location.getValue().getValue(),
					       location.getKey());

      	    replacementMap.put((JCTree) (statementToReplace.getLeaf()), check.getKey());

	    leftToAttributeStatic = new HashMap<JCTree, JCTree>();
	    leftToAttributeStatic.put(check.getValue(), (JCTree) (location.getValue().getKey()));
	    leftToAttributeStatic.put(check.getKey(), (JCTree) (statementToReplace.getLeaf()));
	}

	return replacementMap;
    }
}
