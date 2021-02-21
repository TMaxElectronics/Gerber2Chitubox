package g2c.gerber.contents;
import java.awt.Shape;

public class DrawItem {
	public boolean   drawCopper;
	public Shape     shape;
	
	public DrawItem (Shape shape, boolean drawCopper) {
		this.shape = shape;
		this.drawCopper = drawCopper;
	}
}