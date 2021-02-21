package g2c.application;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import g2c.gerber.loader.GerberExtensionHandler;
import g2c.gerber.loader.GerberLoader;
import g2c.preview.GerberColors;
import g2c.printers.ElegooMars;
import g2c.printers.Printer;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JToggleButton;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;

import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.prefs.Preferences;
import java.awt.event.ActionEvent;
import javax.swing.JList;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JMenuItem;

public class Main extends JFrame {
	private static Preferences    prefs = Preferences.userRoot().node(Main.class.getName());
	private JPanel contentPane;
	static Main frame;
	JList<LayerListObject> list = new JList<LayerListObject>();
	public static Printer currPrinter = new ElegooMars();
	private LayerSettings layerSettings = null;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					frame = new Main();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	/**
	 * Create the frame.
	 */
	public Main() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1171, 796);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnNewMenu = new JMenu("File");
		menuBar.add(mnNewMenu);
		
		JMenu mnNewMenu_1 = new JMenu("Tools");
		menuBar.add(mnNewMenu_1);
		
		JMenuItem mntmNewMenuItem = new JMenuItem("Calibration Wizard");
		mntmNewMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new CalibrationWizard().setVisible(true);
			}
		});
		mnNewMenu_1.add(mntmNewMenuItem);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == 2) list.clearSelection();
				if(e.getButton() == 3) {
					if(list.getSelectedIndex() != -1) {
						if(layerSettings != null) layerSettings.frame.dispose();
						layerSettings = new LayerSettings(list.getSelectedValue());
						
						layerSettings.frame.setVisible(true);
					}
				}
			}
		});
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				repaint();
			}
		});
		contentPane.setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.UNRELATED_GAP_COLSPEC,
				FormSpecs.MIN_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.MIN_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(100dlu;default)"),
				FormSpecs.UNRELATED_GAP_COLSPEC,
				FormSpecs.GLUE_COLSPEC,
				FormSpecs.UNRELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormSpecs.UNRELATED_GAP_ROWSPEC,
				FormSpecs.GLUE_ROWSPEC,
				FormSpecs.UNRELATED_GAP_ROWSPEC,
				RowSpec.decode("23px"),
				FormSpecs.UNRELATED_GAP_ROWSPEC,}));
		
		list.setModel(new DefaultListModel<>());
		contentPane.add(list, "2, 2, 5, 1, fill, fill");
		
		JButton btnNewButton = new JButton("+");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser ch = new JFileChooser();
				ch.setMultiSelectionEnabled(true);
				ch.setSelectedFile(new File(prefs.get("default.dir", "/")));
				ch.showDialog(frame, "ye");
				File[] newLayerFiles = ch.getSelectedFiles();
				if(newLayerFiles.length > 0) {
					for(File f : newLayerFiles) {
						String ext = (f.getName().indexOf('.') == -1) ? null : f.getName().substring(f.getName().indexOf('.'));
						System.out.println("ext = " +  f.length());
						if(ext.hashCode() == ".rar".hashCode() || ext.hashCode() == ".zip".hashCode() || ext.hashCode() == ".log".hashCode() || f.length() > 10000000) continue;
						GerberLoader load = new GerberLoader(f);
						LayerListObject newObj = new LayerListObject(load, GerberExtensionHandler.getDefaultLayerColor(ext), GerberExtensionHandler.getDefaultLayerName(ext));
						((DefaultListModel<LayerListObject>) list.getModel()).addElement(newObj);
					}
					prefs.put("default.dir", newLayerFiles[0].getAbsolutePath());
					layerAddedHandler();
				}
			}
		});
		contentPane.add(btnNewButton, "2, 4, right, top");
		
		JPanel panel = new PreviewPane(list);
		contentPane.add(panel, "8, 2, 1, 3, fill, fill");
		
		JButton btnNewButton_1 = new JButton("-");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(list.getSelectedIndex() != -1) ((DefaultListModel<LayerListObject>) list.getModel()).remove(list.getSelectedIndex());
				list.clearSelection();
				repaint();
			}
		});
		contentPane.add(btnNewButton_1, "4, 4");
		
		JButton btnNewButton_2 = new JButton("Export");
		btnNewButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Exporter exp = new Exporter(list.getSelectedValue());
				exp.frame.setVisible(true);
			}
		});
		contentPane.add(btnNewButton_2, "6, 4");
	}
	
	public void layerAddedHandler() {
		DefaultListModel<LayerListObject> objects = (DefaultListModel<LayerListObject>) list.getModel();
		for(int i = 0; i < list.getModel().getSize(); i++) {
			objects.getElementAt(i).forceRender();
		}
		repaint();
	}
}
