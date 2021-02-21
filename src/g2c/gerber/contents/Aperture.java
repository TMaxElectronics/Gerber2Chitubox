package g2c.gerber.contents;

import java.util.LinkedList;
import java.util.List;

public class Aperture {
	public int           type;
	public List<Double>  parms = new LinkedList<>();

    public Aperture (int type) {
    	this.type = type;
    }

    public void addParm (double parm) {
    	parms.add(parm);
    }
}