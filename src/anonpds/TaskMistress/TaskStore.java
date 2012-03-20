/* TaskStore
 * Written in 2012 by anonymous.
 * 
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighbouring rights to
 * this software to the public domain worldwide. This software is distributed without any warranty.
 * 
 * Full license at <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package anonpds.TaskMistress;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/* TODO separate the TaskStore from the TaskTreeNode:
 *      this is essential to allow different methods of storing the nodes on disk (for example as XML).
 *      There should be a general TaskTreeNode structure that only contains the node data. The class can then be
 *      overwritten by different TaskStore modules to include the information that is relevant to those classes. 
 */

/* TODO how to fix movement of nodes:
 *      first: the fs names of moved nodes need to be set to null, since they may no longer be valid
 *      second: the old node folders need to be removed or moved (the latter is probably more complex)
 *        - this could be implemented by first adding the exist/not-exist flag to nodes, and then actually making
 *          a copy of the moved nodes, instead of really moving them; the old nodes would be marked non-existent
 *          and would thus be removed from the file system the next time the nodes are saved
 *        - the above way is quite inefficient, but it should do for now -- it will not be a problem, unless there
 *          are huge numbers of nodes to move/save 
 */

/* TODO this class should probably use a TreeModelListener, instead of storing the root node itself. */

/**
 * A class that handles the storage of task trees in Task Mistress.
 * @author anonpds <anonpds@gmail.com>
 */
public class TaskStore {
	/** The name of the file that contains task meta data. */
	private static final String META_FILE = "meta.txt";

	/** The name of the file that contains task text. */
	private static final String TEXT_FILE = "task.txt";
	
	/** The task tree that contains the stored tasks. */
	TaskTreeNode taskTree = new TaskTreeNode(null);

	/** The file system path in which the file is stored. */
	private File path;
	
	/**
	 * Creates a new task store from the specified directory. The directory is created if it doesn't exist and an
	 * empty task store is initialized.
	 * @param path the directory that stores the tasks
	 * @throws Exception on any IO errors
	 */
	public TaskStore(File path) throws Exception {
		/* make sure the path exists or can at least be created */
		if (!path.exists() && !path.mkdirs()) throw new Exception("cannot create '" + path.getPath() + "'");
		this.path = path;
		
		/* add the directories and their sub-directories recursively as nodes */
		/* TODO the addTaskDirectory function should be able to deal with the root node, so that there is no need
		 * for this ugle bit of code.
		 */
		File[] files = this.path.listFiles();
		for (File file : files) {
			if (file.isDirectory()) this.addTaskDirectory(this.taskTree, file);
		}
		
		/* DEBUG */ print();
	}

