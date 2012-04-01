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
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * A sub-class of Task, which adds file system storage features to the task.
 * @author anonpds <anonpds@gmail.com>
 */
public class FileSystemTask extends Task {
	/** Configuration variable name for the task name. */
	private static final String CONFIG_NAME = "name";
	
	/** Configuration variable name for the task creation time. */
	private static final String CONFIG_CREATION_TIME = "creation_time";
	
	/** Configuration variable for the task status. */
	private static final String CONFIG_STATUS = "status";

	/** The name of the file that contains task meta data. */
	private static final String META_FILE = "task.cfg";

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

		/* read the meta data; if the meta data file does not exist, the path does not contain a task */
		String name = null, date = null, status;
		File metaFile = new File(path, META_FILE);
		if (!metaFile.exists()) return null;

		/* use the fancy Configuration class to read and parse the meta data variables */
		Configuration conf = Configuration.parse(metaFile);
		name = conf.get(CONFIG_NAME);
		date = conf.get(CONFIG_CREATION_TIME);
		status = conf.get(CONFIG_STATUS);
		
		/* validate and parse the variables */
		if (name == null) throw new Exception("no name in metadata " + metaFile.getPath());
		if (date == null) throw new Exception("no date in metadata " + metaFile.getPath());
		long timeStamp = Long.parseLong(date);

		/* set the name, creation date and status */
		task.setName(name);
		task.setCreationTime(timeStamp);
		try { task.setStatus(Short.parseShort(status)); } catch (Exception e) {}
		
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
		
		/* write the meta data in new format only */
		File metaFile = new File(path, META_FILE);
		Configuration conf = new Configuration();
		conf.add(CONFIG_NAME, this.getName());
		conf.add(CONFIG_CREATION_TIME, this.getCreationTime());
		conf.add(CONFIG_STATUS, this.getStatus());
		conf.store(metaFile);
		
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

	/**
	 * Removes the task files from a directory.
	 * @param path the directory path from which to remove the files
	 */
	public static void removeTaskFiles(File path) {
		File metaFile = new File(path, META_FILE);
		File textFile = new File(path, TEXT_FILE);
		
		if (metaFile.exists()) metaFile.delete();
		if (textFile.exists()) textFile.delete();
	}
}
