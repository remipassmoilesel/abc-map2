package org.abcmap.gui.iegroup.docks;

import java.util.ArrayList;

import abcmap.gui.GuiIcons;
import abcmap.gui.ie.InteractionElement;
import abcmap.gui.iegroup.InteractionElementGroup;
import org.abcmap.gui.GuiIcons;
import org.abcmap.gui.ie.InteractionElement;
import org.abcmap.gui.ie.InteractionElementGroup;

public class GroupPlugins extends InteractionElementGroup {

	public GroupPlugins() {
		label = "Modules d'extension";
		blockIcon = GuiIcons.GROUP_PLUGINS;

		// ajouter tous les plugins disponibles
		ArrayList<InteractionElement> plgs = InteractionElement.getAllAvailablesPlugins();

		for (InteractionElement ie : plgs) {
			addInteractionElement(ie);
		}

	}

}
