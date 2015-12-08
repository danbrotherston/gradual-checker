package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeCopier;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import org.checkerframework.javacutil.TreeUtils;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * @author danbrotherston
 *
 * This is a specialization of the replacing tree translator that takes a list of
 * runtime check locations (as values which need to tested) and replaces their
 * enclosing statements with runtime checks.
 */
public class RuntimeCheckInserterTreeTranslator extends GeneralTreeTranslator {

    /**
     * Builder to create runtime checks for the locations which need a check.
     */
    RuntimeCheckBuilder checkBuilder = null;
    
    /**
     * Locations which need runtime checks added.
     */
    Map<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>>
	replacementLocations = null;

    /**
     * Copier used for copying portions of the tree.
     */
    private final TreeCopier<Void> copier;

    /**
     * Fixer used to fix all copied trees non-local exit's targets.
     */
    private final NonLocalExitStatementFixerTreeTranslator fixer;

    /**
     * Stack keeping track of our position within the tree, allowing us to get
     * a node's parent node.
     */
    Stack<JCTree> treeStack = new Stack<JCTree>();

    /**
     * Nullness Runtime check dots expression.
     */
    private final String nullnessRuntimeCheck =
	"org.checkerframework.checker.gradualnullness.NullnessRuntimeCheck";

    /**
     * Map of trees to types that need to be checked at runtime.
     */
    Map<Tree, AnnotatedTypeMirror> replacementMap = null;

    /**
     * Map of statements to replace.
     */
    public Map<JCTree.JCStatement, JCTree.JCStatement> runtimeCheckMap =
	new HashMap<JCTree.JCStatement, JCTree.JCStatement>();

    /**
     * Map of expressions to replace.
     */
    Map<JCTree.JCExpression, JCTree.JCExpression> runtimeExpressionMap =
	new HashMap<JCTree.JCExpression, JCTree.JCExpression>();

    /**
     * Processing environment.
     */
    ProcessingEnvironment env = null;

    /**
     * Eraser to erase types from variable trees.
     * TODO: Explain
     */
    TypeEraserTreeTranslator eraser = null;

    class TypeEraserTreeTranslator extends GeneralTreeTranslator {

	/**
	 * Javac types instance.
	 */
	protected final Types types;

	TypeEraserTreeTranslator(GradualNullnessChecker c,
				 ProcessingEnvironment env,
				 TreePath p) {
	    super(c, env, p);
	    this.types = Types.instance(((JavacProcessingEnvironment)env).getContext());
	}

	private Type fixTypes(Type t) {
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
		Type outer = fixTypes(t.getEnclosingType());
		com.sun.tools.javac.util.List<Type> fixedTyArgs =
		    com.sun.tools.javac.util.List.<Type>nil();

		Type newType = new ClassType(outer, fixedTyArgs, t.tsym);
		return newType;
		
	    case ARRAY:
		return new ArrayType(fixTypes(types.elemtype(t)), t.tsym);
	    case ERROR:
		return t;
	    default:
		return t;
	    }
	}

