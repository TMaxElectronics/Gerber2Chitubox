package g2c.printers;

import java.awt.Dimension;

public interface Printer {
	public Dimension getScreenResolution();
	public Dimension getScreenSizeMM();
	
	public double getBedXmm();
	public double getBedYmm();
	public double getBedZmm();
	Dimension getScreenPPI();
	Dimension getBezelMargin();
	String getName();;
}
