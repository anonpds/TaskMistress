/* Task.java - Part of Task Mistress
 * Written in 2012 by anonymous.
 * 
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighbouring rights to
 * this software to the public domain worldwide. This software is distributed without any warranty.
 * 
 * Full license at <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package anonpds.TaskMistress;

import javax.swing.tree.DefaultMutableTreeNode;

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
	
	/** The time stamp of the task creation. */
	private long timeStamp;

	/** The text of the task. */
	private String text;

	/** The status of the task; done, undone or default. */
	private short status;
	
	/** Constructs an empty task without a parent. Useful as a root node of a Task tree. */
	public Task() {
		this.parent = null;
		this.name = null;
		this.text = null;
		this.timeStamp = 0;
		this.dirty = true;
		this.status = STATUS_DEFAULT;
	}
	
	/**
	 * Constructs an empty task with parent.
	 * @param parent the parent node
	 */
	public Task(Task parent) {
		this.parent = parent;
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
