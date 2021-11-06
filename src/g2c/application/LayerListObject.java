package g2c.application;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JCheckBox;

import g2c.gerber.loader.GerberLoader;

public class LayerListObject extends JCheckBox{
	GerberLoader gerber;
	String name = "layer";
	Color color = Color.pink;
	
	public LayerListObject(GerberLoader gerber, Color c, String name) {
		color = c;
		this.gerber = gerber;
		this.name = name;
		gerber.readFile();
		gerber.renderImage(c);
	}
	
	public LayerListObject(String name) {
		this.name = name;
	}
	
	public BufferedImage getImage() {
		return gerber.getImage();
	}
	
	public BufferedImage getCorrectedImage() {
		return gerber.getCorrectedImage();
	}
	
	public void setColor(Color c) {
		color = c;
		gerber.renderImage(c);
	}
	
	public String toString() {
		return name;
	}
	
	public String getText() {
		return name;
	}

	public void forceRender() {
		gerber.renderImage();
	}

	public Color getColor() {
		return color;
	}

	public String getName() {
		return name;
	}

	public void setName(String newName) {
		this.name = newName;
	}

	public void applyCorrection(double xOffset, double yOffset, int angle) {
		gerber.applyCorrection(xOffset, yOffset, angle);
	}
}
