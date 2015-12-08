package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

import org.checkerframework.checker.nullness.AbstractNullnessFbcChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.qual.Dynamic;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.javacutil.ErrorReporter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A checker extending the Nullness checker which provides the dynamic
 * qualifier to allow gradual type checking.  This checker overrides
 * the normal defaults and uses dynamic any time untyped code is returning
 * a value.  It then performs runtime checks to ensure validity.
 */
@TypeQualifiers({ Dynamic.class })
@StubFiles("../nullness/astubs/gnu-getopt.astub")
public class GradualNullnessChecker extends AbstractNullnessFbcChecker {
    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
	return new GradualNullnessVisitor(this, true);
    }

    @Override
    public void init(ProcessingEnvironment env) {
	super.init(env);
	this.trees = Trees.instance(env);
    }

    private JCTree toCompilationUnit(Element elem) {
	return (JCCompilationUnit) (toTreePath(elem).getCompilationUnit());
    }

    private TreePath toTreePath(Element elem) {
	return trees == null ? null : trees.getPath(elem);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
			   RoundEnvironment environment) {
	super.process(annotations, environment);

	for (Element elem : environment.getRootElements()) {
	    // System.out.println("Processing Element: " + elem);
	    JCTree tree = toCompilationUnit(elem);
	    TreePath path = toTreePath(elem);

	    MethodRefactoringTreeTranslator methodTranslator =
		new MethodRefactoringTreeTranslator(this, getProcessingEnvironment(), path);

	    ConstructorRefactoringTranslator constructorTranslator =
		new ConstructorRefactoringTranslator(this, getProcessingEnvironment(), path);

	    // System.out.println("Tree Before: " + tree);
            methodTranslator.shouldReplaceStaticInitializer = true;
	    tree.accept(methodTranslator);
	    tree.accept(constructorTranslator);
	    //System.out.println("Tree after: " + tree);
        }

	return false;
    }

    @Override
    public void typeProcess(TypeElement element, TreePath path) {
	// First perform normal typechecking.
	//System.err.println("Starting processing Element: " + element);
	//System.err.println("Before typecheck tree: " +
	//		   ((JCTree)path.getCompilationUnit()).toString());
	super.typeProcess(element, path);
	// System.out.println("Processing Element: " + element);

	// Check for errors.
	
	if (element == null || path == null || this.visitor == null ||
	    this.errsOnLastExit > 0 && this.normalExit) {
	    return;
	}

	// Get the locations for runtime checks as determined in the first typecheck
	// pass.
	Map<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>> runtimeCheckLocations =
	    ((GradualNullnessVisitor) this.getVisitor()).getRuntimeCheckLocations();

        JCTree tree = (JCTree) path.getCompilationUnit();
	try {
	    // System.out.println("After typecheck tree: " + tree);
	    // Setup runtime check configuration.
	    Method runtimeCheck =
		(NullnessRuntimeCheck.class).getDeclaredMethod("runtimeCheck",
							       Object.class,
							       String.class);

	    Method runtimeCheckFailure =
		(NullnessRuntimeCheck.class).getDeclaredMethod("runtimeFailure",
							       Object.class,
							       String.class);

	    RuntimeCheckBuilder checkBuilder =
		new RuntimeCheckBuilder(this, NullnessRuntimeCheck.class, runtimeCheck,
	    				runtimeCheckFailure, getProcessingEnvironment());

	    SortedMap<Integer, Map<Tree, AnnotatedTypeMirror>> locations =
		new TreeMap<Integer, Map<Tree, AnnotatedTypeMirror>>(Collections.reverseOrder());
	    for (Map.Entry<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>> location :
		     runtimeCheckLocations.entrySet()) {
		Integer value = 0;
		TreePath depthCounter = location.getKey();
		while (depthCounter.getParentPath() != null) {
		    value++;
		    depthCounter = depthCounter.getParentPath();
		}

		Map<Tree, AnnotatedTypeMirror> map = locations.get(value);
		if (map == null) { map = new HashMap<Tree, AnnotatedTypeMirror>(); }

		map.put(location.getValue().getKey(),
			location.getValue().getValue());

		locations.put(value, map);
	    }

	    for (Map.Entry<Integer, Map<Tree, AnnotatedTypeMirror>> locationsAtDepth :
		     locations.entrySet()) {
		//System.err.println("Depth: " + locationsAtDepth.getKey());
		for (Map.Entry<Tree, AnnotatedTypeMirror> location :
			 locationsAtDepth.getValue().entrySet()) {

		    Map<Tree, AnnotatedTypeMirror> locationMap =
			new HashMap<Tree, AnnotatedTypeMirror>();
		    locationMap.put(location.getKey(), location.getValue());

		    RuntimeCheckInserterTreeTranslator checkInserter =
			new RuntimeCheckInserterTreeTranslator(this, getProcessingEnvironment(),
							       locationMap, path, checkBuilder);
		    tree.accept(checkInserter);

		    Map<JCTree, JCTree> unattributedTrees = new HashMap<JCTree, JCTree>();
		    for (Map.Entry<JCTree.JCStatement, JCTree.JCStatement> entry :
			     checkInserter.runtimeCheckMap.entrySet()) {
			unattributedTrees.put(entry.getValue(), entry.getKey());
		    }

		    AttributingTreeTranslator attributer =
			new AttributingTreeTranslator(this, getProcessingEnvironment(), path,
						      unattributedTrees);
		    tree.accept(attributer);
		}
	    }
	
	    // System.out.println("Tree 1: " + tree);
	    // Insert runtime checks.
	    //	    RuntimeCheckTreeTranslator replacer =
	    //	new RuntimeCheckTreeTranslator(this, getProcessingEnvironment(), path,
	    //				       runtimeCheckLocations, checkBuilder);

	    // System.out.println("Tree with new methods");
	    // System.out.println(tree);
	    //	    tree.accept(replacer);

	    // Now attribute the new trees.
	    //Map<JCTree, JCTree> unattributedTrees = replacer.getUnattributedTrees();

	    //AttributingTreeTranslator translator =
	    //new AttributingTreeTranslator(this, getProcessingEnvironment(), path,
	    //				      unattributedTrees);

	    // System.out.println("Tree with new methods");
	    // System.out.println(tree);
	    // tree.accept(translator);

	    //	    System.err.println("Modified tree");
	    //	    System.err.println(tree);

	    MethodRenamingTreeTranslator methodRenamer =
		new MethodRenamingTreeTranslator(this, getProcessingEnvironment(), path);

	    ConstructorInvocationRefactoringTranslator constructorRefactorer =
		new ConstructorInvocationRefactoringTranslator(this,
							       getProcessingEnvironment(),
							       path);

	    FillInTypePlaceholderTreeTranslator fillInTypePlaceholders =
		new FillInTypePlaceholderTreeTranslator(this,
							getProcessingEnvironment(),
							path);

	    tree.accept(methodRenamer);
	    tree.accept(constructorRefactorer);
	    tree.accept(fillInTypePlaceholders);
	    System.err.println("Final Tree:");
	    System.err.println(tree);

	} catch (NoSuchMethodException e) {
	    ErrorReporter.errorAbort("Invalid method configuration for runtime checks.");
	} catch (NullPointerException e) {
	    e.printStackTrace();
            ErrorReporter.errorAbort("NPE: " + e.getMessage());
	}
    }
}
