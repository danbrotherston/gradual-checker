package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.Tree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.trees.TreeBuilder;
import org.checkerframework.javacutil.TreeUtils;

/**
 * @author danbrotherston
 *
 * This translator converts all method application instances to call
 * this safe version, leaving unchecked code to call the original
 * version of the method.  The original version of the method is
 * converted to perform runtime checks before calling the safe
 * version.
 *
 * This code is based on the MethodBindingTranslator from enerj,
 * but is simpler because the translation is unconditional.
 */
public class MethodRenamingTreeTranslator extends HelpfulTreeTranslator<GradualNullnessChecker> {
    public MethodRenamingTreeTranslator(GradualNullnessChecker c,
					   ProcessingEnvironment env,
					   TreePath p) {
	super(c, env, p);
	this.builder = new TreeBuilder(env);
	this.aTypeFactory = c.getTypeFactory();
    }

    /**
     * This field stores the postfix to apply to save methods, and all
     * checked method calls.
     */
    protected final String methodNamePostfix = "_$safe";

    /**
     * This field stores a tree builder used for building trees used in renaming
     * and converting methods.
     */
    protected final TreeBuilder builder;

    /**
     * The type factory to use for manipulating annotated types.
     */
    protected final AnnotatedTypeFactory aTypeFactory;

    /**
     * Visit method application to change the names of checked applications
     * to point to the safe method without checks.  This is an optimization
     * so that checked code does not have runtime checks.
     */
    @Override
    public void visitApply(JCTree.JCMethodInvocation tree) {
	result = renameMethodApplication(tree);
    }

    /**
     * This method renames a method in a method application tree.
     *
     * @param tree The method application (invocation) tree to rename.
     * @return The renamed method application tree.
     */
    protected JCTree renameMethodApplication(JCTree.JCMethodInvocation tree) {
	JCTree.JCExpression methodSelect = tree.getMethodSelect();
	
	if (methodSelect instanceof JCTree.JCFieldAccess) {
	    JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) methodSelect;
	    fieldAccess.selected = translate(fieldAccess.selected);

	    AnnotatedTypeMirror receiverType =
		aTypeFactory.getAnnotatedType(fieldAccess.selected);
	    TypeMirror underlyingReceiverType = receiverType.getUnderlyingType();

	    if (underlyingReceiverType instanceof DeclaredType ||
		underlyingReceiverType instanceof TypeVariable) {

		Name methodIdentifier = fieldAccess.getIdentifier();
		MethodSymbol methodSymbol = (MethodSymbol) fieldAccess.sym;

		tree = renameMethod(tree, fieldAccess.selected, underlyingReceiverType,
				    methodIdentifier, methodSymbol);
	    }
	} else if (methodSelect instanceof IdentifierTree) {
	    JCTree.JCIdent identifier = (JCTree.JCIdent) methodSelect;

	    AnnotatedTypeMirror receiverType = aTypeFactory.getAnnotatedType(
	        TreeUtils.enclosingClass(aTypeFactory.getPath(tree)));
	    TypeMirror underlyingReceiverType = receiverType.getUnderlyingType();

	    Name methodIdentifier = identifier.getName();
	    MethodSymbol methodSymbol = (MethodSymbol) identifier.sym;

	    tree = renameMethod(tree, thisExp(), underlyingReceiverType,
				methodIdentifier, methodSymbol);
	}

	tree.meth = translate(tree.meth);
	tree.args = translate(tree.args);
	return tree;
    }

    /**
     * This method is a helper method to replace a method invocation with
     * one with a different name.
     */
    private JCTree.JCMethodInvocation renameMethod(JCTree.JCMethodInvocation tree,
						   JCTree.JCExpression receiver,
						   TypeMirror receiverType,
						   Name originalName,
						   MethodSymbol originalSymbol) {

	Name newName = names.fromString(originalName + this.methodNamePostfix);
	AnnotatedExecutableType originalExecutable =
	    aTypeFactory.getAnnotatedType(originalSymbol);

	for (Element elem : typeutils.asElement(receiverType).getEnclosedElements()) {
	    if (elem.getSimpleName().equals(newName)) {
		AnnotatedExecutableType newExectuable =
		    aTypeFactory.getAnnotatedType((ExecutableElement) elem);

		// Check compatibility, should be correct.

		JCTree.JCExpression newMethodSelect =
		    maker.Select(receiver, (Symbol) elem);

		// Arguments are the same, just use the original.
		JCTree.JCMethodInvocation newMethodCall =
		    maker.Apply(null, newMethodSelect, tree.getArguments());

		// System.err.println("Method found: " + elem.getSimpleName());
		// Attribute the new tree.
		attr.attribExpr(newMethodCall, this.getAttrEnv(tree),
				(Type)((ExecutableElement) elem).getReturnType());

		return newMethodCall;
	    }
	}

	// System.err.println("No method found: ." + newName);
	// Thread.dumpStack();
	return tree;
    }
}
