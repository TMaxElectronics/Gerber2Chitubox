package g2c.gerber.loader;

import java.awt.Color;

public class GerberExtensionHandler {
	public static Color getDefaultLayerColor(String extension) {
		switch (extension.toLowerCase()) {
		case ".gbl": 
			return Color.blue;
		case ".gtl": 
			return Color.red;
		case ".gbs": 
			return new Color(0xff00ff);
		case ".gts": 
			return new Color(0x800080);
		case ".gbo": 
			return new Color(0x808000);
		case ".gto": 
			return new Color(0xFFFF00);
		case ".gbp": 
			return new Color(0x800000);
		case ".gtp": 
			return new Color(0x808080);
		case ".g1": 
			return new Color(0xBC8E00);
		case ".g2": 
			return new Color(0x70DBFA);
		case ".gm8": //altium outline
		case ".gko": 
			return new Color(0xFF9900);
		case ".xln": 
		case ".txt": 
			return new Color(0xC0C0C0);
		case ".gm9": 
			return new Color(0x008000);
		}
		return Color.CYAN;
	}

	public static String getDefaultLayerName(String extension) {
		switch (extension.toLowerCase()) {
		case ".gbl": 
			return "Copper bottom";
		case ".gtl": 
			return "Copper top";
		case ".gbs": 
			return "Soldermask bottom";
		case ".gts": 
			return "Soldermask top";
		case ".gbo": 
			return "Silkscreen bottom";
		case ".gto": 
			return "Silkscreen top";
		case ".gbp": 
			return "Paste bottom";
		case ".gtp": 
			return "Paste top";
		case ".g1": 
			return "Copper inner 1";
		case ".g2": 
			return "Copper inner 2";
		case ".g3": 
			return "Copper inner 3";
		case ".g4": 
			return "Copper inner 4";
		case ".gm8": //altium outline
		case ".gko": 
			return "Outline";
		case ".xln": 
		case ".txt": 
			return "Drills";
		case ".gm9": 
			return "Milling";
		}
		return "layer";
	}
}
