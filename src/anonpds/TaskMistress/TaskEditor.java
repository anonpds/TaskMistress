/* TaskEditor
 * Written in 2012 by anonymous.
 * 
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighbouring rights to
 * this software to the public domain worldwide. This software is distributed without any warranty.
 * 
 * Full license at <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package anonpds.TaskMistress;

import javax.swing.JTextArea;

/**
 * Class that implements an editor component for editing tasks in Task Mistress.
 * @author anonpds
 */
@SuppressWarnings("serial")
public class TaskEditor extends JTextArea {
	/** Default constructor. */
	public TaskEditor() {
		super();
		
		/* enable word wrapping */
		this.setWrapStyleWord(true);
		this.setLineWrap(true);
		
		/* disable editing by default */
		this.close(null);
	}

	/**
	 * Tells if the editor is open or closed.
	 * @return true is the editor is open, false if closed
	 */
	public boolean isOpen() {
		return this.isEditable();
	}
	
	/**
	 * Opens the editor by setting the initial text and making the text editable.
	 * @param text the initial text of the editor
	 */
	public void open(String text) {
		super.setText(text);
		this.setEditable(true);
	}
	
	/**
	 * Closes the editor by making it non-editable and optionally setting a new text for it.
	 * @param text
	 */
	public void close(String text) {
		if (text != null) this.setText(text);
		this.setEditable(false);
	}
}