	/**
	 * Adds the task to the store from a directory and recursively adds all the sub-tasks in sub-directories.
	 * @param tree the tree node to add to
	 * @param path the directory path to add
	 * @throws Exception on any IO errors
	 */
	private void addTaskDirectory(TaskTreeNode tree, File path) throws Exception {
		/* must be a directory */
		if (!path.isDirectory()) throw new Exception("'" + path.getPath() + "' not a directory");

		/* the directory must contain the meta data file */
		File metaFile = new File(path, META_FILE);
		
		/* skip the directory if the meta data file does not exist */
		if (!metaFile.exists()) return;

		/* create new tree node to hold the data */
		TaskTreeNode node = new TaskTreeNode(path.getName());
		
		/* set the node non-dirty, as it was just read from the disk */
		node.setDirty(false);

		/* read the meta data */
		/* TODO implement a better way to handle the meta data */
		String name = null, date = null;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(metaFile));
			name = reader.readLine();
			date = reader.readLine();
			reader.close();
		} catch (Exception e) {
			throw new Exception("can not access '" + metaFile.getPath() + "': " + e.getMessage());
		}

		/* attempt to parse the meta data */
		if (name == null) throw new Exception("no name in metadata " + metaFile.getPath());
		node.setName(name);
		
		if (date == null) throw new Exception("no date in metadata " + metaFile.getPath());
		try {
			node.setCreationTime(Long.parseLong(date));
		} catch (NumberFormatException e) {
			throw new Exception("date '" + date + "' invalid");
		}
		
		/* read the task text if it exists */
		File textFile = new File(path, TEXT_FILE);
		String text = "";
		if (textFile.exists()) {
			try {
				String line;
				BufferedReader reader = new BufferedReader(new FileReader(textFile));
				while ((line = reader.readLine()) != null) {
					if (text.length() > 0) text = text + "\n";
					text = text + line;
				}
				reader.close();
			} catch (Exception e) { /* TODO error handling */ }
		}
		node.setText(text);
		
		/* add the node to the tree */
		tree.add(node);
		
		/* finally recurse into any potential sub-directories */
		File[] files = path.listFiles();
		for (File file : files) {
			if (file.isDirectory()) this.addTaskDirectory(node, file);
		}
	}
	
	/**
	 * Returns the root node of the tree.
	 * @return the root node
	 */
	public TaskTreeNode getRoot() {
		return this.taskTree;
	}

	/**
	 * Adds a named node as a child of the given node.
	 * @param parent the parent node
	 * @param name the name of the new node to add
	 * @return the added node
	 */
	public TaskTreeNode addChild(TaskTreeNode parent, String name) {
		/* TODO rename this to just add; I considered having both addChild and addSibling, but now I can't think of
		 * a reason to have addSibling, when you can just call: addChild(node.getParent(), name)
		 */
		TaskTreeNode node = new TaskTreeNode(null);
		node.setName(name);
		parent.add(node);
		node.setDirty(true);
		return(node);
	}

	/**
	 * Returns the file system path of the node.
	 * @param node the node to query
	 * @return the file system path to the node
	 */
	private File getNodePath(TaskTreeNode node) {
		/* start from the task tree root directory */
		File path = this.path;

		/* traverse the tree path to this node */
		TreePath treePath = node.getPath();
		Object[] paths = treePath.getPath();
		/* start traversal from second path object; first is root and already accounted for */
		for (int i = 1; i < paths.length; i++) {
			TaskTreeNode curNode = (TaskTreeNode) paths[i];
			String curPath = curNode.getFileSystemName();
			if (curPath == null) this.setFileSystemName(path, curNode);
			path = new File(path, curNode.getFileSystemName());
		}
		
		return path;
	}
	
	/**
	 * Deletes a directory and all its contents recursively.
	 * @param path the directory to delete
	 */
	private void deleteDirectory(File path) {
		if (!path.isDirectory()) return;
		File[] files = path.listFiles();
		for (File file : files) {
			if (file.isDirectory()) deleteDirectory(file); /* recurse on directories */
			else file.delete(); /* delete normal files; TODO errors */
		}
		/* when the directory is finally empty, delete itself; TODO errors*/
		path.delete();
	}
	
	/**
	 * Removes a node and all its children from the tree.
	 * @param node the node to remove
	 */
	public void remove(TaskTreeNode node) {
		/* TODO perhaps the node should just be marked non-existing, so that it could be removed from the file system
		 * the next time the tree is saved (at the same time the node would be removed from memory) 
		 */
		if (node.isRoot()) return; /* never remove the root node */
		File path = this.getNodePath(node);
		this.deleteDirectory(path);
		node.getParent().remove(node);
	}
	
	/**
	 * Moves a node and all its children under another node.
	 * @param dest the destination node
	 * @param node the node to move
	 */
	public void move(TaskTreeNode dest, TaskTreeNode node) {
		/* CRITICAL this will mess out the file system layout; do not use it yet! */
		
		/* never move root node */
		if (node.isRoot()) return;
		
		node.getParent().remove(node);
		dest.add(node);
	}
	
	/**
	 * Writes the tasks to disk. 
	 * @throws Exception on any error
	 */
	public void writeOut() throws Exception {
		/* TODO add a version of this that takes the root path as a parameter; useful for when there is an error
		 * saving to the path set in the class instance, so the tree can be saved to another location
		 */
		this.writeOutRecurse(this.path, this.getRoot());
	}

	/**
	 * Writes the given node and all its children to disk recursively.
	 * @param path the path to write to
	 * @param node the node to write out
	 * @throws Exception on IO errors
	 */
	private void writeOutRecurse(File path, TaskTreeNode node) throws Exception {
		/* DEBUG */ System.out.println("Writing out " + this.path.getPath() + ", " + node.getName() + "(" + node.isDirty() + ")");
		/* create the path if it doesn't exist */
		if (!path.exists() && !path.mkdirs()) throw new Exception("can not create " + path);
		
		/* write the node if not root and if dirty */
		if (!node.isRoot() && node.isDirty()) {
			/* write the meta data */
			File metaFile = new File(path, META_FILE);
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(metaFile));
				writer.write(node.getName() + "\n");
				writer.write(Long.toString(node.getCreationTime()) + "\n");
				writer.close();
			} catch (Exception e) {
				throw new Exception("can not write to " + metaFile.getPath() + ": " + e.getMessage());
			}
			
			/* write the task text, if any */
			File textFile = new File(path, TEXT_FILE);
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(textFile));
				if (node.getText() != null) writer.write(node.getText());
				writer.close();
			} catch (Exception e) {
				throw new Exception("can not write to " + textFile.getPath() + ": " + e.getMessage());
			}
		}
		
		/* recurse for each of the child nodes */
		Object[] children = node.getChildren();
		for (Object o : children) {
			TaskTreeNode child = (TaskTreeNode) o;
			
			/* make sure the file system name has been set for the node */
			this.setFileSystemName(path, child);
			File newPath = new File(path, child.getFileSystemName());
			this.writeOutRecurse(newPath, child);
		}
	}

	/**
	 * Sets the file system name of a tree node, if it isn't set already.
	 * @param path the file system path where the node resides
	 * @param node the node
	 */
	/* TODO this can be implemented without the path parameter */
	private void setFileSystemName(File path, TaskTreeNode node) {
		/* don't set for root */
		if (node.isRoot()) return;
		
		/* don't reset the name */
		if (node.getFileSystemName() != null) return;

		/* remove all silly characters from the node name and make everything lower-case */
		/* TODO limit the file system name to something like 20 characters */
		String name = "";
		for (int i = 0; i < node.getName().length(); i++) {
			char ch = node.getName().charAt(i);
			if (ch < 256 && Character.isLetterOrDigit(ch)) name = name + Character.toLowerCase(ch);
		}
		
		/* if any characters were left, try the name as is */
		if (name.length() > 0) {
			File newPath = new File(path, name);
			if (!newPath.exists()) {
				/* success */
				node.setFileSystemName(name);
				return;
			}
		}
		
		/* append numbers at the end of the name until non-existing path is found; */
		/* TODO only 100 numbers are tried at most! Do something about this. */
		for (int i = 0; i < 100; i++) {
			String newName = name + i;
			File newPath = new File(path, newName);
			if (!newPath.exists()) {
				/* success! */
				node.setFileSystemName(newName);
				return;
			}
		}
		
		/* error, no suitable name found */
		throw new RuntimeException("no suitable path found for " + node.getName());
	}

	/* TODO move this to its own file */
	/**
	 * Class that implements a tree node.
	 * @author anonpds <anonpds@gmail.com>
	 */
	static class TaskTreeNode implements TreeNode {
		/** Tells whether the node has changed since last write to disk. Should be set true when a node is created from
		 * scratch and false to when a node is loaded from disk. Every change to the node should set it true. */
		private boolean dirty;
		
		/** The name of the tree node. */
		private String name;
		
		/** The time stamp of the node creation. */
		private long timeStamp;

		/** The list of children in this node. */
		private Vector<TaskTreeNode> children = new Vector<TaskTreeNode>();

		/** The parent of this node; null for the root node. */
		private TaskTreeNode parent;

		/** The file system name of the node. */
		private String fsName;

		/** The text of the task. */
		private String text;

		/* TODO add several constructors; most should set the node dirty */
		/**
		 * Constructs a new tree node.
		 * @param fsName the file system name; set to null, unless adding tasks from file
		 */
		public TaskTreeNode(String fsName) {
			this.fsName = fsName;
			this.dirty = false;
			this.parent = null;
			this.name = null;
			this.timeStamp = System.currentTimeMillis();
			this.text = "";
		}

		/**
		 * Tells whether this node can have children.
		 * @return always true
		 */
		public boolean getAllowsChildren() {
			return true;
		}
		
		/**
		 * Adds a new node to the tree.
		 * @param node the node to add
		 */
		public void add(TaskTreeNode node) {
			node.parent = this;
			this.children.add(node);
		}

		/**
		 * Removes a child from the node.
		 * @param node the node to remove
		 */
		public void remove(TaskTreeNode node) {
			this.children.remove(node);
		}

		/**
		 * Returns the children of the node as an Enumeration.
		 * @return the children of the node
		 */
		public Enumeration<TaskTreeNode> children() {
			return this.children.elements();
		}
		/**
		 * Returns the number of children this node has.
		 * @return the number of children
		 */
		public int getChildCount() {
			return this.children.size();
		}

		/**
		 * Returns the specified child.
		 * @param index the index of the child to return
		 * @return the child node
		 */
		public TaskTreeNode getChildAt(int index) {
			return this.children.get(index);
		}
		
		/**
		 * Returns the children of the node as an array.
		 * @return the children array
		 */
		public Object[] getChildren() {
			return this.children.toArray();
		}
		
		/**
		 * Returns the index of the specified children in the node.
		 * @param child the child whose index to query
		 * @return the index
		 */
		public int getIndex(TreeNode child) {
			return this.children.indexOf(child);
		}

		/**
		 * Returns the parent of this node.
		 * @return the parent node or null if this is the root node
		 */
		public TaskTreeNode getParent() {
			return this.parent;
		}
		
		/**
		 * Tells whether the node is the root node or not.
		 * @return true if this is the root node, false if not
		 */
		public boolean isRoot() {
			return this.parent == null;
		}
		
		/**
		 * Tells whether this is a leaf node or not. A leaf node does not have any children.
		 * @return true if this is a leaf node, false if not
		 */
		public boolean isLeaf() {
			return(this.getChildCount() == 0);
		}

		/**
		 * Tells whether the node was modified since it was last written out.
		 * @return true if the node has unsaved changes, false it not
		 */
		public boolean isDirty() {
			return this.dirty;
		}

		/**
		 * Sets the node to dirty or non-dirty. Only dirty nodes are saved to disk when the tree is written out.
		 * @param dirty true to set the node dirty, false for non-dirty
		 */
		public void setDirty(boolean dirty) {
			this.dirty = dirty;
		}

		/**
		 * Sets the creation time of the node.
		 * @param timeStamp the time of the node's creation
		 */
		public void setCreationTime(long timeStamp) {
			this.timeStamp = timeStamp;
		}
		
		/**
		 * Returns the node creation time.
		 * @return the creation time stamp
		 */
		public long getCreationTime() {
			return this.timeStamp;
		}

		/**
		 * Sets the name of the node.
		 * @param name the new name of the node
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Returns the name of the node.
		 * @return the name of the node
		 */
		public String getName() {
			return this.name;
		}
		
		/**
		 * Sets the file system name of the node.
		 * @param name the file system name
		 */
		public void setFileSystemName(String name) {
			this.fsName = name;
		}

		/**
		 * Returns the file system name of the node.
		 * @return the file system name of the node
		 */
		public String getFileSystemName() {
			return this.fsName;
		}

		/**
		 * Returns the text of the node.
		 * @return the text of the node
		 */
		public String getText() {
			return this.text;
		}

		/**
		 * Sets the text of the node.
		 * @param text the text to set
		 */
		public void setText(String text) {
			this.dirty = true;
			this.text = text;
		}
		
		/**
		 * Returns the string representation of this object.
		 * @return the string
		 */
		public String toString() {
			return this.name;
		}

		/**
		 * Creates the path from root to this node.
		 * @return the path from the tree root to this node
		 */
		public TreePath getPath() {
			/* create a vector of the bottom to top path */
			Vector<TaskTreeNode> v = new Vector<TaskTreeNode>();
			for (TaskTreeNode node = this; node != null; node = node.getParent()) v.add(node);
			
			/* reverse the path as an array of objects */
			Object[] path = new Object[v.size()];
			for (int i = 0; i < v.size(); i++)
				path[i] = v.get(v.size() - i - 1);
			
			return new TreePath(path);
		}
	}
	
	/* DEBUG */
	@SuppressWarnings("javadoc")
	public void print() {
		print(0, this.getRoot());
	}
	/* DEBUG */
	@SuppressWarnings("javadoc")
	public void print(int depth, TaskTreeNode node) {
		for (int i = 0; i < depth; i++) System.out.print("  ");
		System.out.println(node.getName() + ": " + node.getText());
		for (int i = 0; i < node.getChildCount(); i++)
			print(depth + 1, node.getChildAt(i));
	}
}
