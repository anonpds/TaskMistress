/* TaskStore test program
 * Written in 2012 by anonymous.
 * 
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighbouring rights to
 * this software to the public domain worldwide. This software is distributed without any warranty.
 * 
 * Full license at <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package anonpds.TaskMistress;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import anonpds.TaskMistress.TaskStore.TaskTreeNode;

/**
 * Test program for TaskStore class.
 * @author anonpds <anonpds@gmail.com>
 */
public class TaskStoreTest implements ActionListener {
	/** Text of the remove task button. */
	private static final String REMOVE_TEXT = "Remove";

	/** Text of the add task button. */
	private static final String ADD_TEXT = "Add";

	/** Text of the save task button. */
	private static final String SAVE_TEXT = "Save";
	
	/** Text of the move task button. */
	private static final String MOVE_TEXT = "Move";

	/** The program main window */
	JFrame frame = new JFrame("TaskStore test");
	
	/** The TaskStore used to store the tasks. */
	TaskStore store;
	
	/** Button for removing tasks. */
	private JButton removeButton = new JButton(REMOVE_TEXT);
	
	/** Button to add tasks. */
	private JButton addButton = new JButton(ADD_TEXT);
	
	/** Button to move tasks. */
	private JButton moveButton = new JButton(MOVE_TEXT);
	
	/** The item to move through the moveButton. */
	private TreeItem moveItem = null;
	
	/** Panel that contains the task list. */
	private JPanel taskPanel = new JPanel(new GridLayout(1,1));

	/** List of tree items currently displayed. */
	private JList displayList;

	/** Button for saving the task list. */
	private JButton saveButton = new JButton(SAVE_TEXT);

	/** The editor for editing task contents. */
	private TaskEditor editor = new TaskEditor();

	/**
	 * Initialises the test program and creates the program window. 
	 * @throws Exception if initialisation of the TaskStore fails 
	 */
	public TaskStoreTest() throws Exception {
		/* first allow the user to choose the path to use for the test */
		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		/* do nothing if the action was cancelled */
		if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
		
		/* give scathing feedback, if the user selected something other than a directory */
		File path = chooser.getSelectedFile();
		if (path.exists() && !path.isDirectory()) throw new Exception("'" + path.getPath() + "' is not a directory"); 
		
		/* read in the stored tasks */
		this.store = new TaskStore(path);
		
		/* make the menubar */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(this.removeButton);
		menuBar.add(this.addButton);
		menuBar.add(this.moveButton);
		menuBar.add(this.saveButton);

		/* add action listener to buttons */
		this.removeButton.addActionListener(this);
		this.addButton.addActionListener(this);
		this.moveButton.addActionListener(this);
		this.saveButton.addActionListener(this);
		
		/* task list and task editor in a JSplitPane */
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
		                                      new JScrollPane(this.taskPanel),
		                                      new JScrollPane(this.editor));

		/* add a populated JList to the task list panel */
		this.taskPanel.add(this.populateJList());
		
		/* layout the components */
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(menuBar, BorderLayout.NORTH);
		panel.add(splitPane, BorderLayout.CENTER);
		
