package org.checkerframework.checker.gradualnullness;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;

import org.checkerframework.checker.nullness.AbstractNullnessFbcChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.qual.Dynamic;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.TypeElement;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

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
    public void typeProcess(TypeElement element, TreePath path) {
	super.typeProcess(element, path);

	if (element == null || path == null || this.visitor == null) {
	    return;
	}

	Map<TreePath, Map.Entry<Tree, AnnotatedTypeMirror>> runtimeCheckLocations =
	    ((GradualNullnessVisitor) this.getVisitor()).getRuntimeCheckLocations();

        JCTree tree = (JCTree) path.getCompilationUnit();
	try {
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
	    
	    RuntimeCheckTreeTranslator replacer =
		new RuntimeCheckTreeTranslator(this, getProcessingEnvironment(), path,
					       runtimeCheckLocations, checkBuilder);

	    tree.accept(replacer);

	    Map<JCTree, JCTree> unattributedTrees = replacer.getUnattributedTrees();

	    AttributingTreeTranslator translator =
		new AttributingTreeTranslator(this, getProcessingEnvironment(), path,
					      unattributedTrees);

	    tree.accept(translator);
	} catch (NoSuchMethodException e) {
	    ErrorReporter.errorAbort("Invalid method configuration for runtime checks.");
	}
    }
}
