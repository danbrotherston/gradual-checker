package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeCopier;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import org.checkerframework.javacutil.TreeUtils;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * @author danbrotherston
 *
 * This is a specialization of the replacing tree translator that takes a list of
 * runtime check locations (as values which need to tested) and replaces their
 * enclosing statements with runtime checks.
 */
public class NonLocalExitStatementFixerTreeTranslator extends
	HelpfulTreeTranslator<GradualNullnessChecker> {

    /**
     * Stack keeping track of our position within the tree, allowing us to get
     * a node's parent node.
     */
    Stack<JCTree.JCStatement> loopStack = null;

    /**
     * Processing environment.
     */
    ProcessingEnvironment env = null;

    public void fixTree(JCTree that) {
	this.loopStack = new Stack<JCTree.JCStatement>();
	that.accept(this);
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
    public NonLocalExitStatementFixerTreeTranslator(GradualNullnessChecker c,
						    ProcessingEnvironment env,
						    TreePath p) {
	super(c, env, p);
	this.env = env;
    }

    @Override
    public void visitForLoop(JCTree.JCForLoop that) {
	loopStack.push(that);
	super.visitForLoop(that);
	loopStack.pop();
    }

    @Override
    public void visitForeachLoop(JCTree.JCEnhancedForLoop that) {
	loopStack.push(that);
	super.visitForeachLoop(that);
	loopStack.pop();
    }

    @Override
    public void visitDoLoop(JCTree.JCDoWhileLoop that) {
	loopStack.push(that);
	super.visitDoLoop(that);
	loopStack.pop();
    }

    @Override
    public void visitWhileLoop(JCTree.JCWhileLoop that) {
	loopStack.push(that);
	super.visitWhileLoop(that);
	loopStack.pop();
    }

    @Override
    public void visitLabelled(JCTree.JCLabeledStatement that) {
	loopStack.push(that);
	super.visitLabelled(that);
	loopStack.pop();
    }

    @Override
    public void visitSwitch(JCTree.JCSwitch that) {
	loopStack.push(that);
	super.visitSwitch(that);
	loopStack.pop();
    }
    
    @Override
    public void visitBreak(JCTree.JCBreak that) {
	if (!loopStack.empty()) {
	    if (that.getLabel() == null) {
		that.target = loopStack.peek();
	    } else {
		that.target = getMatchingLabeledStatement(that.getLabel().toString(), that.target);
	    }
	}

	result = that;
    }

    @Override
    public void visitContinue(JCTree.JCContinue that) {
	if (!loopStack.empty()) {
	    if (that.getLabel() == null) {
		//System.err.println("Replacing label + " + that + " loopStack: " + loopStack.peek());
		that.target = loopStack.peek();
	    } else {
		that.target = getMatchingLabeledStatement(that.getLabel().toString(), that.target);
	    }
	}

	result = that;
    }

    private JCTree getMatchingLabeledStatement(String label, JCTree oldTarget) {
	@SuppressWarnings("unchecked")
	Stack<JCTree.JCStatement> stack = (Stack<JCTree.JCStatement>) (this.loopStack.clone());

	while (!stack.empty()) {
	    if (stack.peek() instanceof JCTree.JCLabeledStatement) {
		JCTree.JCLabeledStatement statement = (JCTree.JCLabeledStatement) stack.peek();
		if (statement.getLabel().toString().equals(label)) {
		    //System.err.println("replacing labelled label: " + label);
		    return statement.getStatement();
		}
	    }

	    stack.pop();
	}

	return oldTarget;
    }
}
