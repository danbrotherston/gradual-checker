package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;

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
	this.procEnv = env;
    }

    /**
     * This field stores the postfix to apply to save methods, and all
     * checked method calls.
     */
    protected final String safeMethodNamePostfix = "_$safe";

    /**
     * The postfix to use for the runtime method selector to determine whether to
     * invoke the safe version or the unsafe version of a method.
     */
    protected final String maybeMethodNamePostfix = "_$maybe";

    /**
     * Marker field name.
     */
    protected final String markerFieldName = "$isTypeCheckedMarker";

    /**
     * This field stores the string representation of a super call identifier.
     */
    protected static final String superCallIdentifier = "super";

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

    private Element lastSymbolOwner = null;    

    /**
     * Visit method application to change the names of checked applications
     * to point to the safe method without checks.  This is an optimization
     * so that checked code does not have runtime checks.
     */
    @Override
    public void visitApply(JCTree.JCMethodInvocation tree) {
	result = renameMethodApplication(tree);
    }

    public void visitMethodDef(JCTree.JCMethodDecl tree) {
	Element prevSymbolOwner = lastSymbolOwner;
	lastSymbolOwner = tree.sym;
	if (tree.getName().toString().endsWith(this.maybeMethodNamePostfix)) {
	    result = tree;
	} else {
	    super.visitMethodDef(tree);
	}

	lastSymbolOwner = prevSymbolOwner;
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
	    // System.out.println("Method select tree: " + methodSelect);
	    JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) methodSelect;
	    fieldAccess.selected = translate(fieldAccess.selected);

	    //System.err.println("Tree: " + tree);
	    //System.err.println("Get type of: " + fieldAccess.selected);
	    //AnnotatedTypeMirror receiverType =
	    //aTypeFactory.getAnnotatedType(fieldAccess.selected);
	    TypeMirror underlyingReceiverType = fieldAccess.selected.type;

	    if (fieldAccess.selected.toString()
		.equals(MethodRenamingTreeTranslator.superCallIdentifier)) {

		DeclaredType thisType = (DeclaredType) underlyingReceiverType;
	        TypeElement thisTypeElement = (TypeElement) thisType.asElement();

		// System.out.println("This Type: " + underlyingReceiverType);
		// System.out.println("thisTypeElement: " + thisTypeElement);
		// underlyingReceiverType = thisTypeElement.getSuperclass();

		// System.out.println("SuperClass: " + underlyingReceiverType);

		if (underlyingReceiverType == null) {
		    throw new RuntimeException("Super method call with no super class");
		}
	    }

	    // System.out.println("Selected: " + fieldAccess.selected);
	    // System.out.println("Selected Class: " + fieldAccess.selected.getClass());
	    // System.out.println("Underlying Reciever Type: " + underlyingReceiverType);

	    if (underlyingReceiverType instanceof DeclaredType ||
		underlyingReceiverType instanceof TypeVariable) {

		Name methodIdentifier = fieldAccess.getIdentifier();
		MethodSymbol methodSymbol = (MethodSymbol) fieldAccess.sym;

		tree = renameMethod(tree, fieldAccess.selected, underlyingReceiverType,
				    methodIdentifier, methodSymbol);
	    }
	} else if (methodSelect instanceof IdentifierTree) {
	    // System.out.println("Method Select Tree: " + methodSelect);
	    JCTree.JCIdent identifier = (JCTree.JCIdent) methodSelect;

	    //AnnotatedTypeMirror receiverType = aTypeFactory.getAnnotatedType(
	    JCTree enclosing = (JCTree) TreeUtils.enclosingClass(aTypeFactory.getPath(tree));
	    TypeMirror underlyingReceiverType = enclosing.type;// receiverType.getUnderlyingType();
	    // System.err.println("Method: " + identifier.getName() + " ReceiverType: "
	    //                    + underlyingReceiverType + " Ident: " + identifier
	    //                    + " symbol: " + identifier.sym);

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

	if (originalName.toString().endsWith(this.safeMethodNamePostfix) ||
	    originalName.toString().endsWith(this.maybeMethodNamePostfix)) {
	    return tree;
	}

	// AnnotatedExecutableType originalExecutable =
	//    aTypeFactory.getAnnotatedType(originalSymbol);

	// If the static flag is set on the method, we should immediately go to the
	// safe version because there is no dynamic dispatch here.
	// If this is a super call, we should immediately dispatch to the safe version.
	// If this is a private method we should immediately dispatch to the safe version.
	// Why?  Because all of these instances are dispatched statically, not virtually
	// in java (and will result in the invokestatic/invokespecial bytecode instruction.
	Name newName = null;
	if (((originalSymbol.flags() & Flags.STATIC) != 0) ||
	    ((originalSymbol.flags() & Flags.PRIVATE) != 0) ||
	    receiver.toString().equals(MethodRenamingTreeTranslator.superCallIdentifier)) {
	    newName = names.fromString(originalName + this.safeMethodNamePostfix);
	} else {
	    newName = names.fromString(originalName + this.maybeMethodNamePostfix);
	}

	TypeElement receiverTypeElement = (TypeElement) typeutils.asElement(receiverType);

	// System.out.println("In method: " + this.lastSymbolOwner);
	// System.out.println("For method name: " + newName);
	// System.out.println("Tree: " + tree);
	// System.out.println("Receiver Type: " + receiverType);
	// System.out.println("All members: " +
        // 		   this.procEnv.getElementUtils().getAllMembers(receiverTypeElement));
	// System.out.println("Receiver Type Element: " + receiverTypeElement);
	// for (Element elem : typeutils.asElement(receiverType).getEnclosedElements()) {

	// TODO: Maybe have to see if we should search sequentially up the inheritance chain
	// because maybe there is an untyped class in between that implements f but not f_safe
	// and thus that f should be called first.  Ughhh.
	for (Element elem : this.procEnv.getElementUtils().getAllMembers(receiverTypeElement)) {
	    // System.out.println("Searching: " + elem.getSimpleName());
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
		// System.err.println("Attribbing: " + newMethodCall);
		attr.attribExpr(newMethodCall, this.getAttrEnv(tree),
				(Type)((ExecutableElement) elem).getReturnType());

		return newMethodCall;
	    }
	}
	/*
	if (newName.toString().contains("other")) {
	    JCTree.JCExpression newMethodSelect =
		maker.Select(receiver,
			     new MethodSymbol(originalSymbol.flags_field,
					      newName,
					      originalSymbol.type,
					      (Symbol) this.lastSymbolOwner));
	    JCTree.JCMethodInvocation newMethodCall =
		maker.Apply(null, newMethodSelect, tree.getArguments());
	    // attr.attribExpr(newMethodCall, this.getAttrEnv(tree), originalSymbol.getReturnType());
	    System.out.println("Synthesizing method call");
	    return newMethodCall;
	}
	*/

	// System.err.println("No method found: ." + newName);
	// Thread.dumpStack();
	return tree;
    }
}
