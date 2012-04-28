/* MainWindow.java - Part of Task Mistress
 * Written in 2012 by anonymous.
 * 
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighbouring rights to
 * this software to the public domain worldwide. This software is distributed without any warranty.
 * 
 * Full license at <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package anonpds.TaskMistress;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * Implements the main window of the Task Mistress program.
 * @author anonpds <anonpds@gmail.com>
 */
@SuppressWarnings("serial")
public class MainWindow extends JFrame implements TreeSelectionListener, ActionListener {
	/** Text for the button that adds a task. */
	private static final String ADD_BUTTON_TEXT = "Add";

	/** Text for the button that removes a task (tree). */
	private static final String REMOVE_BUTTON_TEXT = "Delete";

	/** Text for the button that opens another task tree. */
	private static final String OPEN_BUTTON_TEXT = "Open";

	/** Text of the button that renames a task. */
	private static final String RENAME_BUTTON_TEXT = "Rename";

	/** Text of the settings button. */
	private static final String SETTINGS_BUTTON_TEXT = "Settings";

	/** Text of the button that opens the debugger. */
	private static final String DEBUG_BUTTON_TEXT = "Debugger";

	/** The name of the variable that stores the window size. */
	private static final String CONFIG_WINDOW_SIZE = "MainWindow.size";

	/** Default width of the window in pixels. */
	private static final int DEFAULT_WIDTH = 640;

	/** Default height of the window in pixels. */
	private static final int DEFAULT_HEIGHT = 400;

	/** The TaskStore managed by this MainWindow. */
	private TaskStore store;

	/** Button that can be pressed to add a task. */
	private JButton addButton;
	
	/** Button that can be used to remove tasks. */
	private JButton removeButton;
	
	/** Button that opens another task tree in a new Task Mistress window. */
	private JButton openButton;
	
	/** Button for renaming tasks. */
	private JButton renameButton;
	
	/** Button that opens the settings window. */
	private JButton settingsButton;
	
	/** Button for opening the Debugger window. */
	private JButton debugButton;
	
	/** The tool bar which contains the action buttons. */
	private JToolBar toolBar;
	
	/** Status bar that displays the status of program. */
	private JLabel statusBar;
	
	/** The task view. */
	private TaskView taskView;
	
	/** The tree view of the tasks. */
	private JTree treeView;

	/**
	 * Initialises a new main window from the given TaskStore and with the given initial window size.
	 * @param store the TaskStore tied to the window 
	 */
	public MainWindow(TaskStore store) {
		this.store = store;

		/* set up the window */
		this.setTitle(TaskMistress.PROGRAM_NAME + " " + TaskMistress.PROGRAM_VERSION);
		this.addWindowListener(new MainWindowListener(this));
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		
		/* try to set the window size from task tree meta data */
		Dimension d = new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		if (this.store.getVariable(CONFIG_WINDOW_SIZE) != null) {
			try {
				d = Util.parseDimension(this.store.getVariable(CONFIG_WINDOW_SIZE));
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this,
				                              "Bad window size in configuration (" + e.getMessage() + "); using the default",
				                              "Warning",
				                              JOptionPane.WARNING_MESSAGE);
			}
		}
		this.setSize(d);

