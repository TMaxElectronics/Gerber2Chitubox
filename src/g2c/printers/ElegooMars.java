package g2c.printers;

import java.awt.Dimension;
import java.awt.Rectangle;

public class ElegooMars implements Printer {

	@Override
	public Dimension getScreenResolution() {
		Dimension ret = new Dimension();
		ret.setSize(2560, 1440);
		return ret;
	}
	
	@Override
	public Dimension getBezelMargin() {
		Dimension ret = new Dimension();
		ret.setSize(35, 35);
		return ret;
	}

	@Override
	public Dimension getScreenSizeMM() {
		Dimension ret = new Dimension();
		ret.setSize(120.96, 68.04);
		return ret;
	}

	@Override
	public Dimension getScreenPPI() {
		Dimension res = getScreenResolution();
		Dimension siz = getScreenSizeMM();
		Dimension ret = new Dimension();
		
		ret.setSize((res.getWidth() * 25.4) / siz.getWidth(), (res.getHeight() * 25.4) / siz.getHeight());
		return ret;
	}

	@Override
	public double getBedXmm() {
		return getScreenSizeMM().getWidth();
	}

	@Override
	public double getBedYmm() {
		return getScreenSizeMM().getHeight();
	}

	@Override
	public double getBedZmm() {
		return 150.0;
	}

	@Override
	public String getName() {
		return "Elegoo Mars";
	}
}
