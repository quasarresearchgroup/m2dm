package ejm2.views;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import ejm2.ui.EJM2ActionGroup;

public class EJM2View extends ViewPart {

	private Label label, label2, label3;
	private Combo projectSelector;
	private Text input, output;
	private EJM2ActionGroup ag;
	private Button loadUSE, loadEJMM;
	

	@Override
	public void createPartControl(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		c.setLayout(new GridLayout(1, false));
		
		label = new Label(c, SWT.NONE);
		label.setText("Press the Run JM2Loader button to instantiate a Java project's metamodel");
		
		Composite c1 = new Composite(c, SWT.NONE);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		c1.setLayoutData(gridData);
		c1.setLayout(new GridLayout(2, false));
		
		label2 = new Label(c1, SWT.NONE);
		label2.setText("Select the Java project to load");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		label2.setLayoutData(gridData);
		
		loadUSE = new Button(c1, SWT.NONE);
		loadUSE.setText("Select USE directory");
		loadUSE.setToolTipText("Select the folder where USE is located");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		loadUSE.setLayoutData(gridData);
		
		projectSelector = new Combo(c1, SWT.READ_ONLY | SWT.DROP_DOWN);
		
		loadEJMM = new Button(c1, SWT.NONE);
		loadEJMM.setText("Select EJMM path");
		loadEJMM.setToolTipText("Select the location of the EJMM .use file");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		loadEJMM.setLayoutData(gridData);
		
		label3 = new Label(c, SWT.NONE);
		label3.setText("Insert OCL queries here:");
		
		input = new Text(c, SWT.SINGLE | SWT.BORDER | SWT.WRAP);
		input.setEditable(false);
		input.setEnabled(false);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		input.setLayoutData(gridData);
		
		output = new Text(c, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL);
		output.setEditable(false);
		Device device = Display.getCurrent();
		output.setBackground(new Color(device, 255,255,255));
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessVerticalSpace = true;
		output.setLayoutData(gridData);
		
		ag = new EJM2ActionGroup(this);
		IActionBars actionBars = getViewSite().getActionBars();
		ag.fillActionBars(actionBars); 
	}

	public void setLabelText(String s){
		label.setText(s);
	}
	
	public void setLabel2Text(String s){
		label2.setText(s);
	}
	
	public Combo getProjectSelector(){
		return projectSelector;
	}
	
	public Text getOutputEditor(){
		return output;
	}
	
	public Text getInputEditor(){
		return input;
	}
	
	public Button getLoadUSE() {
		return loadUSE;
	}

	public Button getLoadEJMM() {
		return loadEJMM;
	}
	
	@Override
	public void setFocus() {
	}

}
