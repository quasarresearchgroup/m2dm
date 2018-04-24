package org.quasar.m2dm;

import org.quasar.juse.api.JUSE_ProgramingFacade;
import org.quasar.juse.api.implementation.ProgramingFacade;

import org.tzi.use.uml.sys.MObject;


/***********************************************************
 * @author fba 24 de Mar de 2012
 * 
 ***********************************************************/
public class FLAME
{
	private static String	USE_BASE_DIRECTORY	= "D:/Google Drive/EclipseWorkspace/use-5.0.0";
	
	private static String	WORKING_DIRECTORY	= "metamodels\\UML1.3";
	
//	 private static String WORKING_DIRECTORY = "D:\\Google Drive\\TOPICS\\_ModelDrivenEngineering\\OCL&METRICS\\MOOD_FLAME";
	
	private static String METAMODEL_FILE = "UML13_v14.use";

	private static String SOIL_FILE = "Navio2013.cmd";

	private static JUSE_ProgramingFacade api;

	/***********************************************************
	 * @param args
	 * @throws InterruptedException
	 ***********************************************************/
	public static void main(String[] args) throws InterruptedException
	{
		 loadFLAME(args);		
	}


	/***********************************************************
	* 
	***********************************************************/
	static void loadFLAME(String[] args)
	{
		api = new ProgramingFacade();

		api.initialize(args, USE_BASE_DIRECTORY, WORKING_DIRECTORY);

		api.compileSpecification(METAMODEL_FILE, true);

//		api.command("check");

		api.readSOIL(WORKING_DIRECTORY, SOIL_FILE, false);
		
//		api.command("info vars");

//		 api.command("info state");

		// api.createShell();
		 
	//	 System.out.println(api.oclEvaluator("MMClass.allInstances"));
		 
		 System.out.println(api.oclEvaluator("MMClass.allInstances"));
		 
		 for (MObject instance: api.allInstances("MMClass"))
			 outputMetricsLine(instance, "CHIN()", "DESN()", "PARN()", "ASCN()", 
					 "NAN()", "DAN()", "IAN()", "OAN()", "AAN()", "NON()", "DON()",	"ION()", "OON()", "AON()");	
		 
		 System.out.println("________________________________________________________________");

		 System.out.println(api.oclEvaluator("Package.allInstances"));
		 
		 for (MObject instance: api.allInstances("Package"))
			 outputMetricsLine(instance, "AIF()", "OIF()", "IIF()", "AHF()", "OHF()", "AHEF()", "OHEF()");
	}
	
	
	
	static void outputMetricsLine(MObject target, String... metrics)
	{
		System.out.print(target.toString() + "\t");
		for (String metric: metrics)
			System.out.print(api.oclEvaluator(target.toString() + "." + metric) + "\t");
		System.out.println();
	}
	
	
	
	
}
