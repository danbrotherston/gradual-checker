package org.checkerframework.framework.gradual;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;

import java.util.Collections;

import javax.annotation.processing.ProcessingEnvironment;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * @author danbrotherston
 *
 * This is a very simple utility class built on top of the replacing tree translator.
 * It simply replaces one tree with another.
 */
public class SingleReplacementTreeTranslator<Checker extends BaseTypeChecker>
        extends ReplacingTreeTranslator<Checker> {
    public SingleReplacementTreeTranslator(Checker c,
					   ProcessingEnvironment env,
					   TreePath p,
					   JCTree oldTree,
					   JCTree newTree) {
	super(c, env, p, Collections.singletonMap(oldTree, newTree));
    }
}
