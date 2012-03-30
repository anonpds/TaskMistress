/* SettingsWindow.java - Part of Task Mistress
 * Written in 2012 by anonymous.
 * 
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighbouring rights to
 * this software to the public domain worldwide. This software is distributed without any warranty.
 * 
 * Full license at <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package anonpds.TaskMistress;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Implements the window that can be used to alter the settings of Task Mistress.
 * @author anonpds <anonpds@gmail.com>
 */
@SuppressWarnings("serial")
public class SettingsWindow extends JFrame implements ActionListener {
	/** The SettingsWindow; only one instance may exist at a time. */
	private static SettingsWindow window = null;
	
	/** The configuration to operate on. */
	private Configuration config;

	/** Button for accepting the changes in the settings window. */
	JButton okButton = new JButton("OK");

	/** Button for rejecting the changes in the settings window. */
	JButton cancelButton = new JButton("Cancel");

	/** Combo box for selecting the default task tree. */
	JComboBox box;
	
	/** Constructs the settings window. */
	private SettingsWindow(Configuration conf) {
		this.config = conf;
		
		/* initialise the buttons */
		JPanel buttonPanel = new JPanel(new FlowLayout());
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);

		/* init the combo box */
		box = new JComboBox();
		
		/* initialise the default task tree and to menu */
		String path = config.get(TaskMistress.CONFIG_DEFAULT);
		if (path == null) path = "none";
		box.addItem(path);
		
		/* add the history to menu */
		String[] history = TaskMistress.getHistory();
		for (int i = 0; i < history.length; i++) {
			box.addItem(history[i]);
		}
		
		/* Layout the components */
		JPanel panel = new JPanel();
		JPanel labelPanel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		labelPanel.add(new JLabel("Default task tree: "));
		panel.add(labelPanel);
		panel.add(box);
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(panel, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		/* add the main panel as the root component of the window */
		this.add(mainPanel);
		
		// show the window
		this.setTitle(TaskMistress.PROGRAM_NAME + " " + TaskMistress.PROGRAM_VERSION + " : Settings");
		this.pack();
		this.setVisible(true);
		this.setResizable(false);
	}

	/** Applies the changes made to the configuration. */
	private void applyChanges() {
		String path = (String) this.box.getSelectedItem();
		this.config.add(TaskMistress.CONFIG_DEFAULT, path);
		TaskMistress.saveConfiguration();
		this.close();
	}
	
	/** Rejects the changes made to the configuration. */
	private void rejectChanges() {
		this.close();
	}
	
	/**
	 * Opens the SettingsWindow to display the specified config.
	 * @param conf the Configuration to display in the SettingsWindow
	 */
	public static void open(Configuration conf) {
		if (window != null) return; /* already open */
		window = new SettingsWindow(conf);
	}
	
	/** Closes the window. */
	public void close() {
		this.setVisible(false);
		this.dispose();
		SettingsWindow.window = null;
	}

	/**
	 * Listens to the buttons.
	 * @param event the ActionEvent
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == this.okButton) this.applyChanges();
		else if (event.getSource() == this.cancelButton) this.rejectChanges();
	}
}
