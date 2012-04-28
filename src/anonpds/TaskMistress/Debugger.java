/* Debugger.java - Part of Task Mistress
 * Written in 2012 by anonymous.
 * 
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighbouring rights to
 * this software to the public domain worldwide. This software is distributed without any warranty.
 * 
 * Full license at <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package anonpds.TaskMistress;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

/**
 * Simple debugger module for Task Mistress. Catches debug messages and can be used to display run-time information
 * about the running instances of Task Mistress.
 * @author anonpds <anonpds@gmail.com>
 */
public class Debugger {
	/** The maximum number of stored debug messages. */
	private static final int MAX_MESSAGES = 10000;
	
	/** The stored debug messages. */
	private static Vector<String> messages = null;
	
	/** Is the debugger window shown? */
	private static boolean debuggerShown = false;
	
	/**
	 * Adds a debug message.
	 * @param msg the message to add
	 */
	public static void addMessage(String msg) {
		if (messages == null) messages = new Vector<String>(MAX_MESSAGES);
		
		/* remove the first if maximum number reached */
		if (messages.size() >= MAX_MESSAGES) {
			messages.remove(0);
		}
		
		/* add to the end */
		DateFormat df = DateFormat.getTimeInstance(DateFormat.MEDIUM);
		messages.add(df.format(new Date()) + ": " + msg);
	}
	
	/**
	 * Shows the debugger window. 
	 * @param owner the owner (used to make the debugger window modal)
	 * @param store the TaskStore to debug
	 */
	public static void showDebugger(JFrame owner, TaskStore store) {
		/* quit if the debugger window is already open */
		if (!debuggerShown) new DebuggerWindow(owner, store);
		debuggerShown = true;
	}
	
	@SuppressWarnings("serial")
	static class DebuggerWindow extends JDialog {
		private TaskStore store;

		public DebuggerWindow(JFrame owner, TaskStore store) {
			super(owner, true);

			this.store = store;
			
			/* create a tabbed pane with the components in tabs */
			JTabbedPane tabs = new JTabbedPane();
			tabs.add("Tree", createTreeView());
			tabs.add("Messages", createMessageView());
			
			/* add to the window and set the window */
			this.setTitle(TaskMistress.PROGRAM_NAME + " " + TaskMistress.PROGRAM_VERSION + ": Debugger");
			this.addWindowListener(new CloseListener());
			this.add(tabs);
			this.setSize(600, 400);
			this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			this.setVisible(true);
		}
		
		private JComponent createTreeView() {
			/* create a non-editable JTextArea */
			JTextArea text = new JTextArea("");
			text.setEditable(false);
			
			/* create a model of the tree structure in text */
			StringBuffer buffer = new StringBuffer("The tree structure:\n\n");
			this.mapTask(buffer, this.store.getRoot(), 0);
			text.setText(buffer.toString());
			
			/* put the text area in a panel and return the panel*/
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(new JScrollPane(text), BorderLayout.CENTER);
			return(panel);
		}
		
		private JComponent createMessageView() {
			/* create a non-editable JTextArea */
			JTextArea text = new JTextArea("");
			text.setEditable(false);

			/* add the messages to the text area */
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < messages.size(); i++) {
				buffer.append(messages.get(i) + "\n");
			}
			text.setText(buffer.toString());

			/* put the text area in a panel and return the panel*/
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(new JScrollPane(text), BorderLayout.CENTER);
			return(panel);
		}
		
		public void mapTask(StringBuffer buffer, Task task, int depth) {
			for (int i = 0; i < task.getChildCount(); i++) {
				for (int j = 0; j < depth; j++) buffer.append("  ");
				Object obj = task.getChildAt(i);
				if (obj instanceof Task) {
					Task child = (Task) obj;
					
					if (child instanceof FileSystemTask) buffer.append("(FileSystemTask) ");
					else buffer.append("(Task) ");
					
					buffer.append(child.getName());
					buffer.append(" (" + (child.isDirty() ? "dirty" : "clean") + ")");
					buffer.append("\n");

					/* recurse for child tasks */
					mapTask(buffer, child, depth + 1);
				} else buffer.append("(Object) " + obj + "\n");
			}
		}
		
		class CloseListener extends WindowAdapter {
			@Override
			public void windowClosed(WindowEvent e) {
				Debugger.debuggerShown = false;
			}
		}
	}
}
