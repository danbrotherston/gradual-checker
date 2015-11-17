package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCSkip;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCSynchronized;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCAssert;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCArrayTypeTree;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeUnion;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import com.sun.tools.javac.tree.JCTree.TypeBoundKind;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCErroneous;
import com.sun.tools.javac.tree.JCTree.LetExpr;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * @author danbrotherston
 *
 * This tree translator converts the HelpfulTreeTranslator from a node specific translator
 * to a generic translator.  In the root JC visitor classes, all node types call the
 * visitTree method, as a base case.  However, in the JC translator classes, all node
 * specific visitor methods are overriden to do node specific translations.  Thus,
 * the root visitTree method is never called.
 *
 * This class reverses that.  It first calls the super method of every node specific
 * visitor, resulting in a depth first traversal, then calls visitTree on the given (that)
 * tree node.  Thus visitTree is now called for every node in descendent translators.
 */
public class GeneralTreeTranslator extends HelpfulTreeTranslator<GradualNullnessChecker> {
    public GeneralTreeTranslator(GradualNullnessChecker c,
				 ProcessingEnvironment env,
				 TreePath p) {

	super(c, env, p);
    }

    public void aboutToVisitTree(JCTree that) {
    }

    @Override
    public void visitTopLevel(JCCompilationUnit that) {
	aboutToVisitTree(that);
	super.visitTopLevel(that);
	visitTree(that);
    }

    @Override
    public void visitImport(JCImport that) {
	aboutToVisitTree(that);
	super.visitImport(that);
	visitTree(that);
    }

    @Override
    public void visitClassDef(JCClassDecl that) {
	aboutToVisitTree(that);
	super.visitClassDef(that);
	visitTree(that);
    }

    @Override
    public void visitMethodDef(JCMethodDecl that) {
	aboutToVisitTree(that);
	super.visitMethodDef(that);
	visitTree(that);
    }

    @Override
    public void visitVarDef(JCVariableDecl that) {
	aboutToVisitTree(that);
	super.visitVarDef(that);
	visitTree(that);
    }

    @Override
    public void visitSkip(JCSkip that) {
	aboutToVisitTree(that);
	super.visitSkip(that);
	visitTree(that);
    }

    @Override
    public void visitBlock(JCBlock that) {
	aboutToVisitTree(that);
	super.visitBlock(that);
	visitTree(that);
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop that) {
	aboutToVisitTree(that);
	super.visitDoLoop(that);
	visitTree(that);
    }

    @Override
    public void visitWhileLoop(JCWhileLoop that) {
	aboutToVisitTree(that);
	super.visitWhileLoop(that);
	visitTree(that);
    }

    @Override
    public void visitForLoop(JCForLoop that) {
	aboutToVisitTree(that);
	super.visitForLoop(that);
	visitTree(that);
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop that) {
	aboutToVisitTree(that);
	super.visitForeachLoop(that);
	visitTree(that);
    }

    @Override
    public void visitLabelled(JCLabeledStatement that) {
	aboutToVisitTree(that);
	super.visitLabelled(that);
	visitTree(that);
    }

    @Override
    public void visitSwitch(JCSwitch that) {
	aboutToVisitTree(that);
	super.visitSwitch(that);
	visitTree(that);
    }

    @Override
    public void visitCase(JCCase that) {
	aboutToVisitTree(that);
	super.visitCase(that);
	visitTree(that);
    }

    @Override
    public void visitSynchronized(JCSynchronized that) {
	aboutToVisitTree(that);
	super.visitSynchronized(that);
	visitTree(that);
    }

    @Override
    public void visitTry(JCTry that) {
	aboutToVisitTree(that);
	super.visitTry(that);
	visitTree(that);
    }

    @Override
    public void visitCatch(JCCatch that) {
	aboutToVisitTree(that);
	super.visitCatch(that);
	visitTree(that);
    }

    @Override
    public void visitConditional(JCConditional that) {
	aboutToVisitTree(that);
	super.visitConditional(that);
	visitTree(that);
    }

    @Override
    public void visitIf(JCIf that) {
	aboutToVisitTree(that);
	super.visitIf(that);
	visitTree(that);
    }

    @Override
    public void visitExec(JCExpressionStatement that) {
	aboutToVisitTree(that);
	super.visitExec(that);
	visitTree(that);
    }

    @Override
    public void visitBreak(JCBreak that) {
	aboutToVisitTree(that);
	super.visitBreak(that);
	visitTree(that);
    }

    @Override
    public void visitContinue(JCContinue that) {
	aboutToVisitTree(that);
	super.visitContinue(that);
	visitTree(that);
    }

