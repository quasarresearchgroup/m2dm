package ejm2.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.quasar.juse.api.JUSE_ProgramingFacade;
import org.tzi.use.uml.mm.MAssociation;
import org.tzi.use.uml.ocl.type.ObjectType;
import org.tzi.use.uml.ocl.value.BooleanValue;
import org.tzi.use.uml.ocl.value.IntegerValue;
import org.tzi.use.uml.ocl.value.ObjectValue;
import org.tzi.use.uml.ocl.value.SetValue;
import org.tzi.use.uml.ocl.value.StringValue;
import org.tzi.use.uml.ocl.value.Value;
import org.tzi.use.uml.sys.MLink;
import org.tzi.use.uml.sys.MObject;
import org.tzi.use.uml.sys.MSystemException;

/**
 * Class responsible for processing the statements of a method or initializer.
 * Creates instances of the Statement metaclass of the EJMM.
 * 
 * @author Pedro Coimbra
 *
 */
public class StatementInspector {
	
	private static final String CATCH_CLAUSE_NAME = "CatchClause";
	private JUSE_ProgramingFacade api;
	
	// The Method or Initializer MObject that is currently being analyzed
	private MObject statementLocation;
	
	// Boolean determining if the statementLocation corresponds to a Method. 
	// If false, it is expected to be an initializer instead.
	private boolean isMethod;
	
	public StatementInspector(JUSE_ProgramingFacade api, MObject statementLocation, boolean isMethod) {
		this.api = api;
		this.statementLocation = statementLocation;
		this.isMethod = isMethod;
	}
	
	/**
	 * Creates a new Statement meta-object.
	 * 
	 * @param node
	 * 			The statement AST node to instantiate
	 * @return The newly-created Statement MObject
	 */
	public MObject createStatementObject(ASTNode node){
		String className = "";
		try{
			className = node.getClass().getSimpleName();
		}
		catch(NullPointerException e){
			node.toString();
			e.printStackTrace();
		}
		int allInstancesCount = api.allInstances(className).size()+1;

		MObject statementMObject = api.createObject(className+allInstancesCount, className);
		// api.setObjectAttribute(statementMObject, api.attributeByName(statementMObject, "contents"), new StringValue(node.toString()));
		api.setObjectAttribute(statementMObject, api.attributeByName(statementMObject, "startPosition"), IntegerValue.valueOf(node.getStartPosition()));
		api.setObjectAttribute(statementMObject, api.attributeByName(statementMObject, "length"), IntegerValue.valueOf(node.getLength()));
		return statementMObject;
	}
	
	/**
	 * Base method for statement inspection. Specific Statement subclasses have 
	 * their corresponding method overloading this one.
	 * <br>
	 * Note that the meta-object corresponding to the statement to inspect must be
	 * created <b>before</b> it is inspected and passed on as a parameter.
	 * 
	 * @param node
	 * 			The statement to inspect
	 * @param nodeMObject
	 * 			The statement's corresponding MObject
	 */
	public void inspectStatement(Statement node, MObject nodeMObject){
		switch(node.getClass().getSimpleName()){
		case"AssertStatement":
			inspectStatement((AssertStatement)node, nodeMObject);
			break;
		case"Block":
			inspectStatement((Block)node, nodeMObject);
			break;
		case"BreakStatement":
			inspectStatement((BreakStatement)node, nodeMObject);
			break;
		case"ConstructorInvocation":
			inspectStatement((ConstructorInvocation)node, nodeMObject);
			break;
		case"ContinueStatement":
			inspectStatement((ContinueStatement)node, nodeMObject);
			break;
		case"DoStatement":
			inspectStatement((DoStatement)node, nodeMObject);
			break;
		case"EnhancedForStatement":
			inspectStatement((EnhancedForStatement)node, nodeMObject);
			break;
		case"ExpressionStatement":
			inspectStatement((ExpressionStatement)node, nodeMObject);
			break;
		case"ForStatement":
			inspectStatement((ForStatement)node, nodeMObject);
			break;
		case"IfStatement":
			inspectStatement((IfStatement)node, nodeMObject);
			break;
		case"LabeledStatement":
			inspectStatement((LabeledStatement)node, nodeMObject);
			break;
		case"ReturnStatement":
			inspectStatement((ReturnStatement)node, nodeMObject);
			break;
		case"SuperConstructorInvocation":
			inspectStatement((SuperConstructorInvocation)node, nodeMObject);
			break;
		case"SwitchCase":
			inspectStatement((SwitchCase)node, nodeMObject);
			break;
		case"SwitchStatement":
			inspectStatement((SwitchStatement)node, nodeMObject);
			break;
		case"SynchronizedStatement":
			inspectStatement((SynchronizedStatement)node, nodeMObject);
			break;
		case"ThrowStatement":
			inspectStatement((ThrowStatement)node, nodeMObject);
			break;
		case"TryStatement":
			inspectStatement((TryStatement)node, nodeMObject);
			break;
		case"TypeDeclarationStatement":
			inspectStatement((TypeDeclarationStatement)node, nodeMObject);
			break;
		case"VariableDeclarationStatement":
			inspectStatement((VariableDeclarationStatement)node, nodeMObject);
			break;
		case"WhileStatement":
			inspectStatement((WhileStatement)node, nodeMObject);
			break;
		default:
			break;
		}
	}
	
