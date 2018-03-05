package ejm2.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.quasar.juse.api.JUSE_ProgramingFacade;
import org.quasar.juse.api.implementation.ProgramingFacade;
import org.tzi.use.uml.ocl.value.BooleanValue;
import org.tzi.use.uml.ocl.value.EnumValue;
import org.tzi.use.uml.ocl.value.IntegerValue;
import org.tzi.use.uml.ocl.value.StringValue;
import org.tzi.use.uml.ocl.value.ObjectValue;
import org.tzi.use.uml.ocl.value.Value;
import org.tzi.use.uml.sys.MObject;
import org.tzi.use.uml.sys.MSystem;

/**
 * Class responsible for creating full EJMM instances.
 * 
 * @author Pedro Coimbra
 *
 */
public class JM2Loader {
	/*
	 * This class is a mess. The workflow goes:
	 *  loadEJMMfromProject -> 
	 *  processX (e.g. processType) ->
	 *  secondXProcess (e.g. secondTypeProcess)
	 */
	
	private static MSystem system;
	
	/**
	 * Used to store all class files for the second instantiation phase.
	 */
	private static HashSet<IClassFile> allClassFiles;
	
	/**
	 * Used to store all compilation units for the second instantiation phase.
	 */
	private static HashSet<ICompilationUnit> allCompilationUnits;
	
	/**
	 * Used to store all Type MObjects for easier retrieval.
	 */
	private static HashMap<String, MObject> allTypeObjects;

	private static JUSE_ProgramingFacade api = new ProgramingFacade();
	private static String useDirectory; // = "C:\\Users\\Pedro\\Desktop\\MIG\\eclipse\\workspace\\use-3.0.6"; // TODO Need relative path
	private static String modelDirectory; // = "C:\\Users\\Pedro\\Desktop\\MIG\\eclipse\\workspace\\EJM2Metrics\\metamodel"; // TODO Need relative path
	private static String modelFile; // = "JavaMMv3_FLAME.use";
	private static final String ARRAY_PARAMETER_IDENTIFIER = "ARRAY_PARAM";
	static final String METHOD_IDENTIFIER = "METHOD_";
	static final String FIELD_IDENTIFIER = "FIELD_";
	
	
	/**
	 * Sets the USE tool installation folder.
	 * 
	 * @param directory
     *			the absolute path of the USE installation folder
	 */
	public static void setUseDirectory(String directory){
		useDirectory = directory;
	}
	
	/**
	 * Sets the folder where the EJMM USE specification can be found.
	 * 
	 * @param directory
     *			path of the folder that contains the EJMM
	 */
	public static void setModelDirectory(String directory){
		modelDirectory = directory;
	}
	
	/**
	 * Sets the EJMM USE specification file name.
	 * @param fileName
	 * 			the file name
	 */
	public static void setModelFile(String fileName){
		modelFile = fileName;
	}
	
