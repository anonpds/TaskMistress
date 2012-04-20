/* Task.java - Part of Task Mistress
 * Written in 2012 by anonymous.
 * 
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighbouring rights to
 * this software to the public domain worldwide. This software is distributed without any warranty.
 * 
 * Full license at <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package anonpds.TaskMistress;

import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

/**
 * Class that implements a task tree node.
 * @author anonpds <anonpds@gmail.com>
 */
@SuppressWarnings("serial")
class Task extends DefaultMutableTreeNode {
	/** The status code for task that has been done. */
	public static final short STATUS_DONE = 1;

	/** The status code for task that has not been done. */
	public static final short STATUS_UNDONE = 2;

	/** The status code for task that doesn't have done/undone status. */
	public static final short STATUS_DEFAULT = 3;

	/** Tells whether the task has changed since last write to disk. */
	private boolean dirty;
	
	/** The name of the task. */
	private String name;
	
	/**
	 * Plain name of the task. This name must be unique among sibling nodes and should be suitable for using as a
	 * file name (or a part of file name), which means that all non-printable ASCII characters are stripped out and
	 * all letters are changed to lower-case (for case-insensitive file systems).
	 */
	private String plainName;
	
	/** Maximum plain name length in characters. */
	public static int MAX_PLAIN_NAME_LEN = 14;
	
	/** The time stamp of the task creation. */
	private long timeStamp;

	/** The text of the task. */
	private String text;

	/** The status of the task; done, undone or default. */
	private short status;
	
	/** Constructs an empty task without a parent. Useful as a root node of a Task tree. */
	public Task() {
		this(null, null, null, 0L, true);
	}
	
	/**
	 * Constructs an empty task with parent.
	 * @param parent the parent node
	 */
	public Task(Task parent) {
		this(parent, null, null, 0L, true);
	}
	
	/**
	 * Constructs a new task object.
	 * @param parent the parent of this Task
	 * @param name the name of the task 
	 * @param text the text of the task
	 * @param timeStamp the creation time stamp of the task
	 * @param dirty true if the task is newly created, false if it was loaded from disk
	 */
	public Task(Task parent, String name, String text, long timeStamp, boolean dirty) {
		this.parent = parent;
		this.name = name;
		this.text = text;
		this.timeStamp = timeStamp;
		this.dirty = dirty;
		this.status = STATUS_DEFAULT;
		
		this.setPlainName();
	}
	
	/**
	 * Override the setParent method to update the plain name when a Task is moved in the tree.
	 * @param parent the new parent
	 */
	@Override
	public void setParent(MutableTreeNode parent) {
		super.setParent(parent);
		this.setPlainName();
	}
	
	/**
	 * Sets the plain name of a task. The plain name is a name with all the non-ASCII characters stripped out, all
	 * characters set to lower-case and with possible filler characters to make the name unique among siblings of
	 * the node.
	 */
	public void setPlainName() {
		/* root does not need a plain name */
		if (this.getParent() == null) return;
		
		/* don't change the plain name if it's already set and unique */
		if (this.plainName != null && !this.isPlainNameUsed(this.plainName)) return;
		
		/* strip all non-printable, non-ASCII characters from the name and make all letters lower-case */
		String plainName = "";
		for (int i = 0; this.name != null && i < this.name.length(); i++) {
			char ch = Character.toLowerCase(this.name.charAt(i));
			if (ch < 128 && Character.isLetterOrDigit(ch)) plainName = plainName + ch;
		}
		
		/* make sure the plain name is not too long */
		if (plainName.length() > MAX_PLAIN_NAME_LEN) plainName = plainName.substring(0, 14);
		
		/* see if the plain name is unique among the node's siblings */
		if (plainName.length() > 0) {
			if (!this.isPlainNameUsed(plainName)) {
				this.plainName = plainName;
				return;
			}
			/* if no duplicate found, use the name as is */
		}
		
		/* the plain name is not unique; add random characters to the end until a unique one is found */
		String chars = "0123456789abcdefghijklmnopqrstuvwxyz"; /* list of chars to add */
		for (int i = 1; i < Integer.MAX_VALUE; i++) {
			/* make the string of additional characters */
			String add = "";
			for (int j = i; j > 0; j = j / chars.length()) {
				add = add + chars.charAt(j % chars.length());
			}

			/* make sure the string stays short enough */
			String newName = plainName + add;
			if (plainName.length() + add.length() > MAX_PLAIN_NAME_LEN) {
				newName = plainName.substring(0, MAX_PLAIN_NAME_LEN - add.length()) + add;
			}
			
			/* see if the new name is unique */
			if (!this.isPlainNameUsed(newName)) {
				this.plainName = newName;
				return;
			}
		}
	}
	
