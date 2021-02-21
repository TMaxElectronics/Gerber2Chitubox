package g2c.gerber.loader;

import java.awt.Rectangle;
import java.awt.Shape;

public class PositionManager {
	private static Rectangle.Double bounds = new Rectangle.Double();    // Computed bounding box for PCB layer
	private static double minX = 100000.0, minY = 100000.0;
	
	public static void reset() {
		bounds = new Rectangle.Double(); 
		minX = 100000.0; 
		minY = 100000.0;
	}
	
	public static void registerShape(Shape shape) {
		if(shape.getBounds2D().getMinX() < minX) minX = shape.getBounds2D().getMinX();
		if(shape.getBounds2D().getMinY() < minY) minY = shape.getBounds2D().getMinY();
		bounds.add(shape.getBounds2D());
	}
	
	public static double getMinX() {
		return minX;
	}
	
	public static double getHeight() {
		return bounds.getHeight();
	}
	
	public static double getMinY() {
		return minY;
	}
}