		/* initialise the window */
		this.frame.add(panel);
		this.frame.setSize(640, 400);
		this.frame.setVisible(true);
		splitPane.setDividerLocation(0.32);
	}

	private void updateTaskList() {
		this.taskPanel.remove(0);
		this.taskPanel.add(this.populateJList());
		//this.taskPanel.validate();
		this.frame.validate();
		this.frame.repaint();
	}
	
	/**
	 * Generates a JList component from the stored tasks.
	 * @return the populated JList
	 */
	private JList populateJList() {
		/* store the items in a vector of JLabels */
		Vector<TreeItem> items = new Vector<TreeItem>();

		/* recursively add all the tasks */
		this.populateJListRecurse("", items, this.store.getRoot());

		this.displayList = new JList(items);
		return this.displayList;
	}
	
	/**
	 * Recursively generates a Vector of tasks.
	 * @param prefix for the names of the tasks
	 * @param items the Vector to add the tasks to
	 * @param node the node to add the tasks from
	 */
	private void populateJListRecurse(String prefix, Vector<TreeItem> items, TaskTreeNode node) {
		/* don't add root node */
		if (!node.isRoot()) {
			items.add(new TreeItem(node, prefix));
			prefix = "    " + prefix;
		}
		
		/* add the children recursively */
		Object[] children = node.getChildren();
		for (Object o : children) populateJListRecurse(prefix, items, (TaskTreeNode)o);
	}

	/**
	 * Runs the test program
	 * @param args command line arguments (unused)
	 */
	public static void main(String[] args) {
		/* set native look and feel if possible */
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) { /* no error handling :) */ }
		
		/* run the test */
		try {
			new TaskStoreTest();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Fatal error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Action listener for the tool bar buttons of the main window.
	 * @param event the event
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == this.addButton) {
			/* add button pressed; ask a name for the new task */
			String name = JOptionPane.showInputDialog(this.frame, "Name the task", "Add Task", JOptionPane.QUESTION_MESSAGE);
			if (name == null) return;
			
			/* if an item is selected from the JList, add as the child of that node */
			TaskTreeNode addTo = this.store.getRoot();
			if (this.displayList.getSelectedValue() != null) {
				TreeItem item = (TreeItem)this.displayList.getSelectedValue();
				addTo = item.getNode();
			}
			
			/* add */
			this.store.addChild(addTo, name);
			
			/* finally, update the task list to reflect the changes */
			this.updateTaskList();
		} else if (event.getSource() == this.removeButton) {
			/* remove button pressed */
			if (this.displayList.getSelectedValue() != null) {
				/* get the selected item and remove it */
				TreeItem item = (TreeItem)this.displayList.getSelectedValue();
				this.store.remove(item.getNode());
				
				/* update the task list */
				this.updateTaskList();
			}
		} else if (event.getSource() == this.saveButton) {
			/* save button pressed */
			try {
				this.store.writeOut();
			} catch (Exception e) {
				/* CRITICAL it must be possible to change the save path if saving fails */
				JOptionPane.showMessageDialog(this.frame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		} else if (event.getSource() == this.moveButton) {
			/* move button pressed; this is done in two phases:
			 *  - on first press of the button the node to move is selected
			 *  - on second press the destination node is selected */
			TreeItem dest = (TreeItem)this.displayList.getSelectedValue();

			if (this.moveItem != null) {
				/* node to move selected; if destination not selected, move to root, else move under selected node */
				if (dest == null) this.store.move(this.store.getRoot(), this.moveItem.getNode());
				else this.store.move(dest.getNode(), this.moveItem.getNode());
				
				/* movement done; update the list */
				this.updateTaskList();
				this.moveItem = null;
			} else {
				/* no node selected yet for moving */
				if (this.displayList.getSelectedValue() == null) {
					/* error, no node selected */
					JOptionPane.showMessageDialog(this.frame, "Please choose a node first.");
				}
				
				/* use the selected node as the node to move, clear selection */
				this.moveItem = dest; 
				this.displayList.clearSelection();
			}
		}
	}
	
	/**
	 * A class for holding tree items temporarily.
	 * @author anonpds <anonpds@gmail.com>
	 */
	class TreeItem {
		/** The tree node related to this item. */
		private TaskTreeNode node;
		
		/** The name of this item. */
		private String name;

		/**
		 * Constructs a new tree item with the given node and message.
		 * @param node the node stored in the tree item
		 * @param message the message related to the node
		 */
		public TreeItem(TaskTreeNode node, String message) {
			this.node = node;
			this.name = message;
		}
		
		/**
		 * Returns the message string.
		 * @return the message string
		 */
		public String getName() {
			return this.name;
		}
		
		/**
		 * Returns the node of this tree item.
		 * @return the node
		 */
		public TaskTreeNode getNode() {
			return this.node;
		}
		
		/**
		 * Returns the string representation of the tree item.
		 * @return the string
		 */
		public String toString() {
			if (node != null) return this.name + node.getName(); 
			return this.name;
		}
	}
}
