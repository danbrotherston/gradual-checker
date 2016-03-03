package org.checkerframework.framework.gradual;

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
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.trees.TreeBuilder;
import org.checkerframework.javacutil.TreeUtils;

/**
 * @author danbrotherston
 *
 * This tree translator converts methods into "safe" versions
 * which have no checks, and are renamed with a "_safe" postfix.
 *
 * This code is based on the MethodBindingTranslator from enerj,
 * but is simpler because the translation is unconditional.
 */
public class MethodRefactoringTreeTranslator<Checker extends BaseTypeChecker>
        extends HelpfulTreeTranslator<Checker> {
    public MethodRefactoringTreeTranslator(Checker c,
					   ProcessingEnvironment env,
					   TreePath p,
					   String argumentCheckMethodName) {
	super(c, env, p);
	this.builder = new TreeBuilder(env);
	copier = new TreeCopier<Void>(maker);
	this.procEnv = env;
	this.argumentCheckFunctionName = argumentCheckMethodName;
    }

    /**
     * This field stores the postfix to apply to safe methods, and all
     * checked method calls.
     */
    protected final String safeMethodNamePostfix = "_$safe";

    /**
     * Marker field name.
     */
    protected final String markerFieldName = "$isTypeCheckedMarker";

    /**
     * The postfix to use for the runtime method selector to determine whether to
     * invoke the safe version or the unsafe version of a method.
     */
    protected final String maybeMethodNamePostfix = "_$maybe";

    /**
     * The secret name used for constructors to look for when identifying constructors.
     */
    protected final String constructorMethodName = "<init>";

    /**
     * This string references the isChecked method to determine if an object is
     * based on a class which has been checked by the checker framework.
     */
    protected final String runtimeCheckIsCheckedFQMethodName =
	"org.checkerframework.framework.gradual.RuntimeCheck.isChecked";

    /**
     * String references the runtimeCheckArgument function in the NullnessRunctimeCheck
     * class.
     */
    protected final String argumentCheckFunctionName;

    /**
     * This is necessary because the checker framework typechecking has not run at this
     * point yet, thus the checker framework type is unavailable.  We mark the string
     * literal with this value so that we can fill the type in at a later time.
     *
     * If the end user program author was to use this particular string in their code
     * they would cause the checker framework to replace their string constant with a type.
     */
    protected final String stringLiteralFillInMarker =
	"$%^CheckerFrameworkFillInTypeHere!@#";

    /**
     * This field stores a tree builder used for building trees used in renaming
     * and converting methods.
     */
    protected final TreeBuilder builder;

    /**
     * The current processing environment this translator is invoked within.
     */
    protected final ProcessingEnvironment procEnv;

    /**
     * Copier used for copying portions of the tree.
     */
    private final TreeCopier<Void> copier;

    /**
     * The current class definition being processed.  We must know this in order
     * to properly insert new methods into this class.
     */
    protected JCTree.JCClassDecl currentClassDef;

    /**
     * The type factory to use for manipulating annotated types.
     */
    //    protected final AnnotatedTypeFactory aTypeFactory;

    /**
     * The methods to add to the current class.
     */
    protected ListBuffer<JCTree> newMethods;

    @Override
    public void visitIdent(JCTree.JCIdent tree) {
        result = copier.copy(tree);
    }

    /**
     * We must record the current class we are processing in order to know which class
     * to enter new methods into.
     */
    @Override
    public void visitClassDef(JCTree.JCClassDecl tree) {
	// Must store previous class def so we can restore it after this class is finished
	// processing, in order to accomodate nested classes.  Effectively a stack.
	JCTree.JCClassDecl prevClassDef = this.currentClassDef;
	ListBuffer<JCTree> prevNewMethods = this.newMethods;

	// Setup for class processing.
	this.currentClassDef = tree;
	this.newMethods = new ListBuffer<JCTree>();

	// Process class.
	super.visitClassDef(tree);

	// Add new methods to the class.
	this.newMethods.appendList(tree.defs);

	// If the marker field is not yet present,
	// If this is not an interface
	// Then add a marker field.
	if (!this.markerFieldPresent(tree.defs) &&
	    (tree.mods.flags & Flags.INTERFACE) == 0) {
	    this.newMethods.append(this.createMarkerField());
	}

	tree.defs = this.newMethods.toList();

	/*
	List<JCTree> defs = tree.defs;
	while(defs != null && defs.head != null) {
	    System.out.println("Def: " + defs.head);
	    System.out.println("Has Type: " + defs.head.getClass());
	    defs = defs.tail;
	    }*/

	result = tree;

	// Restore state.
	this.currentClassDef = prevClassDef;
	this.newMethods = prevNewMethods;
    }

    /**
     * Generate a new field to insert into every class which has been checked by the
     * checker framework.  This allows us to check quickly at runtime if a class has
     * been checked.
     */
    protected JCTree createMarkerField() {
	JCTree.JCVariableDecl newField = (JCTree.JCVariableDecl)
	    builder.buildVariableDecl(this.procEnv.getTypeUtils().getPrimitiveType(TypeKind.BOOLEAN),
				      this.markerFieldName,
				      this.currentClassDef.sym,
				      builder.buildLiteral(true));

	newField.mods.flags =
	    (newField.mods.flags | Flags.PRIVATE | Flags.FINAL)
	    & ~(Flags.PUBLIC | Flags.PROTECTED);

	//enterClassMember(this.currentClassDef, newField);

	return newField;
    }

    /**
     * On encountering a method definition, we need to rename it to the new method
     * while also creating a new method with the original name, with runtime checks
     * for untyped code to call.
     */
    @Override
    public void visitMethodDef(JCTree.JCMethodDecl tree) {
	// Only transform methods which take parameters.
	if (tree.params != null && tree.params.head != null) {
  	  result = runtimeCheckMethod(tree);
	} else {
	    super.visitMethodDef(tree);
	}
    }

    /**
     * Builds a checked argument to a function by calling the argument checking code.
     */
    private JCTree.JCExpression makeCheckedArgument(JCTree.JCExpression argument,
						    JCTree.JCExpression argumentType,
						    Object sym) {
	JCTree.JCExpression checkerFunction = dotsExp(this.argumentCheckFunctionName);
	JCTree.JCExpression checkedArgument =
	    maker.Apply(null, checkerFunction,
			List.of(argument, maker.Literal(this.stringLiteralFillInMarker)));
	//return maker.TypeCast(argumentType, checkedArgument);
        if (argumentType == null ||
	    argumentType.toString().equals("java.lang.Object") ||
	    argumentType.toString().equals("Object")) {
	    return checkedArgument;
	} else {
	    return maker.TypeCast(argumentType, checkedArgument);
        }
    }

    /**
     * Builds a checked method call for the given method declaration.
     *
     * TODO (danbrotherston): Refactor within the "makeMethodCall" function.
     * Generalize this code so it can be configured instead of hard coding this
     * function.
     */
    private JCTree.JCStatement makeCheckedMethodCall(JCTree.JCMethodDecl tree) {
	JCTree.JCExpression selectMethod;
	// System.out.println("Current class symbol type: " + this.currentClassDef.sym.type);

	if (tree.getModifiers().getFlags().contains(Modifier.STATIC)) {
	    // Static method call.
	    //selectMethod = maker.Select(dotsExp(this.currentClassDef.sym.toString()),
	    //			(Symbol) TreeUtils.elementFromDeclaration(tree));
            selectMethod = maker.Select(dotsExp(this.currentClassDef.sym.toString()), tree.name);
	} else {
	    // "This" method call.
	    //selectMethod = maker.Select(maker.This(this.currentClassDef.sym.type),
	    //			(Symbol) TreeUtils.elementFromDeclaration(tree));
            selectMethod = maker.Select(dotsExp("this"), tree.name);
	}
	ListBuffer<JCTree.JCExpression> args = new ListBuffer<JCTree.JCExpression>();
	List<JCTree.JCVariableDecl> params = tree.params;
	while (params != null && params.head != null) {
	    args.append(makeCheckedArgument(maker.Ident(params.head.name),
                                            copier.copy(params.head.vartype),
					    params.head.sym));
	    params = params.tail;
	}

	// TODO: Proper type test. Do better than this.
	if (tree.getReturnType() == null || tree.getReturnType().toString().equals("void")) {
	    return maker.Exec(maker.Apply(null, selectMethod, args.toList()));
	} else {
	    return maker.Return(maker.Apply(null, selectMethod, args.toList()));
	}
    }
       

    /**
     * Builds a method call for the given method declaration.
     */
    private JCTree.JCStatement makeMethodCall(JCTree.JCMethodDecl tree) {
	JCTree.JCExpression selectMethod;
	// System.out.println("Current class symbol type: " + this.currentClassDef.sym.type);

	if (tree.getModifiers().getFlags().contains(Modifier.STATIC)) {
	    // Static method call.
	    selectMethod = maker.Select(dotsExp(this.currentClassDef.sym.toString()),
					(Symbol) TreeUtils.elementFromDeclaration(tree));
	} else {
	    // "This" method call.
	    selectMethod = maker.Ident(tree.getName());
	}

	ListBuffer<JCTree.JCExpression> args = new ListBuffer<JCTree.JCExpression>();
	List<JCTree.JCVariableDecl> params = tree.params;
	while (params != null && params.head != null) {
	    args.append(maker.Ident(params.head.name));
	    params = params.tail;
	}

	// TODO: Proper type test. Do better than this.
	if (tree.getReturnType() == null || tree.getReturnType().toString().equals("void")) {
	    return maker.Exec(maker.Apply(null, selectMethod, args.toList()));
	} else {
	    return maker.Return(maker.Apply(null, selectMethod, args.toList()));
	}
    }

    /**
     * Make a new method and enter it into the type system.
     *
     * @param tree The original method to base it off.
     * @param namePostfix  The method name postfix to use.
     *
     * @return The new method declaration tree.
     */
    private JCTree.JCMethodDecl makeNewMethod(JCTree.JCMethodDecl tree,
					      String namePostfix,
					      JCTree.JCBlock methodBody) {
	Name originalName = tree.getName();
	Name newName = names.fromString(originalName + namePostfix);

	JCTree.JCMethodDecl newMethod =
	    maker.MethodDef(copier.copy(tree.mods),
			    newName,
			    copier.copy(tree.restype),
			    copier.copy(tree.typarams),
			    copier.copy(tree.params),
			    copier.copy(tree.thrown),
			    copier.copy(methodBody),
			    null);

	// If this is an interface we must make the methods default
	/*	if ((this.currentClassDef.mods.flags & Flags.INTERFACE) != 0) {
	    System.out.println("Making default: " + newName);
	    newMethod.mods.flags = (newMethod.mods.flags | Flags.DEFAULT);
	    }*/

	// attr.attribStat(newMethod, enter.getClassEnv(this.currentClassDef.sym));
	// enterClassMember(this.currentClassDef, newMethod);
	this.newMethods.append(newMethod);

	return newMethod;
    }

    /**
     * Build the body for the maybe method call.
     *
     * @param safeMethod The method declaration for the safe version of the method.
     * @param normalMethod The method declaration for the normal, unchecked version
     *                     of the method.
     *
     * @return A method body for the maybe method.
     */
    private JCTree.JCBlock makeMaybeMethod(JCTree.JCMethodDecl safeMethod,
					   JCTree.JCMethodDecl normalMethod) {
	JCTree.JCExpression selectCheckMethod =
	    dotsExp(this.runtimeCheckIsCheckedFQMethodName);

	JCTree.JCExpression checkInvocation =
	    maker.Apply(null, selectCheckMethod,
			//List.of(maker.This(this.currentClassDef.sym.type)));
                        List.of(dotsExp("this")));

	JCTree.JCStatement ifPart = makeMethodCall(safeMethod);
	JCTree.JCStatement elsePart = makeMethodCall(normalMethod);

	JCTree.JCStatement checkedStatement = (JCTree.JCStatement)
	    builder.buildIfStatement(checkInvocation, ifPart, elsePart);

	return (JCTree.JCBlock) builder.buildStmtBlock(checkedStatement);
    }

    /**
     * Determine if the class already defines the safe (and maybe) methods for the
     * given method.  Check method name as well as params.
     *
     * @param classDef The class to check if the safe method is defined on.
     * @param methodDef The method to check for safe version of.
     * @return True iff there is already a safe version of the method.
     */
    protected boolean alreadyDefinedSafeMethod(JCTree.JCClassDecl classDef,
					       JCTree.JCMethodDecl methodDef) {
	List<JCTree> defs = classDef.defs;
	while(defs != null && defs.head != null) {
	    if (this.isDefSafeMethodOf(defs.head, methodDef)) {
		return true;
	    }
	    defs = defs.tail;
	}

	return false;
    }

    /**
     * Determines if a def matches a method, and is the safe version of that method.
     *
     * @param def The def to compare with the method.
     * @param methodDef The method to compare with.
     * @return True iff the def is the safe version of the methodDef.
     */
    protected boolean isDefSafeMethodOf (JCTree def, JCTree.JCMethodDecl methodDef) {
	// Check def is a method.
	if (!(def instanceof JCTree.JCMethodDecl)) {
	    return false;
	}

	// Check def name matches.
	JCTree.JCMethodDecl possibleMatch = (JCTree.JCMethodDecl) def;
	if (!possibleMatch.getName().toString().equals(methodDef.getName().toString()
						       + this.safeMethodNamePostfix)) {
	    return false;
	}

	// Check params match.
	List<JCTree.JCVariableDecl> params = possibleMatch.params;
	List<JCTree.JCVariableDecl> params2 = methodDef.params;
	while (params != null && params.head != null) {
	    // Wrong number of params.
	    if (params2 == null || params2.head == null) {
		return false;
	    }

	    // Params don't match
	    if (!this.variableDeclsMatch(params.head, params2.head)) {
		return false;
	    }
	    params = params.tail;
	    params2 = params2.tail;
	}

	// All else fails methods match.
	return true;
    }

    /**
     * Check if a list of defs contains the marker field we define.
     *
     * @param defs The list of defs to search for the marker field.
     * @return True iff the defs list contains a marker field.
     */
    protected boolean markerFieldPresent (List<JCTree> defs) {
	while (defs != null && defs.head != null) {
	    if (defs.head instanceof JCTree.JCVariableDecl) {
		JCTree.JCVariableDecl varDecl = (JCTree.JCVariableDecl) defs.head;
		PrimitiveType booleanType =
		    this.procEnv.getTypeUtils().getPrimitiveType(TypeKind.BOOLEAN);
		if (varDecl.getName().toString().equals(this.markerFieldName) &&
		    varDecl.vartype.toString().equals(booleanType.toString())) {
		    return true;
		}
	    }

	    defs = defs.tail;
	}
	return false;
    }

    /**
     * Check to see if variable decls match in both type and name.
     *
     * @param varDecl1
     * @param varDecl2
     * @return True iff varDecl1 and varDecl2 have the same name and type.
     */
    protected boolean variableDeclsMatch(JCTree.JCVariableDecl varDecl1,
					 JCTree.JCVariableDecl varDecl2) {
	return varDecl1.name.toString().equals(varDecl2.name.toString()) &&
	    varDecl1.vartype.toString().equals(varDecl2.vartype.toString());
    }

     /**
      * Actually perform runtime check additions.
      */
    private JCTree runtimeCheckMethod(JCTree.JCMethodDecl tree) {
	//System.err.println("Method name: " + tree.getName());
	//System.err.println("Method: " + tree);
	if (tree.getName().toString().equals(this.constructorMethodName) ||
	    tree.getName().toString().endsWith(this.safeMethodNamePostfix) ||
	    tree.getName().toString().endsWith(this.maybeMethodNamePostfix) ||
	    this.alreadyDefinedSafeMethod(this.currentClassDef, tree)) {
	    return tree;
	}

	JCTree.JCMethodDecl newSafeMethod =
	    makeNewMethod(tree, this.safeMethodNamePostfix, tree.body);

	if (!tree.mods.getFlags().contains(Modifier.STATIC)) {
	    JCTree.JCMethodDecl maybeMethod =
		makeNewMethod(tree,
			      this.maybeMethodNamePostfix,
			      makeMaybeMethod(newSafeMethod, tree));

	    if ((this.currentClassDef.mods.flags & Flags.INTERFACE) != 0) {
		maybeMethod.mods.flags |= (Flags.DEFAULT + 0L);
	    }

            maybeMethod.mods.flags = maybeMethod.mods.flags & ~(Flags.ABSTRACT);
	}

	// Translate the body after attributing the new method so that the new
	// name is available.  There is no need to translate the maybe method
	// because we constructed it so we know what it contains and there is
	// nothing our translation would care to modify there.
	newSafeMethod.body = translate(newSafeMethod.body);

	// If this is an interface, and if the original method had no body, which
	// is likely for an interface, unless it was a default method, then we
	// should keep it a default method so that classes implementing the interface
	// aren't required to implement the method, but that the interface type may
	// still have a safe version.
	if ((this.currentClassDef.mods.flags & Flags.INTERFACE) != 0
	    && newSafeMethod.body == null) {
            newSafeMethod.mods.flags = newSafeMethod.mods.flags & ~(Flags.ABSTRACT);
	    newSafeMethod.mods.flags |= Flags.DEFAULT;
	    
	    newSafeMethod.body = maker.Block(0, List.of((JCTree.JCStatement)maker.Throw(
		maker.NewClass(null,
			       List.<JCTree.JCExpression>nil(),
			       maker.Ident(names.fromString("RuntimeException")),
			       List.<JCTree.JCExpression>nil(),
			       null))));
	} else {
	    // Put the original method back with a new safe call.
	    JCTree.JCStatement newCode = makeCheckedMethodCall(newSafeMethod);
	    tree.body = maker.Block(0L, List.of(newCode));
            tree.mods.flags = tree.mods.flags & ~(Flags.ABSTRACT);
	}

	// System.err.println("Method: " + tree);
	// System.err.println("Return value: " + tree.getReturnType());
	// attributeInMethod(tree.body, tree, tree.body);

	return tree;
    }
}
