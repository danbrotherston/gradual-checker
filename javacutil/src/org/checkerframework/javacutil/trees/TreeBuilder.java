package org.checkerframework.javacutil.trees;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TypesUtils;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

/**
 * The TreeBuilder permits the creation of new AST Trees using the
 * non-public Java compiler API TreeMaker.
 */

public class TreeBuilder {
    protected final Elements elements;
    protected final Types modelTypes;
    protected final com.sun.tools.javac.code.Types javacTypes;
    protected final TreeMaker maker;
    protected final Names names;
    protected final Symtab symtab;
    protected final ProcessingEnvironment env;

    public TreeBuilder(ProcessingEnvironment env) {
        this.env = env;
        Context context = ((JavacProcessingEnvironment)env).getContext();
        elements = env.getElementUtils();
        modelTypes = env.getTypeUtils();
        javacTypes = com.sun.tools.javac.code.Types.instance(context);
        maker = TreeMaker.instance(context);
        names = Names.instance(context);
        symtab = Symtab.instance(context);
    }

    /**
     * Builds an AST Tree to access the iterator() method of some iterable
     * expression.
     *
     * @param iterableExpr  an expression whose type is a subtype of Iterable
     * @return  a MemberSelectTree that accesses the iterator() method of
     *    the expression
     */
    public MemberSelectTree buildIteratorMethodAccess(ExpressionTree iterableExpr) {
        DeclaredType exprType =
            (DeclaredType)TypesUtils.upperBound(InternalUtils.typeOf(iterableExpr));
        assert exprType != null : "expression must be of declared type Iterable<>";

        TypeElement exprElement = (TypeElement)exprType.asElement();

        // Find the iterator() method of the iterable type
        Symbol.MethodSymbol iteratorMethod = null;

        for (ExecutableElement method :
                 ElementFilter.methodsIn(elements.getAllMembers(exprElement))) {
            Name methodName = method.getSimpleName();

            if (method.getParameters().size() == 0) {
                if (methodName.contentEquals("iterator")) {
                    iteratorMethod = (Symbol.MethodSymbol)method;
                }
            }
        }

        assert iteratorMethod != null : "no iterator method declared for expression type";

        Type.MethodType methodType = (Type.MethodType)iteratorMethod.asType();
        Symbol.TypeSymbol methodClass = methodType.asElement();
        DeclaredType iteratorType = (DeclaredType)methodType.getReturnType();
        TypeMirror elementType;

        if (iteratorType.getTypeArguments().size() > 0) {
            elementType = iteratorType.getTypeArguments().get(0);
            // Remove captured type from a wildcard.
            if (elementType instanceof Type.CapturedType) {
                elementType = ((Type.CapturedType)elementType).wildcard;
            }

            iteratorType =
                modelTypes.getDeclaredType((TypeElement)modelTypes.asElement(iteratorType),
                                      elementType);
        }


        // Replace the iterator method's generic return type with
        // the actual element type of the expression.
        Type.MethodType updatedMethodType =
            new Type.MethodType(com.sun.tools.javac.util.List.<Type>nil(),
                                (Type)iteratorType,
                                com.sun.tools.javac.util.List.<Type>nil(),
                                methodClass);

        JCTree.JCFieldAccess iteratorAccess =
            (JCTree.JCFieldAccess)
            maker.Select((JCTree.JCExpression)iterableExpr,
                         iteratorMethod);
        iteratorAccess.setType(updatedMethodType);

        return iteratorAccess;
    }

