package org.checkerframework.framework.gradual;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.code.TypeTag.*;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.trees.TreeBuilder;
import org.checkerframework.javacutil.TreeUtils;

import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

/**
 * This class builds runtime checks given the required methods and classes.
 */
public class RuntimeCheckBuilder<Checker extends BaseTypeChecker> {

    /**
     * Class to use to perform runtime checks.
     */
    protected final Class<?> runtimeCheckClass;

    /**
     * Static method on the above class to actually call to perform static checks.
     */
    protected final Method runtimeCheckMethod;

    /**
     * Static method on the above class to call in the event of a runtime
     * type error.
     */
    protected final Method runtimeFailureMethod;

    /**
     * Helper class to help with building runtime check trees.
     */
    protected final TreeBuilder builder;

    /**
     * Processing environment to build the trees within.
     */
    protected final ProcessingEnvironment procEnv;

    /**
     * String representing the variable name used to store the value being tested
     * in the runtime test.  Must be in a variable so the value can be used multiple
     * times without duplicating side effects.
     */
    protected final String runtimeValueVarName = "checkerRuntimeValueVar$";

    /**
     * The checker which is building the checks.
     */
    protected final Checker checker;

    /**
     * Javac Tree Maker.
     */
    protected final TreeMaker maker;

    /**
     * Javac types instance.
     */
    protected final Types types;

    /**
     * Provide the required methods and class to call them on in order to build
     * runtime typechecks.
     *
     * @param runtimeCheckClass should be a class with at least two static methods.
     * @param runtimeCheckMethod should be a static method on the runtimeCheckClass
     * which takes one Object parameter and an AnnotatedTypeMirror parameter and
     * return a booleans to determine if the check succeeds.
     * @param runtimeFailureMethod takes the same parameters as the runtime check
     * method but should be used to report an error.
     * @param env The processing environment to use while building trees.
     */
    public RuntimeCheckBuilder(Checker c,
			       Class<?> runtimeCheckClass,
			       Method runtimeCheckMethod,
			       Method runtimeFailureMethod,
			       ProcessingEnvironment env) {
	this.runtimeCheckClass = runtimeCheckClass;
	this.runtimeCheckMethod = runtimeCheckMethod;
	this.runtimeFailureMethod = runtimeFailureMethod;
	this.checker = c;
	this.builder = new TreeBuilder(env);
	this.maker = TreeMaker.instance(((JavacProcessingEnvironment)env).getContext());
	this.types = Types.instance(((JavacProcessingEnvironment)env).getContext());
	this.procEnv = env;
    }

    /**
     * Gets the owner of a given Tree node (given its tree path), in order to create
     * and properly own symbols.
     *
     * TODO(danbrotherston):  This is based on code from
     *     CFGBuilder.java:getAssertionsEnabledVariable and maybe other places.
     *     Consider refactoring it into TreeUtils since it seems to be common, and
     *     verify that this is robust.
     *
     * @param path The path to the node to get symbol owners for.
     * @return The owner element to use for building symbols referenced in this
     *         context.
     */
    protected Element getSymbolOwner(TreePath path) {
	MethodTree enclosingMethod = TreeUtils.enclosingMethod(path);
	Element owner =  null;
	if (enclosingMethod != null) {
	    owner = TreeUtils.elementFromDeclaration(enclosingMethod);
	} else {
	    ClassTree enclosingClass = TreeUtils.enclosingClass(path);
	    owner = TreeUtils.elementFromDeclaration(enclosingClass);
	}

	return owner;
    }

