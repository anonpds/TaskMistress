/* TaskEditor test program
 * Written in 2012 by anonymous.
 * 
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighbouring rights to
 * this software to the public domain worldwide. This software is distributed without any warranty.
 * 
 * Full license at <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package anonpds.TaskMistress;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/* TODO add display of task meta data (creation date, last modification date, file system name) */

/**
 * Test program for TaskEditor class.
 * @author anonpds <anonpds@gmail.com>
 */
public class TaskEditorTest implements ActionListener, DocumentListener {
	private JFrame frame = new JFrame("TaskEditor test");
	private TaskEditor editor = new TaskEditor();
	private JButton openButton = new JButton("Open");
	private JButton closeButton = new JButton("Close");
	private JLabel dateLabel = new JLabel("Creation date");
	private boolean modified = false;

	/** Default constructor. */
	public TaskEditorTest() {
		/* make a menu bar with new and close buttons */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(openButton);
		menuBar.add(closeButton);
		menuBar.add(dateLabel);
		
		/* add listener for the buttons */
		openButton.addActionListener(this);
		closeButton.addActionListener(this);

		/* layout the menubar and editor */
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(menuBar, BorderLayout.NORTH);
		panel.add(new JScrollPane(this.editor), BorderLayout.CENTER);
		frame.add(panel);
		
		/* set document change listener for the editor */
		this.editor.getDocument().addDocumentListener(this);
		
		/* set the window size and make it visible */
		frame.setSize(640, 400);
		frame.setVisible(true);
	}
	
	/**
	 * Listens to actions on the menu bar buttons.
	 * @param e the ActionEvent
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.openButton) {
			if (this.editor.isOpen()) {
				/* if the editor is already open, prompt for overwriting the message */
				if (JOptionPane.showConfirmDialog(this.frame,
						"Discard the old text and start new?",
						"Editor already open",
						JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) return;
			}
			this.dateLabel.setText((new Date(System.currentTimeMillis())).toString());
			this.editor.open("");
			this.modified = false;
		} else if (e.getSource() == this.closeButton) {
			/* if the editor is already closed, do nothing */
			if (!this.editor.isOpen()) return;
			
			/* otherwise close the editor and put a nice non-editable text on it */
			this.editor.close("Editor closed. Ha-ha! You can't edit me.");
			this.dateLabel.setText("Creation time");
		}
	}

	/**
	 * The test program.
	 * @param args command line arguments (unused)
	 */
	public static void main(String[] args) {
		/* set native look and feel if possible */
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) { /* no error handling :) */ }
		
		/* run the editor test */
		new TaskEditorTest();
	}

	/**
	 * Updates the status line to tell that the text has been modified.
	 */
	private void textModified() {
		if (this.modified) return;
		this.dateLabel.setText(this.dateLabel.getText() + " (modified)");
		this.modified = true;
	}

	/**
	 * Listens to text changes in the editor.
	 * @param e the change event
	 */
	@Override
	public void changedUpdate(DocumentEvent e) {
		this.textModified();
	}

	/**
	 * Listens to text inserts in the editor.
	 * @param e the change event
	 */
	@Override
	public void insertUpdate(DocumentEvent e) {
		this.textModified();
	}

	/**
	 * Listens to text removals in the editor.
	 * @param e the change event
	 */
	@Override
	public void removeUpdate(DocumentEvent e) {
		this.textModified();
	}
}