    /**
     * Builds an AST Tree to access the hasNext() method of an iterator.
     *
     * @param iteratorExpr  an expression whose type is a subtype of Iterator
     * @return  a MemberSelectTree that accesses the hasNext() method of
     *    the expression
     */
    public MemberSelectTree buildHasNextMethodAccess(ExpressionTree iteratorExpr) {
        DeclaredType exprType = (DeclaredType)InternalUtils.typeOf(iteratorExpr);
        assert exprType != null : "expression must be of declared type Iterator<>";

        TypeElement exprElement = (TypeElement)exprType.asElement();

        // Find the hasNext() method of the iterator type
        Symbol.MethodSymbol hasNextMethod = null;

        for (ExecutableElement method :
                 ElementFilter.methodsIn(elements.getAllMembers(exprElement))) {
            Name methodName = method.getSimpleName();

            if (method.getParameters().size() == 0) {
                if (methodName.contentEquals("hasNext")) {
                    hasNextMethod = (Symbol.MethodSymbol)method;
                }
            }
        }

        assert hasNextMethod != null : "no hasNext method declared for expression type";

        JCTree.JCFieldAccess hasNextAccess =
            (JCTree.JCFieldAccess)
            maker.Select((JCTree.JCExpression)iteratorExpr,
                         hasNextMethod);
        hasNextAccess.setType(hasNextMethod.asType());

        return hasNextAccess;
    }

    /**
     * Builds an AST Tree to access the next() method of an iterator.
     *
     * @param iteratorExpr  an expression whose type is a subtype of Iterator
     * @return  a MemberSelectTree that accesses the next() method of
     *    the expression
     */
    public MemberSelectTree buildNextMethodAccess(ExpressionTree iteratorExpr) {
        DeclaredType exprType = (DeclaredType)InternalUtils.typeOf(iteratorExpr);
        assert exprType != null : "expression must be of declared type Iterator<>";

        TypeElement exprElement = (TypeElement)exprType.asElement();

        // Find the next() method of the iterator type
        Symbol.MethodSymbol nextMethod = null;

        for (ExecutableElement method :
                 ElementFilter.methodsIn(elements.getAllMembers(exprElement))) {
            Name methodName = method.getSimpleName();

            if (method.getParameters().size() == 0) {
                if (methodName.contentEquals("next")) {
                    nextMethod = (Symbol.MethodSymbol)method;
                }
            }
        }

        assert nextMethod != null : "no next method declared for expression type";

        Type.MethodType methodType = (Type.MethodType)nextMethod.asType();
        Symbol.TypeSymbol methodClass = methodType.asElement();
        Type elementType;

        if (exprType.getTypeArguments().size() > 0) {
            elementType = (Type)exprType.getTypeArguments().get(0);
        } else {
            elementType = symtab.objectType;
        }

        // Replace the next method's generic return type with
        // the actual element type of the expression.
        Type.MethodType updatedMethodType =
            new Type.MethodType(com.sun.tools.javac.util.List.<Type>nil(),
                                elementType,
                                com.sun.tools.javac.util.List.<Type>nil(),
                                methodClass);

        JCTree.JCFieldAccess nextAccess =
            (JCTree.JCFieldAccess)
            maker.Select((JCTree.JCExpression)iteratorExpr,
                         nextMethod);
        nextAccess.setType(updatedMethodType);

        return nextAccess;
    }

    /**
     * Builds an AST Tree to dereference the length field of an array
     *
     * @param expression  the array expression whose length is being accessed
     * @return  a MemberSelectTree to dereference the length of the array
     */
    public MemberSelectTree buildArrayLengthAccess(ExpressionTree expression) {

        return (JCTree.JCFieldAccess)
            maker.Select((JCTree.JCExpression)expression, symtab.lengthVar);
    }

    /**
     * Builds an AST Tree to call a method designated by the argument expression.
     *
     * @param methodExpr  an expression denoting a method with no arguments
     * @return  a MethodInvocationTree to call the argument method
     */
    public MethodInvocationTree buildMethodInvocation(ExpressionTree methodExpr) {
        return maker.App((JCTree.JCExpression)methodExpr);
    }

    /**
     * Builds an AST Tree to call a method designated by methodExpr,
     * with one argument designated by argExpr.
     *
     * @param methodExpr  an expression denoting a method with one argument
     * @param argExpr  an expression denoting an argument to the method
     * @return  a MethodInvocationTree to call the argument method
     */
    public MethodInvocationTree buildMethodInvocation(ExpressionTree methodExpr,
            ExpressionTree argExpr) {
        return maker.App((JCTree.JCExpression)methodExpr,
                com.sun.tools.javac.util.List.of((JCTree.JCExpression)argExpr));
    }