    /**
     * This method builds a static check for a given value, when that value
     * occurs in the context of a variable declaration.  Since a variable
     * declaration affects scope, we must treat it specially, ensuring that
     * the variable remains in scope after the check.  This involves directly
     * editing the blcok the variable declaration is contained within.
     * 
     * We add a variable declaration in the actual statement, with no
     * no initializer, then in the check on the value, if successful, the
     * variable is set to the correct initializer value with an assignment.
     *
     * This function uses the standard check builder, by passing our
     * constructed assignment statement instead of the variable declaration
     * to build the check.
     *
     * @param value The given runtime value to test for runtime typing (this
     *              is part of the variable decl initializer).
     * @param statement The variable declaration statement.
     * @param containingBlock The parent node to the variable declaraion.
     * @param compilationUnit The compilation unit this check occurs within.
     * @param type The compile time type to test the value against at runtime.
     * @param path The tree path to the variable declaration.
     * @return This function returns a Map.Entry<JCTree, JCTree> representing
     *         a tuple.  The first value is the modified JCBlock and the
     *         second is the new assignment statement to be attributed.
     *
     * TODO (danbrotherston): This function is highly coupled to the tree
     * translator.  There is likely some refactoring here that could reduce
     * this coupling.
     */

    public JCTree.JCVariableDecl buildNewVariableDecl(JCVariableDecl variableDecl,
						      Element symbolOwner) {

	// Build a variable declaration which will create the original variable in
	// scope but initialize it to a null/0 value.
	JCTree.JCVariableDecl newVarDecl = (JCTree.JCVariableDecl)
	    builder.buildVariableDecl(variableDecl.getType(),
				      variableDecl.getName().toString(),
				      symbolOwner, null);

	return newVarDecl;
    }

    public JCTree.JCStatement buildAssignmentStatement(JCVariableDecl originalDecl,
						       JCVariableDecl newDecl) {

	// Build an assignment from the old initializer, this will be the new statement passed
	// to the runtimeCheck builder.
	JCTree.JCStatement assignment = (JCTree.JCStatement)
	    builder.buildAssignment(newDecl, originalDecl.getInitializer());

	return assignment;
    }

    public Map.Entry<JCTree, JCTree>  buildVarDeclRuntimeCheck(JCExpression value,
							       JCVariableDecl variableDecl,
							       JCBlock containingBlock,
							       TreePath compilationUnit,
							       AnnotatedTypeMirror type,
							       TreePath path) {
	// Build a variable declaration.  The declaration will create the original variable
	// in scope, but leave it uninitialized.
	JCTree.JCVariableDecl variable = (JCTree.JCVariableDecl)
	    builder.buildVariableDecl(variableDecl.getType(),
				      variableDecl.getName().toString(),
				      this.getSymbolOwner(path), null);

	// Build an assignment from the old initializer, this will be the new statement passed
	// to the runtimeCheck builder.
	JCTree.JCStatement assignment = (JCTree.JCStatement)
	    builder.buildAssignment(variable, variableDecl.getInitializer());

	Map.Entry<JCTree, JCTree> checkAndVar =
	    this.buildRuntimeCheck(value, assignment, compilationUnit, type, path);

	JCTree.JCStatement check = (JCTree.JCStatement) (checkAndVar.getKey());

	// Iterate through the statements in the block.
	com.sun.tools.javac.util.List<JCStatement> stats = containingBlock.stats;
	while (stats != null && stats.head != null) {
	    // When we get to the variable declaration, replace it with the
	    // the new empty declaration, followed by the check.
	    if (stats.head == variableDecl) {
		stats.head = variable;

		com.sun.tools.javac.util.List<JCStatement> newStat =
		    com.sun.tools.javac.util.List.of(check);

		// Splice linked lists.
		newStat.tail = stats.tail;
		stats.tail = newStat;
		break;
	    }

	    stats = stats.tail;
	}

	return new SimpleEntry<JCTree, JCTree>(containingBlock, variable);
    }