    @Override
    public void visitReturn(JCReturn that) {
	aboutToVisitTree(that);
	super.visitReturn(that);
	visitTree(that);
    }

    @Override
    public void visitThrow(JCThrow that) {
	aboutToVisitTree(that);
	super.visitThrow(that);
	visitTree(that);
    }

    @Override
    public void visitAssert(JCAssert that) {
	aboutToVisitTree(that);
	super.visitAssert(that);
	visitTree(that);
    }

    @Override
    public void visitApply(JCMethodInvocation that) {
	aboutToVisitTree(that);
	super.visitApply(that);
	visitTree(that); 
    }

    @Override
    public void visitNewClass(JCNewClass that) {
	aboutToVisitTree(that);
	super.visitNewClass(that);
	visitTree(that); 
    }

    @Override
    public void visitNewArray(JCNewArray that) {
	aboutToVisitTree(that);
	super.visitNewArray(that);
	visitTree(that);
    }

    @Override
    public void visitParens(JCParens that) {
	aboutToVisitTree(that);
	super.visitParens(that);
	visitTree(that);
    }

    @Override
    public void visitAssign(JCAssign that) {
	aboutToVisitTree(that);
	super.visitAssign(that);
	visitTree(that);
    }

    @Override
    public void visitAssignop(JCAssignOp that) {
	aboutToVisitTree(that);
	super.visitAssignop(that);
	visitTree(that);
    }

    @Override
    public void visitUnary(JCUnary that) {
	aboutToVisitTree(that);
	super.visitUnary(that);
	visitTree(that);
    }

    @Override
    public void visitBinary(JCBinary that) {
	aboutToVisitTree(that);
	super.visitBinary(that);
	visitTree(that);
    }

    @Override
    public void visitTypeCast(JCTypeCast that) {
	aboutToVisitTree(that);
	super.visitTypeCast(that);
	visitTree(that);
    }

    @Override
    public void visitTypeTest(JCInstanceOf that) {
	aboutToVisitTree(that);
	super.visitTypeTest(that);
	visitTree(that);
    }

    @Override
    public void visitIndexed(JCArrayAccess that) {
	aboutToVisitTree(that);
	super.visitIndexed(that);
	visitTree(that);
    }

    @Override
    public void visitSelect(JCFieldAccess that) {
	aboutToVisitTree(that);
	super.visitSelect(that);
	visitTree(that);
    }

    @Override
    public void visitIdent(JCIdent that) {
	aboutToVisitTree(that);
	super.visitIdent(that);
	visitTree(that);
    }

    @Override
    public void visitLiteral(JCLiteral that) {
	aboutToVisitTree(that);
	super.visitLiteral(that);
	visitTree(that);
    }

    @Override
    public void visitTypeIdent(JCPrimitiveTypeTree that) {
	aboutToVisitTree(that);
	super.visitTypeIdent(that);
	visitTree(that);
    }

    @Override
    public void visitTypeArray(JCArrayTypeTree that) {
	aboutToVisitTree(that);
	super.visitTypeArray(that);
	visitTree(that);
    }

    @Override
    public void visitTypeApply(JCTypeApply that) {
	aboutToVisitTree(that);
	super.visitTypeApply(that);
	visitTree(that);
    }

    @Override
    public void visitTypeUnion(JCTypeUnion that) {
	aboutToVisitTree(that);
	super.visitTypeUnion(that);
	visitTree(that);
    }

    @Override
    public void visitTypeParameter(JCTypeParameter that) {
	aboutToVisitTree(that);
	super.visitTypeParameter(that);
	visitTree(that);
    }

    @Override
    public void visitWildcard(JCWildcard that) {
	aboutToVisitTree(that);
	super.visitWildcard(that);
	visitTree(that);
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind that) {
	aboutToVisitTree(that);
	super.visitTypeBoundKind(that);
	visitTree(that);
    }

    @Override
    public void visitAnnotation(JCAnnotation that) {
	aboutToVisitTree(that);
	super.visitAnnotation(that);
	visitTree(that);
    }

    @Override
    public void visitModifiers(JCModifiers that) {
	aboutToVisitTree(that);
	super.visitModifiers(that);
	visitTree(that);
    }

    @Override
    public void visitErroneous(JCErroneous that) {
	aboutToVisitTree(that);
	super.visitErroneous(that);
	visitTree(that);
    }

    @Override
    public void visitLetExpr(LetExpr that) {
	aboutToVisitTree(that);
	super.visitLetExpr(that);
	visitTree(that);
    }
}
