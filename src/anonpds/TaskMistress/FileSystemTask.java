/* FileSystemTask.java - Part of Task Mistress
 * Written in 2012 by anonymous.
 * 
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighbouring rights to
 * this software to the public domain worldwide. This software is distributed without any warranty.
 * 
 * Full license at <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package anonpds.TaskMistress;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * A sub-class of Task, which adds file system storage features to the task.
 * @author anonpds <anonpds@gmail.com>
 */
public class FileSystemTask extends Task {
	/** The name of the file that contains task meta data. */
	private static final String OLD_META_FILE = "meta.txt";

	/** The name of the file that contains task text. */
	private static final String TEXT_FILE = "task.txt";

	/** The plain name of the task, which is used when writing the task to disk. */
	private String plainName;

	/** Default constructor; creates an empty task. */
	public FileSystemTask() {
		super(null, null, 0, false);
		this.plainName = null;
	}
	
	/**
	 * Constructs a new Task with the given parameters.
	 * @param name
	 * @param text
	 * @param timeStamp
	 * @param dirty
	 * @param plainName
	 */
	public FileSystemTask(String name, String text, long timeStamp, boolean dirty, String plainName) {
		super(name, text, timeStamp, dirty);
		this.plainName = plainName;
	}
	
	/**
	 * Loads a task from disk.
	 * @param path the directory path to load the task from
	 * @return the loaded Task or null if the directory is not a task directory
	 * @throws Exception on errors
	 */
	public static FileSystemTask load(File path) throws Exception {
		/* make sure the path exists and is a directory */
		if (!path.exists() || !path.isDirectory()) throw new Exception(path.getPath() + " does not exist.");

		FileSystemTask task = new FileSystemTask();
		
		/* get the file system name from the path */
		String plainName = path.getName();
		task.setPlainName(plainName);

		/* the directory must contain the meta data file; skip the directory if doesn't exist */
		File metaFile = new File(path, OLD_META_FILE);
		if (!metaFile.exists()) return null;

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
		
		/* validate and parse the meta data */
		if (name == null) throw new Exception("no name in metadata " + metaFile.getPath());
		if (date == null) throw new Exception("no date in metadata " + metaFile.getPath());
		long timeStamp = Long.parseLong(date);

		/* set the name and creation date */
		task.setName(name);
		task.setCreationTime(timeStamp);
		
		/* read the task text if it exists */
		File textFile = new File(path, TEXT_FILE);
		if (textFile.exists()) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(textFile), "UTF-8"));
			String text = "", line;
			while ((line = reader.readLine()) != null) {
				if (text.length() > 0) text = text + "\n";
				text = text + line;
			}
			task.setText(text);
			reader.close();
		} else task.setText(null);
		
		/* clear the dirty flag, since the Task was just read from disk */
		task.setDirty(false);

		return task;
	}
	
	/**
	 * Saves the Task to disk.
	 * @param path the path to save the task to
	 * @throws Exception on IO errors
	 */
	public void save(File path) throws Exception {
		/* create the path if it doesn't exist */
		if (!path.exists() && !path.mkdirs()) throw new Exception("can not create " + path);
		
		/* write the node only if dirty */
		if (!this.isDirty()) return;
		
		/* write the meta data */
		File metaFile = new File(path, OLD_META_FILE);
		try {
			PrintWriter writer = new PrintWriter(metaFile, "UTF-8");
			writer.print(this.getName() + "\n");
			writer.print(Long.toString(this.getCreationTime()) + "\n");
			writer.close();
		} catch (Exception e) {
			throw new Exception("can not write to " + metaFile.getPath() + ": " + e.getMessage());
		}
		
		/* write the task text, if any */
		File textFile = new File(path, TEXT_FILE);
		try {
			PrintWriter writer = new PrintWriter(textFile, "UTF-8");
			if (this.getText() != null) writer.print(this.getText());
			writer.close();
		} catch (Exception e) {
			throw new Exception("can not write to " + textFile.getPath() + ": " + e.getMessage());
		}
		
		/* clear the dirty flag, since the task was just saved */
		this.setDirty(false);
	}

	/**
	 * Returns the plain name of the node.
	 * @return the file system name of the node
	 */
	public String getPlainName() {
		return this.plainName;
	}

	/**
	 * Sets the plain name of the node.
	 * @param name the file system name
	 */
	public void setPlainName(String name) {
		this.plainName = name;
	}
}
