package g2c.application;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class LayerSettings {

	JFrame frame;
	private JTextField textField;
	JPanel panel = new JPanel();

	/**
	 * Initialize the contents of the frame.
	 */
	public LayerSettings(LayerListObject obj) {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 118);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JButton btnNewButton = new JButton("Choose");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				obj.setColor(JColorChooser.showDialog(frame, "Choose a new color for " + obj.getName(), obj.getColor()));
				panel.setBackground(obj.getColor());
				Main.frame.repaint();
			}
		});
		btnNewButton.setBounds(335, 45, 89, 23);
		frame.getContentPane().add(btnNewButton);
		
		panel.setBackground(obj.getColor());
		panel.setBounds(300, 45, 25, 23);
		frame.getContentPane().add(panel);
		
		JLabel lblNewLabel = new JLabel("Color");
		lblNewLabel.setBounds(10, 45, 280, 23);
		frame.getContentPane().add(lblNewLabel);
		
		JLabel lblName = new JLabel("Name");
		lblName.setBounds(10, 11, 89, 23);
		frame.getContentPane().add(lblName);
		
		textField = new JTextField(obj.getName());
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				obj.setName(textField.getText());
				Main.frame.repaint();
			}
		});
		textField.setBounds(109, 11, 315, 23);
		frame.getContentPane().add(textField);
		textField.setColumns(10);
	}
}