    /**
     * Builds an AST Tree to call a method designed by methodExpr,
     * with two arguments designated by argExpr1 and argExpr2.
     *
     * @param methodExpr An expression denoting a method with two arguments.
     * @param argExpr1 An expression denoting the first argument to the method.
     * @param argExpr2 An expression denoting the second argument to the method.
     * @return A MethodInvocationTree to call the method with its two arguments.
     */
    public MethodInvocationTree buildMethodInvocation(ExpressionTree methodExpr,
						      ExpressionTree argExpr1,
						      ExpressionTree argExpr2) {
	return maker.App((JCTree.JCExpression) methodExpr,
	        com.sun.tools.javac.util.List.of((JCTree.JCExpression) argExpr1,
						 (JCTree.JCExpression) argExpr2));
    }

    /**
     * Builds an AST Tree to call a method designated by methodExpr,
     * with a list of arguments designated by argExprList.
     *
     * @param methodExpr An expression denoting a method with multiple arguments.
     * @param argExprList A list of expressions representing the arguments to the
     *                    method.
     * @return a MethodInvocationTree to call the method with its arguments.
     */
    public MethodInvocationTree buildMethodInvocation(ExpressionTree methodExpr,
						      List<ExpressionTree> argExprList) {
	com.sun.tools.javac.util.List<JCTree.JCExpression> convertedArgs =
	    com.sun.tools.javac.util.List.<JCTree.JCExpression>nil();
	Collections.reverse(argExprList);
	for (ExpressionTree argExpr : argExprList) {
	    convertedArgs = convertedArgs.prepend((JCTree.JCExpression) argExpr);
	}

	return maker.App((JCTree.JCExpression) methodExpr, convertedArgs);
    }

    /**
     * Builds an AST Tree to declare and initialize a variable, with no modifiers.
     *
     * @param type  the type of the variable
     * @param name  the name of the variable
     * @param owner  the element containing the new symbol
     * @param initializer  the initializer expression
     * @return  a VariableDeclTree declaring the new variable
     */
    public VariableTree buildVariableDecl(TypeMirror type,
                                          String name,
                                          Element owner,
                                          ExpressionTree initializer) {
        DetachedVarSymbol sym =
            new DetachedVarSymbol(0, names.fromString(name),
                                  (Type)type, (Symbol)owner);
        VariableTree tree = maker.VarDef(sym, (JCTree.JCExpression)initializer);
        sym.setDeclaration(tree);
        return tree;
    }

    /**
     * Builds an AST Tree to declare and initialize a variable.  The
     * type of the variable is specified by a Tree.
     *
     * @param type  the type of the variable, as a Tree
     * @param name  the name of the variable
     * @param owner  the element containing the new symbol
     * @param initializer  the initializer expression
     * @return  a VariableDeclTree declaring the new variable
     */
    public VariableTree buildVariableDecl(Tree type,
                                          String name,
                                          Element owner,
                                          ExpressionTree initializer) {
        Type typeMirror = (Type)InternalUtils.typeOf(type);
        DetachedVarSymbol sym =
            new DetachedVarSymbol(0, names.fromString(name),
                                  typeMirror, (Symbol)owner);
        JCTree.JCModifiers mods = maker.Modifiers(0);
        JCTree.JCVariableDecl decl = maker.VarDef(mods, sym.name,
                                                  (JCTree.JCExpression)type,
                                                  (JCTree.JCExpression)initializer);
        decl.setType(typeMirror);
        decl.sym = sym;
        sym.setDeclaration(decl);
        return decl;
    }

    /**
     * Builds an AST Tree to refer to a variable.
     *
     * @param decl  the declaration of the variable
     * @return  an IdentifierTree to refer to the variable
     */
    public IdentifierTree buildVariableUse(VariableTree decl) {
        return (IdentifierTree)maker.Ident((JCTree.JCVariableDecl)decl);
    }

