package org.abcmap.gui.iegroup.docks;

import org.abcmap.gui.GuiIcons;
import org.abcmap.gui.ie.InteractionElementGroup;
import org.abcmap.gui.toProcess.gui.ie.export.*;

public class GroupExport extends InteractionElementGroup {

	public GroupExport() {
		label = "Export et impression";
		blockIcon = GuiIcons.GROUP_EXPORT;

		addInteractionElement(new PrintLayouts());

		addSeparator();
		addInteractionElement(new PngMapExport());
		addInteractionElement(new PngLayoutExport());
		addInteractionElement(new ShpExport());

		addSeparator();
		addInteractionElement(new GpxExport());
		addInteractionElement(new KmlExport());

	}

}