    /**
     * This method buids a static check for a given value and a given tree.
     *
     * @param value The given runtime value to test for runtime typing.
     * @param statement The statement which the value appears in which must be
     *                  executed in the event of a successful test.
     * @param compilationUnit The compilation unit this statement occurs within.
     * @param type The type to test the value against at runtime.
     * @param path The path to the statement.
     *
     * This method works by building a JCTree (AST Tree) comprised of an if
     * statement with a condition which calls the above runtime test and provides
     * as a parameter the given value expression tree.  The if-true branch of the
     * if statement executes the given statement, and the if-false branch of the
     * tree executes the runtimeFailureMethod with the given values.
     * 
     * The type is provided statically at compile time to test the value against.
     * The user of this method is expected to replace the given statement with
     * the return value of this function which is itself a statement AST tree.
     * The user of this function is also responsible for correctly attributing
     * the AST tree so that it can be used in the context in which it is placed.
     *
     * TODO: The last issue of consideration is how to deal with serializing the
     * compile time type.  Right now, it is simply stringified and the runtime
     * test is responsible for serializing and determineing the type.  However,
     * this works only for simple types.  Types with complex annotations and
     * possible composition and lists of parameters to the annotations may be
     * expensive to parse.  Consideration should be given to to a more efficient
     * method of representing the type as a constant.  Also, this doesn't allow
     * complext type hierarchys to be represented.  To overcome this, the type
     * hierarchy in question must be decoupled from the compiler and moved into
     * portable code that can be loaded at runtime.  Then the type should be
     * converted into the most specific type in that hierarchy which represents
     * the value under consideration, then the standard subtype test applied.
     * This would be required for general type hierarchys.
     *
     * @return This function returns a Map.Entry<JCTree, JCTree> effecting a
     *         tuple of two values.  The first is the new statement, the actual
     *         runtime check.  The other is the modified original statement that
     *         needs to be attributed.
     */

    private int counter = 0;

    private com.sun.tools.javac.util.List<Type>
	fixTypes(com.sun.tools.javac.util.List<Type> originalTypes, boolean isOuter) {

	com.sun.tools.javac.util.List<Type> newTypes = com.sun.tools.javac.util.List.<Type>nil();
	com.sun.tools.javac.util.List<Type> iterator = null;

	while (originalTypes != null && !originalTypes.isEmpty() && originalTypes.head != null) {
	    Type newType = fixTypes(originalTypes.head, isOuter);
	    
	    if (iterator == null) {
		iterator = com.sun.tools.javac.util.List.of(newType);
		newTypes = iterator;
	    } else {
		iterator.tail = com.sun.tools.javac.util.List.of(newType);
		iterator = iterator.tail;
	    }

	    originalTypes = originalTypes.tail;
	}

	return newTypes;
    }
							 

    private Type fixTypes(Type t, boolean isOuter) {
	if (t == null) { return null; }
	switch (t.getTag()) {
	case BYTE: case CHAR: case DOUBLE: case INT: case LONG: case FLOAT:
	case BOOLEAN: case VOID:
	    return t;
	case TYPEVAR:
	    return t;
	case WILDCARD:
	    return t;
	case CLASS:
	    /*	    System.err.println("ClassType: " + t);
	    Type outer = t.getEnclosingType();
	    System.err.println("Outer: " + outer);
	    outer = fixTypes(outer, true);
	    System.err.println("Outer after fixing: " + outer);
	    com.sun.tools.javac.util.List<Type> tyParams = null;
	    if (isOuter) {
		tyParams = com.sun.tools.javac.util.List.<Type>nil();
	    } else {
		tyParams = t.getTypeArguments();
	    }
	    System.err.println("TypeArgs: " + tyParams);
	    tyParams = fixTypes(tyParams, false);
	    System.err.println("TypeArgs after fix: " + tyParams);
	    return new ClassType(outer, tyParams, t.tsym);*/

	    Type outer = fixTypes(t.getEnclosingType(), true);

	    if (t.tsym.getModifiers().contains(Modifier.PRIVATE)) {
		System.err.println("outer to noType");
		outer = Type.noType;
		//Type newType = new ClassType(Type.noType, t.getTypeArguments(), t.tsym);
		//System.err.println("Since private, replace: " + t + " with: " + newType);
		//return newType;
	    }

	    com.sun.tools.javac.util.List<Type> fixedTyArgs = fixTypes(t.getTypeArguments(), false);

	    Type newType = new ClassType(outer, fixedTyArgs, t.tsym);
	    System.err.println("New Type: " + newType + " replaces: " + t);
	    return newType;

	case ARRAY:
	    return new ArrayType(fixTypes(types.elemtype(t), isOuter), t.tsym);
	case ERROR:
	    return t;
	default:
	    return t;
	}
    }

