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
import com.sun.tools.javac.tree.TreeCopier;
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

import org.checkerframework.common.basetype.BaseTypeChecker;
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
public class FixAnnotationsFormatTreeTranslator<Checker extends BaseTypeChecker>
        extends HelpfulTreeTranslator<Checker> {
    public FixAnnotationsFormatTreeTranslator(Checker c,
					      ProcessingEnvironment env,
					      TreePath p) {
	super(c, env, p);
	this.builder = new TreeBuilder(env);
	this.procEnv = env;
    }

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
    protected final String runtimeCheckArgumentFQMethodName =
	"org.checkerframework.checker.gradualnullness.NullnessRuntimeCheck.runtimeCheckArgument";

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
	if (tree.mods != null &&
	    (tree.mods.flags & Flags.DEFAULT) != 0 &&
	    (tree.mods.flags & Flags.ABSTRACT) != 0) {
	    TreeCopier<Void> copier = new TreeCopier<Void>(maker);
	    tree.mods = copier.copy(tree.mods);
	    boolean hasBody = tree.body == null;
	    if (hasBody) {
		tree.mods.flags = tree.mods.flags & ~(Flags.ABSTRACT);
	    } else {
		tree.mods.flags = tree.mods.flags & ~(Flags.DEFAULT);
	    }
	}

	result = tree;
    }

    
    /*
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

	    result = maker.Literal(getCurrentArgType());
	    // System.out.println("Replacing with: " + result.getValue());
	} else {
	    result = tree;
	}
    }

    private String getCurrentArgType() {
	JCTree.JCVariableDecl arg = this.getCurrentArg();
	// System.out.println("arg: " + arg);
	AnnotatedTypeMirror argType = aTypeFactory.getAnnotatedType(arg.sym);
	// System.out.println("argType: " + argType);
	return argType.toString();
    }

    private JCTree.JCVariableDecl getCurrentArg() {
	List<JCTree.JCVariableDecl> params = this.lastMethodDef.params;
	// System.out.println("Params: " + params);
	// System.out.println("Current index: " + this.argIndex);

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
	this.argIndex = 0;
	for (List<JCTree.JCExpression> l = args; l.nonEmpty(); l = l.tail) {
	    // System.err.println("Trans arg: " + l.head);
	    l.head = translate(l.head);
	    this.argIndex++;
	}

	this.argIndex = -1;
	return args;
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation tree) {
	tree.meth = translate(tree.meth);
	// System.out.println("Tree meth: " + tree.meth.toString());
	if (!tree.meth.toString().equals(this.runtimeCheckArgumentFQMethodName)) {
	    tree.args = recordAndTranslate(tree.args);
	} else {
	    tree.args = translate(tree.args);
	}
	result = tree;
	}*/
/*
    @Override
    public void visitVarDef(JCTree.JCVariableDecl tree) {
	TreeCopier<Void> copier = new TreeCopier<Void>(maker);

	
	if (tree.getModifiers().getAnnotations() != null &&
	    tree.getModifiers().getAnnotations().nonEmpty()) {
	    System.err.println("Fixing one: " + tree);
	    if (tree.vartype instanceof JCTree.JCAnnotatedType) {
		System.err.println("Already has annotated type: " + tree.vartype);
	    }
	    tree.vartype =
		maker.AnnotatedType(copier.copy(tree.getModifiers().getAnnotations()),
				    copier.copy(tree.vartype));

	    tree.getModifiers().annotations = List.<JCTree.JCAnnotation>nil();
	}

	result = tree;
    }*/
}