    /**
     * Builds an AST Tree to cast the type of an expression.
     *
     * @param type  the type to cast to
     * @param expr  the expression to be cast
     * @return  a cast of the expression to the type
     */
    public TypeCastTree buildTypeCast(TypeMirror type,
                                      ExpressionTree expr) {
        return maker.TypeCast((Type)type, (JCTree.JCExpression)expr);
    }

    /**
     * Builds an AST Tree to assign an expression to a variable.
     *
     * @param variable  the declaration of the variable to assign to
     * @param expr      the expression to be assigned
     * @return  a statement assigning the expression to the variable
     */
    public StatementTree buildAssignment(VariableTree variable,
                                         ExpressionTree expr) {
        return maker.Assignment(TreeInfo.symbolFor((JCTree)variable),
                                (JCTree.JCExpression)expr);
    }

    /**
     * Builds an AST Tree to assign an RHS expression to an LHS expression.
     *
     * @param lhs  the expression to be assigned to
     * @param rhs  the expression to be assigned
     * @return  a statement assigning the expression to the variable
     */
    public AssignmentTree buildAssignment(ExpressionTree lhs,
                                          ExpressionTree rhs) {
        JCTree.JCAssign assign =
            maker.Assign((JCTree.JCExpression)lhs, (JCTree.JCExpression)rhs);
        assign.setType((Type)InternalUtils.typeOf(lhs));
        return assign;
    }

    /**
     * Builds an AST Tree representing a literal value of primitive
     * or String type.
     */
    public LiteralTree buildLiteral(Object value) {
        return maker.Literal(value);
    }

    /**
     * Builds an AST Tree to compare two operands with less than.
     *
     * @param left  the left operand tree
     * @param right  the right operand tree
     * @return  a Tree representing "left &lt; right"
     */
    public BinaryTree buildLessThan(ExpressionTree left, ExpressionTree right) {
        JCTree.JCBinary binary =
            maker.Binary(JCTree.Tag.LT, (JCTree.JCExpression)left,
                         (JCTree.JCExpression)right);
        binary.setType((Type)modelTypes.getPrimitiveType(TypeKind.BOOLEAN));
        return binary;
    }

    /**
     * Builds an AST Tree to dereference an array.
     *
     * @param array  the array to dereference
     * @param index  the index at which to dereference
     * @return  a Tree representing the dereference
     */
    public ArrayAccessTree buildArrayAccess(ExpressionTree array,
                                            ExpressionTree index) {
        ArrayType arrayType = (ArrayType)InternalUtils.typeOf(array);
        JCTree.JCArrayAccess access =
            maker.Indexed((JCTree.JCExpression)array, (JCTree.JCExpression)index);
        access.setType((Type)arrayType.getComponentType());
        return access;
    }

    /**
     * Builds an AST Tree to refer to a class name.
     *
     * @param elt  an element representing the class
     * @return  an IdentifierTree referring to the class
     */
    public IdentifierTree buildClassUse(Element elt) {
        return maker.Ident((Symbol)elt);
    }

    /**
     * 

    /**
     * Builds an AST Tree to access the valueOf() method of boxed type
     * such as Short or Float.
     *
     * @param expr  an expression whose type is a boxed type
     * @return  a MemberSelectTree that accesses the valueOf() method of
     *    the expression
     */
    public MemberSelectTree buildValueOfMethodAccess(Tree expr) {
        TypeMirror boxedType = InternalUtils.typeOf(expr);

        assert TypesUtils.isBoxedPrimitive(boxedType);

        // Find the valueOf(unboxedType) method of the boxed type
        Symbol.MethodSymbol valueOfMethod = getValueOfMethod(env, boxedType);

        Type.MethodType methodType = (Type.MethodType)valueOfMethod.asType();

        JCTree.JCFieldAccess valueOfAccess =
            (JCTree.JCFieldAccess)
            maker.Select((JCTree.JCExpression)expr, valueOfMethod);
        valueOfAccess.setType(methodType);

        return valueOfAccess;
    }

