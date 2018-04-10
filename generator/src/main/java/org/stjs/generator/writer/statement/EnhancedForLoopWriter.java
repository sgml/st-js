package org.stjs.generator.writer.statement;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.stjs.generator.GenerationContext;
import org.stjs.generator.javac.InternalUtils;
import org.stjs.generator.javac.TypesUtils;
import org.stjs.generator.javascript.JavaScriptBuilder;
import org.stjs.generator.writer.WriterContributor;
import org.stjs.generator.writer.WriterVisitor;
import org.stjs.javascript.Array;
import org.stjs.javascript.Map;

import com.sun.source.tree.EnhancedForLoopTree;

import java.util.Collections;

/**
 * generates from
 *
 * <pre>
 * for (String x : list) {
 * }
 * </pre>
 *
 * <pre>
 * for(var x in list) {
 * }
 * </pre>
 *
 * Warning: the iteration is on indexes as in JavaScript, not on values as in Java!
 * @author acraciun
 */
public class EnhancedForLoopWriter<JS> implements WriterContributor<EnhancedForLoopTree, JS> {
	@Override
	public JS visit(WriterVisitor<JS> visitor, EnhancedForLoopTree tree, GenerationContext<JS> context) {
		// Java Statement
		//     for (String s : myCollection)
		// Scanned values
		//     iterator = (VariableDeclaration) --> "String s"
		//     iterated = (Name) --> "myCollection"
		JS iterator = visitor.scan(tree.getVariable(), context);
		JS iterated = visitor.scan(tree.getExpression(), context);
		JS body = visitor.scan(tree.getStatement(), context);

		TypeMirror iteratedType = InternalUtils.typeOf(tree.getExpression());
		if (TypesUtils.isDeclaredOfName(iteratedType, Array.class.getName())
				|| TypesUtils.isDeclaredOfName(iteratedType, Map.class.getName())) {
			return generateForEachInObject(tree, context, iterator, iterated, body);

		} else if (isErasuredClassAssignableFromType(Iterable.class, iteratedType, context)) {
			return generateForEachWithIterable(tree, context, iterated, body);
		} else if (TypeKind.ARRAY == iteratedType.getKind()) {
			return generateForWithIndex(tree, context, iterated, body);
		} else {
			return context.withPosition(tree, context.js().forInLoop(iterator, iterated, body));
		}
	}

	private boolean isErasuredClassAssignableFromType(Class clazz, TypeMirror iteratedType, GenerationContext<JS> context) {
		TypeMirror erasedClassToCheck = context.getTypes().erasure(TypesUtils.typeFromClass(context.getTypes(), context.getElements(), clazz));
		TypeMirror erasedIteratedType = context.getTypes().erasure(iteratedType);

		return context.getTypes().isAssignable(erasedIteratedType, erasedClassToCheck);
	}

	private JS generateForEachInObject(EnhancedForLoopTree tree, GenerationContext<JS> context, JS iterator, JS iterated, JS body) {
		return context.withPosition(tree, context.js().forOfLoop(iterator, iterated, body));
	}

	private JS generateForEachWithIterable(EnhancedForLoopTree tree, GenerationContext<JS> context, JS iterated, JS body) {
		JavaScriptBuilder<JS> js = context.js();

		// Java source code:
		// ---------------------------------------------
		//	 for (String oneOfTheString : myStringList) {
		//	   // do whatever you want with 'oneOfTheString'
		//	 }
		//
		// Translated Javascript:
		// ---------------------------------------------
		//   for (var iterator$oneOfTheString = myStringList.iterator(); iterator$oneOfTheString.hasNext(); ) {
		//     var oneOfTheString = iterator$oneOfTheString.next();
		//   }
		String initialForLoopVariableName = tree.getVariable().getName().toString();

		JS iteratorMethodCall = js.functionCall(
				js.property(iterated, "iterator"),
				Collections.<JS>emptyList());

		String newIteratorName = "iterator$" + initialForLoopVariableName;
		JS forLoopIterator = js.name(newIteratorName);
		JS init = js.variableDeclaration(false, newIteratorName, iteratorMethodCall, false);
		JS condition = js.functionCall(js.property(forLoopIterator, "hasNext"), Collections.<JS>emptyList());
		JS update = js.emptyExpression();

		JS iteratorNextStatement = js.variableDeclaration(true, initialForLoopVariableName,
			js.functionCall(js.property(forLoopIterator, "next"), Collections.<JS>emptyList()), false);
		JS newBody = js.addStatementBeginning(body, iteratorNextStatement);

		return context.withPosition(tree, context.js().forLoop(init, condition, update, newBody));
	}

	private JS generateForWithIndex(EnhancedForLoopTree tree, GenerationContext<JS> context, JS iterated, JS body) {
		JavaScriptBuilder<JS> js = context.js();

		// Java source code:
		// ---------------------------------------------
		//   String[] myStringArray = ...
		//	 for (String str : myStringArray) {
		//	   // do whatever you want with 'str'
		//	 }
		//
		// Translated Javascript:
		// ---------------------------------------------
		//   for (let str of myStringArray) {
		//	   // do whatever you want with 'str'
		//   }
		String initialForLoopVariableName = tree.getVariable().getName().toString();

		JS iterator = js.variableDeclaration(false, initialForLoopVariableName, null, false);
		return context.withPosition(tree, context.js().forOfLoop(iterator, iterated, body));
	}

}
