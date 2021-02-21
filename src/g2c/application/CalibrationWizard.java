package g2c.application;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import photon.file.PhotonFile;
import photon.file.parts.PhotonFileLayer;
import photon.file.parts.PhotonFilePreview;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JSpinner;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JCheckBox;

public class CalibrationWizard extends JFrame {
	private static Preferences    prefs = Preferences.userRoot().node(CalibrationWizard.class.getName());
	private JPanel contentPane;
	BufferedImage preview;
	JSpinner fromSpinner = new JSpinner();
	JSpinner toSpinner = new JSpinner();
	JSpinner stepSpinner = new JSpinner();
	JFrame frame;
	JPanel panel = new JPanel();
	JCheckBox invertBox = new JCheckBox("invert");

	/**
	 * Create the frame.
	 */
	public CalibrationWizard() {
		frame = this;
		preview = new BufferedImage(Main.currPrinter.getScreenResolution().width, Main.currPrinter.getScreenResolution().height, BufferedImage.TYPE_INT_ARGB);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1008, 640);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JButton btnNewButton = new JButton("Export");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser ch = new JFileChooser();
				ch.setMultiSelectionEnabled(true);
				ch.setSelectedFile(new File(prefs.get("default.dir", "/")));
				ch.showDialog(frame, "ye");
				File exportFile = ch.getSelectedFile();
				if(exportFile != null) {
					prefs.put("default.dir", exportFile.getAbsolutePath());
					generateFile(exportFile);
				}
			}
		});
		btnNewButton.setBounds(893, 569, 89, 23);
		contentPane.add(btnNewButton);
		
		panel.setBounds(10, 42, 972, 516);
		contentPane.add(panel);
		
		fromSpinner.setModel(new SpinnerNumberModel(10, 10, 10000, 10));
		fromSpinner.setBounds(10, 11, 89, 20);
		fromSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				drawPattern();
			}
		});
		contentPane.add(fromSpinner);

		toSpinner.setModel(new SpinnerNumberModel(10, 10, 10000, 10));
		toSpinner.setBounds(148, 11, 89, 20);
		toSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				drawPattern();
			}
		});
		contentPane.add(toSpinner);
		
		JLabel lblNewLabel = new JLabel("to");
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setBounds(109, 14, 29, 14);
		contentPane.add(lblNewLabel);
		
		JLabel lblIn = new JLabel("in");
		lblIn.setHorizontalAlignment(SwingConstants.CENTER);
		lblIn.setBounds(247, 14, 29, 14);
		contentPane.add(lblIn);
		stepSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				drawPattern();
			}
		});
		stepSpinner.setModel(new SpinnerNumberModel(1, 1, 8, 1));
		
		stepSpinner.setBounds(286, 11, 89, 20);
		contentPane.add(stepSpinner);
		
		JLabel lblSteps = new JLabel("Steps");
		lblSteps.setHorizontalAlignment(SwingConstants.CENTER);
		lblSteps.setBounds(385, 14, 62, 14);
		contentPane.add(lblSteps);
		
		invertBox.setBounds(790, 569, 97, 23);
		contentPane.add(invertBox);
	}
	
	private void drawPattern() {
		int segCount = (int) stepSpinner.getValue();
		int segWidth = preview.getWidth() / segCount;
		preview = new BufferedImage(Main.currPrinter.getScreenResolution().width, Main.currPrinter.getScreenResolution().height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = preview.createGraphics();
		g.setColor(Color.BLACK);
		double secIncr = (double) ((int) toSpinner.getValue() - (int) fromSpinner.getValue()) / (double) (segCount - 1);
		
		g.setStroke(new BasicStroke(3));
		g.setFont(new Font("Arial", Font.BOLD, 175));
		
		for(int i = 0; i < segCount; i++) {
			int baseX = i * segWidth;
			g.drawRect(baseX + 1, 1, segWidth - 1, preview.getHeight() - 1);
			
			String text = "Test @ " + Integer.toString((int) fromSpinner.getValue() + (int) ((double) i * secIncr)) + " sec";
			AffineTransform orig = g.getTransform();
			g.rotate(Math.PI/2);
			int stringHeight = (int) g.getFontMetrics().getStringBounds(text, g).getHeight();
			int stringWidth = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
			g.drawString(text, preview.getHeight() / 2 - stringWidth / 2, - baseX - segWidth / 2 + stringHeight / 2 - 25);
			g.setTransform(orig);
			
		}
		
		g.dispose();
		g = (Graphics2D) panel.getGraphics();
		g.setColor(panel.getBackground());
		g.fillRect(0, 0, panel.getWidth(), panel.getHeight());
		
		if(invertBox.isSelected()) {
			BufferedImage temp = new BufferedImage(preview.getWidth(), preview.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for (int x = 0; x < preview.getWidth(); x++) {
	            for (int y = 0; y < preview.getHeight(); y++) {
	                int rgba = preview.getRGB(x, y);
	                Color col = new Color(rgba, true);
	                col = new Color(255 - col.getRed(),
	                                255 - col.getGreen(),
	                                255 - col.getBlue());
	                temp.setRGB(x, y, col.getRGB());
	            }
	        }
			g.drawImage(temp, 10, 30, 1024, 576, null);
		}else {
			g.drawImage(preview, 10, 30, 1024, 576, null);
		}
        g.setColor(Color.RED);
        g.drawArc(10, 10, 80, 80, 184, 149);
        g.setColor(Color.BLUE);
        g.drawArc(100, 10, 80, 80, -10, 180);
        
		g.dispose();
	}
	
	void generateFile(File f) {
		int segCount = (int) stepSpinner.getValue();
		int segWidth = preview.getWidth() / segCount;
		double secIncr = (double) ((int) toSpinner.getValue() - (int) fromSpinner.getValue()) / (double) (segCount - 1);

		PhotonFile exp = new PhotonFile(Main.currPrinter);
		
		for (int currLayer = 0; currLayer < segCount; currLayer++) {
			BufferedImage layerImage = new BufferedImage(Main.currPrinter.getScreenResolution().width, Main.currPrinter.getScreenResolution().height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = layerImage.createGraphics();
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke(3));
			g.setFont(new Font("Arial", Font.BOLD, 175));
			
			for(int i = 0; i < segCount - currLayer; i++) {
				int baseX = i * segWidth;
				g.drawRect(baseX + 1, 1, segWidth - 1, preview.getHeight() - 1);
				
				String text = "Test @ " + Integer.toString((int) toSpinner.getValue() - (int) ((double) i * secIncr)) + " sec";
				AffineTransform orig = g.getTransform();
				g.rotate(Math.PI/2);
				int stringHeight = (int) g.getFontMetrics().getStringBounds(text, g).getHeight();
				int stringWidth = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
				g.drawString(text, preview.getHeight() / 2 - stringWidth / 2, - baseX - segWidth / 2 + stringHeight / 2 - 25);
				g.setTransform(orig);
				
			}
			g.dispose();
			
			PhotonFileLayer layer = new PhotonFileLayer(layerImage, invertBox.isSelected(), exp.getPhotonFileHeader());
			layer.setLayerExposure((float) ((currLayer == 0) ? ((int) fromSpinner.getValue()) : secIncr));
			exp.addLayer(layer);
			try {
				exp.calculate(currLayer);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		exp.getPhotonFileHeader().setExposureBottomTimeSeconds((float) (int) fromSpinner.getValue());
		exp.getPhotonFileHeader().setExposureTimeSeconds((float) secIncr);
		exp.getPhotonFileHeader().setBottomLayers(1);
		exp.adjustLayerSettings();
		
		PhotonFilePreview newPreview = new PhotonFilePreview(preview);
		exp.setPreviewOne(newPreview);	exp.setPreviewTwo(newPreview);
		try {
			exp.saveFile(f);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