		/* build the UI */
		this.buildUI();
	}

	/**
	 * Immediately closes the main window without saving the tasks. This should be called once the user has decided to
	 * close the window and the tasks have already been saved (or the user has chosen not to save them).
	 */
	public void closeImmediately() {
		/* write the window size */
		this.store.setVariable(CONFIG_WINDOW_SIZE, Util.dimensionString(this.getWidth(), this.getHeight()));
		
		/* close the task store: this is done in a loop to handle errors */
		boolean promptDirectory = false;
		while (true) {
			try {
				/* if the saving failed before and user chose to try another directory, prompt for it */
				if (promptDirectory) {
					File path = TaskMistress.showPathDialog();
					if (path != null) this.store.setPath(path);
				}
				
				/* close the task store, saving all dirty tasks */
				this.store.close();
				
				/* if no errors, break the loop */
				break;
			} catch (Exception e) {
				/* error: prompt the user whether to try again */
				int choice = JOptionPane.showConfirmDialog(this,
				                                           "Saving the task tree failed. Try again?",
				                                           "Error!",
				                                           JOptionPane.YES_NO_CANCEL_OPTION);
				if (choice == JOptionPane.YES_OPTION) continue;
				if (choice == JOptionPane.CANCEL_OPTION) return;
				
				/* prompt the user whether to save the task tree in another place */
				choice = JOptionPane.showConfirmDialog(this,
				                              "Do you want to save the task tree to another directory?",
				                              "Error!",
				                              JOptionPane.YES_NO_OPTION);
				
				/* if "no" is selected, just exit without trying to save again */ 
				if (choice != JOptionPane.YES_OPTION) break;
				/* "yes" selected: set the directory to be prompted */
				promptDirectory = true;
			}
		}
		
		/* make sure the task store is collected */
		this.store = null;

		/* dispose of the window */
		this.setVisible(false);
		this.dispose();
	}

	/**
	 * Builds the user interface of the window. Should only be called from the constructor!
	 * 
	 * The current component hierarchy is as follows:
	 * <ul>
	 *   <li>JPanel: contains the following components in BorderLayout
	 *   <ul>
	 *     <li>toolBar: the program toolbar (BorderLayout.NORTH)</li>
	 *     <li>statusBar: the status bar (BorderLayout.SOUTH)</li>
	 *     <li>JSplitPane: split pane that with the following components 
	 *     <ul>
	 *       <li>taskTree: the tree view of the tasks</li>
	 *       <li>taskView: TaskView component that displays the currently selected task</li>
	 *     </ul></li>
	 *   </ul>
	 */
	private void buildUI() {
		this.setVisible(false);

		/* set up the tool bar and its buttons */
		this.addButton = new JButton(ADD_BUTTON_TEXT);
		this.removeButton = new JButton(REMOVE_BUTTON_TEXT);
		this.openButton = new JButton(OPEN_BUTTON_TEXT);
		this.renameButton = new JButton(RENAME_BUTTON_TEXT);
		this.settingsButton = new JButton(SETTINGS_BUTTON_TEXT);
		this.debugButton = new JButton(DEBUG_BUTTON_TEXT);
	
		this.toolBar = new JToolBar();
		this.toolBar.add(this.addButton);
		this.toolBar.add(this.removeButton);
		this.toolBar.add(this.openButton);
		this.toolBar.add(this.renameButton);
		this.toolBar.add(this.settingsButton);
		this.toolBar.add(this.debugButton);
		
		/* set the action listeners; the same action listener is used for all buttons */
		this.addButton.addActionListener(this);
		this.removeButton.addActionListener(this);
		this.openButton.addActionListener(this);
		this.renameButton.addActionListener(this);
		this.settingsButton.addActionListener(this);
		this.debugButton.addActionListener(this);
		
		/* set up the status bar */
		this.statusBar = new JLabel(" ");
		
		/* initialise the treeView */
		this.treeView = new JTree(this.store.getTreeModel());
		this.treeView.setRootVisible(false);
		this.treeView.setShowsRootHandles(true);
		this.treeView.setDragEnabled(true);
		this.treeView.setDropMode(DropMode.ON_OR_INSERT);
		this.treeView.addTreeSelectionListener(this);
		this.treeView.addMouseListener(new TreeViewMouseListener(this));
		this.treeView.setTransferHandler(new TreeViewTransferHandler(this));
		this.treeView.setFocusable(true);
		this.treeView.addKeyListener(new TreeViewKeyListener(this));
		this.treeView.setEditable(true);
		this.treeView.setCellRenderer(new TaskNodeRenderer());
		
		/* initialise the TaskView */
		this.taskView = new TaskView((DefaultTreeModel) this.store.getTreeModel());
		
		/* set up the split pane that contains the task tree view and editor */
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(this.treeView), this.taskView);
		
		/* set up the main panel and add the components*/
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(this.toolBar, BorderLayout.NORTH);
		mainPanel.add(this.statusBar, BorderLayout.SOUTH);
		mainPanel.add(splitPane, BorderLayout.CENTER);
		
		/* show the number of tasks as a status message (minus one for root) */
		this.statusBar.setText((this.store.getRoot().countNodes() - 1) + " tasks loaded.");
		
		/* add the components to the main window and set the window visible */
		this.add(mainPanel);
		this.setVisible(true);

		/* adjust the split between task tree view and task editor */
		splitPane.setDividerLocation(0.30);
	}

	/**
	 * Returns the currently selected node in the treeView. Returns the root node if no node is selected.
	 * @return currently selected node or root if no selection
	 */
	private Task getCurrentSelection() {
		/* find the currently selected task; use tree root if no task selected */
		Task node = this.store.getRoot();
		TreePath path = this.treeView.getSelectionPath();
		if (path != null) node = (Task) path.getLastPathComponent();
		return node;
	}
		
	/**
	 * Finds the currently selected node and adds a child node to it. Called when the Add button in the tool bar is
	 * activated.
	 */
	private void addButtonPressed() {
		/* get the currently selected node and add a child task to it */
		Task node = this.getCurrentSelection();
		this.add(node);
	}

	/**
	 * Adds task under the given task tree node. The task name is prompted from the user and the task is made active
	 * in the treeView after it has been created.
	 * @param parent the parent node under which to add the new tasks
	 */
	private void add(Task parent) {
		/* ask the user for task name */
		String name = JOptionPane.showInputDialog("Enter task name");
		if (name == null) return;
		
		/* add the new node and inform the treeView of the changed structure */
		Task newNode = this.store.add(parent, name);

		/* set the added task as the current selection */
		TreeNode[] newPath = newNode.getPath();
		TreePath treePath = new TreePath(newPath);
		this.treeView.setSelectionPath(treePath);
		
		/* show a message */
		this.statusBar.setText(newNode.getName() + " added.");
	}

	/**
	 * Removes the currently selected task from the task tree.
	 * Called by the tool bar button listener when the Remove button has been pressed.
	 */
	private void removeSelected() {
		/* get the selection; if no selection, do nothing */
		TreePath path = this.treeView.getSelectionPath();
		if (path == null) return;

		/* get the selected node; don't remove root node */
		Task node = (Task) path.getLastPathComponent();
		if (node.isRoot()) return;

		/* make sure the user wants to remove the task and its children */
		int answer = JOptionPane.showConfirmDialog(this,
		                                           "Remove '" + node.getName() + "' and its children?",
		                                           "Confirm delete",
		                                           JOptionPane.YES_NO_OPTION);
		if (answer != JOptionPane.YES_OPTION) return;
		
		/* remove the node */
		this.store.remove(node);

		/* show a message */
		this.statusBar.setText(node.getName() + " removed.");
	}

	/**
	 * Opens another task tree in new Task Mistress window. 
	 * Called by the tool bar button listener when the Open button has been pressed.
	 */
	private void openButtonPressed() {
		TaskMistress.openTaskTree();
	}

	/** Opens the cell editor for the currently selected node. */
	private void renameButtonPressed() {
		TreePath path = this.treeView.getSelectionPath();
		if (path == null) return;
		
		this.treeView.startEditingAtPath(path);
	}

	/**
	 * Moves node under another node.
	 * @param dest the destination node
	 * @param node the node to move
	 */
	public void move(Task dest, int index, Task node) {
		try {
			this.store.move(dest, index, node);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot move node", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Class that listens to the window events of the MainWindow.
	 * @author anonpds <anonpds@gmail.com>
	 */
	class MainWindowListener extends WindowAdapter {
		/** The MainWindow this listener listens to. */
		private MainWindow window;

		/**
		 * Constructs the listener class.
		 * @param window the MainWindow to listen to
		 */
		public MainWindowListener(MainWindow window) {
			this.window = window;
		}

		/**
		 * Handles the event of the main window closing.
		 * @param event the window event
		 */
		@Override
		public void windowClosing(WindowEvent event) {
			/* update the currently open task */
			if (this.window.taskView.getTask() != null) this.window.taskView.updateText();
			
			this.window.closeImmediately();
		}
	}

	/**
	 * Handles the event of task tree selection changing.
	 * @param event the selection event
	 */
	@Override
	public void valueChanged(TreeSelectionEvent event) {
		/* save the text of the old selection */
		TreePath path = event.getOldLeadSelectionPath();
		if (path != null) {
			Task node = (Task) path.getLastPathComponent();
			if (node.getParent() != null) { /* don't save root */
				this.taskView.updateText();
				if (node.isDirty()) { 
					try {
						this.store.writeOut(node);
						this.statusBar.setText(node.getName() + " written to disk.");
					} catch (Exception e) { /* TODO error; call a general error function? */ }
				} else this.statusBar.setText(" ");
			}
		}
		
		/* set the taskView with the Task of the new selection */
		path = event.getNewLeadSelectionPath();
		if (path != null) {
			Task node = (Task) path.getLastPathComponent();
			this.taskView.setTask(node);
		}
	}

	/**
	 * Handles the action of one of the tool bar buttons being pressed.
	 * @param event the action event
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == this.addButton) this.addButtonPressed();
		else if (event.getSource() == this.removeButton) this.removeSelected();
		else if (event.getSource() == this.openButton) this.openButtonPressed();
		else if (event.getSource() == this.renameButton) this.renameButtonPressed();
		else if (event.getSource() == this.settingsButton) TaskMistress.showSettings();
		else if (event.getSource() == this.debugButton) Debugger.showDebugger(this, store);
	}

	/**
	 * A class that listens to the mouse events of the treeView component in the MainWindow.
	 * @author anonpds <anonpds@gmail.com>
	 */
	class TreeViewMouseListener extends MouseAdapter {
		/** The MainWindow whose treeView this listener listens to. */
		private MainWindow window;
		
		/**
		 * Constructs a new listener.
		 * @param window the window which contains the listened treeView
		 */
		public TreeViewMouseListener(MainWindow window) {
			this.window = window;
		}
		
		/**
		 * Handles the event of mouse clicks on the treeView.
		 * @param event the mouse event
		 */
		@Override
		public void mouseClicked(MouseEvent event) {
			/* calculate the treeView row in which the click occurred */
			int row = event.getY() / this.window.treeView.getRowHeight();
			/* get the rectangle of the row bounds; clear selection if the click is not inside any row */
			Rectangle r = this.window.treeView.getRowBounds(row);
			if (r == null || !r.contains(event.getX(), event.getY())) this.window.treeView.setSelectionPath(null);
		}
	}
	
	/**
	 * A class that handles the data transfer with drag and drop events in the treeView.
	 * @author anonpds <anonpds@gmail.com>
	 */
	class TreeViewTransferHandler extends TransferHandler {
		/** The MainWindow whose data transfer is handled. */
		private MainWindow window;
		
		/** The destination path of the move. */
		private TreePath path;
		
		/** The destination index of the move. */
		private int index;
		
		/**
		 * The default constructor.
		 * @param window the MainWindow whose data transfer to handle
		 */
		public TreeViewTransferHandler(MainWindow window) {
			this.window = window;
		}
		
		/**
		 * Returns the allowed actions. Only moving of elements is currently supported.
		 * @param c the component for which to return the allowed actions (unused)
		 */
		@Override
		public int getSourceActions(JComponent c) {
			return MOVE;
		}
		
		/**
		 * Creates a new Transferable class to allow the data transfer between source and destination of the drag
		 * and drop event.
		 * @param source the source component for the drag and drop event
		 */
		@Override
		protected Transferable createTransferable(JComponent source) {
			/* only allow JTree as the source */
			if (!(source instanceof JTree)) return null;
			
			/* set the currently selected tree component as the transferable node */
			JTree tree = (JTree) source;
			Task node = (Task) tree.getSelectionPath().getLastPathComponent();
			
			/* if no selected node, return null to indicate no transfer */
			if (node == null) return null;
			return new TreeNodeTransferable(node);
		}
		
		/**
		 * Finishes the drag and drop event; this is where the data gets moved.
		 * @param source source component
		 * @param data the data to transfer
		 * @param action the action (move, copy, cut)
		 */
		@Override
		protected void exportDone(JComponent source, Transferable data, int action) {
			/* only handle moves of TreeNodes inside JTree */
			if (action != MOVE || !(source instanceof JTree) || !(data instanceof TreeNodeTransferable)) return;

			JTree tree = (JTree) source;
			Task node = ((TreeNodeTransferable)data).getNode();
			
			/* get the destination path; if it's null, move under root node */
			Task dest = (Task) this.path.getLastPathComponent();
			/* DEBUG */ System.out.println(dest + ", " + this.index);
			
			/* execute the move */
			this.window.move(dest, this.index, node);
		}
		
		/**
		 * Tells whether something can be transferred through drag and drop. Always returns true, because the filtering
		 * is done elsewhere (createTransferable and exportDone only accept particular classes as parameters).
		 * @return always true
		 */
		@Override
		public boolean canImport(TransferSupport support) {
			return true;
		}
		
		/**
		 * Perform the data import.
		 * @param support the transfer operation
		 */
		@Override
		public boolean importData(TransferSupport support) {
			if (!support.isDrop()) return false;

			/* set the destination path and index */
			JTree.DropLocation dl = (javax.swing.JTree.DropLocation) support.getDropLocation();
			this.path = dl.getPath();
			this.index = dl.getChildIndex();
			
			return true;
		}
	}
	
	/**
	 * Sub-class of Transferable that can transfer TaskNode objects.
	 * @author anonpds <anonpds@gmail.com>
	 */
	class TreeNodeTransferable implements Transferable {
		/** The node to transfer. */
		private Task node;
		
		/** The "flavours" of data accepted. */
		private DataFlavor[] flavor;

		/**
		 * Default constructor.
		 * @param node the node to transfer
		 */
		public TreeNodeTransferable(Task node) {
			this.node = node;
			/* create a list of the accepted data "flavours"; only TaskNode classes are accepted */
			this.flavor = new DataFlavor[1];
			this.flavor[0] = new DataFlavor(Task.class, Task.class.getName());
		}
		
		/**
		 * Returns the node that is being transferred.
		 * @return the transferred node
		 */
		public Task getNode() {
			return this.node;
		}

		/**
		 * Returns the transfer data of the specified "flavour".
		 * @param flavor the flavour of the data to receive
		 * @return the data object in the given flavour
		 */
		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (!this.isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
			return node;
		}

		/**
		 * Returns the list of accepted data "flavours".
		 * @return list of accepted flavours
		 */
		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return this.flavor;
		}

		/**
		 * Tells whether a particular data flavour is supported.
		 * @param flavor the flavour to dest
		 * @return true if the flavour is supported, false if not
		 */
		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			for (int i = 0; i < this.flavor.length; i++)
				if (this.flavor[i].equals(flavor)) return true;
			return false;
		}
	}
	
	/**
	 * Listens to the key events of treeView.
	 * @author anonpds <anonpds@gmail.com>
	 */
	class TreeViewKeyListener extends KeyAdapter {
		/** The MainWindow in which the listened treeView resides. */
		private MainWindow window;

		/**
		 * Default constructor.
		 * @param window the MainWindow which contains the listened treeView.
		 */
		public TreeViewKeyListener(MainWindow window) {
			this.window = window;
		}

		/**
		 * Handles the event of a key being typed.
		 * @param e the key event
		 */
		@Override
		public void keyTyped(KeyEvent e) {
			if (e.getKeyChar() == KeyEvent.VK_SPACE) {
				/* SPACE: add task */
				Task node = this.window.getCurrentSelection();
				
				/* if shift down, add as sibling, otherwise add as child */
				if (e.isShiftDown()) node = (Task) node.getParent();
				if (node != null) this.window.add(node);
			} else if (e.getKeyChar() == KeyEvent.VK_DELETE) {
				this.window.removeSelected();
			}
		}
	}
}
