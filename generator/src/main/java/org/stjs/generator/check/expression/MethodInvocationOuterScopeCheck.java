package org.stjs.generator.check.expression;

import javacutils.TreeUtils;

import javax.lang.model.element.Element;

import org.stjs.generator.GenerationContext;
import org.stjs.generator.GeneratorConstants;
import org.stjs.generator.utils.JavaNodes;
import org.stjs.generator.visitor.TreePathScannerContributors;
import org.stjs.generator.visitor.VisitorContributor;
import org.stjs.generator.writer.expression.MethodInvocationWriter;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Scope;

public class MethodInvocationOuterScopeCheck implements VisitorContributor<MethodInvocationTree, Void, GenerationContext> {

	@Override
	public Void visit(TreePathScannerContributors<Void, GenerationContext> visitor, MethodInvocationTree tree, GenerationContext context,
			Void prev) {
		Element methodElement = TreeUtils.elementFromUse(tree);

		if (JavaNodes.isStatic(methodElement)) {
			// only instance methods
			return null;
		}

		String name = MethodInvocationWriter.buildMethodName(tree);

		if (GeneratorConstants.THIS.equals(name) || GeneratorConstants.SUPER.equals(name)) {
			// this and super call are ok
			return null;
		}

		if (!(tree.getMethodSelect() instanceof IdentifierTree)) {
			// check for Outer.this check
			return null;
		}
		Scope currentScope = context.getTrees().getScope(context.getCurrentPath());

		Element currentScopeClassElement = IdentifierAccessOuterScopeCheck.getEnclosingElementSkipAnonymousInitializer(currentScope
				.getEnclosingClass());
		Element methodOwnerElement = methodElement.getEnclosingElement();
		if (!context.getTypes().isSubtype(currentScopeClassElement.asType(), methodOwnerElement.asType())) {
			context.addError(tree, "In Javascript you cannot call methods or fields from the outer type. "
					+ "You should define a variable var that=this outside your function definition and call the methods on this object");
		}
		return null;
	}
}
