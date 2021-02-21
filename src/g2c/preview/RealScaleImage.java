package g2c.preview;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import g2c.preview.GerberColors.PixelValue;

public class RealScaleImage {
	double ppmX, ppmY;
	boolean renderIsUpToDate = false;
	PixelValue[][] renderedImageData;
	Color background = Color.black, foreground = Color.red;
	
	public RealScaleImage(double ppmX, double ppmY, int width, int heigt) {
		this.ppmX = ppmX; this.ppmY = ppmY;
		renderedImageData = new PixelValue[heigt][width];
	}
	
	public BufferedImage getRenderedImage() {
		// convert byte[] back to a BufferedImage
        BufferedImage newBi = new BufferedImage(renderedImageData[0].length, renderedImageData.length, BufferedImage.TYPE_3BYTE_BGR);
        int x,y;
        for(x = 0; x < newBi.getWidth(); x ++) {
            for(y = 0; y < newBi.getHeight(); y ++) {
            	
            	switch (renderedImageData[y][x]) {
				case blank:
	                newBi.setRGB(x, y, GerberColors.Blank.getRGB());
					break;
				case outline:
	                newBi.setRGB(x, y, GerberColors.Outline.getRGB());
					break;
				case primitive:
	                newBi.setRGB(x, y, GerberColors.Primitive.getRGB());
					break;
				case error:
	                newBi.setRGB(x, y, GerberColors.Error.getRGB());
					break;
				default:
					System.out.println("Unexpected value: " + renderedImageData[y][x]);
				}
            }
        }
        return newBi;
	}
}
