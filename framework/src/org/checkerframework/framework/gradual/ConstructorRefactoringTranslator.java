package org.checkerframework.framework.gradual;

import com.sun.source.tree.Tree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAnnotatedType;
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
import com.sun.tools.javac.util.Pair;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
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
 * This tree translator converts constructors into "safe" versions
 * which have no checks, and have an extra dummy marker parameter
 * added to disambiguate them from standard constructor.
 *
 * It then replaces the method body of the original constructor with
 * call to the new one guarded by runtime checks on the parameter values.
 *
 * TODO(danbrotherston): Refactor common logic here with
 * MethodRefactoringTreeTranslator.
 */
public class ConstructorRefactoringTranslator<Checker extends BaseTypeChecker>
    extends HelpfulTreeTranslator<Checker> {

    public ConstructorRefactoringTranslator(Checker c,
					    ProcessingEnvironment env,
					    TreePath p,
					    String argumentCheckMethod) {
	super(c, env, p);
	this.builder = new TreeBuilder(env);
	this.procEnv = env;
	this.argumentCheckFunctionName = argumentCheckMethod;
	this.copier = new TreeCopier<Void>(maker);
    }

    /**
     * The class of the dummy marker class used to indicate safe
     * constructors.
     */
    protected final Class<SafeConstructorMarkerDummy> constructorMarkerClass =
	SafeConstructorMarkerDummy.class;

    /**
     * Copier used for copying portions of the tree.
     */
    private final TreeCopier<Void> copier;

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
     * The class of the nullable annotation class used for adding
     * a nullable annotation to correctly typecheck the dummy param.
     */
    protected final String nullableAnnotationFQClassName =
	"org.checkerframework.checker.nullness.qual.Nullable";

    /**
     * The secret name given to constructors in Java.
     */
    protected final String constructorMethodName = "<init>";

    /**
     * The parameter name ot use for the marker param in constructors.
     */
    protected final String paramName = "dummyMarkerParam$";

    /**
     * The processing environment this translator operates in.
     */
    protected final ProcessingEnvironment procEnv;

    /**
     * The current class definition being processed.  We must know this
     * in order to properly insert new methods into this class.
     */
    protected JCTree.JCClassDecl currentClassDef;

    /**
     * This field stores a tree builder used for building trees used in
     * converting the constructor.
     */
    protected final TreeBuilder builder;

    /**
     * The new constructors to add to the current class.
     */
    protected ListBuffer<JCTree> newConstructors;

    @Override
    public void visitClassDef(JCTree.JCClassDecl tree) {
	// Check for classes which can't have constructors
	if ((tree.mods.flags & (Flags.INTERFACE | Flags.ENUM)) != 0) {
	    result = tree;
	    return;
	}

	// Must store previous class def so we can restore it after this class is finished
	// processing, in order to accomodate nested classes.  Effectively a stack.
	JCTree.JCClassDecl prevClassDef = this.currentClassDef;
	ListBuffer<JCTree> prevNewConstructors = this.newConstructors;

	// Setup for class processing.
	this.currentClassDef = tree;
	this.newConstructors = new ListBuffer<JCTree>();

	// Process class.
	super.visitClassDef(tree);

	// Add new constructors to the class.
	this.newConstructors.appendList(tree.defs);
	tree.defs = this.newConstructors.toList();
	result = tree;

	// Restore state.
	this.currentClassDef = prevClassDef;
	this.newConstructors = prevNewConstructors;
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl tree) {
	// System.out.println("Visiting Method: " + tree.getName().toString());
	// Only transform constructors which take parameters.
	if (tree.params != null && tree.params.head != null &&
	    tree.getName().toString().equals(this.constructorMethodName)) {
	    // System.out.println("Transforming method");

	    //	System.out.println("Ctor Body: " + tree.body);
	    //	if (tree.body.stats != null && tree.body.stats.head != null &&
	    //      tree.getName().toString().equals(this.constructorMethodName)) {
	    //	    System.out.println("First statement tree: " + tree.body.stats.head.getClass());
	    //		System.out.println("Expression statement expr: " +
	    //			   ((JCTree.JCMethodInvocation)(((JCTree.JCExpressionStatement
	    //                     (tree.body.stats.head)).getExpression())).meth.getClass());}

	    result = runtimeCheckConstructor(tree);
	    //	super.visitMethodDef(tree);
	} else {
	    super.visitMethodDef(tree);
	}
    }

    /**
     * Returns the marker class type object.
     */
    private Type getMarkerClassType() {
	return ((Symbol) (this.builder.getClassSymbolElement(this.constructorMarkerClass,
							     this.procEnv))).type;
    }	

    /**
     * Add runtime checks to the constructor call, and add a new constructor
     * without them.
     */
    private JCTree.JCMethodDecl runtimeCheckConstructor(JCTree.JCMethodDecl tree) {
	// ASSERT: JCMethodDecl is a constructor call.

	/*	JCTree.JCVariableDecl newParam =
	    maker.Param(names.fromString(this.paramName),
			this.getMarkerClassType(),
			this.currentClassDef.sym);*/
	
	Class<?> nullableAnnotationClass = null;
	try {
	    nullableAnnotationClass = Class.forName(this.nullableAnnotationFQClassName);
	} catch (ClassNotFoundException e) {
	    System.err.println("linking error, cannot get the annotation nullable class name during compilation.");
	}
	Type nullableAnnotationType = 
	    ((Symbol) (this.builder.getClassSymbolElement(nullableAnnotationClass,
							  this.procEnv))).type;

        JCTree.JCAnnotation nullableAnnotation =
	    maker.Annotation(new Attribute.Compound(nullableAnnotationType,
						    List.<Pair<MethodSymbol, Attribute>>nil()));
	JCTree.JCAnnotatedType markerClassTypeAnnotatedNullable =
	    maker.AnnotatedType(List.of(nullableAnnotation),
				maker.Type(this.getMarkerClassType()));

	JCTree.JCVariableDecl newParam =
	    maker.VarDef(maker.Modifiers(Flags.PARAMETER,
					 List.<JCTree.JCAnnotation>nil()),
			 names.fromString(this.paramName),
			 markerClassTypeAnnotatedNullable,
			 null);

	ListBuffer<JCTree.JCVariableDecl> newParamList = new ListBuffer<JCTree.JCVariableDecl>();
	List<JCTree.JCVariableDecl> params = tree.params;

	/*JCTree.JCVariableDecl newParam = copier.copy(tree.params.head);
	newParam.name = names.fromString(this.paramName);
	newParam.vartype = markerClassTypeAnnotatedNullable;
        newParam.mods.flags = (newParam.mods.flags & ~(Flags.VARARGS));*/

	while (params != null && params.head != null) {
	    newParamList.append(copier.copy(params.head));
	    params = params.tail;
	}

        System.err.println("NEWCONSTRUCTOR");
	JCTree.JCMethodDecl newConstructor =
	    maker.MethodDef(tree.mods,
			    tree.name,
			    tree.restype,
			    tree.typarams,
			    newParamList.toList().prepend(newParam),
			    tree.thrown,
			    tree.body,
			    tree.defaultValue);

	// System.err.println("Sym: " + tree.sym);
	newParam.sym = new VarSymbol(Flags.PARAMETER,
				     names.fromString(this.paramName),
				     this.getMarkerClassType(),
				     tree.sym);
	newParam.sym.pos = 1000000;

	JCTree.JCStatement newCode = makeConstructorCall(tree);	

	// enterClassMember(this.currentClassDef, newConstructor);
	this.newConstructors.append(newConstructor);

	newConstructor.body = translate(newConstructor.body);

	tree.body = maker.Block(0L, List.of(newCode));
	return tree;
    }

    /**
     * Builds a checked argument to a function by calling the argument checking code.
     */
    private JCTree.JCExpression makeCheckedArgument(JCTree.JCExpression argument,
						    JCTree.JCExpression argumentType,
						    Object sym) {
	JCTree.JCExpression checkerFunction = dotsExp(this.argumentCheckFunctionName);
        System.err.println("TESTINSERTED");
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
     * Implement a constructor call to the new constructor.
     */
    private JCTree.JCStatement makeConstructorCall(JCTree.JCMethodDecl tree) {
	JCTree.JCExpression constructor = dotsExp("this");

	ListBuffer<JCTree.JCExpression> args = new ListBuffer<JCTree.JCExpression>();
	List<JCTree.JCVariableDecl> params = tree.params;
	while (params != null && params.head != null) {
	    args.append(makeCheckedArgument(maker.Ident(params.head.name),
                                            copier.copy(params.head.vartype),
					    params.head.sym));
	    params = params.tail;
	}
	
	List<JCTree.JCExpression> argList = args.toList();
	JCTree.JCExpression newArg =
	    maker.TypeCast(this.getMarkerClassType(),
			   maker.Literal(TypeTag.BOT, null));
	argList = argList.prepend(newArg);
	return maker.Exec(maker.Apply(null, constructor, argList));
    }
}
