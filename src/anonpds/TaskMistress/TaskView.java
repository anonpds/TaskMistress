/* TaskView.java - Part of Task Mistress
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
import java.text.DateFormat;
import java.util.Date;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultTreeModel;

/* CRITICAL does this task have to have its own dirty status? Why not just use the tasks one? */

/**
 * Implements a component that displays task editor and information about the currently edited task.
 * 
 * Currently the view consists of the following arrangement of components, laid out inside the TaskView itself with
 * BorderLayout:
 * 
 * <ul>
 *   <li>statusBar: JLabel component that displays the status of the Task (BorderLayout.NORTH)</li>
 *   <li>editor: TaskEditor component wrapped in JScrollPane; displays the task editor (BorderLayout.CENTER)</li>
 * </ul>
 * 
 * @author anonpds <anonpds@gmail.com>
 */
@SuppressWarnings("serial")
public class TaskView extends JPanel implements DocumentListener, ActionListener {
	/** Text of task default status. */
	private static final String DEFAULT_TEXT = "Default";

	/** Text of task done status. */
	private static final String DONE_TEXT = "Done";

	/** Text of task undone status. */
	private static final String UNDONE_TEXT = "Undone";

	/** Label that displays the Task status. */
	private JLabel statusBar;
	
	/** The editor, which may be used to edit tasks. */
	private TaskEditor editor;

	/** The currently displayed Task. */
	private Task task;

	/** Indicates whether the editor text has changed since it was loaded from the task. */
	private boolean dirty;

	/** Combo box that displays the status of the task (done, undone or default). */
	private JComboBox statusBox;
	
	/** statusBox choices. */
	private String[] comboBoxChoices = { DEFAULT_TEXT, DONE_TEXT, UNDONE_TEXT };

	/** TreeModel that contains the task. Informed of changes to task status. */
	private DefaultTreeModel treeModel;

	/**
	 * Constructs the TaskView.
	 * @param model 
	 */
	public TaskView(DefaultTreeModel model) {
		this.treeModel = model;
		
		/* build the user interface */
		this.editor = new TaskEditor();
		this.statusBar = new JLabel("No task selected.");
		this.statusBox = new JComboBox(this.comboBoxChoices);
		this.statusBox.addActionListener(this);

		JPanel statusPanel = new JPanel(new BorderLayout());
		statusPanel.add(this.statusBar, BorderLayout.WEST);
		statusPanel.add(this.statusBox, BorderLayout.EAST);

		this.setLayout(new BorderLayout());
		this.add(statusPanel, BorderLayout.NORTH);
		this.add(new JScrollPane(this.editor), BorderLayout.CENTER);
		
		/* set the document listener to the editor to watch for changes */
		this.editor.getDocument().addDocumentListener(this);
	}

	/**
	 * Returns the currently displayed task.
	 * @return the currently displayed task or null if no task displayed 
	 */
	public Object getTask() {
		return this.task;
	}
	
	/**
	 * Sets the Task that is displayed in the TaskView.
	 * @param task the Task to display
	 * @param node the tree node that contains the task
	 */
	public void setTask(Task task) {
		if (task.getParent() == null) {
			this.task = null;
			this.statusBar.setText("No task selected.");
			this.editor.close("");
			this.setStatusBox(Task.STATUS_DEFAULT);
		} else {
			this.task = task;
			this.editor.open(this.task.getText());
			this.setDirty(false);
			this.setStatusBox(task.getStatus());
			this.updateStatus();
		}
	}

	/**
	 * Sets the status combo box value.
	 * @param status the new status for the combo box
	 */
	private void setStatusBox(short status) {
		if (status == Task.STATUS_DEFAULT) this.statusBox.setSelectedItem(DEFAULT_TEXT);
		else if (status == Task.STATUS_DONE) this.statusBox.setSelectedItem(DONE_TEXT);
		else if (status == Task.STATUS_UNDONE) this.statusBox.setSelectedItem(UNDONE_TEXT);
	}

	/** Sets the Task status. */
	private void setTaskStatus(short status) {
		if (task == null || status == this.task.getStatus()) return;

		try { this.task.setStatus(status); } catch (Exception e) { /* TODO error */ }
		this.setDirty(true);
		this.updateStatus();
		
		if (this.treeModel != null) this.treeModel.reload(this.task);
	}
	
	/** Updates the status bar text. */
	public void updateStatus() {
		/* has the task been changed since last save */
		String edited = "";
		if (this.isDirty()) edited = " (changed)";

		/* creation date formatting */
		Date date = new Date(this.task.getCreationTime());
		DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
		
		/* set the text */
		this.statusBar.setText("Created: " + format.format(date) + edited);
	}

	/** Handles changes in the editor component; sets the dirty flag and updates the status text. */
	public void editorChanged() {
		if (!this.isDirty()) {
			this.setDirty(true);
			this.updateStatus();
		}
	}

	/** Updates the Task text from the editor. */ 
	public void updateText() {
		if (this.isDirty())	this.task.setText(this.editor.getText());
	}

	/**
	 * Tells whether the task text has been edited since the editor was initialised.
	 * @return true if the text has changed, false if not
	 */
	private boolean isDirty() {
		return this.dirty; 
	}

	/**
	 * Sets the dirty status of the task text.
	 * @param dirty the new dirty status (true if the text has changed, false if not)
	 */
	private void setDirty(boolean dirty) {
		this.dirty = dirty; 
	}

	/**
	 * Handles the event of the text change in the editor.
	 * @param e the document event
	 */
	@Override
	public void changedUpdate(DocumentEvent e) {
		this.editorChanged();
	}

	/**
	 * Handles the event of the text insertion in the editor.
	 * @param e the document event
	 */
	@Override
	public void insertUpdate(DocumentEvent e) {
		this.editorChanged();
	}

	/**
	 * Handles the event of the text removal in the editor.
	 * @param e the document event
	 */
	@Override
	public void removeUpdate(DocumentEvent e) {
		this.editorChanged();
	}

	/**
	 * Listens to the task status combo box.
	 * @param event the action event
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		/* do nothing if no task active */
		if (this.task == null) return;
		
		/* change the task status */
		String selected = (String) this.statusBox.getSelectedItem();
		if (selected == DEFAULT_TEXT) this.setTaskStatus(Task.STATUS_DEFAULT);
		else if (selected == DONE_TEXT) this.setTaskStatus(Task.STATUS_DONE);
		else if (selected == UNDONE_TEXT) this.setTaskStatus(Task.STATUS_UNDONE);
	}
}
