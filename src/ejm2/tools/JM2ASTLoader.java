package ejm2.tools;

import java.util.Arrays;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.quasar.juse.api.JUSE_ProgramingFacade;
import org.tzi.use.uml.ocl.value.IntegerValue;
import org.tzi.use.uml.sys.MObject;

/**
 * Class responsible for analyzing ASTs to further instantiate the EJMM. One JM2ASTLoader
 * object is meant to be created for each compilation unit to analyze.
 * 
 * <br>
 * 
 * Contains methods to scan a compilation unit for comments, methods and initializers.
 * 
 * @author Pedro Coimbra
 *
 */
public class JM2ASTLoader {

	private JUSE_ProgramingFacade api;
	// private StatementInspector inspector;
	private ASTParser parser;
	private MObject compUnitMObject;
	
	/**
	 * Constructor for JM2ASTLoader. Each object of this class is meant to analyze only
	 * one compilation unit.
	 * 
	 * @param api
	 * 			The JUSE_ProgramingFacade for the current USE session
	 * @param compUnitMObject
	 * 			The CompilationUnit MObject corresponding to the compilation unit to analyze.
	 */
	public JM2ASTLoader(JUSE_ProgramingFacade api, MObject compUnitMObject) {
		this.api = api;
		parser = ASTParser.newParser(AST.JLS4);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		this.compUnitMObject = compUnitMObject;
	}
	
	/**
	 * Creates and analyzes an AST from the given compilation unit.
	 * 
	 * @param source
	 * 			The compilation unit from which to create the AST
	 */
	public void processAST(ICompilationUnit source) {
		parser.setSource(source);
		ASTNode ast_instance = parser.createAST(null);
		JM2Visitor visitor = new JM2Visitor(source);
		ast_instance.accept(visitor);
	}

	/**
	 * Visitor used to find method declarations, initializer declarations and comments
	 * in an AST.
	 * 
	 * @author Pedro Coimbra
	 * @see org.eclipse.jdt.core.dom.ASTVisitor
	 */
	private class JM2Visitor extends ASTVisitor {
		
		private int initializer_count, initializer_start_pos;
		
		public JM2Visitor(ICompilationUnit compilationUnit){
			initializer_count = 0;
			initializer_start_pos = 0;
		}
			
		@Override
		public boolean visit(MethodDeclaration node){
			MObject methodMObject = api.objectByName(JM2Loader.METHOD_IDENTIFIER + JM2Loader.processName((node.resolveBinding().getJavaElement().getHandleIdentifier())));		
			Block body = node.getBody();
			if(body != null){
				StatementInspector inspector = new StatementInspector(api, methodMObject, true);
				MObject bodyMObject = inspector.createStatementObject(body);
				api.createLink(api.associationByName("A_Method_Block"), Arrays.asList(methodMObject, bodyMObject));
				inspector.inspectStatement(body, bodyMObject);
			}
			if(node.resolveBinding() == null)
				System.out.println(node.getName());
			return false;
		}
		
		@Override
		public boolean visit(Initializer node){
			if(node.getStartPosition()>initializer_start_pos){
				initializer_start_pos = node.getStartPosition();
				initializer_count++;
				Block body = node.getBody();
				
				// Trying to find the Initializer MObject name.
				String initializerName = "";
				if(node.getParent() instanceof EnumDeclaration)
					initializerName = JM2Loader.processName(((EnumDeclaration)node.getParent()).resolveBinding().getJavaElement().getHandleIdentifier())+"_Initializer"+initializer_count;
				else 
					initializerName = JM2Loader.processName(((TypeDeclaration)node.getParent()).resolveBinding().getJavaElement().getHandleIdentifier())+"_Initializer"+initializer_count;
				assert !initializerName.equals("");
				
				MObject initializerMObject = api.objectByName(initializerName);
				
				StatementInspector inspector = new StatementInspector(api, initializerMObject, false);
				
				MObject bodyMObject = inspector.createStatementObject(body);
				
				
				if(initializerMObject != null)
					api.createLink(api.associationByName("A_Initializer_Block"), Arrays.asList(initializerMObject, bodyMObject));

				inspector.inspectStatement(body, bodyMObject);
			}
			return false;
		}
		
		
		@Override
		public boolean visit(LineComment node){
			createCommentObject(node);
			return false;
		}
		
		@Override
		public boolean visit(BlockComment node){
			createCommentObject(node);
			return false;
		}
		
		@Override
		public boolean visit(Javadoc node){
			createCommentObject(node);
			return false;
		}
	}

	/**
	 * Creates a new Comment meta-object.
	 * 
	 * @param node
	 * 			The comment AST node
	 * @return The new Comment MObject
	 */
	private MObject createCommentObject(Comment node){
		MObject commentMObject = api.createObject(null, node.getClass().getSimpleName());
		api.setObjectAttribute(commentMObject, api.attributeByName(commentMObject, "startPosition"), IntegerValue.valueOf(node.getStartPosition()));
		api.setObjectAttribute(commentMObject, api.attributeByName(commentMObject, "length"), IntegerValue.valueOf(node.getLength()));
		api.createLink(api.associationByName("A_CompilationUnit_Comment"), Arrays.asList(compUnitMObject, commentMObject));
		return commentMObject;
	}
	
}
