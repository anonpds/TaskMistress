/* TaskStore
 * Written in 2012 by anonymous.
 * 
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighbouring rights to
 * this software to the public domain worldwide. This software is distributed without any warranty.
 * 
 * Full license at <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

/* TODO add dirty flag to TaskTreeNode to see which nodes have changed; only write out those nodes */

package anonpds.TaskMistress;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Vector;

/**
 * A class that handles the storage of task trees in Task Mistress.
 * @author anonpds
 */
public class TaskStore {
	/** The name of the file that contains task meta data. */
	private static final String META_FILE = "meta.txt";
	
	/** The task tree that contains the stored tasks. */
	TaskTreeNode taskTree = new TaskTreeNode(null);

	/** The file system path in which the file is stored. */
	private File path;
	
	/**
	 * Creates a new task store from the specified directory.
	 * @param path the directory that stores the tasks
	 * @throws Exception on any IO errors
	 */
	public TaskStore(File path) throws Exception {
		/* make sure the path exists or can at least be created */
		if (!path.exists() && !path.mkdirs()) throw new Exception("cannot create '" + path.getPath() + "'");
		this.path = path;
		
		/* add the directories and their sub-directories recursively as nodes */
		File[] files = this.path.listFiles();
		for (File file : files) {
			if (file.isDirectory()) this.addTaskDirectory(this.taskTree, file);
		}
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

		/* read the meta data */
		String name = null, date = null;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(metaFile));
			name = reader.readLine();
			date = reader.readLine();
			reader.close();
		} catch (Exception e) {
			throw new Exception("can not access '" + metaFile.getPath() + "': " + e.getMessage());
		}

		/* attempt to parse the metadata */
		if (name == null) throw new Exception("no name in metadata " + metaFile.getPath());
		node.setName(name);
		
		if (date == null) throw new Exception("no date in metadata " + metaFile.getPath());
		try {
			node.setCreationTime(Long.parseLong(date));
		} catch (NumberFormatException e) {
			throw new Exception("date '" + date + "' invalid");
		}
		
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
	 * @param node the parent node
	 * @param name the name of the new node to add
	 */
	public void addChild(TaskTreeNode node, String name) {
		TaskTreeNode newNode = new TaskTreeNode(null);
		newNode.setName(name);
		node.add(newNode);
	}

	/**
	 * Removes a node and all its children from the tree.
	 * @param node the node to remove
	 */
	public void remove(TaskTreeNode node) {
		if (node.isRoot()) return; /* never remove the root node */
		node.getParent().remove(node);
	}
	
	/**
	 * Writes the tasks to disk. 
	 * @throws Exception on any error
	 */
	public void writeOut() throws Exception {
		this.writeOutRecurse(this.path, this.getRoot());
	}

	/**
	 * Writes the given node and all its children to disk recursively.
	 * @param path the path to write to
	 * @param node the node to write out
	 * @throws Exception on IO errors
	 */
	private void writeOutRecurse(File path, TaskTreeNode node) throws Exception {
/* DEBUG */ System.out.println("Writing " + node.getName() + " to " + path.getPath());
		/* create the path if it doesn't exist */
		if (!path.exists() && !path.mkdirs()) throw new Exception("can not create " + path);
		
		/* write the node (if not root) */
		if (!node.isRoot()) {
			File metaFile = new File(path, META_FILE);
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(metaFile));
				writer.write(node.getName() + "\n");
				writer.write(Long.toString(node.getCreationTime()) + "\n");
				writer.close();
			} catch (Exception e) {
				throw new Exception("can not write to " + metaFile.getPath() + ": " + e.getMessage());
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
	 * @throws Exception if a suitable file system name cannot be found
	 */
	private void setFileSystemName(File path, TaskTreeNode node) throws Exception {
		if (node.getFileSystemName() != null) return;
/* DEBUG */ System.out.println("Setting name for " + node.getName());
		/* remove all silly characters from the node name and make everything lower-case */
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
		throw new Exception("no suitable path found for " + node.getName());
	}

	/**
	 * Class that implements a tree node.
	 * @author anonpds
	 */
	static class TaskTreeNode {
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

		/**
		 * Constructs a new tree node.
		 * @param fsName the file system name; set to null, unless adding tasks from file
		 */
		public TaskTreeNode(String fsName) {
			this.fsName = fsName;
			this.parent = null;
			this.name = null;
			this.timeStamp = System.currentTimeMillis();
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
		 * Returns the children of the node as an array.
		 * @return the children array
		 */
		public Object[] getChildren() {
			return this.children.toArray();
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
	}
}