	@Override
        public void visitTree(JCTree tree) {
	    if (!(tree instanceof JCTree.JCTypeApply)) {
		tree.setType(fixTypes(tree.type));
	    }
	    result = tree;
	}
    }

    /**
     * @constructor
     * @param c The checker invoking this particular translator.
     * @param env The annotation processing environment this is invoked from.
     * @param replacementLocations All the information needed to build the runtime checks.
     *        The location map is built as such: <TreePath, <Tree, TypeMirror>>
     * Where TreePath is the path within the tree to the value node to replace.
     *       Tree is the actual value node to replace.
     *       AnnotatedTypeMirror is the compile time type this value must conform to at runtime.
     * @param checkBuilder A properly configured RuntimeCheckBuilder to use to build the runtime
     *                     checks.
     */
    public RuntimeCheckInserterTreeTranslator(GradualNullnessChecker c,
					      ProcessingEnvironment env,
					      TreePath p,
					      Map<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>>
					      replacementLocations,
					      RuntimeCheckBuilder checkBuilder) {
	super(c, env, p);
	this.env = env;
	eraser = new TypeEraserTreeTranslator(c, env, p);
	copier = new TreeCopier<Void>(maker);
	fixer = new NonLocalExitStatementFixerTreeTranslator(c, env, p);
	this.checkBuilder = checkBuilder;
	this.replacementLocations = replacementLocations;
	this.replacementMap = new HashMap<Tree, AnnotatedTypeMirror>();
	for (Map.Entry<Tree, AnnotatedTypeMirror> entry : replacementLocations.values()) {
	    this.replacementMap.put(entry.getKey(), entry.getValue());
	}
    }

    public RuntimeCheckInserterTreeTranslator(GradualNullnessChecker c,
					      ProcessingEnvironment env,
					      Map<Tree, AnnotatedTypeMirror>
					      replacementLocations,
					      TreePath p,
					      RuntimeCheckBuilder checkBuilder) {
	super(c, env, p);
	this.env = env;
	eraser = new TypeEraserTreeTranslator(c, env, p);
	copier = new TreeCopier<Void>(maker);
	fixer = new NonLocalExitStatementFixerTreeTranslator(c, env, p);
	this.checkBuilder = checkBuilder;
	this.replacementMap = new HashMap<Tree, AnnotatedTypeMirror>();
	for (Map.Entry<Tree, AnnotatedTypeMirror> entry : replacementLocations.entrySet()) {
	    this.replacementMap.put(entry.getKey(), entry.getValue());
	}
    }


    @Override
    public void aboutToVisitTree(JCTree that) {
	treeStack.push(that);
    }

    @Override
    public void visitTree(JCTree that) {
	if (this.replacementMap.containsKey(that)) {
	    result = doCheck(that);
	} else if (this.runtimeCheckMap.containsKey(that)) {
	    JCTree.JCStatement statement = this.runtimeCheckMap.get(that);
	    //this.attribute((JCTree.JCStatement) that, statement);
	    System.err.println("Replacing: " + that + " with: " + statement);
	    result = statement;
	}
	treeStack.pop();
    }

    JCTree.JCStatement getParentStatement(JCTree that) {
	@SuppressWarnings("unchecked")
	Stack<JCTree> tempStack = (Stack<JCTree>) this.treeStack.clone();
	JCTree statement = tempStack.pop();
	while ((!(statement instanceof JCTree.JCStatement)) && statement != null) {
	    statement = tempStack.pop();
	}

	if (tempStack.peek() instanceof JCTree.JCForLoop) {
	    statement = tempStack.peek();
	}

	return (JCTree.JCStatement) statement;
    }

    JCTree.JCBlock getContainingBlock(JCTree statement) {
	@SuppressWarnings("unchecked")
	Stack<JCTree> tempStack = (Stack<JCTree>) this.treeStack.clone();
	JCTree iterator = tempStack.pop();
	while (iterator != statement) { iterator = tempStack.pop(); }
	return (JCTree.JCBlock) tempStack.peek();
    }

    JCTree doCheck(JCTree that) {
	// Get parent statement.
	JCTree.JCStatement parentStatement = getParentStatement(that);

	if (parentStatement instanceof JCTree.JCVariableDecl) {
	    return replaceVariableDecl(that, (JCTree.JCVariableDecl) parentStatement);
	} else {
	    return replaceOtherStatement(that, parentStatement);
	}
    }

    JCTree replaceVariableDecl(JCTree that, JCTree.JCVariableDecl parentStatement) {
	Element symbolOwner = getSymbolOwner();
	AnnotatedTypeMirror type = this.replacementMap.get(that);
	JCTree.JCBlock containingBlock = getContainingBlock(parentStatement);

	JCTree.JCVariableDecl newVarDecl =
	    this.checkBuilder.buildNewVariableDecl(parentStatement, symbolOwner);

	JCTree.JCStatement newAssignment =
	    this.checkBuilder.buildAssignmentStatement(parentStatement, newVarDecl);

	JCTree.JCExpression value = (JCTree.JCExpression) that;
	JCTree.JCVariableDecl variable =
	    this.checkBuilder.buildVariable(value.type, symbolOwner, copier.copy(value));
	variable.accept(this.eraser);
	
	// Build the variable use to replace the original statement.
	JCTree.JCExpression result = this.checkBuilder.buildVariableUse(variable);
	
	// Replace that value within the original statement with an instance of the variable.
	// See last comment in this method for more information.
	SingleReplacementTreeTranslator replacer =
	    new SingleReplacementTreeTranslator(this.checker, this.env, this.path,
						that, result);

	newAssignment.accept(replacer);

	// Copy the statement.
	JCTree.JCStatement newAssignmentCopy = copier.copy(newAssignment);

	// Access the nullness runtime check.
	JCTree.JCExpression checkMethod = dotsExp(this.nullnessRuntimeCheck + ".runtimeCheck");
	JCTree.JCExpression failureMethod = dotsExp(this.nullnessRuntimeCheck + ".runtimeFailure");

	// Build the replacement statement.
	JCTree.JCStatement runtimeCheckStatement =
	    this.checkBuilder.buildRuntimeCheck(newAssignmentCopy, newAssignmentCopy,
						variable, type,
						checkMethod, failureMethod);

	JCTree.JCBlock containingBlockCopy = copier.copy(containingBlock);
	fixer.fixTree(containingBlockCopy);

	// Iterate through the statements in the block.
	com.sun.tools.javac.util.List<JCStatement> stats = containingBlock.stats;
	com.sun.tools.javac.util.List<JCStatement> statsCopy = containingBlockCopy.stats;
	while (stats != null && stats.head != null) {
	    // When we get to the variable declaration, replace it with the
	    // the new empty declaration, followed by the check.
	    if (stats.head == parentStatement) {
		statsCopy.head = newVarDecl;

		com.sun.tools.javac.util.List<JCStatement> newStat =
		    com.sun.tools.javac.util.List.of(runtimeCheckStatement);

		// Splice linked lists.
		newStat.tail = statsCopy.tail;
		statsCopy.tail = newStat;
		break;
	    }

	    stats = stats.tail;
	    statsCopy = statsCopy.tail;
	}

	this.runtimeCheckMap.put(containingBlock, containingBlockCopy);

	// Now we're returning the original statement so that we put it back, so attribution
	// can succeed (because the variable is not yet inserted into the tree, otherwise we
	// would fail to generate the attribution environment due to cannot find symbol).
	// But since we've already copied the original statement tree, that change persists
	// in the replacement tree.
	return that;
    }

    JCTree replaceOtherStatement(JCTree that, JCTree.JCStatement parentStatement) {
	Element symbolOwner = getSymbolOwner();
	JCTree.JCExpression value = (JCTree.JCExpression) that;
	AnnotatedTypeMirror type = this.replacementMap.get(that);
	JCTree.JCVariableDecl variable =
	    this.checkBuilder.buildVariable(value.type, symbolOwner, copier.copy(value));
	variable.accept(eraser);

	// Build the variable use to replace the original statement.
	JCTree.JCExpression result = this.checkBuilder.buildVariableUse(variable);

	// Replace the value within the original statement with an instance of the variable.
	// See the last comment in this method for more information.
	SingleReplacementTreeTranslator replacer =
	    new SingleReplacementTreeTranslator(this.checker, this.env, this.path,
						that, result);
	// System.out.println("Statement Before: " + statement);
	parentStatement.accept(replacer);
	// System.out.println("Statement After: " + statement);

	// Copy the statement.
	JCTree.JCStatement parentStatementCopy = copier.copy(parentStatement);
	JCTree.JCStatement parentStatementCopy2 = copier.copy(parentStatement);
	fixer.fixTree(parentStatementCopy);
	fixer.fixTree(parentStatementCopy2);

	// Access the nullness runtime check.
	JCTree.JCExpression checkMethod = dotsExp(this.nullnessRuntimeCheck + ".runtimeCheck");
	JCTree.JCExpression failureMethod = dotsExp(this.nullnessRuntimeCheck + ".runtimeFailure");

	// Build the replacement statement.
	JCTree.JCStatement replacementStatement =
	    this.checkBuilder.buildRuntimeCheck(parentStatementCopy, parentStatementCopy2,
						variable, type,
						checkMethod, failureMethod);

	this.runtimeCheckMap.put(parentStatement, replacementStatement);

	//	System.err.println("replacing: " + that + " with: " + result);
	//	System.err.println("remembering to replace: " + parentStatement + " with: " +
	//		   replacementStatement);
	this.runtimeExpressionMap.put((JCTree.JCExpression) that, result);

	// Now we're returning the original statement so that we put it back, so attribution
	// can succeed (because the variable is not yet inserted into the tree, otherwise we
	// would fail to generate the attribution environment due to cannot find symbol).
	// But since we've already copied the original statement tree, that change persists
	// in the replacement tree.
	return that;
    }
    
    /**
     * Gets the owner of a given Tree node (using the stack), in order to create
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
    protected Element getSymbolOwner() {
	@SuppressWarnings("unchecked")
	Stack<JCTree> stack = (Stack<JCTree>) (this.treeStack.clone());

	JCTree enclosingElement = stack.pop();
	while (!(enclosingElement instanceof JCTree.JCMethodDecl ||
		 enclosingElement instanceof JCTree.JCClassDecl)) {
	    enclosingElement = stack.pop();
	}

	if (enclosingElement instanceof JCTree.JCMethodDecl) {
	    return TreeUtils.elementFromDeclaration((JCTree.JCMethodDecl)enclosingElement);
	} else if (enclosingElement instanceof JCTree.JCClassDecl) {
	    return TreeUtils.elementFromDeclaration((JCTree.JCClassDecl)enclosingElement);
	} else {
	    throw new RuntimeException();
	}
    }
}
