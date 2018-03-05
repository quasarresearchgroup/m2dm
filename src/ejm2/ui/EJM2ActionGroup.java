package ejm2.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionGroup;
import org.quasar.juse.api.JUSE_ProgramingFacade;

import ejm2.tools.JM2Loader;
import ejm2.views.EJM2View;

public class EJM2ActionGroup extends ActionGroup{

	private JM2LoaderAction jm2la;
	private FileSelectingAction fsa;
	private EJM2View view;
	
	private String[] extraModelPaths;
	private String ejmmDirectory;
	private String ejmmFile;

	private String useLocation;
	
	public void setExtraModelPaths(String[] newModels){
		extraModelPaths = newModels;
	}
	
	public EJM2ActionGroup(EJM2View view){
		jm2la = new JM2LoaderAction();
		fsa = new FileSelectingAction();
		this.view = view;
		
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		String[] projectNames = new String[projects.length];
		for(int i = 0; i != projects.length; ++i)
			projectNames[i] = projects[i].getName();
		this.view.getProjectSelector().setItems(projectNames);
		
		view.getLoadUSE().addSelectionListener(new LoadUSEListener());
		view.getLoadEJMM().addSelectionListener(new LoadEJMMListener());
		
	}
	
	public void fillActionBars(IActionBars actionBars) {
        super.fillActionBars(actionBars);
        fillToolBar(actionBars.getToolBarManager());
        fillViewMenu(actionBars.getMenuManager());
    }

    void fillToolBar(IToolBarManager toolBar) {
        toolBar.removeAll();
        toolBar.add(fsa);
		toolBar.add(jm2la);
     }

    void fillViewMenu(IMenuManager menu) {
		menu.add(fsa);
		menu.add(jm2la);
        menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));        
    }

    public void fillContextMenu(IMenuManager menu) {
        super.fillContextMenu(menu);
    }
	
	public static IEditorPart getActiveEditor() {
		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page= window.getActivePage();
			if (page != null) {
				return page.getActiveEditor();
			}
		}
		return null;
	}
	
	class FileSelectingAction extends Action{
		public FileSelectingAction(){
			super("Load models");
			setToolTipText("Select extra model files");
		}
		
		public void run(){
			Shell shell = new Shell (Display.getCurrent());
			FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
			dialog.setFilterExtensions(new String [] {"*.use"});
			dialog.setFilterPath(null);
			dialog.open();
			String[] files = dialog.getFileNames();
			String filterPath = dialog.getFilterPath();
			
			for(int i = 0; i != files.length; ++i)
				files[i]=filterPath+"\\"+files[i];
			
			setExtraModelPaths(files);
		}
	}

	class JM2LoaderAction extends Action{
		public JM2LoaderAction(){
			super("Instantiate EJMM");
			setToolTipText("Load the selected project as a EJMM instance");
		}
		
		public void run(){
			try{
			if(!(useLocation == null || useLocation.isEmpty())){
				if(!(ejmmDirectory == null || ejmmDirectory.isEmpty()) && !(ejmmFile == null || ejmmFile.isEmpty())){
					
					JM2Loader.setUseDirectory(useLocation);
					JM2Loader.setModelDirectory(ejmmDirectory);
					JM2Loader.setModelFile(ejmmFile);
					
					Text input = view.getInputEditor();
					IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			
					int selection = view.getProjectSelector().getSelectionIndex();
					if(selection >= 0){
						IJavaProject project = JavaCore.create(projects[selection]);
						int severity = IMarker.SEVERITY_INFO;
						try {
							severity = project.getResource().findMaxProblemSeverity(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
							System.out.println("Severity: " + severity);
						} catch (CoreException e) {
							e.printStackTrace();
						}
						if(!(severity == IMarker.SEVERITY_ERROR)){
							view.getOutputEditor().setText("");
							view.getOutputEditor().redraw();
							JUSE_ProgramingFacade api = JM2Loader.loadEJMMfromProject(project);
							view.setLabel2Text("Processing finished.");
							input.addTraverseListener(new InputListener(api));
							input.setEditable(true);
							input.setEnabled(true);
						}else
							view.setLabel2Text("Project has errors, cannot load");
					}
				}else
					view.setLabel2Text("EJMM location undefined, please locate the EJMM file");

			}else
				view.setLabel2Text("USE location undefined, please locate the USE folder");
			}catch(Exception e){
				view.setLabel2Text("EJMM load failed.");
			}
		}
		
		class InputListener implements TraverseListener{

			JUSE_ProgramingFacade api;
			
			public InputListener(JUSE_ProgramingFacade api){
				this.api = api;
			}
			
			private String readOCLExpression(String expression){
				return api.oclEvaluator(expression).toString();
			}

			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.detail==SWT.TRAVERSE_RETURN){
					String input = view.getInputEditor().getText();
					String output = readOCLExpression(input);
					view.getOutputEditor().setText(output);
					view.getOutputEditor().redraw();
				}
			}
		}
	}
	
	class LoadUSEListener implements SelectionListener {

		@Override
		public void widgetSelected(SelectionEvent e) {
			Shell shell = new Shell (Display.getCurrent());
			DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN | SWT.MULTI);
			dialog.setFilterPath(null);
			useLocation = dialog.open();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		
	}
	
	class LoadEJMMListener implements SelectionListener {

		@Override
		public void widgetSelected(SelectionEvent e) {
			Shell shell = new Shell (Display.getCurrent());
			FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
			dialog.setFilterExtensions(new String [] {"*.use"});
			dialog.setFilterPath(null);
			dialog.open();
			ejmmDirectory = dialog.getFilterPath();
			ejmmFile = dialog.getFileName();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		
	}
}