    public JCVariableDecl buildVariable(Type type,
					Element symbolOwner,
					JCExpression value) {
	// Build a variable declaration.  The declaration will be used so that any side
	// effects of the value expression being tested aren't duplicated.  This variable
	// is initialized to the value of the value expression, then references to this
	// variable are used any time the expression value is needed.
	Type realType = type;
	System.err.println("Building variable with type: " + realType);
	if (realType instanceof Type.ClassType) {
	    System.err.println("Outer: " + realType.getEnclosingType());
	}
	/*Type newType = fixTypes(realType, false);
	realType = newType;
	System.err.println("Building variable with type after erasure: " + realType);*/

	VariableTree variable =
	    builder.buildVariableDecl(type, 
				      this.runtimeValueVarName + this.counter++,
				      symbolOwner, value);
	System.err.println("Built tree: " + variable);
	return (JCVariableDecl) variable;
    }



    public JCExpression buildVariableUse(VariableTree variable) {
	// We need one more variable use instance in order to replace the value expression
	// within the original statement.
	JCExpression variableUse = (JCExpression) builder.buildVariableUse(variable);
	return variableUse;
    }

    public JCStatement buildRuntimeCheck(JCStatement statement,
					 JCStatement statementCopy,
					 VariableTree variable,
					 AnnotatedTypeMirror type,
					 ExpressionTree checkMethodAccess,
					 ExpressionTree failureMethodAccess) {
	// First get two class tree symbols to call the actual methods.
	/*	Element classTree1 =
	    builder.getClassSymbolElement(this.runtimeCheckClass, procEnv);

	Element classTree2 =
	builder.getClassSymbolElement(this.runtimeCheckClass, procEnv);

	// Get the method access tree for each the check and failure methods.
	JCTree checkMethodAccess = (JCTree)
	    builder.buildMethodAccess(this.runtimeCheckMethod, classTree1);
	//				      builder.buildClassUse(classTree1));

	JCTree failureMethodAccess = (JCTree)
	    builder.buildMethodAccess(this.runtimeFailureMethod, classTree2);
	//				      builder.buildClassUse(classTree2));
	*/
	// Build the method invocation tree given the method access tree and the parameters.
	JCTree checkMethodInvocation =
	    this.maker.Apply(null, (JCTree.JCExpression) checkMethodAccess,
			     com.sun.tools.javac.util.List.of((JCTree.JCExpression)
							      builder.buildVariableUse(variable),
							      (JCTree.JCExpression)
							      builder.buildLiteral(type.toString())));

	    /*(JCTree)
	    builder.buildMethodInvocation((JCExpression) checkMethodAccess,
 					  (JCExpression) builder.buildVariableUse(variable),
					  (JCExpression) builder.buildLiteral(type.toString()));*/

	JCTree failureMethodInvocation = 
	    this.maker.Apply(null, (JCTree.JCExpression) failureMethodAccess,
			     com.sun.tools.javac.util.List.of((JCTree.JCExpression)
							      builder.buildVariableUse(variable),
							      (JCTree.JCExpression)
							      builder.buildLiteral(type.toString())));
	    /*(JCTree)
	    builder.buildMethodInvocation((JCExpression) failureMethodAccess,
					  (JCExpression) builder.buildVariableUse(variable),
					  (JCExpression) builder.buildLiteral(type.toString()));*/
	
	// The if condition is simply the check method invocation.
	JCExpression condition = (JCExpression) checkMethodInvocation;

	// Build the rest of the if statement.  The if part is the original (but modified)
	// statement.  The else part is a call to the failure method, sequenced with the
	// original statement in the event the program should continue past the failure
	// method.
	JCStatement ifPart = (JCStatement) builder.buildStmtBlock(statement);
	JCStatement elsePart = (JCStatement) builder.buildStmtBlock(
	    builder.buildExpressionStatement((ExpressionTree) failureMethodInvocation),
	    statementCopy);

	// The whole check is the variable declaration sequenced with the if statement,
	// enclosed within a block to limit the scope of the variable declaration.
        JCStatement checkedStatement = (JCStatement) builder.
	    buildStmtBlock(variable,
	    		   builder.buildIfStatement(condition, ifPart, elsePart));

	return checkedStatement;
    }

