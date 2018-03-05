package ejm2.tools;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeLiteral;

/**
 * Class responsible for analyzing expressions conained in statements for Statement
 * dependencies and the conditionalOperatorCount attribute.
 * 
 * @author Pedro Coimbra
 *
 */
public class ExpressionAnalyzer {
	
	/**
	 * Analyzes an expression for dependencies. This is a recursive operation that analyzes
	 * the given expression and all expression contained inside it. 
	 * <br>
	 * Type, method and variable (field) bindings are all added to the same set 
	 * to be returned, so clients of this method need to distinguish between the three
	 * kinds of IBinding themselves. IMethodBinding for methods calls, IVariableBinding
	 * for field accesses and ITypeBinding for type dependencies.
	 * 
	 * @param e
	 * 		The expression to analyze
	 * @return All type, method and variable (field) bindings found in this expression.
	 */
	public static IBinding[] analyzeExpressionDependencies(Expression e){
		// Since all bindings implement the same IBinding interface, they can easily be stored in one set.
		// Clients of this method thus need to distinguish between the three kinds of IBinding
		HashSet<IBinding> dependencies = new HashSet<IBinding>();
		assert e != null : "Null expression";
		switch(e.getClass().getSimpleName()){
		case "Assignment":
			if(((Assignment)e).getLeftHandSide() != null)
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((Assignment)e).getLeftHandSide())));
			if(((Assignment)e).getRightHandSide() != null)
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((Assignment)e).getRightHandSide())));
			break;
		case "CastExpression":
			dependencies.add(((CastExpression)e).getType().resolveBinding());
			if(((CastExpression)e).getExpression() != null)
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((CastExpression)e).getExpression())));
			break;
		case "ClassInstanceCreation":
			for(Object argument: ((ClassInstanceCreation)e).arguments()){
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies((Expression)argument)));
			}
			dependencies.add(((ClassInstanceCreation)e).resolveConstructorBinding());
			dependencies.add(((ClassInstanceCreation)e).resolveTypeBinding());
			if(((ClassInstanceCreation)e).getExpression() != null)
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((ClassInstanceCreation)e).getExpression())));
			break;
		case "ConditionalExpression":
			if(((ConditionalExpression)e).getExpression() != null)
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((ConditionalExpression)e).getExpression())));
			if(((ConditionalExpression)e).getThenExpression() != null)
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((ConditionalExpression)e).getThenExpression())));
			if(((ConditionalExpression)e).getElseExpression() != null)
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((ConditionalExpression)e).getElseExpression())));
			break;
		case "FieldAccess":
			dependencies.add(((FieldAccess)e).resolveTypeBinding());
			dependencies.add(((FieldAccess)e).resolveFieldBinding());
			dependencies.add(((FieldAccess)e).resolveFieldBinding().getDeclaringClass());
			if(((FieldAccess)e).getExpression() != null)
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((FieldAccess)e).getExpression())));
			break;
		case "InfixExpression":
			if(((InfixExpression)e).getLeftOperand() != null)
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((InfixExpression)e).getLeftOperand())));
			if(((InfixExpression)e).getRightOperand() != null)
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((InfixExpression)e).getRightOperand())));
			
			for(Object o: ((InfixExpression)e).extendedOperands())
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((Expression)o))));

			break;
		case "InstanceofExpression":
			if(((InstanceofExpression)e).getLeftOperand() != null)
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((InstanceofExpression)e).getLeftOperand())));
			dependencies.add(((InstanceofExpression)e).getRightOperand().resolveBinding());
			break;
		case "MethodInvocation":
			IMethodBinding methodBinding = ((MethodInvocation)e).resolveMethodBinding();
			if(methodBinding != null){
				dependencies.add(methodBinding.getDeclaringClass());
				dependencies.add(methodBinding.getReturnType());
				dependencies.add(methodBinding);
			}
			for(Object o: ((MethodInvocation)e).arguments())
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies((Expression)o)));
			
			if(((MethodInvocation)e).getExpression() != null)
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((MethodInvocation)e).getExpression())));
			break;
		case "ParenthesizedExpression":
			if(((ParenthesizedExpression)e).getExpression() != null)
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((ParenthesizedExpression)e).getExpression())));
			break;
		case "PostfixExpression":
			if(((PostfixExpression)e).getOperand() != null)
				dependencies.addAll(Arrays.asList(analyzeExpressionDependencies(((PostfixExpression)e).getOperand())));
			break;
		case "SimpleName":
			dependencies.add(((SimpleName)e).resolveTypeBinding());
			break;
		case "QualifiedName":
			dependencies.add(((QualifiedName)e).resolveTypeBinding());
			break;
		case "TypeLiteral":
			dependencies.add(((TypeLiteral)e).getType().resolveBinding());
			break;
		default:
			break;
		}
		dependencies.remove(null);
		return dependencies.toArray(new IBinding[0]);
	}
	
	/**
	 * Analyzes the given expression for conditional expressions and conditional operators.
	 * This is a recursive method that analyzes the given expression and all expressions
	 * contained inside it.
	 * 
	 * @param e
	 * 			The expression to analyze
	 * @return The total number of conditional expressions and operators found
	 */
	public static int analyzeExpressionConditions(Expression e){
		int result = 0;
		switch(e.getClass().getSimpleName()){
		case "Assignment":
			if(((Assignment)e).getLeftHandSide() != null)
				result += analyzeExpressionConditions(((Assignment)e).getLeftHandSide());
			if(((Assignment)e).getRightHandSide() != null)
				result += analyzeExpressionConditions(((Assignment)e).getRightHandSide());
			break;
		case "CastExpression":
			if(((CastExpression)e).getExpression() != null)
				result += analyzeExpressionConditions(((CastExpression)e).getExpression());
			break;
		case "ClassInstanceCreation":
			for(Object argument: ((ClassInstanceCreation)e).arguments()){
				result += analyzeExpressionConditions((Expression)argument);
			}
			if(((ClassInstanceCreation)e).getExpression() != null)
				result += analyzeExpressionConditions(((ClassInstanceCreation)e).getExpression());
			break;
		case "ConditionalExpression":
			if(((ConditionalExpression)e).getExpression() != null)
				result += analyzeExpressionConditions(((ConditionalExpression)e).getExpression());
			if(((ConditionalExpression)e).getThenExpression() != null)
				result += analyzeExpressionConditions(((ConditionalExpression)e).getThenExpression());
			if(((ConditionalExpression)e).getElseExpression() != null)
				result += analyzeExpressionConditions(((ConditionalExpression)e).getElseExpression());
			result++;
			break;
		case "FieldAccess":
			if(((FieldAccess)e).getExpression() != null)
				result += analyzeExpressionConditions(((FieldAccess)e).getExpression());
			break;
		case "InfixExpression":
			if(((InfixExpression)e).getLeftOperand() != null)
				result += analyzeExpressionConditions(((InfixExpression)e).getLeftOperand());
			if(((InfixExpression)e).getRightOperand() != null)
				result += analyzeExpressionConditions(((InfixExpression)e).getRightOperand());
			
			for(Object o: ((InfixExpression)e).extendedOperands())
				result += analyzeExpressionConditions(((Expression)o));

			Operator op = ((InfixExpression)e).getOperator();
			if(op.equals(Operator.CONDITIONAL_AND) || op.equals(Operator.CONDITIONAL_OR))
				result += 1 + ((InfixExpression)e).extendedOperands().size();
			break;
		case "InstanceofExpression":
			if(((InstanceofExpression)e).getLeftOperand() != null)
				result += analyzeExpressionConditions(((InstanceofExpression)e).getLeftOperand());
			break;
		case "MethodInvocation":
			for(Object o: ((MethodInvocation)e).arguments())
				result += analyzeExpressionConditions(((Expression)o));

			if(((MethodInvocation)e).getExpression() != null)
				result += analyzeExpressionConditions(((MethodInvocation)e).getExpression());
			break;
		case "ParenthesizedExpression":
			if(((ParenthesizedExpression)e).getExpression() != null)
				result += analyzeExpressionConditions(((ParenthesizedExpression)e).getExpression());
			break;
		case "PostfixExpression":
			if(((PostfixExpression)e).getOperand() != null)
				result += analyzeExpressionConditions(((PostfixExpression)e).getOperand());
			break;
		default:
			break;
		}
		return result;
	}
}
