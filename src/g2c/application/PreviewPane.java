package g2c.application;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;

public class PreviewPane extends JPanel{
	JList<LayerListObject> list;
	DefaultListModel<LayerListObject> objects;
	LayerListObject layer;
	
	boolean singleLayer = false;
	
	public PreviewPane(JList list) {
		this.list = list;
		objects = (DefaultListModel<LayerListObject>) list.getModel();
	}
	
	public PreviewPane(LayerListObject layer) {
		this.layer = layer;
		singleLayer = true;
	}
	

	
	@Override
	public void paint (Graphics g) {
		double scaleX = (double) (getWidth() - 6) / Main.currPrinter.getScreenResolution().getWidth();
		double scaleY = (double) (getHeight() - 6) / Main.currPrinter.getScreenResolution().getHeight();
		double scale = Math.min(scaleX, scaleY);
		int width = (int) (scale * Main.currPrinter.getScreenResolution().getWidth());
		int height = (int) (scale * Main.currPrinter.getScreenResolution().getHeight());
		
		if(singleLayer) {
			g.drawImage(layer.getImage(), 3, 3, width, height, null);
			
		}else {
			
			if(list.getSelectedValue() == null) {
				for(int i = 0; i < list.getModel().getSize(); i++) {
					System.out.println("now");
					g.drawImage(objects.getElementAt(i).getImage(), 3, 3, width, height, null);
				}
			}else {
				g.drawImage(list.getSelectedValue().getImage(), 3, 3, width, height, null);
			}
		}
		if(objects.size() > 0) {
			((Graphics2D) g).setColor(Color.gray);
	  		((Graphics2D) g).setStroke(new BasicStroke(2));
	  		((Graphics2D) g).drawRect(3, 3, width, height);
		}
	}

}
