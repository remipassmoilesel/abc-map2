package abcmap.gui.wizards;

import javax.swing.JButton;
import javax.swing.JPanel;

public class ExportWorkWizard extends Wizard {

	public ExportWorkWizard( ) {
		super();
		
		title = "Export de carte";
		setDescription("Cet assistant vous permet d'exporter une carte ou des données aux format"
				+ "GPX, SHP, PDF, ...");


	}

}