    /**
     * Returns the valueOf method of a boxed type such as Short or Float.
     */
    public static Symbol.MethodSymbol getValueOfMethod(ProcessingEnvironment env, TypeMirror boxedType) {
        Symbol.MethodSymbol valueOfMethod = null;

        TypeMirror unboxedType = env.getTypeUtils().unboxedType(boxedType);
        TypeElement boxedElement = (TypeElement)((DeclaredType)boxedType).asElement();
        for (ExecutableElement method :
                 ElementFilter.methodsIn(env.getElementUtils().getAllMembers(boxedElement))) {
            Name methodName = method.getSimpleName();

            if (methodName.contentEquals("valueOf")) {
                List<? extends VariableElement> params = method.getParameters();
                if (params.size() == 1 && env.getTypeUtils().isSameType(params.get(0).asType(), unboxedType)) {
                    valueOfMethod = (Symbol.MethodSymbol)method;
                }
            }
        }

        assert valueOfMethod != null : "no valueOf method declared for boxed type";
        return valueOfMethod;
    }

    /**
     * Gets a ClassSymbol element representing a given a class.  This class must be
     * both in the current execution classpath as well as the compilation classpath.
     * 
     * @param clazz The class you wish to get a reference too.
     * @param procEnv The ProcessingEnvironment in which this lookup will be compiled.
     * @return Element The element representing the symbol of this class.
     */
    public Element getClassSymbolElement(Class<?> clazz, ProcessingEnvironment procEnv) {
	JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) procEnv;
	JavacElements javacElem = (JavacElements) procEnv.getElementUtils();
	return javacElem.getTypeElement(clazz.getName());
    }

    /**
     * Creates a member select tree to access a given method from the given tree.
     *
     * @param method The method to access from the given tree.
     * @param tree The tree on which to access the given method.
     * @return A MemberSelectTree representing accessing the given method on the
     *         given tree, or null if the method does not exist.
     */
    public MemberSelectTree buildMethodAccess(Method method, ExpressionTree tree) {
	TypeMirror treeType = InternalUtils.typeOf(tree);
	TypeElement treeElement = (TypeElement)((DeclaredType)treeType).asElement();

	String methodName = method.getName();
	for (ExecutableElement treeMethod :
		 ElementFilter.methodsIn(elements.getAllMembers(treeElement))) {
	    Name treeMethodName = treeMethod.getSimpleName();
	    if (treeMethodName.contentEquals(methodName)) {
		Symbol.MethodSymbol treeMethodSymbol = (Symbol.MethodSymbol) treeMethod;
		Type.MethodType treeMethodType = (Type.MethodType) treeMethodSymbol.asType();

		JCTree.JCFieldAccess treeMethodAccess =
		    (JCTree.JCFieldAccess) maker.Select((JCTree.JCExpression) tree,
							treeMethodSymbol);
		treeMethodAccess.setType(treeMethodType);
		return treeMethodAccess;
	    }
	}

	return null;
    }

    /**
     * Builds an AST Tree to access the *Value() method of a
     * boxed type such as Short or Float, where * is the corresponding
     * primitive type (i.e. shortValue or floatValue).
     *
     * @param expr  an expression whose type is a boxed type
     * @return  a MemberSelectTree that accesses the *Value() method of
     *    the expression
     */
    public MemberSelectTree buildPrimValueMethodAccess(Tree expr) {
        TypeMirror boxedType = InternalUtils.typeOf(expr);
        TypeElement boxedElement = (TypeElement)((DeclaredType)boxedType).asElement();

        assert TypesUtils.isBoxedPrimitive(boxedType);
        TypeMirror unboxedType = modelTypes.unboxedType(boxedType);

        // Find the *Value() method of the boxed type
        String primValueName = unboxedType.toString() + "Value";
        Symbol.MethodSymbol primValueMethod = null;

        for (ExecutableElement method :
                 ElementFilter.methodsIn(elements.getAllMembers(boxedElement))) {
            Name methodName = method.getSimpleName();

            if (methodName.contentEquals(primValueName) &&
                method.getParameters().size() == 0) {
                primValueMethod = (Symbol.MethodSymbol)method;
            }
        }

        assert primValueMethod != null : "no *Value method declared for boxed type";

        Type.MethodType methodType = (Type.MethodType)primValueMethod.asType();

        JCTree.JCFieldAccess primValueAccess =
            (JCTree.JCFieldAccess)
            maker.Select((JCTree.JCExpression)expr, primValueMethod);
        primValueAccess.setType(methodType);

        return primValueAccess;
    }

    /**
     * Builds an AST Tree to represent an If-else statement.
     *
     * @param cond An expression representing the If condition. 
     *             It should type to boolean.
     * @param ifPart A statement to execute if it "if" evaluates to true.
     * @param elsePart A statement to execute if the "if" evaluates to false.
     * @return an If Statement that is composed of the provided building blocks.
     */
    public IfTree buildIfStatement(ExpressionTree cond,
				   StatementTree ifPart,
				   StatementTree elsePart) {
	return maker.If((JCTree.JCExpression) cond,
			(JCTree.JCStatement) ifPart,
			(JCTree.JCStatement) elsePart);
    }

    /**
     * Map public AST Tree.Kinds to internal javac JCTree.Tags.
     */
    public JCTree.Tag kindToTag(Tree.Kind kind) {
        switch (kind) {
        case AND:
            return JCTree.Tag.BITAND;
        case AND_ASSIGNMENT:
            return JCTree.Tag.BITAND_ASG;
        case ANNOTATION:
            return JCTree.Tag.ANNOTATION;
        case ANNOTATION_TYPE:
            return JCTree.Tag.TYPE_ANNOTATION;
        case ARRAY_ACCESS:
            return JCTree.Tag.INDEXED;
        case ARRAY_TYPE:
            return JCTree.Tag.TYPEARRAY;
        case ASSERT:
            return JCTree.Tag.ASSERT;
        case ASSIGNMENT:
            return JCTree.Tag.ASSIGN;
        case BITWISE_COMPLEMENT:
            return JCTree.Tag.COMPL;
        case BLOCK:
            return JCTree.Tag.BLOCK;
        case BREAK:
            return JCTree.Tag.BREAK;
        case CASE:
            return JCTree.Tag.CASE;
        case CATCH:
            return JCTree.Tag.CATCH;
        case CLASS:
            return JCTree.Tag.CLASSDEF;
        case CONDITIONAL_AND:
            return JCTree.Tag.AND;
        case CONDITIONAL_EXPRESSION:
            return JCTree.Tag.CONDEXPR;
        case CONDITIONAL_OR:
            return JCTree.Tag.OR;
        case CONTINUE:
            return JCTree.Tag.CONTINUE;
        case DIVIDE:
            return JCTree.Tag.DIV;
        case DIVIDE_ASSIGNMENT:
            return JCTree.Tag.DIV_ASG;
        case DO_WHILE_LOOP:
            return JCTree.Tag.DOLOOP;
        case ENHANCED_FOR_LOOP:
            return JCTree.Tag.FOREACHLOOP;
        case EQUAL_TO:
            return JCTree.Tag.EQ;
        case EXPRESSION_STATEMENT:
            return JCTree.Tag.EXEC;
        case FOR_LOOP:
            return JCTree.Tag.FORLOOP;
        case GREATER_THAN:
            return JCTree.Tag.GT;
        case GREATER_THAN_EQUAL:
            return JCTree.Tag.GE;
        case IDENTIFIER:
            return JCTree.Tag.IDENT;
        case IF:
            return JCTree.Tag.IF;
        case IMPORT:
            return JCTree.Tag.IMPORT;
        case INSTANCE_OF:
            return JCTree.Tag.TYPETEST;
        case LABELED_STATEMENT:
            return JCTree.Tag.LABELLED;
        case LEFT_SHIFT:
            return JCTree.Tag.SL;
        case LEFT_SHIFT_ASSIGNMENT:
            return JCTree.Tag.SL_ASG;
        case LESS_THAN:
            return JCTree.Tag.LT;
        case LESS_THAN_EQUAL:
            return JCTree.Tag.LE;
        case LOGICAL_COMPLEMENT:
            return JCTree.Tag.NOT;
        case MEMBER_SELECT:
            return JCTree.Tag.SELECT;
        case METHOD:
            return JCTree.Tag.METHODDEF;
        case METHOD_INVOCATION:
            return JCTree.Tag.APPLY;
        case MINUS:
            return JCTree.Tag.MINUS;
        case MINUS_ASSIGNMENT:
            return JCTree.Tag.MINUS_ASG;
        case MODIFIERS:
            return JCTree.Tag.MODIFIERS;
        case MULTIPLY:
            return JCTree.Tag.MUL;
        case MULTIPLY_ASSIGNMENT:
            return JCTree.Tag.MUL_ASG;
        case NEW_ARRAY:
            return JCTree.Tag.NEWARRAY;
        case NEW_CLASS:
            return JCTree.Tag.NEWCLASS;
        case NOT_EQUAL_TO:
            return JCTree.Tag.NE;
        case OR:
            return JCTree.Tag.BITOR;
        case OR_ASSIGNMENT:
            return JCTree.Tag.BITOR_ASG;
        case PARENTHESIZED:
            return JCTree.Tag.PARENS;
        case PLUS:
            return JCTree.Tag.PLUS;
        case PLUS_ASSIGNMENT:
            return JCTree.Tag.PLUS_ASG;
        case POSTFIX_DECREMENT:
            return JCTree.Tag.POSTDEC;
        case POSTFIX_INCREMENT:
            return JCTree.Tag.POSTINC;
        case PREFIX_DECREMENT:
            return JCTree.Tag.PREDEC;
        case PREFIX_INCREMENT:
            return JCTree.Tag.PREINC;
        case REMAINDER:
            return JCTree.Tag.MOD;
        case REMAINDER_ASSIGNMENT:
            return JCTree.Tag.MOD_ASG;
        case RETURN:
            return JCTree.Tag.RETURN;
        case RIGHT_SHIFT:
            return JCTree.Tag.SR;
        case RIGHT_SHIFT_ASSIGNMENT:
            return JCTree.Tag.SR_ASG;
        case SWITCH:
            return JCTree.Tag.SWITCH;
        case SYNCHRONIZED:
            return JCTree.Tag.SYNCHRONIZED;
        case THROW:
            return JCTree.Tag.THROW;
        case TRY:
            return JCTree.Tag.TRY;
        case TYPE_CAST:
            return JCTree.Tag.TYPECAST;
        case TYPE_PARAMETER:
            return JCTree.Tag.TYPEPARAMETER;
        case UNARY_MINUS:
            return JCTree.Tag.NEG;
        case UNARY_PLUS:
            return JCTree.Tag.POS;
        case UNION_TYPE:
            return JCTree.Tag.TYPEUNION;
        case UNSIGNED_RIGHT_SHIFT:
            return JCTree.Tag.USR;
        case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
            return JCTree.Tag.USR_ASG;
        case VARIABLE:
            return JCTree.Tag.VARDEF;
        case WHILE_LOOP:
            return JCTree.Tag.WHILELOOP;
        case XOR:
            return JCTree.Tag.BITXOR;
        case XOR_ASSIGNMENT:
            return JCTree.Tag.BITXOR_ASG;
        default:
            return JCTree.Tag.NO_TAG;
        }
    }

    /**
     * Builds an AST Tree to perform a binary operation.
     *
     * @param type  result type of the operation
     * @param op    AST Tree operator
     * @param left  the left operand tree
     * @param right  the right operand tree
     * @return  a Tree representing "left &lt; right"
     */
    public BinaryTree buildBinary(TypeMirror type, Tree.Kind op, ExpressionTree left, ExpressionTree right) {
        JCTree.Tag jcOp = kindToTag(op);
        JCTree.JCBinary binary =
            maker.Binary(jcOp, (JCTree.JCExpression)left,
                         (JCTree.JCExpression)right);
        binary.setType((Type)type);
        return binary;
    }

}