	/**
	 * Initiates a full EJMM instantiation procedure for the given Java project.
	 * 
	 * @param javaProject
	 * 			The Java project to analyze
	 * @return The JUSE_ProgramingFacade with a USE session with the EJMM instantiation
	 */
	public static JUSE_ProgramingFacade loadEJMMfromProject(IJavaProject javaProject){		
		System.out.println("----------------------\n"+"Starting JM2Loader process\n"+"----------------------");
		
		
		long startTime = System.nanoTime();
		
		allClassFiles = new HashSet<IClassFile>();
		allCompilationUnits= new HashSet<ICompilationUnit>();
		allTypeObjects = new HashMap<String, MObject>();

		// Initialize USE and load EJMM specification
		api.initialize(new String[0], useDirectory, modelDirectory);
		system = api.compileSpecification(modelFile);
		
		// Create default objects
		System.out.println("Creating default objects");
		createDefaultObjects();

		String processedName = (processName(javaProject.getElementName()));
		System.out.println("*****************************************\nProcessing project: " + javaProject.getElementName() + " -- " + processedName + "\nHandle identifier: " + javaProject.getHandleIdentifier()+"\n*****************************************");
		
		// Instantiate JavaProject
		MObject javaProjectMObject = api.createObject(processedName, "JavaProject");
		api.setObjectAttribute(javaProjectMObject, api.attributeByName(javaProjectMObject, "name"), new StringValue(javaProject.getElementName()));
		api.setObjectAttribute(javaProjectMObject, api.attributeByName(javaProjectMObject, "handleIdentifier"), new StringValue(javaProject.getHandleIdentifier()));

		// Process PackageFragmentRoots
		try {
			for(IPackageFragmentRoot pfr: javaProject.getAllPackageFragmentRoots()){
				if(!pfr.isExternal() && pfr.getJavaProject().equals(javaProject)){
					String processedPFRName = processName(pfr.getResource().getName());
					MObject pfrMObject = api.createObject(processedPFRName, "PackageFragmentRoot");
					api.setObjectAttribute(pfrMObject, api.attributeByName(pfrMObject, "name"), new StringValue(pfr.getElementName()));
					api.setObjectAttribute(pfrMObject, api.attributeByName(pfrMObject, "handleIdentifier"), new StringValue(pfr.getHandleIdentifier()));
					api.createLink(api.associationByName("A_JavaProject_PackageFragmentRoot"), Arrays.asList(javaProjectMObject, pfrMObject));
					
					if(!pfr.isArchive())
						api.setObjectAttribute(pfrMObject, api.attributeByName(pfrMObject, "packageFragmentRootType"), new EnumValue(api.enumByName("PackageFragmentRootType"), "Folder"));
					else{
						String extension = pfr.getPath().getFileExtension();
						if(extension != null){
							if(extension.equals("zip"))
								api.setObjectAttribute(pfrMObject, api.attributeByName(pfrMObject, "packageFragmentRootType"), new EnumValue(api.enumByName("PackageFragmentRootType"), "Zip"));
							else
								if(extension.equals("jar"))
									api.setObjectAttribute(pfrMObject, api.attributeByName(pfrMObject, "packageFragmentRootType"), new EnumValue(api.enumByName("PackageFragmentRootType"), "Jar"));
						}
					}
							
					// Process PackageFragments
					for(IJavaElement packagefrag: pfr.getChildren()){
						if(((IPackageFragment)packagefrag).containsJavaResources())
							processPackageFragment((IPackageFragment)packagefrag,pfrMObject);
					}
				}
			}

			System.out.println("*****************************************\nStarting second processing moment\n*****************************************");
			// Second processing phase, after all types have been instantiated as USE objects
			for(IClassFile cf: allClassFiles){
					secondClassFileProcess(cf);
			}
			for(ICompilationUnit c: allCompilationUnits){
					secondCompilationUnitProcess(c);
			}
		} catch (JavaModelException e) {
				e.printStackTrace();
		}
		

		long endTime = System.nanoTime();
		long duration = endTime - startTime;
		double durationInSeconds = duration/1000000000d;

		System.out.println("\nTotal time of execution = " + durationInSeconds + " seconds. (" + duration + " nanoseconds).");
		
		api.command("info state");
		
		return api;
	}
	
	
	/**
	 * Getter for the MSystem with the current USE session.
	 * 
	 * @return The current MSystem (system)
	 */
	public static MSystem getSystem() {
		return system;
	}

	
	/**
	 * Returns all Type meta-objects currently created. The same Meta-object might
	 * have several keys (e.g. EJM handle identifier, type signature, simple name).
	 *
	 * @return A map containing all Type MObjects
	 */
	public static HashMap<String, MObject> getAllTypeObjects() {
		return allTypeObjects;
	}

	
	/**
	 * Returns the same string excluding all non-alphanumeric and non-underscore
	 * characters removed (in compliance of USE MObject naming rules). 
	 * <br>
	 * If the string contains the characters that make an array identifier 
	 * in a handle identifier, it is replaced by the array parameter identifier 
	 * constant (ARRAY_PARAMETER_IDENTIFIER = "ARRAY_PARAM").
	 * 
	 * @param name
	 * 			The string to process
	 * @return The parameter with all non-alphanumeric and non-underscore characters excluded.
	 */
	public static String processName(String name){
		String acc = "";
		for(int i = 0; i != name.length(); i++){
			char c = name.charAt(i);
			if(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'	|| c == '_')
				acc+=c;
			else
				if(c == '$')
					acc+='_';
				else
					if( c == '\\' && ((i + 1) < name.length()) && name.charAt(i+1) == '[')
						acc+=ARRAY_PARAMETER_IDENTIFIER;
		}
			
		return acc;
	}

	
	/**
	 * Initiates the second processing phase procedures for a class file.
	 * <br>
	 * Currently, class file contents are not part of the second processing phase,
	 * so this method does nothing.
	 * 
	 * @param cf
	 * 			The class file to process
	 */
	private static void secondClassFileProcess(IClassFile cf) {
		IType t = cf.getType();
		// TODO Fix second type process for class files (current problem: class/class file mixups)!
		// secondTypeProcess(t);
	}
	
	/**
	 * Initiates the second processing phase procedures for a compilation unit. 
	 * First, processes the package declaration annotation. Then, processes types.
	 * Finally, processes the compilation unit AST.
	 * 
	 * @param c
	 * 			The compilation unit to process
	 */
	private static void secondCompilationUnitProcess(ICompilationUnit c) {
		try {
			MObject compUnitMObject = api.objectByName(processName(c.getHandleIdentifier()));
			for(IPackageDeclaration pd: c.getPackageDeclarations())
				processAnnotations(compUnitMObject, pd.getAnnotations());
			for(IType t: c.getAllTypes()) 
				secondTypeProcess(t);
			JM2ASTLoader astLoader = new JM2ASTLoader(api, compUnitMObject);
			// System.out.println(c.getElementName());
			astLoader.processAST(c);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Processes a set of annotations to create Annotation and AnnotationValue meta-objects.
	 * <br>
	 * This method is only meant to be used during the second instantiation phase.
	 * 
	 * @param annotatableMObject
	 * 			The Annotatable meta-object to which the annotations are attached.
	 * @param annotations
	 * 			The annotations to process
	 * @throws JavaModelException
	 */
	private static void processAnnotations(MObject annotatableMObject, IAnnotation[] annotations) throws JavaModelException {
		for(IAnnotation annotation: annotations){
			MObject annotationMObject = api.createObject(processName(annotation.getHandleIdentifier()), "Annotation");
			api.setObjectAttribute(annotationMObject, api.attributeByName(annotationMObject, "name"), new StringValue(annotation.getElementName()));
			api.setObjectAttribute(annotationMObject, api.attributeByName(annotationMObject, "handleIdentifier"), new StringValue(annotation.getHandleIdentifier()));
			api.createLink(api.associationByName("A_Annotatable_Annotation"), Arrays.asList(annotatableMObject, annotationMObject));	
			
			String typeName = annotation.getElementName();
			// TODO check if annotation is from a class file
			MObject annotationType = allTypeObjects.get(typeName);
			if(annotationType != null)
				api.createLink(api.associationByName("A_Type_Annotation"), Arrays.asList(annotationType, annotationMObject));
			else{
					annotationType = createExternalTypeFromName(typeName);
					api.createLink(api.associationByName("A_Type_Annotation"), Arrays.asList(annotationType, annotationMObject));
			}

			// Processing annotation values
			for(IMemberValuePair mvp: annotation.getMemberValuePairs()){
				MObject valueMObject = api.createObject(processName(annotation.getHandleIdentifier()+"_"+mvp.toString()), "AnnotationValue");
				api.setObjectAttribute(valueMObject, api.attributeByName(valueMObject, "value"), new StringValue(mvp.getValue().toString()));
				api.createLink(api.associationByName("A_Annotation_AnnotationValue"), Arrays.asList(annotationMObject, valueMObject));	
				
				// Finding the corresponding field
				Value fieldValue = api.oclEvaluator(annotationType.name()+".fields->any(name = '" + mvp.getMemberName() + "')");
				if(fieldValue.isDefined() && fieldValue.isObject() && ((ObjectValue)fieldValue).value().cls().equals(api.classByName("Field")))
					api.createLink(api.associationByName("A_Field_AnnotationValue"), Arrays.asList(((ObjectValue)fieldValue).value(), valueMObject));
				else // If the field is not found, a new one is created
					api.createLink(api.associationByName("A_Field_AnnotationValue"), Arrays.asList(createAnnotationField(annotationType, mvp.getMemberName()), valueMObject));
			}
		}
	}

	/**
	 * Creates a new Field meta-object for an external annotation type.
	 * 
	 * @param annotationTypeMObject
	 * 			The annotation type
	 * @param fieldName
	 * 			The name of the field to create
	 * @return The new Field meta-object
	 */
	private static MObject createAnnotationField(MObject annotationTypeMObject, String fieldName){
		MObject fieldMObject = api.createObject(null, "Field");
		api.setObjectAttribute(fieldMObject, api.attributeByName(fieldMObject, "name"), new StringValue(fieldName));
		api.createLink(api.associationByName("A_Type_Field"), Arrays.asList(annotationTypeMObject, fieldMObject));
		return fieldMObject;
	}
	
	/**
	 * Procedures of the second instantiation phase for the Type metaclass.
	 * <br>
	 * Types are scanned for annotations, type parameters, type parameter bounds, 
	 * inheritance, type nesting, field types, method return types and method 
	 * parameters.
	 * 
	 * @param t
	 * 			The type to process
	 */
	private static void secondTypeProcess(IType t) {
		MObject typeMObject = api.objectByName(processName(t.getHandleIdentifier()));
		try {
			// Processing type parameters
			for(ITypeParameter tp: t.getTypeParameters()){
				MObject typeParameterMObject = api.createObject(t.getElementName() + '_' + tp.getElementName(), "TypeParameter");
				api.setObjectAttribute(typeParameterMObject, api.attributeByName(typeParameterMObject, "name"), new StringValue(tp.getElementName()));
				api.setObjectAttribute(typeParameterMObject, api.attributeByName(typeParameterMObject, "handleIdentifier"), new StringValue(tp.getHandleIdentifier()));
				for(String bound: tp.getBoundsSignatures()){
					MObject boundTypeMObject = getTypeObjectFromSignature(bound);
					api.createLink(api.associationByName("B_Type_TypeParameter"), Arrays.asList(boundTypeMObject, typeParameterMObject));
				}
			}
			
			// Processing inheritance
			for(String interfaceSignature: t.getSuperInterfaceTypeSignatures()){
				MObject interfaceMObject = getTypeObjectFromSignature(interfaceSignature);
				api.createLink(api.associationByName("C_Type_Type"), Arrays.asList(interfaceMObject, typeMObject));
			}
			
			String superclassSignature = t.getSuperclassTypeSignature();
			if(superclassSignature != null){
				MObject superclassMObject = getTypeObjectFromSignature(superclassSignature);
				api.createLink(api.associationByName("B_Type_Type"), Arrays.asList(superclassMObject, typeMObject));
			}
			
			// Processing type nesting
			IType declaringType = t.getDeclaringType();
			if(declaringType != null){
				MObject declaringTypeMObject = api.objectByName(processName(declaringType.getHandleIdentifier()));
				if(declaringTypeMObject != null)
					api.createLink(api.associationByName("A_Type_Type"), Arrays.asList(typeMObject, declaringTypeMObject));
			}
			
			// Processing method parameters, exceptions and return type
			for(IMethod m: t.getMethods()){
				MObject methodMObject = api.objectByName(METHOD_IDENTIFIER + processName(m.getHandleIdentifier()));
				
				for(ILocalVariable p: m.getParameters()){
					int arrayCount =  Signature.getArrayCount(p.getTypeSignature());
					MObject parameterMObject = api.createObject(processName(p.getHandleIdentifier()), "LocalVariable");
					api.setObjectAttribute(parameterMObject, api.attributeByName(parameterMObject, "name"), new StringValue(p.getElementName()));
					api.setObjectAttribute(parameterMObject, api.attributeByName(parameterMObject, "handleIdentifier"), new StringValue(p.getHandleIdentifier()));
					api.setObjectAttribute(parameterMObject, api.attributeByName(parameterMObject, "arrayDimensions"), IntegerValue.valueOf(arrayCount));
					api.createLink(api.associationByName("B_Method_LocalVariable"), Arrays.asList(methodMObject, parameterMObject));
					
					MObject parameterType = getTypeObjectFromSignature(p.getTypeSignature());
					api.createLink(api.associationByName("A_LocalVariable_Type"), Arrays.asList(parameterMObject, parameterType));
				}
				
				MObject returnTypeMObject = getTypeObjectFromSignature(m.getReturnType());
				int arrayCount =  Signature.getArrayCount(m.getReturnType());
				api.setObjectAttribute(methodMObject, api.attributeByName(methodMObject, "returnTypeArrayDimensions"), IntegerValue.valueOf(arrayCount));
				api.createLink(api.associationByName("A_Method_Type"), Arrays.asList(methodMObject, returnTypeMObject));
				
				for(String exception: m.getExceptionTypes()){
					MObject exceptionMObject = getTypeObjectFromSignature(exception);
					api.createLink(api.associationByName("B_Method_Type"), Arrays.asList(methodMObject, exceptionMObject));
				}
				
				processAnnotations(methodMObject, m.getAnnotations());
			}
			
			// Processing field type
			for(IField f: t.getFields()){
				MObject fieldMObject = api.objectByName(FIELD_IDENTIFIER + processName(f.getHandleIdentifier()));
				MObject fieldTypeMObject = getTypeObjectFromSignature(f.getTypeSignature());
				api.createLink(api.associationByName("A_Field_Type"), Arrays.asList(fieldTypeMObject, fieldMObject));
				int arrayCount = Signature.getArrayCount(f.getTypeSignature());
				api.setObjectAttribute(fieldMObject, api.attributeByName(fieldMObject, "arrayDimensions"), IntegerValue.valueOf(arrayCount));

				processAnnotations(fieldMObject, f.getAnnotations());
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the Type meta-object for the corresponding type signature. If none
	 * can be found, a new external type is created.
	 * 
	 * @param signature
	 * 			the type signature.
	 * @return The Type MObject corresponding to the given signature
	 */
	public static MObject getTypeObjectFromSignature(String signature){
		// With the changes in MObject naming and the allTypeObjects mapping, 
		// it may be possible to omit some parts of this method without any side-effects.
		String s = Signature.getElementType(signature);
		switch(Signature.getSimpleName(s)){
		case Signature.SIG_BOOLEAN:
			return api.objectByName("boolean");
		case Signature.SIG_BYTE:
			return api.objectByName("byte");
		case Signature.SIG_CHAR:
			return api.objectByName("char");
		case Signature.SIG_DOUBLE:
			return api.objectByName("double");
		case Signature.SIG_FLOAT:
			return api.objectByName("float");
		case Signature.SIG_INT:
			return api.objectByName("int");
		case Signature.SIG_LONG:
			return api.objectByName("long");
		case Signature.SIG_SHORT:
			return api.objectByName("short");
		case Signature.SIG_VOID:
			return api.objectByName("void");
		case "String":
			return api.objectByName("String");
		default: 
			break;
		}
		MObject typeMObject = allTypeObjects.get(signature);
		if(typeMObject != null)
			return typeMObject;
		else{
			typeMObject = allTypeObjects.get(Signature.getElementType(signature));
			if(typeMObject != null)
				return typeMObject;
			else{
				typeMObject = allTypeObjects.get(Signature.getQualifier(signature) + Signature.getSimpleName(signature));
				if(typeMObject != null) 
					return typeMObject;
				else{
					typeMObject = allTypeObjects.get(Signature.getSimpleName(signature));
					if(typeMObject != null)
						return typeMObject;
					else
						return createExternalTypeFromSignature(signature);
				}
			}
		}
	}


	/**
	 * Creates a new Type meta-object from the given type signature. It is an external
	 * type, and thus meta-linked to the ExternalTypesClassFile meta-object.
	 * 
	 * @param signature
	 * 			The signature of the type to create
	 * @return The new Type MObject
	 */
	private static MObject createExternalTypeFromSignature(String signature) {
		String qualifiedName = Signature.getQualifier(signature) + Signature.getSimpleName(signature);
		MObject typeMObject = api.objectByName(processName(qualifiedName));
		if(typeMObject == null){
			typeMObject = api.createObject(processName(qualifiedName), "Type");
			api.setObjectAttribute(typeMObject, api.attributeByName(typeMObject, "name"), new StringValue(Signature.getSimpleName(signature)));
			api.createLink(api.associationByName("A_TypeRoot_Type"), Arrays.asList(api.objectByName("ExternalTypesClassFile"), typeMObject));
			allTypeObjects.put(qualifiedName, typeMObject);
		}
		return typeMObject;
	}
	
	/**
	 * Creates a new Type meta-object from the given type name. It is an external
	 * type, and thus meta-linked to the ExternalTypesClassFile meta-object.
	 * 
	 * @param typeName
	 * 			The name of the type to create. Can be qualified or simple.
	 * @return The new Type MObject
	 */
	public static MObject createExternalTypeFromName(String typeName) {
		String qualifier = Signature.getQualifier(typeName);
		String simpleName = Signature.getSimpleName(typeName);	
		
		if(qualifier.isEmpty()){
			MObject typeMObject = api.createObject(processName(simpleName), "Type");
			api.setObjectAttribute(typeMObject, api.attributeByName(typeMObject, "name"), new StringValue(simpleName));
			api.createLink(api.associationByName("A_TypeRoot_Type"), Arrays.asList(api.objectByName("ExternalTypesClassFile"), typeMObject));
			allTypeObjects.put(simpleName, typeMObject);
			return typeMObject;
		}
		else{
			MObject typeMObject = api.createObject(processName(typeName), "Type");
			api.setObjectAttribute(typeMObject, api.attributeByName(typeMObject, "name"), new StringValue(simpleName));
			api.createLink(api.associationByName("A_TypeRoot_Type"), Arrays.asList(api.objectByName("ExternalTypesClassFile"), typeMObject));
			allTypeObjects.put(typeName, typeMObject);
			return typeMObject;
		}
	}


	/**
	 * Processes a given package fragment, creating a PackageFragment meta-object and
	 * meta-objects for all containing compilation units and class files. 
	 * 
	 * @param packageFragment
	 * 			The package fragment to analyze
	 * @param packageFragmentRootMObject
	 * 			The PackageFragmentRoot meta-object that contains this PackageFragment
	 * @throws JavaModelException
	 */
	private static void processPackageFragment(IPackageFragment packageFragment, MObject packageFragmentRootMObject) throws JavaModelException{
		String processedName = processName(packageFragment.getHandleIdentifier());
		processedName = (processedName == "" || processedName.isEmpty()) ? null : processedName;
		MObject packagefragMObject = api.createObject(processedName, "PackageFragment");
		api.setObjectAttribute(packagefragMObject, api.attributeByName(packagefragMObject, "name"), new StringValue(packageFragment.getElementName()));
		api.setObjectAttribute(packagefragMObject, api.attributeByName(packagefragMObject, "handleIdentifier"), new StringValue(packageFragment.getHandleIdentifier()));
		api.createLink(api.associationByName("A_PackageFragmentRoot_PackageFragment"), Arrays.asList(packageFragmentRootMObject, packagefragMObject));
		for(ICompilationUnit compUnit: packageFragment.getCompilationUnits()){
			String processedCompUnitName = processName(compUnit.getHandleIdentifier());
			MObject compUnitMObject = api.createObject(processedCompUnitName, "CompilationUnit");
			api.setObjectAttribute(compUnitMObject, api.attributeByName(compUnitMObject, "name"), new StringValue(compUnit.getElementName()));
			api.setObjectAttribute(compUnitMObject, api.attributeByName(compUnitMObject, "handleIdentifier"), new StringValue(compUnit.getHandleIdentifier()));
			api.createLink(api.associationByName("A_PackageFragment_TypeRoot"), Arrays.asList(packagefragMObject, compUnitMObject));
			processCompilationUnitTypes(compUnit, compUnitMObject);
		}
		for(IClassFile classFile: packageFragment.getClassFiles()){
			String processedClassFileName = processName(classFile.getHandleIdentifier());
			MObject classFileMObject = api.createObject(processedClassFileName, "ClassFile");
			api.setObjectAttribute(classFileMObject, api.attributeByName(classFileMObject, "name"), new StringValue(classFile.getElementName()));
			api.setObjectAttribute(classFileMObject, api.attributeByName(classFileMObject, "handleIdentifier"), new StringValue(classFile.getHandleIdentifier()));
			api.createLink(api.associationByName("A_PackageFragment_TypeRoot"), Arrays.asList(packagefragMObject, classFileMObject));
			processClassFileType(classFile, classFileMObject);
		}
		
	}

	/**
	 * Processes the type of a class file.
	 * 
	 * @param classFile
	 * @param classFileMObject
	 * @throws JavaModelException
	 */
	private static void processClassFileType(IClassFile classFile, MObject classFileMObject) throws JavaModelException {
		allClassFiles.add(classFile);
		processType(classFile.getType(), classFileMObject);
	}

	
	/**
	 * Processes all the types of a compilation unit.
	 * 
	 * @param compUnit
	 * @param compUnitMObject
	 * @throws JavaModelException
	 */
	private static void processCompilationUnitTypes(ICompilationUnit compUnit, MObject compUnitMObject) throws JavaModelException {
		allCompilationUnits.add(compUnit);
		for(IType type: compUnit.getAllTypes())
			processType(type, compUnitMObject);
	}

	/**
	 * Processes the given type, creating a Type instance and analyzing the type for
	 * its methods, fields and initializers.
	 * 
	 * @param type
	 * 			The type to analyze
	 * @param typeRootMObject
	 * 			The type root in which the type is declared
	 * @throws JavaModelException
	 */
	private static void processType(IType type, MObject typeRootMObject) throws JavaModelException{
		String elementName = type.getElementName();
		String processedName = processName(type.getHandleIdentifier());
		
		// For binary types
		if(elementName.isEmpty()){
			elementName = type.getTypeQualifiedName();
			processedName += processName(type.getTypeQualifiedName());
		}
		
		MObject typeMObject = api.createObject(processedName, "Type");
		api.setObjectAttribute(typeMObject, api.attributeByName(typeMObject, "name"), new StringValue(elementName));
		api.setObjectAttribute(typeMObject, api.attributeByName(typeMObject, "handleIdentifier"), new StringValue(type.getHandleIdentifier()));
		api.createLink(api.associationByName("A_TypeRoot_Type"), Arrays.asList(typeRootMObject, typeMObject));

		// Lots of different keys are made for the Type map to make sure references made to it
		// during the second phase work correctly.
		allTypeObjects.put(type.getFullyQualifiedName(), typeMObject);
		allTypeObjects.put(type.getFullyQualifiedParameterizedName(), typeMObject);
		allTypeObjects.put(type.getFullyQualifiedName('.'), typeMObject);
		allTypeObjects.put(type.getElementName(), typeMObject);
		allTypeObjects.put(Signature.getSimpleName(type.getFullyQualifiedName()), typeMObject);
		allTypeObjects.put(Signature.getSimpleName(type.getFullyQualifiedName('.')), typeMObject);
		allTypeObjects.put(Signature.createTypeSignature(type.getFullyQualifiedName('.').toCharArray(), type.isResolved()), typeMObject);
		if(!elementName.isEmpty())
			allTypeObjects.put(Signature.createTypeSignature(elementName.toCharArray(), false), typeMObject);

		
		for(IField field: type.getFields()){
			processField(field, typeMObject, typeRootMObject);
		}
		for(IMethod method: type.getMethods()){
			processMethod(method, typeMObject, typeRootMObject);
		}
		for(IInitializer initializer: type.getInitializers()){
			MObject initializerMObject = api.createObject(processedName+"_Initializer"+initializer.getOccurrenceCount(), "Initializer");
			api.setObjectAttribute(initializerMObject, api.attributeByName(initializerMObject, "name"), new StringValue(initializer.getElementName()));
			api.setObjectAttribute(initializerMObject, api.attributeByName(initializerMObject, "handleIdentifier"), new StringValue(initializer.getHandleIdentifier()));
			//api.setObjectAttribute(initializerMObject, api.attributeByName(initializerMObject, "occurrenceCount"), IntegerValue.valueOf(initializer.getOccurrenceCount()));
			api.createLink(api.associationByName("A_Type_Initializer"), Arrays.asList(typeMObject, initializerMObject));
		}
		
		processMemberFlags(type,typeMObject);
		
		if(!type.isBinary())
			processAnonymousTypes(type, typeRootMObject);
	}

	/**
	 * Special procedures for processing anonymous types declared inside a given member.
	 * 
	 * @param member
	 * 			The member to scan for anonymous types
	 * @param typeRootMObject
	 * 			The type root where the member (and anonymous types) are contained
	 * @throws JavaModelException
	 */
	private static void processAnonymousTypes(IMember member,
			MObject typeRootMObject) throws JavaModelException {
		int anonymousTypeOccurrenceCount = 1;
		IType anonymousClass = null;
		do{
			anonymousClass = member.getType("", anonymousTypeOccurrenceCount);
			if(anonymousClass != null && anonymousClass.exists())
				processType(anonymousClass, typeRootMObject);
			anonymousTypeOccurrenceCount++;
		}while(anonymousClass != null && anonymousClass.exists());
	}
	
	/**
	 * Processes a given field, creating a Field instance and meta-linking it with its
	 * declaring Type.
	 * 
	 * @param field
	 * 			The field to process
	 * @param typeMObject
	 * 			The declaring type
	 * @param typeRootMObject
	 * 			The type root in which the field is contained
	 * @throws JavaModelException
	 */
	private static void processField(IField field, MObject typeMObject, MObject typeRootMObject) throws JavaModelException {
		String processedKey = FIELD_IDENTIFIER + processName(field.getHandleIdentifier());
		// System.out.println("Field: " + processedKey);
		
		MObject fieldMObject = api.createObject(processedKey, "Field");
		api.setObjectAttribute(fieldMObject, api.attributeByName(fieldMObject, "name"), new StringValue(field.getElementName()));
		api.setObjectAttribute(fieldMObject, api.attributeByName(fieldMObject, "handleIdentifier"), new StringValue(field.getHandleIdentifier()));
		api.setObjectAttribute(fieldMObject, api.attributeByName(fieldMObject, "key"), new StringValue(field.getKey()));
		api.createLink(api.associationByName("A_Type_Field"), Arrays.asList(typeMObject, fieldMObject));
		
		processMemberFlags(field,fieldMObject);
		
		if(!field.isBinary())
			processAnonymousTypes(field, typeRootMObject);
	}
	
	/**
	 * Processes the given method, creating a Method instance and meta-linking it to its
	 * declaring Type.
	 * 
	 * @param method
	 * 			The method to analyze
	 * @param typeMObject
	 * 			The declaring Type
	 * @param typeRootMObject
	 * 			The TypeRoot in which the method is contained
	 * @throws JavaModelException
	 */
	private static void processMethod(IMethod method, MObject typeMObject, MObject typeRootMObject) throws JavaModelException{
		String processedKey = METHOD_IDENTIFIER + processName(method.getHandleIdentifier());
		if(method.getHandleIdentifier() == null)
			System.out.println(method.getKey());
		// System.out.println("Methd: " + processedKey);
		
		MObject methodMObject = api.createObject(processedKey, "Method");
		api.setObjectAttribute(methodMObject, api.attributeByName(methodMObject, "name"), new StringValue(method.getElementName()));
		api.setObjectAttribute(methodMObject, api.attributeByName(methodMObject, "handleIdentifier"), new StringValue(method.getHandleIdentifier()));
		api.setObjectAttribute(methodMObject, api.attributeByName(methodMObject, "key"), new StringValue(method.getKey()));
		String[] keyFrags = method.getKey().split("\\.");
		api.setObjectAttribute(methodMObject, api.attributeByName(methodMObject, "shortKey"), new StringValue(keyFrags[keyFrags.length-1]));
		api.createLink(api.associationByName("A_Type_Method"), Arrays.asList(typeMObject, methodMObject));
		
		if(method.isConstructor())
			api.setObjectAttribute(methodMObject, api.attributeByName(methodMObject, "isConstructor"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(methodMObject, api.attributeByName(methodMObject, "isConstructor"), BooleanValue.FALSE);
		
		processMemberFlags(method,methodMObject);
		
		if(!method.isBinary())
			processAnonymousTypes(method, typeRootMObject);
	}
	
	
	/**
	 * Processes the flags of a given member.
	 * <br>
	 * This method first handles attributes common to Types, Methods and Fields. Then,
	 * depending on the member type, it branches off to instantiate specific attributes.
	 *
	 * @param member
	 * 			The member to scan for flags.
	 * @param memberMObject
	 * 			The corresponding Member meta-object
	 * @throws JavaModelException
	 * @see org.eclipse.jdt.core.Flags
	 * 
	 */
	public static void processMemberFlags(IMember member, MObject memberMObject) throws JavaModelException{
		int flags = member.getFlags();
		
		String visibility = "Default";
		if(Flags.isPrivate(flags))
			visibility = "Private";
		else
		if(Flags.isProtected(flags))
			visibility = "Protected";
		else
		if(Flags.isPublic(flags))
			visibility = "Public";
		api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "visibility"), new EnumValue(api.enumByName("VisibilityType"), visibility));
		
		if(Flags.isFinal(flags))
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isFinal"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isFinal"), BooleanValue.FALSE);
		if(Flags.isSynthetic(flags))
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isSynthetic"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isSynthetic"), BooleanValue.FALSE);
		if(Flags.isDeprecated(flags))
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isDeprecated"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isDeprecated"), BooleanValue.FALSE);
		
		if(member instanceof IType){	
			processTypeFlags(memberMObject, flags);
		}
		
		if(member instanceof IField){
			processFieldFlags(memberMObject, flags);
		}
		
		if(member instanceof IMethod){
			processMethodFlags(memberMObject, flags);
		}
	}

