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
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/* TODO add support for checked items; need to implement a new CellRenderer to show the additional icons. */
/* TODO save the default window size in the task tree meta data */

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

	/** Text for the button that saves all the tasks to disk. */
	private static final String SAVE_BUTTON_TEXT = "Save";

	/** Text for the button that opens another task tree. */
	private static final String OPEN_BUTTON_TEXT = "Open";

	/** The TaskStore managed by this MainWindow. */
	private TaskStore store;

	/** Button that can be pressed to add a task. */
	private JButton addButton;
	
	/** Button that can be used to remove tasks. */
	private JButton removeButton;
	
	/** Button that saves the changed tasks to disk. */
	private JButton saveButton;
	
	/** Button that opens another task tree in a new Task Mistress window. */
	private JButton openButton;
	
	/** The tool bar which contains the action buttons. */
	private JToolBar toolBar;
	
	/** Status bar that displays the status of program. */
	private JLabel statusBar;
	
	/** Status bar that displays information about the currently edited task. */
	private JLabel editorBar;
	
	/** The task editor. */
	private TaskEditor editor;
	
	/** The tree view of the tasks. */
	private JTree treeView;

	/**
	 * Initialises a new main window from the given TaskStore.
	 * @param store the TaskStore tied to the window 
	 */
	public MainWindow(TaskStore store) {
		this(store, 640, 480);
	}

	/**
	 * Initialises a new main window from the given TaskStore and with the given initial window size.
	 * @param store the TaskStore tied to the window 
	 * @param width the width of the window to initialise
	 * @param height the height of the window to initialise
	 */
	public MainWindow(TaskStore store, int width, int height) {
		this.store = store;

		/* set up the window */
		this.setTitle(TaskMistress.PROGRAM_NAME + " " + TaskMistress.PROGRAM_VERSION);
		this.addWindowListener(new MainWindowListener(this));
		this.setSize(width, height);

		/* build the UI */
		this.buildUI();
	}

	/**
	 * Immediately closes the main window without saving the tasks. This should be called once the user has decides to
	 * close the window and the tasks have already been saved (or the user has chosen not to save them).
	 */
	public void close() {
		this.setVisible(false);
		this.dispose();
		
		/* make sure the task store is collected */
		this.store = null;
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
	 *       <li>editorPane: JPanel that contains the following components in BoxLayout
	 *       <ul>
	 *         <li>editorBar: the editor status bar</li>
	 *         <li>editor: the task editor</li>
	 *       </ul></li>
	 *     </ul></li>
	 *   </ul>
	 */
	private void buildUI() {
		this.setVisible(false);

		/* set up the tool bar and its buttons */
		this.addButton = new JButton(ADD_BUTTON_TEXT);
		this.removeButton = new JButton(REMOVE_BUTTON_TEXT);
		this.saveButton = new JButton(SAVE_BUTTON_TEXT);
		this.openButton = new JButton(OPEN_BUTTON_TEXT);
	
		this.toolBar = new JToolBar();
		this.toolBar.add(addButton);
		this.toolBar.add(removeButton);
		this.toolBar.add(saveButton);
		this.toolBar.add(openButton);
		
		/* set the action listeners; the same action listener is used for all buttons */
		this.addButton.addActionListener(this);
		this.removeButton.addActionListener(this);
		this.saveButton.addActionListener(this);
		this.openButton.addActionListener(this);
		
		/* set up the status bar */
		this.statusBar = new JLabel("");
		
		/* set up the JPanel that contains the editor and its status bar */
		this.editorBar = new JLabel("No task selected");
		this.editor = new TaskEditor();
		
		JPanel editorPanel = new JPanel(new BorderLayout());
		editorPanel.add(this.editorBar, BorderLayout.NORTH);
		editorPanel.add(new JScrollPane(this.editor), BorderLayout.CENTER);
		
		/* initialise the treeView */
		this.treeView = new JTree(this.store.getTreeModel());
		this.treeView.setRootVisible(false);
		this.treeView.setShowsRootHandles(true);
		this.treeView.addTreeSelectionListener(this);
		this.treeView.addMouseListener(new TreeViewMouseListener(this));
		this.treeView.setDragEnabled(true);
		this.treeView.setTransferHandler(new TreeViewTransferHandler(this));
		
		/* TODO later:
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		renderer.setLeafIcon(new ImageIcon("res/note.gif"));
		this.treeView.setCellRenderer(renderer);
		*/
		
		/* TODO add support for this
		this.treeView.setEditable(true);
		*/

		/* set up the split pane that contains the task tree view and editor */
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.treeView, editorPanel);
		
		/* set up the main panel and add the components*/
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(this.toolBar, BorderLayout.NORTH);
		mainPanel.add(this.statusBar, BorderLayout.SOUTH);
		mainPanel.add(splitPane, BorderLayout.CENTER);
		
		/* add the components to the main window and set the window visible */
		this.add(mainPanel);
		this.setVisible(true);

		/* adjust the split between task tree view and task editor */
		splitPane.setDividerLocation(0.30);
	}

	/**
	 * Sets the node which is currently displayed in the editor.
	 * @param node the node to display in the editor
	 */
	private void setEditorNode(DefaultMutableTreeNode node) {
		Task task = (Task) node.getUserObject();
		if (task != null) {
			/* format the date for editor status bar */
			/* TODO add the changed status to the status bar */
			Date date = new Date(task.getCreationTime());
			DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
			this.editorBar.setText("Created: " + format.format(date));

			/* set the editor text and make it editable */
			this.editor.setText(task.getText());
			this.editor.setEditable(true);
		} else {
			/* no node selected; make the editor uneditable */
			this.editorBar.setText("No task selected");
			this.editor.setText("");
			this.editor.setEditable(false);
		}
		this.validate();
	}

	/**
	 * Prompts the user for a task to add and adds it to the tree, as a child of the currently selected task.
	 * Called by the tool bar button listener when the Add button has been pressed.
	 */
	private void addButtonPressed() {
		/* ask the user for task name */
		String name = JOptionPane.showInputDialog("Enter task name");
		if (name == null) return;
		
		/* find the currently selected task; use tree root if no task selected */
		DefaultMutableTreeNode node = this.store.getRoot();
		TreePath path = this.treeView.getSelectionPath();
		if (path != null) node = (DefaultMutableTreeNode) path.getLastPathComponent();

		/* add the new node and inform the treeView of the changed structure */
		/* TODO the TreeModel business belongs to store.addChild() */
		DefaultMutableTreeNode newNode = this.store.addChild(node, name);
		((DefaultTreeModel)this.treeView.getModel()).reload(node);

		/* set the added task as the current selection */
		TreeNode[] newPath = newNode.getPath();
		TreePath treePath = new TreePath(newPath);
		this.treeView.setSelectionPath(treePath);
	}

	/**
	 * Removes the currently selected task from the task tree.
	 * Called by the tool bar button listener when the Remove button has been pressed.
	 */
	private void removeButtonPressed() {
		/* get the selection; if no selection, do nothing */
		TreePath path = this.treeView.getSelectionPath();
		if (path == null) return;

		/* get the selected node; don't remove root node */
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
		if (node.isRoot()) return;

		/* remove the node */
		/* TODO the TreeModel business belongs to store.remove */
		TreeNode parent = node.getParent();
		this.store.remove(node);
		((DefaultTreeModel)this.treeView.getModel()).reload(parent);
	}

	/**
	 * Saves the task tree to disk.
	 * Called by the tool bar button listener when the Save button has been pressed.
	 */
	private void saveButtonPressed() {
		try { this.store.writeOut(); } catch (Exception e) { /* TODO errors */ }
	}

	/**
	 * Opens another task tree in new Task Mistress window. 
	 * Called by the tool bar button listener when the Open button has been pressed.
	 */
	private void openButtonPressed() {
		/* show the path selection dialog and open the new Task Mistress window, if the user selected a path */
		File path = TaskMistress.showPathDialog();
		if (path != null) {
			try { new TaskMistress(path); } catch (Exception e) { /* TODO error */ }
		}
	}

	/**
	 * Moves node under another node.
	 * @param dest the destination node
	 * @param node the node to move
	 */
	public void move(DefaultMutableTreeNode dest, DefaultMutableTreeNode node) {
		try {
			this.store.move(dest, node);
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
			try {
				this.window.store.writeOut();
			} catch (Exception e) {
				String msg = "Could not save the tasks (" + e.getMessage() + "); exit anyway?";
				int input = JOptionPane.showConfirmDialog(this.window,
				                                          msg,
				                                          "Error!",
				                                          JOptionPane.YES_NO_OPTION,
				                                          JOptionPane.ERROR_MESSAGE);
				if (input == JOptionPane.NO_OPTION) return;
			}
			/* TODO save the window size in the task tree meta data */
			this.window.close();
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
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			Task task = (Task) node.getUserObject();
			if (task != null) task.setText(this.editor.getText());
			/* TODO save the node here; it's better to save them one at a time than all at once */ 
		}
		
		/* set the editor with the text of the new selection */
		path = event.getNewLeadSelectionPath();
		if (path != null) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			this.setEditorNode(node);
		}
	}

	/**
	 * Handles the action of one of the tool bar buttons being pressed.
	 * @param event the action event
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == this.addButton) this.addButtonPressed();
		else if (event.getSource() == this.removeButton) this.removeButtonPressed();
		else if (event.getSource() == this.saveButton) this.saveButtonPressed();
		else if (event.getSource() == this.openButton) this.openButtonPressed();
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
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();
			
			/* if no selected node, return null to indicate no transfer */
			if (node == null) return null;
			return new TreeNodeTransferable(node);
		}
		
		/**
		 * Finishes the drag and drop event; this is where the data gets moved.
		 * @param source source component
		 * @param data the data to transfew
		 * @param action the action (move, copy, cut)
		 */
		@Override
		protected void exportDone(JComponent source, Transferable data, int action) {
			/* only handle moves of TreeNodes inside JTree */
			if (action != MOVE || !(source instanceof JTree) || !(data instanceof TreeNodeTransferable)) return;

			JTree tree = (JTree) source;
			DefaultMutableTreeNode node = ((TreeNodeTransferable)data).getNode();
			
			/* get the destination path; if it's null, move under root node */
			DefaultMutableTreeNode dest = this.window.store.getRoot();
			TreePath path = tree.getSelectionPath();
			if (path != null) dest = (DefaultMutableTreeNode) path.getLastPathComponent();
			
			/* execute the move */
			this.window.move(dest, node);
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
		 * No idea what this function is actually for, but doesn't work without it.
		 * @return always returns true
		 */
		@Override
		public boolean importData(TransferSupport support) {
			return true;
		}
	}
	
	/**
	 * Sub-class of Transferable that can transfer DefaultMutableTreeNode objects.
	 * @author anonpds <anonpds@gmail.com>
	 */
	/* TODO maybe make this ObjectTranferable, which transfers any kind of objects? */
	class TreeNodeTransferable implements Transferable {
		/** The node to transfer. */
		private DefaultMutableTreeNode node;
		
		/** The "flavours" of data accepted. */
		private DataFlavor[] flavor;

		/**
		 * Default constructor.
		 * @param node the node to transfer
		 */
		public TreeNodeTransferable(DefaultMutableTreeNode node) {
			this.node = node;
			/* create a list of the accepted data "flavours"; only DefaultMutableTreeNode classes are accepted */
			this.flavor = new DataFlavor[1];
			this.flavor[0] = new DataFlavor(DefaultMutableTreeNode.class, "DefaultMutableTreeNode");
		}
		
		/**
		 * Returns the node that is being transferred.
		 * @return the transferred node
		 */
		public DefaultMutableTreeNode getNode() {
			return this.node;
		}

		/**
		 * Returns the transfer data of the specified "flavour".
		 * @param flavor the flavour of the data to receive
		 * @return the data object in the given flavour
		 */
		/* TODO is this actually needed? Or perhaps getNoder() should be removed and this used instead? */
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
}
