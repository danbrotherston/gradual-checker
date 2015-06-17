package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
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
import javax.lang.model.element.VariableElement;
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

import java.util.ListIterator;

/**
 * @author danbrotherston
 *
 * This translator converts all constructor invocations in checked
 * code to the version containing the marking parameter.  This ensures
 * that checked code doesn't undergo unnecessary type checks.
 *
 * This code is based on the MethodBindingTranslator from enerj,
 * but is simpler because the translation is unconditional.
 */
public class ConstructorInvocationRefactoringTranslator
    extends HelpfulTreeTranslator<GradualNullnessChecker> {
    public ConstructorInvocationRefactoringTranslator(GradualNullnessChecker c,
						      ProcessingEnvironment env,
						      TreePath p) {
	super(c, env, p);
	this.builder = new TreeBuilder(env);
	this.aTypeFactory = c.getTypeFactory();
	this.procEnv = env;
    }

    /**
     * Thie class of the dummy marker class used to indicate safe constructors.
     */
    protected final Class<SafeConstructorMarkerDummy> constructorMarkerClass =
	SafeConstructorMarkerDummy.class;

    /**
     * The secret name given to constructors in java.
     */
    protected final String constructorMethodName = "<init>";

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
     * The current class definition being processed.  We must know this
     * in order to properly insert new methods into this class.
     */
    protected JCTree.JCClassDecl currentClassDef;

    /**
     * This field stores the processing environment this translator was
     * constructed with.
     */
    protected final ProcessingEnvironment procEnv;

    /**
     * The type factory to use for manipulating annotated types.
     */
    protected final AnnotatedTypeFactory aTypeFactory;

    /**
     * The method we're currently processing who will be the symbol owner for
     * the next invocation.
     */
    private Element lastSymbolOwner = null;

    protected boolean modifiedLastConstructor = false;

    /**
     * Visit method application to change the names of checked applications
     * to point to the safe method without checks.  This is an optimization
     * so that checked code does not have runtime checks.
     */
    @Override
    public void visitApply(JCTree.JCMethodInvocation tree) {
	result = renameMethodApplication(tree);
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl tree) {
	JCTree.JCClassDecl prevClassDef = this.currentClassDef;
	this.currentClassDef = tree;
	super.visitClassDef(tree);
	this.currentClassDef = prevClassDef;
    }

    @Override
    public void visitNewClass(JCTree.JCNewClass tree) {
	//result = renameNewClass(tree);
	result = tree;
	// System.out.println("Tree: " + tree);
	// System.out.println("Tree def: " + tree.def);
	// System.out.println("Tree encl: " + tree.encl);
	// System.out.println("Tree args: " + tree.args);
	// System.out.println("Tree clazz: " + tree.clazz);

	List<JCTree.JCExpression> args = tree.args;
	JCTree.JCExpression newArg =
	    maker.TypeCast(this.getMarkerClassType(),
			   maker.Literal(TypeTag.BOT, null));
	args = args.prepend(newArg);

	result = maker.NewClass(tree.encl,
				tree.typeargs,
				tree.clazz,
				args,
				tree.def);
	// System.out.println("Result: " + result);
	attribute((JCTree.JCNewClass)result, tree);
    }

    /**
     * Returns the marker class type object.
     */
    private Type getMarkerClassType() {
	return ((Symbol) (this.builder.getClassSymbolElement(this.constructorMarkerClass,
							     this.procEnv))).type;
    }	

    public void visitMethodDef(JCTree.JCMethodDecl tree) {
	Element prevLastSymbolOwner = lastSymbolOwner;
	lastSymbolOwner = tree.sym;
	super.visitMethodDef(tree);
	// Attribute this method.
	if (this.modifiedLastConstructor) {
	    attributeInMethod(tree.body, tree, tree.body);
	}
	lastSymbolOwner = prevLastSymbolOwner;
    }

    /**
     * This method renames a method in a method application tree.
     *
     * @param tree The method application (invocation) tree to rename.
     * @return The renamed method application tree.
     */
    protected JCTree renameMethodApplication(JCTree.JCMethodInvocation tree) {
	JCTree.JCExpression methodSelect = tree.getMethodSelect();
	
	if (methodSelect instanceof IdentifierTree) {
	    // System.out.println("Method Select Tree: " + methodSelect);
	    // System.out.println("Identifier tree: " + tree);
	    JCTree.JCIdent identifier = (JCTree.JCIdent) methodSelect;

	    AnnotatedTypeMirror receiverType = aTypeFactory.getAnnotatedType(
	        TreeUtils.enclosingClass(aTypeFactory.getPath(tree)));
	    TypeMirror underlyingReceiverType = receiverType.getUnderlyingType();
	    // System.out.println("Method: " + identifier.getName() + " ReceiverType: "
	    //     + underlyingReceiverType);

	    Name methodIdentifier = identifier.getName();
	    MethodSymbol methodSymbol = (MethodSymbol) identifier.sym;

	    if (methodIdentifier.toString().equals(ConstructorInvocationRefactoringTranslator.
						   superCallIdentifier)) {
		DeclaredType thisType = (DeclaredType) underlyingReceiverType;
		TypeElement thisTypeElement = (TypeElement) thisType.asElement();
		underlyingReceiverType = thisTypeElement.getSuperclass();

		if (underlyingReceiverType == null) {
		    throw new RuntimeException("Super constructor call with no super class");
		}
	    }

	    // System.out.println("Method Symbol: " + methodSymbol + " method ident: "
	    //     + methodIdentifier + " method symbol name: " + methodSymbol.name);

	    if (methodSymbol.name.toString().equals(this.constructorMethodName)) {
		tree = renameMethod(tree, thisExp(), underlyingReceiverType,
				    methodIdentifier, methodSymbol);
	    }
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
	AnnotatedExecutableType originalExecutable =
	    aTypeFactory.getAnnotatedType(originalSymbol);

	// System.out.println("For method name: " + originalName);
	// System.out.println("Original Executable: " + originalExecutable);
	// System.out.println("Receiver Type: " + receiverType);

        // Make param list.
	List<JCExpression> args = tree.getArguments();
	JCTree.JCExpression newArg =
	    maker.TypeCast(this.getMarkerClassType(),
			   maker.Literal(TypeTag.BOT, null));
	args = args.prepend(newArg);

	// for (Element elem : typeutils.asElement(receiverType).getEnclosedElements()) {
	TypeElement receiverTypeElement = (TypeElement) typeutils.asElement(receiverType);
	methodLoop:
	for (Element elem : this.procEnv.getElementUtils().getAllMembers(receiverTypeElement)) {
	    //	    System.out.println("Simple Name: '" + elem.getSimpleName()
	    //		       + "' constructor name: '" + this.constructorMethodName); 
	    if (elem.getSimpleName().toString().equals(this.constructorMethodName)) {
		// System.out.println("Testing Elem: " + elem);
		AnnotatedExecutableType newExecutable =
		    aTypeFactory.getAnnotatedType((ExecutableElement) elem);

		// Check correct parameters, to verify typechecking.
		// This also checks the first argument which verifies that the
		// constructor is the safe version.
		java.util.List<? extends TypeMirror>  paramTypes =
		    newExecutable.getUnderlyingType().getParameterTypes();
		List<JCTree.JCExpression> arg = args;
		boolean firstArg = true;
		// System.out.println("Testing type list: " + paramTypes);
		// System.out.println("With arguments: " + args);
		for (TypeMirror paramType : paramTypes) {
		    if (arg != null && arg.head != null) {
			TypeMirror argumentType = null;
			if (firstArg) {
			    argumentType = this.getMarkerClassType();
			    firstArg = false;
			} else {
			    // argumentType = aTypeFactory.
			    //	getAnnotatedType(arg.head.type.asElement()).getUnderlyingType();
			    argumentType = arg.head.type;
			}
			if (!this.procEnv.getTypeUtils().isAssignable(argumentType, paramType)) {
			    // System.out.println("Skipping arg not assignable: "
			    //		       + argumentType + " = " + paramType);
			    continue methodLoop;
			}
			arg = arg.tail;
		    } else {
			// System.out.println("Skipping wrong number of args");
			continue methodLoop;
		    }
		}

		java.util.List<? extends TypeMirror> oldParamTypes =
		    originalExecutable.getUnderlyingType().getParameterTypes();

		// Verify all types are the same.
		ListIterator<? extends TypeMirror> newParamsIterator = paramTypes.listIterator();
		// Advance iterator one position to skip the marker element.
		if (newParamsIterator.hasNext()) { newParamsIterator.next(); }
		else { continue methodLoop; }

		for (TypeMirror oldParamType : oldParamTypes) {
		    if (newParamsIterator.hasNext()) {
			TypeMirror newParamType = newParamsIterator.next();
			if (!this.procEnv.getTypeUtils().isSameType(oldParamType,
								    newParamType)) {
			    // System.out.println("Argument Types don't match: "
			    // 		       + oldParamType + " is not: " + newParamType);
			    continue methodLoop;
			}
		    } else {
			// System.out.println("Wrong number of args");
			continue methodLoop;
		    }
		}

		JCTree.JCExpression constructor = maker.This(this.currentClassDef.sym.type);

		// Arguments are the same, just use the original.
		JCTree.JCMethodInvocation newConstructorCall =
		    maker.Apply(null, constructor, args);

		// System.err.println("Method found: " + elem.getSimpleName());
		// Attribute the new tree.
		// attr.attribExpr(newConstructorCall, this.getAttrEnv(tree),
		//		(Type)((ExecutableElement) elem).getReturnType());
		// attribute(newConstructorCall, tree);

		this.modifiedLastConstructor = true;
		// this.newCall = newConstructorCall;
		return newConstructorCall;
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

	// System.err.println("No method found: ." + originalName);
	// Thread.dumpStack();
	return tree;
    }
}
