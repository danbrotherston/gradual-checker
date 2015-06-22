package org.checkerframework.checker.gradualnullness;

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
 * This tree translator converts methods into "safe" versions
 * which have no checks, and are renamed with a "_safe" postfix.
 *
 * This code is based on the MethodBindingTranslator from enerj,
 * but is simpler because the translation is unconditional.
 */
public class MethodRefactoringTreeTranslator extends HelpfulTreeTranslator<GradualNullnessChecker> {
    public MethodRefactoringTreeTranslator(GradualNullnessChecker c,
					   ProcessingEnvironment env,
					   TreePath p) {
	super(c, env, p);
	this.builder = new TreeBuilder(env);
	this.procEnv = env;
    }

    /**
     * This field stores the postfix to apply to save methods, and all
     * checked method calls.
     */
    protected final String methodNamePostfix = "_$safe";

    /**
     * Marker field name.
     */
    protected final String markerFieldName = "$isTypeCheckedMarker";

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
	this.newMethods.append(this.createMarkerField());
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

	enterClassMember(this.currentClassDef, newField);

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
	    selectMethod = maker.Select(maker.This(this.currentClassDef.sym.type),
					(Symbol) TreeUtils.elementFromDeclaration(tree));
	}

	ListBuffer<JCTree.JCExpression> args = new ListBuffer<JCTree.JCExpression>();
	List<JCTree.JCVariableDecl> params = tree.params;
	while (params != null && params.head != null) {
	    args.append(maker.Ident(params.head));
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
     * Actually perform runtime check additions.
     */
    private JCTree runtimeCheckMethod(JCTree.JCMethodDecl tree) {
	//System.err.println("Method name: " + tree.getName());
	//System.err.println("Method: " + tree);
	if (tree.getName().toString().equals("<init>")) {
	    return tree;
	}

	Name originalName = tree.getName();
	Name newName = names.fromString(originalName + this.methodNamePostfix);

	JCTree.JCMethodDecl newMethod =
	    maker.MethodDef(tree.mods,
			    newName,
			    tree.restype,
			    tree.typarams,
			    tree.params,
			    tree.thrown,
			    tree.body,
			    null);

	enterClassMember(this.currentClassDef, newMethod);
	this.newMethods.append(newMethod);
	// attr.attribStat(newMethod, enter.getClassEnv(this.currentClassDef.sym));

	JCTree.JCStatement newCode = makeMethodCall(newMethod);

	// Translate the body after attributing the new method so that the new
	// name is available.
	newMethod.body = translate(newMethod.body);

	tree.body = maker.Block(0L, List.of(newCode));
	// System.err.println("Method: " + tree);
	// System.err.println("Return value: " + tree.getReturnType());
	// attributeInMethod(tree.body, tree, tree.body);

	return tree;
    }						       
}
