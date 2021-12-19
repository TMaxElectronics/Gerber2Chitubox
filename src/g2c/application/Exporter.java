package g2c.application;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.JPanel;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

import g2c.gerber.loader.GerberLoader;
import photon.file.PhotonFile;
import photon.file.parts.PhotonFileLayer;
import photon.file.parts.PhotonFilePreview;
import photon.file.parts.PhotonLayer;
import photon.file.ui.PhotonLayerImage;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.DefaultComboBoxModel;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class Exporter {
	private static Preferences    prefs = Preferences.userRoot().node(Exporter.class.getName());
	JFrame frame;
	LayerListObject layer;
	JSpinner expTime = new JSpinner();
	PhotonFile exp = new PhotonFile(Main.currPrinter);
	PhotonLayerImage panel;
	JCheckBox mirrorLayer = new JCheckBox("Mirror Layer");
	JComboBox typeBox = new JComboBox();
	JComboBox<LayerListObject> drillOLBox = new JComboBox<LayerListObject>();
	JCheckBox chckbxOverrideHoleDiameter = new JCheckBox("Override Hole diameter");
	JSpinner newHoleDiam = new JSpinner();
	JLabel newHoleDiamMMLBL = new JLabel("mm");
	JSpinner rotation = new JSpinner();
	JSpinner offsetY = new JSpinner();
	JSpinner offsetX = new JSpinner();
	
	/**
	 * Initialize the contents of the frame.
	 */
	public Exporter(LayerListObject layer) {
		this.layer = layer;
		frame = new JFrame();
		frame.setBounds(100, 100, 824, 467);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.UNRELATED_GAP_COLSPEC,
				ColumnSpec.decode("163px"),
				ColumnSpec.decode("100px:grow(5)"),
				ColumnSpec.decode("174px"),
				FormSpecs.UNRELATED_GAP_COLSPEC,
				ColumnSpec.decode("43px"),
				FormSpecs.UNRELATED_GAP_COLSPEC,
				ColumnSpec.decode("24px"),
				FormSpecs.UNRELATED_GAP_COLSPEC,
				ColumnSpec.decode("16px"),
				FormSpecs.UNRELATED_GAP_COLSPEC,
				ColumnSpec.decode("35px:grow"),
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("17px"),
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("43px"),
				FormSpecs.UNRELATED_GAP_COLSPEC,
				ColumnSpec.decode("31px"),
				FormSpecs.UNRELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormSpecs.UNRELATED_GAP_ROWSPEC,
				RowSpec.decode("23px"),
				FormSpecs.UNRELATED_GAP_ROWSPEC,
				RowSpec.decode("22px"),
				FormSpecs.UNRELATED_GAP_ROWSPEC,
				RowSpec.decode("22px"),
				FormSpecs.UNRELATED_GAP_ROWSPEC,
				RowSpec.decode("23px"),
				FormSpecs.UNRELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.UNRELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				RowSpec.decode("161px:grow"),
				RowSpec.decode("22px"),
				FormSpecs.UNRELATED_GAP_ROWSPEC,
				RowSpec.decode("23px"),
				FormSpecs.UNRELATED_GAP_ROWSPEC,}));
		
		JLabel lblNewLabel = new JLabel("Exposure Time:");
		frame.getContentPane().add(lblNewLabel, "6, 4, 7, 1, fill, fill");
		expTime.setModel(new SpinnerNumberModel(250, 10, 1000, 10));
		
		frame.getContentPane().add(expTime, "14, 4, 3, 1, fill, fill");
		
		
		JButton btnSave = new JButton("Save As");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser ch = new JFileChooser();
				ch.setMultiSelectionEnabled(true);
				ch.setSelectedFile(new File(prefs.get("default.dir", "/")));
				ch.showDialog(frame, "ye");
				File exportFile = ch.getSelectedFile();
				if(exportFile != null) {
					prefs.put("default.dir", exportFile.getAbsolutePath());
					exportFile(exportFile);
				}
			}
		});
		
		JLabel lblNewLabel_1_1_2 = new JLabel("Offsets (mm):");
		frame.getContentPane().add(lblNewLabel_1_1_2, "6, 10, 3, 1");
		
		JLabel lblNewLabel_1_1_3 = new JLabel("x");
		lblNewLabel_1_1_3.setHorizontalAlignment(SwingConstants.TRAILING);
		frame.getContentPane().add(lblNewLabel_1_1_3, "10, 10");
		offsetX.setModel(new SpinnerNumberModel(new Double(1.45), new Double(0), null, new Double(1)));
		offsetX.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updatePreview();
			}
		});

		frame.getContentPane().add(offsetX, "12, 10");
		
		JLabel lblNewLabel_1_1_3_1 = new JLabel("y");
		lblNewLabel_1_1_3_1.setHorizontalAlignment(SwingConstants.TRAILING);
		frame.getContentPane().add(lblNewLabel_1_1_3_1, "14, 10");
		offsetY.setModel(new SpinnerNumberModel(new Double(2.1), new Double(0), null, new Double(1)));
		offsetY.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updatePreview();
			}
		});

		frame.getContentPane().add(offsetY, "16, 10");
		
		JLabel lblNewLabel_1_1_2_1 = new JLabel("Rotation (°):");
		frame.getContentPane().add(lblNewLabel_1_1_2_1, "6, 12, 3, 1");
		rotation.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updatePreview();
			}
		});
		
		frame.getContentPane().add(rotation, "10, 12, 7, 1");
		frame.getContentPane().add(btnSave, "4, 16, fill, top");
		
		panel = new PhotonLayerImage(layer.getImage().getWidth(), layer.getImage().getHeight());
		
		frame.getContentPane().add(panel, "2, 2, 3, 12, fill, top");
		
		JButton btnCancel = new JButton("Cancel");
		frame.getContentPane().add(btnCancel, "2, 16, fill, top");
		
		JLabel lblNewLabel_1 = new JLabel("Preset");
		frame.getContentPane().add(lblNewLabel_1, "6, 2, fill, fill");
		
		JComboBox comboBox = new JComboBox();
		comboBox.setEditable(true);
		frame.getContentPane().add(comboBox, "8, 2, 7, 1, fill, fill");
		mirrorLayer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updatePreview();
			}
		});
		frame.getContentPane().add(mirrorLayer, "12, 8, 7, 1, fill, top");
		
		JLabel lblNewLabel_1_1 = new JLabel("Material Type");
		frame.getContentPane().add(lblNewLabel_1_1, "6, 6, 3, 1, fill, fill");
		typeBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updatePreview();
			}
		});
		
		typeBox.setModel(new DefaultComboBoxModel(new String[] {"Positive", "Negative"}));
		frame.getContentPane().add(typeBox, "10, 6, 9, 1, fill, fill");
		
		DefaultComboBoxModel<LayerListObject> temp = new DefaultComboBoxModel<LayerListObject>();
		drillOLBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updatePreview();
			}
		});
		drillOLBox.setModel(temp);
		temp.addElement(new LayerListObject("None"));
		for(int i = 0; i< Main.frame.list.getModel().getSize();i++){
			temp.addElement(Main.frame.list.getModel().getElementAt(i));
        }
		frame.getContentPane().add(drillOLBox, "12, 14, 7, 1, fill, top");
		
		JLabel lblNewLabel_1_1_1 = new JLabel("Add drill guide");
		frame.getContentPane().add(lblNewLabel_1_1_1, "6, 14, 5, 1, fill, fill");
		chckbxOverrideHoleDiameter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updatePreview();
				newHoleDiam.setEnabled(chckbxOverrideHoleDiameter.isSelected());
				newHoleDiamMMLBL.setEnabled(chckbxOverrideHoleDiameter.isSelected());
			}
		});
		frame.getContentPane().add(chckbxOverrideHoleDiameter, "6, 16, 7, 1, fill, top");
		newHoleDiam.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updatePreview();
			}
		});
		newHoleDiam.setModel(new SpinnerNumberModel(0.5, 0.1, 100.0, 0.1));
		
		
		newHoleDiam.setEnabled(false);
		frame.getContentPane().add(newHoleDiam, "14, 16, 3, 1, fill, center");
		
		newHoleDiamMMLBL.setEnabled(false);
		frame.getContentPane().add(newHoleDiamMMLBL, "18, 16, fill, fill");
		
		JButton btnNewButton = new JButton("+");
		frame.getContentPane().add(btnNewButton, "16, 2, 3, 1, right, top");
		
		JButton btnNewButton_1 = new JButton("-");
		frame.getContentPane().add(btnNewButton_1, "16, 2, fill, top");
		
		JLabel lblS = new JLabel("s");
		frame.getContentPane().add(lblS, "18, 4, fill, fill");
		updatePreview();
	}
	
	public void updatePreview() {
		layer.applyCorrection((double) offsetX.getValue(), (double) offsetY.getValue(), (int) rotation.getValue());
		if(drillOLBox.getSelectedIndex() > 0) {
			Main.frame.list.getModel().getElementAt(drillOLBox.getSelectedIndex() - 1).gerber.applyCorrection((double) offsetX.getValue(), (double) offsetY.getValue(), (int) rotation.getValue());
		}
		setLayer(layer.getCorrectedImage());
	}
	
	public void setLayer(BufferedImage data) {
		exp = new PhotonFile(Main.currPrinter);
		BufferedImage flipped = deepCopyImage(data);
		AffineTransform flipper = new AffineTransform();
		
		if(drillOLBox.getSelectedIndex() > 0) {
			double newHoleSize = (double) newHoleDiam.getValue() / 25.4;	//gerber lib talks imperial :(
			flipped = Main.frame.list.getModel().getElementAt(drillOLBox.getSelectedIndex() - 1).gerber.overlayDrills(flipped, chckbxOverrideHoleDiameter.isSelected() ? newHoleSize : -1, Main.currPrinter.getScreenPPI().getWidth(), Main.currPrinter.getScreenPPI().getHeight(), Main.currPrinter.getScreenResolution().width, Main.currPrinter.getScreenResolution().height, true);
		}
		
		if(mirrorLayer.isSelected()) {
			flipper.scale(-1, 1); flipper.translate(-flipped.getWidth(), 0);
		}

		AffineTransformOp dolphin = new AffineTransformOp(flipper, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		flipped = dolphin.filter(flipped, null);
		boolean invert = typeBox.getSelectedIndex() == 0;
		
		BufferedImage out = new BufferedImage(data.getWidth(), data.getHeight(), data.getType());
		Graphics2D outDrawer = out.createGraphics();
		Dimension margin = Main.currPrinter.getBezelMargin();
		outDrawer.drawImage(flipped, (int) (mirrorLayer.isSelected() ? -margin.getWidth() : margin.getWidth()), (int) margin.getHeight(), null);
		outDrawer.dispose();
		
		PhotonFileLayer layer = new PhotonFileLayer(out, invert, exp.getPhotonFileHeader());
		exp.addLayer(layer);
		
		try {
			exp.calculate(0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		panel.drawLayer(exp.getLayer(0), 0);
		frame.update(frame.getGraphics());
	}
	
	private void exportFile(File f) {
		exp.setExposure((int) expTime.getValue());
		PhotonFilePreview newPreview = null;
		
		AffineTransform flipper = new AffineTransform();
		
		//flipper.translate((int) xMargin.getValue(), (int) yMargin.getValue());
		if(mirrorLayer.isSelected()) {
			flipper.scale(-1, 1); flipper.translate(flipper.getTranslateX() - layer.getImage().getWidth(), flipper.getTranslateY());
		}
		
		AffineTransformOp dolphin = new AffineTransformOp(flipper, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		
		newPreview = new PhotonFilePreview(dolphin.filter(layer.getImage(), null));
		exp.setPreviewOne(newPreview);	exp.setPreviewTwo(newPreview);
		try {
			exp.saveFile(f);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static BufferedImage deepCopyImage(BufferedImage bi) {
		 ColorModel cm = bi.getColorModel();
		 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		 WritableRaster raster = bi.copyData(null);
		 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}
}