	/**
	 * Processes flags for attributes specific to the Method metaclass.
	 * 
	 * @param memberMObject
	 * 			The Method meta-object
	 * @param flags
	 * 			The method's flags
	 */
	private static void processMethodFlags(MObject memberMObject, int flags) {
		if(Flags.isStatic(flags))
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isStatic"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isStatic"), BooleanValue.FALSE);
		if(Flags.isSynchronized(flags))
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isSynchronized"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isSynchronized"), BooleanValue.FALSE);
		if(Flags.isNative(flags))
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isNative"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isNative"), BooleanValue.FALSE);
		if(Flags.isBridge(flags))
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isBridge"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isBridge"), BooleanValue.FALSE);
		if(Flags.isVarargs(flags))
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "hasVarargs"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "hasVarargs"), BooleanValue.FALSE);
	}

	/**
	 * Processes flags for attributes specific to the Field metaclass.
	 * @param memberMObject
	 * 			The Field meta-object
	 * @param flags
	 * 			The field's flags
	 */
	private static void processFieldFlags(MObject memberMObject, int flags) {
		if(Flags.isStatic(flags))
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isStatic"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isStatic"), BooleanValue.FALSE);
		if(Flags.isVolatile(flags))
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isVolatile"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isVolatile"), BooleanValue.FALSE);
		if(Flags.isTransient(flags))
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isTransient"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isTransient"), BooleanValue.FALSE);
	}

	/**
	 * Processes flags for attributes specific to the Type metaclass.
	 * @param memberMObject
	 * 			The Type meta-object
	 * @param flags
	 * 			The type's flags
	 */
	private static void processTypeFlags(MObject memberMObject, int flags) {
		String javaType = "ClassType";
		if(Flags.isEnum(flags))
			javaType = "EnumType";
			else
				if(Flags.isInterface(flags))
					javaType = "InterfaceType";
				else
					if(Flags.isAnnotation(flags))
						javaType = "AnnotationType";
		api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "javaType"), new EnumValue(api.enumByName("JavaType"), javaType));
		
		
		if(Flags.isAbstract(flags))
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isAbstract"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isAbstract"), BooleanValue.FALSE);		
		if(Flags.isStrictfp(flags))
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isStrictfp"), BooleanValue.TRUE);
		else
			api.setObjectAttribute(memberMObject, api.attributeByName(memberMObject, "isStrictfp"), BooleanValue.FALSE);
	}
	

	/**
	 * Creates default meta-objects for external PackageFragments, TypeRoots and basic Types.
	 */
	private static void createDefaultObjects() {
		MObject basicPackageMObject = api.createObject("BasicTypes", "PackageFragment");
		api.setObjectAttribute(basicPackageMObject, api.attributeByName(basicPackageMObject, "name"), new StringValue("BasicTypesPackage"));
		MObject externalPackageMObject = api.createObject("ExternalTypes", "PackageFragment");
		api.setObjectAttribute(externalPackageMObject, api.attributeByName(externalPackageMObject, "name"), new StringValue("ExternalTypesPackage"));
		
		MObject basicClassFileMObject = api.createObject("BasicTypesClassFile", "ClassFile");
		api.setObjectAttribute(basicClassFileMObject, api.attributeByName(basicClassFileMObject, "name"), new StringValue("BasicTypes"));
		MObject externalClassFileMObject = api.createObject("ExternalTypesClassFile", "ClassFile");
		api.setObjectAttribute(externalClassFileMObject, api.attributeByName(externalClassFileMObject, "name"), new StringValue("ExternalTypes"));

		api.createLink(api.associationByName("A_PackageFragment_TypeRoot"), Arrays.asList(basicPackageMObject, basicClassFileMObject));
		api.createLink(api.associationByName("A_PackageFragment_TypeRoot"), Arrays.asList(externalPackageMObject, externalClassFileMObject));

		String[] primitiveTypes = {"int","boolean","long","double","float","byte","short","char","void"};
		String[] primitiveTypeSignatures = {Signature.SIG_INT,Signature.SIG_BOOLEAN,Signature.SIG_LONG,
				Signature.SIG_DOUBLE,Signature.SIG_FLOAT,Signature.SIG_BYTE,Signature.SIG_SHORT,Signature.SIG_CHAR,Signature.SIG_VOID};
		for(int i = 0; i != primitiveTypes.length; ++i){
			MObject primMObject = api.createObject(primitiveTypes[i], "Type");
			api.setObjectAttribute(primMObject, api.attributeByName(primMObject, "name"), new StringValue(primitiveTypes[i]));
			api.setObjectAttribute(primMObject, api.attributeByName(primMObject, "handleIdentifier"), new StringValue(primitiveTypes[i]));
			api.setObjectAttribute(primMObject, api.attributeByName(primMObject, "javaType"), new EnumValue(api.enumByName("JavaType"), "Primitive"));
			api.createLink(api.associationByName("A_TypeRoot_Type"), Arrays.asList(basicClassFileMObject, primMObject));
			allTypeObjects.put(primitiveTypes[i], primMObject);
			allTypeObjects.put(primitiveTypeSignatures[i], primMObject);
		}
		
		MObject stringMObject = api.createObject("String", "Type");
		api.setObjectAttribute(stringMObject, api.attributeByName(stringMObject, "name"), new StringValue("String"));
		api.setObjectAttribute(stringMObject, api.attributeByName(stringMObject, "javaType"), new EnumValue(api.enumByName("JavaType"), "ClassType"));
		api.createLink(api.associationByName("A_TypeRoot_Type"), Arrays.asList(basicClassFileMObject, stringMObject));
	}
}