    public Map.Entry<JCTree, JCTree>  buildRuntimeCheck(JCExpression value,
							JCStatement statement,
							TreePath compilationUnit,
							AnnotatedTypeMirror type,
							TreePath path) {
	// First get two class tree symbols to call the actual methods.
	Element classTree1 =
	    builder.getClassSymbolElement(this.runtimeCheckClass, procEnv);

	Element classTree2 =
	    builder.getClassSymbolElement(this.runtimeCheckClass, procEnv);

	// Get the method access tree for each the check and failure methods.
	JCTree checkMethodAccess = (JCTree)
	    builder.buildMethodAccess(this.runtimeCheckMethod,
				      builder.buildClassUse(classTree1));

	JCTree failureMethodAccess = (JCTree)
	    builder.buildMethodAccess(this.runtimeFailureMethod,
				      builder.buildClassUse(classTree2));

	// Build a variable declaration.  The declaration will be used so that any side
	// effects of the value expression being tested aren't duplicated.  This variable
	// is initialized to the value of the value expression, then references to this
	// variable are used any time the expression value is needed.
	VariableTree variable = builder.buildVariableDecl(type.getUnderlyingType(),
							  this.runtimeValueVarName,
							  this.getSymbolOwner(path), value);

	// Build the method invocation tree given the method access tree and the parameters.
	JCTree checkMethodInvocation = (JCTree)
	    builder.buildMethodInvocation((JCExpression) checkMethodAccess,
 					  (JCExpression) builder.buildVariableUse(variable),
					  (JCExpression) builder.buildLiteral(type.toString()));

	JCTree failureMethodInvocation = (JCTree)
	    builder.buildMethodInvocation((JCExpression) failureMethodAccess,
					  (JCExpression) builder.buildVariableUse(variable),
					  (JCExpression) builder.buildLiteral(type.toString()));

	// The if condition is simply the check method invocation.
	JCExpression condition = (JCExpression) checkMethodInvocation;

	// We need one more variable use instance in order to replace the value expression
	// within the original statement.
	JCExpression variableUse = (JCExpression) builder.buildVariableUse(variable);

	// Replace the value within the original statement with an instance of the variable.
	SingleReplacementTreeTranslator<Checker> replacer =
	    new SingleReplacementTreeTranslator<Checker>(this.checker,
							 this.procEnv,
							 compilationUnit,
							 value,
							 variableUse);
	// System.out.println("Statement Before: " + statement);
	statement.accept(replacer);
	// System.out.println("Statement After: " + statement);

	// Build the rest of the if statement.  The if part is the original (but modified)
	// statement.  The else part is a call to the failure method, sequenced with the
	// original statement in the event the program should continue past the failure
	// method.
	JCStatement ifPart = (JCStatement) builder.buildStmtBlock(statement);
	JCStatement elsePart = (JCStatement) builder.buildStmtBlock(
	    builder.buildExpressionStatement((ExpressionTree) failureMethodInvocation),
	    statement);

	// The whole check is the variable declaration sequenced with the if statement,
	// enclosed within a block to limit the scope of the variable declaration.
        JCStatement checkedStatement = (JCStatement) builder.
	    buildStmtBlock(variable,
	    		   builder.buildIfStatement(condition, ifPart, elsePart));

	// System.out.println("Final statement: " + checkedStatement);
	return new SimpleEntry<JCTree, JCTree>(checkedStatement, (JCTree) variable);
    }
}