	/**
	 * Sets the given plain name for the task.
	 * @param plainName the plain name to set
	 */
	public void setPlainName(String plainName) {
		this.plainName = plainName;
		this.setPlainName(); /* make sure the name is unique */
	}

	/**
	 * Checks if the given plain name is used by one of the tasks siblings.
	 * @return true if the name is used, false if not
	 */
	private boolean isPlainNameUsed(String plainName) {
		Task parent = (Task) this.getParent();
		for (int i = 0; i < parent.getChildCount(); i++) {
			Task sibling = (Task) parent.getChildAt(i);
			if (sibling == this) continue; /* ignore ourself */
			
			String siblingPlain = sibling.getPlainName();
			if (siblingPlain != null && plainName.compareTo(siblingPlain) == 0) return(true); /* duplicate found! */
		}
		return(false);
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
	 * Returns the node creation time.
	 * @return the creation time stamp
	 */
	public long getCreationTime() {
		return this.timeStamp;
	}
	
	/**
	 * Sets the creation time of the task.
	 * @param timeStamp the creation time
	 */
	public void setCreationTime(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	/**
	 * Sets the name of the node.
	 * @param name the new name of the node
	 */
	public void setName(String name) {
		if (this.name == name) return;
		
		if (name == null || this.name == null || (this.name != null && name.compareTo(this.name) != 0)) {
			this.dirty = true;
			this.name = name;
			this.setPlainName(); /* update the plain name */
		}
	}

	/**
	 * Returns the name of the node.
	 * @return the name of the node
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Returns the plain name of the node.
	 * @return the plain name of the node
	 */
	public String getPlainName() {
		return this.plainName;
	}
	
	/**
	 * Returns the full plain name of this task, containing a dot separated list of plain names from the root to this
	 * node.
	 * @return the full plain name of this task
	 */
	public String getFullPlainName() {
		/* root doesn't have plain name */
		if (this.isRoot()) return null;
		
		/* create a path of nodes from this node to root */
		Vector<Task> path = new Vector<Task>();
		for (Task task = this; task.getParent() != null; task = (Task) task.getParent()) {
			path.add(task);
		}

		/* traverse the path in reverse order */
		String name = path.get(path.size() - 1).getPlainName();
		for (int i = path.size() - 2; i >= 0; i--) {
			name = name + "." + path.get(i).getPlainName();
		}
		
		return name;
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
		if (this.text == text) return;
		
		if (text == null || this.text == null || (this.text != null && text.compareTo(this.text) != 0)) {
			this.dirty = true;
			this.text = text;
		}
	}
	
	/**
	 * Returns the status of the task.
	 * @return the status of the task
	 */
	public short getStatus() {
		return this.status;
	}
	
	/**
	 * Sets the status of the task.
	 * @param status the new status of the task
	 * @throws Exception if the status parameter is illegal
	 */
	public void setStatus(short status) throws Exception {
		if (status != STATUS_DONE && status != STATUS_UNDONE && status != STATUS_DEFAULT)
			throw new Exception("Bad status");
		if (status == this.status) return; /* no change */

		this.status = status;
		this.setDirty(true);
	}
	
	/**
	 * Returns the string representation of this object.
	 * @return the string
	 */
	public String toString() {
		return this.name;
	}

	/**
	 * Counts the nodes in the tree.
	 * @return the total number of nodes in the tree
	 */
	public int countNodes() {
		int nodes = 1;
		for (int i = 0; i < this.getChildCount(); i++) {
			Task node = (Task) this.getChildAt(i);
			nodes += node.countNodes();
		}
		return(nodes);
	}
	
	/** Sets all the nodes in the tree dirty. */
	public void setAllDirty() {
		this.setDirty(true);
		for (int i = 0; i < this.getChildCount(); i++) {
			((Task)this.getChildAt(i)).setAllDirty();
		}
	}
}