	public void inspectStatement(AssertStatement node, MObject nodeMObject) {
		Expression expression = node.getExpression();
		int conditionalOperatorCount = 0;
		if(expression != null){
			analyzeStatementExpression(nodeMObject, expression);
			conditionalOperatorCount = ExpressionAnalyzer.analyzeExpressionConditions(expression);
		}
		
		api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "conditionalOperatorCount"), IntegerValue.valueOf(conditionalOperatorCount));
		
	}
	
	public void inspectStatement(Block node, MObject nodeMObject) {
		for(Object statement: node.statements()){
			MObject statementMObject = createStatementObject((Statement) statement);
			api.createLink(api.associationByName("A_Block_Statement"), Arrays.asList(nodeMObject, statementMObject));
			inspectStatement((Statement)statement, statementMObject);
		}
	}

	public void inspectStatement(BreakStatement node, MObject nodeMObject) {
		if(node.getLabel() != null)
			api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "optionalLabel"), new StringValue(node.getLabel().toString()));
	}
	

	public void inspectStatement(ConstructorInvocation node, MObject nodeMObject) {
		HashSet<IBinding> dependencies = new HashSet<IBinding>(); 
		int conditionalOperatorCount = 0;
		
		dependencies.add(node.resolveConstructorBinding().getDeclaringClass());
		dependencies.add(node.resolveConstructorBinding());
		
		for(Object argument: node.arguments()){
			dependencies.addAll(Arrays.asList(ExpressionAnalyzer.analyzeExpressionDependencies((Expression) argument)));
			conditionalOperatorCount += ExpressionAnalyzer.analyzeExpressionConditions((Expression) argument);
		}
		
		api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "conditionalOperatorCount"), IntegerValue.valueOf(conditionalOperatorCount));
		
		for(Object ta: node.typeArguments()){
			dependencies.add(((Type)ta).resolveBinding());
		}
		for(IBinding binding: dependencies){
			if(binding instanceof ITypeBinding){
				createTypeDependencyAssociation(nodeMObject, retrieveTypeObject((ITypeBinding)binding));
				for(ITypeBinding argumentBinding: ((ITypeBinding)binding).getTypeArguments())
					createTypeDependencyAssociation(nodeMObject, retrieveTypeObject(argumentBinding));
			}else
				if(binding instanceof IVariableBinding)
					createFieldDependencyAssociation(nodeMObject, retrieveFieldObject((IVariableBinding)binding));
				else
					if(binding instanceof IMethodBinding)
						createMethodDependencyAssociation(nodeMObject, retrieveMethodObject((IMethodBinding)binding));
					else
						System.out.println("Wrong binding");
		}
	}
	
	

	public void inspectStatement(ContinueStatement node, MObject nodeMObject) {
		if(node.getLabel() != null)
			api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "optionalLabel"), new StringValue(node.getLabel().toString()));
	}
	
	public void inspectStatement(DoStatement node, MObject nodeMObject) {
		Statement body = node.getBody();
		MObject bodyMObject = createStatementObject(body);
		api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "body"), new ObjectValue(new ObjectType(api.classByName(body.getClass().getSimpleName())),bodyMObject));
		inspectStatement(body, bodyMObject);
	}

	public void inspectStatement(EnhancedForStatement node, MObject nodeMObject) {
		Statement body = node.getBody();
		MObject bodyMObject = createStatementObject(body);
		api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "body"), new ObjectValue(new ObjectType(api.classByName(body.getClass().getSimpleName())),bodyMObject));
		//TODO associate variable?
		IVariableBinding parameterBinding = node.getParameter().resolveBinding();
		
		if(parameterBinding.getJavaElement() != null) //TODO do something when it IS null
			createLocalVariableObject(parameterBinding.getJavaElement(), parameterBinding.getType());
		
		analyzeStatementExpression(nodeMObject, node.getExpression());
		
		inspectStatement(body, bodyMObject);
	}

	public void inspectStatement(ExpressionStatement node, MObject nodeMObject) {
		analyzeStatementExpression(nodeMObject, node.getExpression());
	}

	public void inspectStatement(ForStatement node, MObject nodeMObject) {
		Statement body = node.getBody();
		MObject bodyMObject = createStatementObject(body);
		api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "body"), new ObjectValue(new ObjectType(api.classByName(body.getClass().getSimpleName())),bodyMObject));
		HashSet<IBinding> dependencies = new HashSet<IBinding>();
		
		Expression expression = node.getExpression();
		int conditionalOperatorCount = 0;

		if(expression != null){
			dependencies.addAll(Arrays.asList(ExpressionAnalyzer.analyzeExpressionDependencies(expression)));
			conditionalOperatorCount += ExpressionAnalyzer.analyzeExpressionConditions(expression);
		}
		
		for(Object initializer: node.initializers()){
			dependencies.addAll(Arrays.asList(ExpressionAnalyzer.analyzeExpressionDependencies((Expression) initializer)));
			conditionalOperatorCount += ExpressionAnalyzer.analyzeExpressionConditions((Expression) initializer);
		}
		for(Object updater: node.updaters()){
			dependencies.addAll(Arrays.asList(ExpressionAnalyzer.analyzeExpressionDependencies((Expression) updater)));
			conditionalOperatorCount += ExpressionAnalyzer.analyzeExpressionConditions((Expression) updater);
		}
		
		api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "conditionalOperatorCount"), IntegerValue.valueOf(conditionalOperatorCount));

		for(IBinding binding: dependencies){
			if(binding instanceof ITypeBinding){
				createTypeDependencyAssociation(nodeMObject, retrieveTypeObject((ITypeBinding)binding));
				for(ITypeBinding argumentBinding: ((ITypeBinding)binding).getTypeArguments())
					createTypeDependencyAssociation(nodeMObject, retrieveTypeObject(argumentBinding));
			}else
				if(binding instanceof IVariableBinding)
					createFieldDependencyAssociation(nodeMObject, retrieveFieldObject((IVariableBinding)binding));
				else
					if(binding instanceof IMethodBinding)
						createMethodDependencyAssociation(nodeMObject, retrieveMethodObject((IMethodBinding)binding));
					else
						System.out.println("Wrong binding");
		}
		

		inspectStatement(body, bodyMObject);
	}

	public void inspectStatement(IfStatement node, MObject nodeMObject) {
		analyzeStatementExpression(nodeMObject, node.getExpression());

		Statement thenStatement = node.getThenStatement();
		MObject thenMObject = createStatementObject(thenStatement);
		api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "thenStatement"), new ObjectValue(new ObjectType(api.classByName(thenStatement.getClass().getSimpleName())),thenMObject));
		inspectStatement(thenStatement, thenMObject);
		
		Statement elseStatement = node.getElseStatement();
		if(elseStatement != null){
			MObject elseMObject = createStatementObject(elseStatement);
			api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "optionalElseStatement"), new ObjectValue(new ObjectType(api.classByName(elseStatement.getClass().getSimpleName())),elseMObject));
			inspectStatement(elseStatement, elseMObject);
		}
	}
	
	public void inspectStatement(LabeledStatement node, MObject nodeMObject) {
		Statement body = node.getBody();
		MObject bodyMObject = createStatementObject(body);
		api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "body"), new ObjectValue(new ObjectType(api.classByName(body.getClass().getSimpleName())),bodyMObject));
		inspectStatement(body, bodyMObject);
	}


	public void inspectStatement(ReturnStatement node, MObject nodeMObject) {
		analyzeStatementExpression(nodeMObject, node.getExpression());
	}
	
	public void inspectStatement(SuperConstructorInvocation node, MObject nodeMObject) {
		analyzeStatementExpression(nodeMObject, node.getExpression());
		
		HashSet<IBinding> dependencies = new HashSet<IBinding>(); 
		int conditionalOperatorCount = 0;
		
		dependencies.add(node.resolveConstructorBinding().getDeclaringClass());
		dependencies.add(node.resolveConstructorBinding());
		
		for(Object argument: node.arguments()){
			dependencies.addAll(Arrays.asList(ExpressionAnalyzer.analyzeExpressionDependencies((Expression) argument)));
			conditionalOperatorCount += ExpressionAnalyzer.analyzeExpressionConditions((Expression) argument);
		}
		
		api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "conditionalOperatorCount"), IntegerValue.valueOf(conditionalOperatorCount));
		
		for(Object ta: node.typeArguments()){
			dependencies.add(((Type)ta).resolveBinding());
		}
		for(IBinding binding: dependencies){
			if(binding instanceof ITypeBinding){
				createTypeDependencyAssociation(nodeMObject, retrieveTypeObject((ITypeBinding)binding));
				for(ITypeBinding argumentBinding: ((ITypeBinding)binding).getTypeArguments())
					createTypeDependencyAssociation(nodeMObject, retrieveTypeObject(argumentBinding));
			}else
				if(binding instanceof IVariableBinding)
					createFieldDependencyAssociation(nodeMObject, retrieveFieldObject((IVariableBinding)binding));
				else
					if(binding instanceof IMethodBinding)
						createMethodDependencyAssociation(nodeMObject, retrieveMethodObject((IMethodBinding)binding));
					else
						System.out.println("Wrong binding");
			
		}
	}

	public void inspectStatement(SwitchCase node, MObject nodeMObject) {
		analyzeStatementExpression(nodeMObject, node.getExpression());

		if(node.isDefault())
			api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "isDefault"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "isDefault"), BooleanValue.FALSE);
	}

	public void inspectStatement(SwitchStatement node, MObject nodeMObject) {
		analyzeStatementExpression(nodeMObject, node.getExpression());

		for(Object statement: node.statements())
			inspectStatement((Statement)statement, createStatementObject((Statement)statement));
			
	}

	public void inspectStatement(SynchronizedStatement node, MObject nodeMObject) {
		Statement body = node.getBody();
		MObject bodyMObject = createStatementObject(body);
		api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "body"), new ObjectValue(new ObjectType(api.classByName(body.getClass().getSimpleName())),bodyMObject));
		
		analyzeStatementExpression(nodeMObject, node.getExpression());

		inspectStatement(body, bodyMObject);
	}

	public void inspectStatement(ThrowStatement node, MObject nodeMObject) {
		analyzeStatementExpression(nodeMObject, node.getExpression());
	}

	public void inspectStatement(TryStatement node, MObject nodeMObject) {
		Statement body = node.getBody();
		MObject bodyMObject = createStatementObject(body);
		api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "body"), new ObjectValue(new ObjectType(api.classByName(body.getClass().getSimpleName())),bodyMObject));
		inspectStatement(body, bodyMObject);
		
		Statement finallyStatement = node.getFinally();
		if(finallyStatement != null){
			MObject finallyMObject = createStatementObject(finallyStatement);
			api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "optionalFinallyBody"), new ObjectValue(new ObjectType(api.classByName(finallyStatement.getClass().getSimpleName())),finallyMObject));
			inspectStatement(finallyStatement, finallyMObject);
		}
				
		HashSet<Value> catchClauseSet = new HashSet<Value>();
		ObjectType catchClauseObjectType = new ObjectType(api.classByName(CATCH_CLAUSE_NAME));
		for(Object cc: node.catchClauses()){
			MObject catchClauseMObject = createCatchClauseObject((CatchClause)cc);
			catchClauseSet.add(new ObjectValue(catchClauseObjectType, catchClauseMObject));
			inspectCatchClause((CatchClause)cc, catchClauseMObject);
		}
		api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "catchClauses"), new SetValue(catchClauseObjectType,catchClauseSet));
	}

	/**
	 * Creates a new CatchClause meta-object from the given catch clause. This method
	 * is kept seperate from the createStatementObject because catch clauses are not
	 * considered statements by the AST.
	 * 
	 * @param cc
	 * 			The catch clause to instantiate
	 * @return The newly created CatchClause MObject
	 */
	private MObject createCatchClauseObject(CatchClause cc) {
		int allInstancesCount = api.allInstances(CATCH_CLAUSE_NAME).size()+1;
		MObject catchClauseMObject = api.createObject(CATCH_CLAUSE_NAME+allInstancesCount, CATCH_CLAUSE_NAME);
		// api.setObjectAttribute(catchClauseMObject, api.attributeByName(catchClauseMObject, "contents"), new StringValue(cc.toString()));
		api.setObjectAttribute(catchClauseMObject, api.attributeByName(catchClauseMObject, "startPosition"), IntegerValue.valueOf(cc.getStartPosition()));
		api.setObjectAttribute(catchClauseMObject, api.attributeByName(catchClauseMObject, "length"), IntegerValue.valueOf(cc.getLength()));
		return catchClauseMObject;
	}

	/**
	 * Inspects the given catch clause for statements. Works similarly to the inspectStatement
	 * method, but kept seperate because catch clauses are not considered statements
	 * by the AST.
	 * 
	 * @param node
	 * 			The catch clause AST node
	 * @param nodeMObject
	 * 			The CatchClause MObject
	 */
	public void inspectCatchClause(CatchClause node, MObject nodeMObject) {
		Statement body = node.getBody();
		MObject bodyMObject = createStatementObject(body);
		api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "body"), new ObjectValue(new ObjectType(api.classByName(body.getClass().getSimpleName())),bodyMObject));
		if(node.getException().resolveBinding() != null){
			MObject localVariableMObject = createLocalVariableObject(node.getException().resolveBinding().getJavaElement(), node.getException().resolveBinding().getType());
			api.createLink(api.associationByName("A_CatchClause_LocalVariable"), Arrays.asList(nodeMObject, localVariableMObject));
		}
		inspectStatement(body, bodyMObject);
	}

	public void inspectStatement(TypeDeclarationStatement node, MObject nodeMObject) {
		MObject typeMObject = api.objectByName(JM2Loader.processName(node.resolveBinding().getJavaElement().getHandleIdentifier()));
		if(typeMObject != null){
			api.createLink(api.associationByName("A_Statement_Type"), Arrays.asList(nodeMObject, typeMObject));
			api.createLink(api.associationByName("A_TypeDeclarationStatement_Type"), Arrays.asList(nodeMObject, typeMObject));
		}
	}

	public void inspectStatement(VariableDeclarationStatement node, MObject nodeMObject) {
		for(Object fragment: node.fragments()){
			MObject localVariableMObject = createLocalVariableObject(((VariableDeclarationFragment)fragment).resolveBinding().getJavaElement(), ((VariableDeclarationFragment)fragment).resolveBinding().getType());
			api.createLink(api.associationByName("A_VariableDeclarationStatement_LocalVariable"), Arrays.asList(nodeMObject, localVariableMObject));
			if(((VariableDeclarationFragment) fragment).getInitializer() != null)
				analyzeStatementExpression(nodeMObject, ((VariableDeclarationFragment)fragment).getInitializer());
		}
		MObject typeMObject = retrieveTypeObject(node.getType().resolveBinding());
		if(typeMObject != null)
			createLink(api.associationByName("A_Statement_Type"), Arrays.asList(nodeMObject, typeMObject));
	}

	public void inspectStatement(WhileStatement node, MObject nodeMObject) {
		Statement body = node.getBody();
		MObject bodyMObject = createStatementObject(body);
		api.setObjectAttribute(nodeMObject, api.attributeByName(nodeMObject, "body"), new ObjectValue(new ObjectType(api.classByName(body.getClass().getSimpleName())),bodyMObject));
		
		analyzeStatementExpression(nodeMObject, node.getExpression());

		inspectStatement(body, bodyMObject);
	}
	
	/**
	 * Creates a new LocalVariable meta-object for a local variable declared inside
	 * a method or initializer.
	 * 
	 * @param variableElement
	 * 			The IJavaElement corresponding to the local variable to instantiate
	 * @param typeBinding
	 * 			The type binding for the local variable
	 * @return The newly created LocalVariable MObject
	 */
	public MObject createLocalVariableObject(IJavaElement variableElement, ITypeBinding typeBinding){
		MObject localVariableMObject = api.createObject(JM2Loader.processName(variableElement.getHandleIdentifier()), "LocalVariable");
		api.setObjectAttribute(localVariableMObject, api.attributeByName(localVariableMObject, "name"), new StringValue(variableElement.getElementName()));
		api.setObjectAttribute(localVariableMObject, api.attributeByName(localVariableMObject, "handleIdentifier"), new StringValue(variableElement.getHandleIdentifier()));
		
		int arrayCount =  Signature.getArrayCount(((ILocalVariable)variableElement).getTypeSignature());
		api.setObjectAttribute(localVariableMObject, api.attributeByName(localVariableMObject, "arrayDimensions"), IntegerValue.valueOf(arrayCount));

		MObject typeMObject = retrieveTypeObject(typeBinding);
		if(typeMObject != null)
			api.createLink(api.associationByName("A_LocalVariable_Type"), Arrays.asList(localVariableMObject, typeMObject));
		
		
		if(this.isMethod)
			api.createLink(api.associationByName("A_Method_LocalVariable"), Arrays.asList(statementLocation, localVariableMObject));
		else
			if(statementLocation != null) //TODO statementLocation should never be null but happens with some initializers
				api.createLink(api.associationByName("A_Initializer_LocalVariable"), Arrays.asList(statementLocation, localVariableMObject));
		
		return localVariableMObject;
	}
	
	/**
	 * Returns a Type MObject corresponding to the given type binding.
	 * 
	 * @param typeBinding
	 * 			The type binding to search
	 * @return The corresponding Type MObject, null if it is a type parameter.
	 */
	private MObject retrieveTypeObject(ITypeBinding typeBinding){
		MObject object = null;
		if(typeBinding.isTypeVariable())
			return object;
		
		if(typeBinding.isWildcardType())
			if(typeBinding.getBound() == null)
				return object;
			else
				return retrieveTypeObject(typeBinding.getBound());
		
		IJavaElement element = typeBinding.getJavaElement();
		
		// Only the base type is retrieved, type arguments are ignored
		String simpleName = typeBinding.getName();
		simpleName = simpleName.split("<")[0];
		String qualifiedName = typeBinding.getQualifiedName();
		qualifiedName = qualifiedName.split("<")[0];
		
		// For anonymous types, their corresponding superclass is retrieved instead
		if(typeBinding.isAnonymous() || qualifiedName.isEmpty()){
			ITypeBinding superClassBinding = typeBinding.getSuperclass();
			if(superClassBinding != null)
				return retrieveTypeObject(superClassBinding);
			else
				return retrieveTypeObject(typeBinding.getInterfaces()[0]);
		}
			
		if(element != null)
			object = api.objectByName(JM2Loader.processName(element.getHandleIdentifier()));
		
		// Several different attempts by trying different combinations of names and sources
		if(object == null){
			if(simpleName != null && !simpleName.isEmpty()){
				object = api.objectByName(JM2Loader.processName(simpleName));
				if(object == null){
					HashMap<String, MObject> allTypeObjects = JM2Loader.getAllTypeObjects();
					object = allTypeObjects.get(simpleName);
					if(object == null){
						if(qualifiedName != null && !qualifiedName.isEmpty()){
							object = api.objectByName(JM2Loader.processName(qualifiedName));
							if(object == null){
								object = allTypeObjects.get(qualifiedName);
								if(object == null)
									object = JM2Loader.createExternalTypeFromName(qualifiedName);
							}
						}
					}
				}
			}
			
		}
		return object;
	}
	
	/**
	 * Creates a typeDependencies meta-link for the given meta-objects 
	 * (A_Statement_Type).
	 * 
	 * @param statementMObject
	 * 			The dependee Statement
	 * @param typeMObject
	 * 			The typeDependencies Type
	 */
	private void createTypeDependencyAssociation(MObject statementMObject, MObject typeMObject) {
		if(typeMObject != null)
			createLink(api.associationByName("A_Statement_Type"), Arrays.asList(statementMObject, typeMObject));
	}
	
	/**
	 * Substitute method for creating a meta-link. Differs from the J-USE createLink
	 * in that it does not print the stack when a MSystemException occurs, since it
	 * is expected in this class to create several meta-links between the same 
	 * meta-objects.
	 * 
	 * @param theAssociation
	 * 			The link to instantiate
	 * @param members
	 * 			The members of the association
	 * @return The new MLink
	 */
	public MLink createLink(MAssociation theAssociation, List<MObject> members)
	{
		MLink result = null;
		try
		{
			result = JM2Loader.getSystem().state().createLink(theAssociation, members, null);
		}
		catch (MSystemException e)
		{
		}
		return result;
	}
	
	/**
	 * Analyzes the given expression of a statement for its dependencies and 
	 * conditionalOperatorCount.
	 * 
	 * @param statementMObject
	 * 			The Statement MObject to which the expression belongs
	 * @param expression
	 * 			The expression to analyze
	 */
	private void analyzeStatementExpression(MObject statementMObject, Expression expression){
		int conditionalOperatorCount = 0;

		if(expression != null){
			for(IBinding binding: ExpressionAnalyzer.analyzeExpressionDependencies(expression)){
				if(binding instanceof ITypeBinding){
					createTypeDependencyAssociation(statementMObject, retrieveTypeObject((ITypeBinding)binding));
					for(ITypeBinding argumentBinding: ((ITypeBinding)binding).getTypeArguments())
						createTypeDependencyAssociation(statementMObject, retrieveTypeObject(argumentBinding));
				}else
					if(binding instanceof IVariableBinding)
						createFieldDependencyAssociation(statementMObject, retrieveFieldObject((IVariableBinding)binding));
					else
						if(binding instanceof IMethodBinding)
							createMethodDependencyAssociation(statementMObject, retrieveMethodObject((IMethodBinding)binding));
						else
							System.out.println("Wrong binding");
			}
			conditionalOperatorCount = ExpressionAnalyzer.analyzeExpressionConditions(expression);
		}
		
		api.setObjectAttribute(statementMObject, api.attributeByName(statementMObject, "conditionalOperatorCount"), IntegerValue.valueOf(conditionalOperatorCount));
	}

	/**
	 * Creates a methodsCalled meta-link for the given meta-objects (A_Statement_Method).
	 * 
	 * @param statementMObject
	 * 			The dependee Statement
	 * @param methodMObject
	 * 			The methodsCalled Method
	 */
	private void createMethodDependencyAssociation(MObject statementMObject,
			MObject methodMObject) {
		if(methodMObject != null)
			createLink(api.associationByName("A_Statement_Method"), Arrays.asList(statementMObject, methodMObject));
	}

	/**
	 * Retrieves the Method meta-object corresponding to the given method binding.
	 * If none is found, a new Method meta-object is created.
	 * 
	 * @param binding
	 * 			The method binding to search
	 * @return The corresponding Method MObject
	 */
	private MObject retrieveMethodObject(IMethodBinding binding) {
		MObject object = null;
		IJavaElement element = binding.getJavaElement();
		
		if(element != null){
			String id = JM2Loader.METHOD_IDENTIFIER+JM2Loader.processName(element.getHandleIdentifier());
			object = api.objectByName(id);
			if(object == null){ // If the method is not found, a new one is created.
				MObject newMethod = api.createObject(id, "Method");
				api.setObjectAttribute(newMethod, api.attributeByName(newMethod, "name"), new StringValue(element.getElementName()));
				api.setObjectAttribute(newMethod, api.attributeByName(newMethod, "handleIdentifier"), new StringValue(element.getHandleIdentifier()));
				api.setObjectAttribute(newMethod, api.attributeByName(newMethod, "key"), new StringValue(((IMethod)element).getKey()));
				String[] keyFrags = ((IMethod)element).getKey().split("\\.");
				api.setObjectAttribute(newMethod, api.attributeByName(newMethod, "shortKey"), new StringValue(keyFrags[keyFrags.length-1]));
				
				try {
					if(((IMethod)element).isConstructor())
						api.setObjectAttribute(newMethod, api.attributeByName(newMethod, "isConstructor"), BooleanValue.TRUE);
					else
						api.setObjectAttribute(newMethod, api.attributeByName(newMethod, "isConstructor"), BooleanValue.FALSE);
					
					JM2Loader.processMemberFlags((IMethod)element, newMethod);
				} catch (JavaModelException e) {
				}
				
				api.setObjectAttribute(newMethod, api.attributeByName(newMethod, "returnTypeArrayDimensions"), IntegerValue.valueOf(binding.getReturnType().getDimensions()));
				
				api.createLink(api.associationByName("A_Type_Method"), Arrays.asList(retrieveTypeObject(binding.getDeclaringClass()), newMethod));
				MObject returnType = retrieveTypeObject(binding.getReturnType());
				if(returnType != null) // TODO returnType should never be null
					api.createLink(api.associationByName("A_Method_Type"), Arrays.asList(newMethod, returnType));
				object = newMethod;
			}
		}
		return object;
	}

	/**
	 * Creates a fieldsAccessed meta-link for the given meta-objects (A_Statement_Field).
	 * 
	 * @param statementMObject
	 * 			The dependee Statement
	 * @param methodMObject
	 * 			The fieldsAccessed Field
	 */
	private void createFieldDependencyAssociation(MObject statementMObject,
			MObject fieldMObject) {
		if(fieldMObject != null)
			createLink(api.associationByName("A_Statement_Field"), Arrays.asList(statementMObject, fieldMObject));
	}

	/**
	 * Retrieves the Field meta-object corresponding to the given variable binding.
	 * If none is found, a new Field meta-object is created.
	 * 
	 * @param binding
	 * 			The variable binding to search
	 * @return The corresponding Field MObject
	 */
	private MObject retrieveFieldObject(IVariableBinding binding) {
		MObject object = null;
		IJavaElement element = binding.getJavaElement();
		
		if(element != null){
			String id = JM2Loader.FIELD_IDENTIFIER+JM2Loader.processName(element.getHandleIdentifier());
			object = api.objectByName(id);
			if(object == null){ // If the field is not found, a new one is created.
				MObject newField = api.createObject(id, "Field");
				api.setObjectAttribute(newField, api.attributeByName(newField, "name"), new StringValue(element.getElementName()));
				api.setObjectAttribute(newField, api.attributeByName(newField, "handleIdentifier"), new StringValue(element.getHandleIdentifier()));
				api.setObjectAttribute(newField, api.attributeByName(newField, "key"), new StringValue(((IField)element).getKey()));
				
				api.setObjectAttribute(newField, api.attributeByName(newField, "arrayDimensions"), IntegerValue.valueOf(binding.getType().getDimensions()));
				
				try {
					JM2Loader.processMemberFlags((IField)element, newField);
				} catch (JavaModelException e) {
				}

				api.createLink(api.associationByName("A_Type_Field"), Arrays.asList(retrieveTypeObject(binding.getDeclaringClass()), newField));
				api.createLink(api.associationByName("A_Field_Type"), Arrays.asList(retrieveTypeObject(binding.getType()), newField));
				
				object = newField;
			}
		}
		return object;
	}
}
