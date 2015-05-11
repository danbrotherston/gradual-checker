package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;

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
import javax.lang.model.util.Types;

/**
 * This class builds runtime checks given the required methods and classes.
 */
public class RuntimeCheckBuilder {

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
    protected final GradualNullnessChecker checker;

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
    public RuntimeCheckBuilder(GradualNullnessChecker c,
			       Class<?> runtimeCheckClass,
			       Method runtimeCheckMethod,
			       Method runtimeFailureMethod,
			       ProcessingEnvironment env) {
	this.runtimeCheckClass = runtimeCheckClass;
	this.runtimeCheckMethod = runtimeCheckMethod;
	this.runtimeFailureMethod = runtimeFailureMethod;
	this.checker = c;
	this.builder = new TreeBuilder(env);
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
     * This method buids a static check for a given value and a given tree.
     *
     * @param value The given runtime value to test for runtime typing.
     * @param statement The statement which the value appears in which must be
     *                  executed in the event of a successful test.
     * @param type The type to test the value against at runtime.
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
     * TODO: There are two issues to overcome right now, first, value should be
     * assigned into a local variable, and re-used in both the test and the
     * actual statement to execute.  Right now, this will have the effect of
     * repeating any sideeffects that occur in the value expression.  This can
     * probably be achieved using a replacer to replace the given tree.
     * 
     * TODO: There should be some way to provide the original statement to the
     * error case, such that if the designer wishes the program to attempt to
     * continue with the runtime type error, they can allow the program to 
     * continue execution.
     */
    public Map.Entry<JCTree, JCTree>  buildRuntimeCheck(JCExpression value,
							JCStatement statement,
							TreePath compilationUnit,
							AnnotatedTypeMirror type,
							TreePath path) {
	Element classTree1 =
	    builder.getClassSymbolElement(this.runtimeCheckClass, procEnv);

	Element classTree2 =
	    builder.getClassSymbolElement(this.runtimeCheckClass, procEnv);

	JCTree checkMethodAccess = (JCTree)
	    builder.buildMethodAccess(this.runtimeCheckMethod,
				      builder.buildClassUse(classTree1));

	JCTree failureMethodAccess = (JCTree)
	    builder.buildMethodAccess(this.runtimeFailureMethod,
				      builder.buildClassUse(classTree2));

	VariableTree variable = builder.buildVariableDecl(type.getUnderlyingType(),
							  this.runtimeValueVarName,
							  this.getSymbolOwner(path), value);

	JCTree checkMethodInvocation = (JCTree)
	    builder.buildMethodInvocation((JCExpression) checkMethodAccess,
 					  (JCExpression) builder.buildVariableUse(variable),
					  (JCExpression) builder.buildLiteral(type.toString()));

	JCTree failureMethodInvocation = (JCTree)
	    builder.buildMethodInvocation((JCExpression) failureMethodAccess,
					  (JCExpression) builder.buildVariableUse(variable),
					  (JCExpression) builder.buildLiteral(type.toString()));

	JCExpression condition = (JCExpression) checkMethodInvocation;

	JCExpression variableUse = (JCExpression) builder.buildVariableUse(variable);

	SingleReplacementTreeTranslator replacer =
	    new SingleReplacementTreeTranslator(this.checker, this.procEnv, compilationUnit,
						value, variableUse);
	// System.out.println("Statement Before: " + statement);
	statement.accept(replacer);
	// System.out.println("Statement After: " + statement);

	JCStatement ifPart = (JCStatement) builder.buildStmtBlock(statement);
	JCStatement elsePart = (JCStatement) builder.buildStmtBlock(
	    builder.buildExpressionStatement((ExpressionTree) failureMethodInvocation),
	    statement);

        JCStatement checkedStatement = (JCStatement) builder.
	    buildStmtBlock(variable,
	    		   builder.buildIfStatement(condition, ifPart, elsePart));

	// System.out.println("Final statement: " + checkedStatement);
	return new SimpleEntry<JCTree, JCTree>(checkedStatement, (JCTree) variable);
    }
}
