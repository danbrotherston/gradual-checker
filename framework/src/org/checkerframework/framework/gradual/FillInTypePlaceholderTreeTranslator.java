package org.checkerframework.framework.gradual;

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
import javax.lang.model.element.AnnotationMirror;
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

import java.lang.annotation.Annotation;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationUtils;
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
@SuppressWarnings("unchecked")
public class FillInTypePlaceholderTreeTranslator<Checker extends BaseTypeChecker>
    extends HelpfulTreeTranslator<Checker> {
    public FillInTypePlaceholderTreeTranslator(Checker c,
					       ProcessingEnvironment env,
					       TreePath p,
					       String runtimeMethodName) {
	super(c, env, p);
	this.builder = new TreeBuilder(env);
	this.aTypeFactory = c.getTypeFactory();
	this.runtimeCheckArgumentFQMethodName = runtimeMethodName;
	this.procEnv = env;
	Class<? extends Annotation> NonNullClass = null;
	try {
	    NonNullClass =
		(Class<? extends Annotation>)(Class.forName("org.checkerframework.checker.nullness.qual.NonNull"));


	} catch (ClassNotFoundException e) {
	    System.err.println("WTF!!!!!");
	}

	NONNULL = AnnotationUtils.fromClass(procEnv.getElementUtils(), NonNullClass);
    }

    /**
     * Holds the arguments to the test method, which we can use to revert to no
     * test if the type supports it.
     */
    private JCTree.JCExpression revertArg = null;

    /**
     * Store the args to the test method so we can revert as needed.
     */
    private List<JCTree.JCExpression> args = null;

    private final AnnotationMirror NONNULL;

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

    /**
     * The secret name given to constructors in Java.
     */
    protected final String constructorMethodName = "<init>";

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

    protected final String safeMethodNamePostfix =
	"_$safe";

    /**
     * This string references the isChecked method to determine if an object is
     * based on a class which has been checked by the checker framework.
     */
    protected final String runtimeCheckArgumentFQMethodName;

    /**
     * The index of the argument we're translating right now.
     */
    private int argIndex = -1;

    /**
     * Keep track of the method def we're processing.
     */
    private JCTree.JCMethodDecl lastMethodDef = null;

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl tree) {
	JCTree.JCMethodDecl prevLastMethod = this.lastMethodDef;
	this.lastMethodDef = tree;
	// System.err.println("Visiting method: " + tree);
	super.visitMethodDef(tree);
	this.lastMethodDef = prevLastMethod;
    }

    @Override
    public void visitLiteral(JCTree.JCLiteral tree) {
	// System.out.println("treevalue: \"" + tree.getValue().toString() + "\"");
	if (tree.getValue() != null &&
              tree.getValue().toString().equals(this.stringLiteralFillInMarker)) {
	    if (this.argIndex < 0) {
		assert false;
		// We aren't processing an argument right now, so why are we seeing this
		// literal.
	    }

	    AnnotatedTypeMirror type = getCurrentParamAnnotatedTypeMirror();
	    if (!type.hasAnnotation(NONNULL)) {
		System.err.println("Setting REVERT");
		this.revertArg = this.getCurrentArg();
	    }
	    result = maker.Literal(type.toString());
	    //result = maker.Literal(getCurrentParamType());
	    // System.out.println("Replacing with: " + result.getValue());
	} else {
	    result = tree;
	}
    }

    private JCTree.JCExpression getCurrentArg() {
	return this.args.head;
    }

    private AnnotatedTypeMirror getCurrentParamAnnotatedTypeMirror() {
	JCTree.JCVariableDecl arg = this.getCurrentParam();
	// System.err.println("arg: " + arg);
	// System.err.println("atypefact: " + aTypeFactory);
	AnnotatedTypeMirror argType = aTypeFactory.getAnnotatedType(arg.sym);
	// System.out.println("argType: " + argType);
	//return argType.toString();
	return argType;
    }

    private JCTree.JCVariableDecl getCurrentParam() {
	List<JCTree.JCVariableDecl> params = this.lastMethodDef.params;
	// System.err.println("lastmeth: " + this.lastMethodDef);
	// System.err.println("Params: " + params);
	// System.err.println("Current index: " + this.argIndex);

	for (int i = 0; i < this.argIndex; i++) {
	    if (params == null || params.head == null) {
		// System.err.println("i: " + i + " argIndex: " + this.argIndex);
		// System.err.println("Method: " + this.lastMethodDef);
		assert false;
		// Insufficient parameters in this particular method, why do we have too many.
	    }

	    params = params.tail;
	}

	return params.head;
    }

    private List<JCTree.JCExpression> recordAndTranslate(List<JCTree.JCExpression> args) {
	if (args == null) return null;
	//int prevArgIndex = this.argIndex;
	//System.err.println("record and translate: " + args);

	if (this.lastMethodDef != null &&
	    this.lastMethodDef.getName() != null &&
	    this.lastMethodDef.getName().toString().equals(this.constructorMethodName)) {
	    this.argIndex = -1;
	} else {
	    this.argIndex = 0;
	}

	for (List<JCTree.JCExpression> l = args; l.nonEmpty(); l = l.tail) {
	    // System.err.println("Trans arg index: " + this.argIndex + " for arg: " + l.head);
	    this.revertArg = null;
	    l.head = translate(l.head);
	    if (revertArg != null) {
		System.err.println("REVERTING ARG");
		l.head = revertArg;
	    }
	    this.argIndex++;
	}

	//this.argIndex = prevArgIndex;
	this.argIndex = -1;
	return args;
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation tree) {
	tree.meth = translate(tree.meth);
	// System.err.println("Tree meth: " + tree);
	int prevArgIndex = this.argIndex;
	if (!tree.meth.toString().equals(this.runtimeCheckArgumentFQMethodName)) {
	    tree.args = recordAndTranslate(tree.args);
	} else {
	    List<JCTree.JCExpression> prevArgs = this.args;
	    this.args = tree.args;
	    tree.args = translate(tree.args);
	    this.args = prevArgs;
	}
	this.argIndex = prevArgIndex;
	result = tree;
    }
}
