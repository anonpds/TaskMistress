/* TaskMistress.java - Part of Task Mistress
 * Written in 2012 by anonymous.
 * 
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighbouring rights to
 * this software to the public domain worldwide. This software is distributed without any warranty.
 * 
 * Full license at <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package anonpds.TaskMistress;

import java.io.File;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/* CRITICAL add a debugger; something that stores debug information and outputs it in case of an error.
 * The debugger should only be called during errors and the debugger will make a large dump of data: the full task
 * tree, the configuration, what went wrong (the exception), an optional message from the caller, the task tree path,
 * etc.
 */

/**
 * A class that runs the TaskMistress program.
 * @author anonpds <anonpds@gmail.com>
 */
public class TaskMistress {
	/** Configuration file directory for the program. */
	private static final String CONFIG_DIR = "TaskMistress";
	
	/** Configuration file name for the program. */
	private static final String CONFIG_FILE = "main.cfg";

	/** The name of the program. */
	public static final String PROGRAM_NAME = "Task Mistress";

	/** The current version of the program. */
	public static final String PROGRAM_VERSION = "0.1f";

	/** The name of the variable that contains the default task tree path. */
	public static final String CONFIG_DEFAULT = "defaultTree";

	/** The number of task tree paths kept in history. */
	public static final int HISTORY_SIZE = 10;

	/** The history configuration variable name; a number is appended to this*/
	public static final String CONFIG_HISTORY = "history.";

	/** The current configuration. */
	private static Configuration config;
	
	/**
	 * Launches an instance of the program at the given file system path.
	 * @param path the path to the task tree to edit
	 * @param ignoreLock set to true to ignore the lock file, false not to ignore it
	 * @throws TaskTreeLockedException if the task tree is locked (already open)
	 * @throws Exception when the TaskStore cannot be initialised
	 */
	public TaskMistress(File path, boolean ignoreLock) throws TaskTreeLockedException, Exception {
		TaskStore store = new TaskStore(path, ignoreLock);
		new MainWindow(store);
	}
	
	/**
	 * Shows a dialog that allows the user to select a directory path.
	 * @return the selected directory or null if no directory was selected
	 */
	public static File showPathDialog() {
		/* create a file chooser dialog that only allows single selection and only directories */
		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		/* show the dialog and return the selected directory (returns null if no directory chosen) */
		chooser.showOpenDialog(null);
		return(chooser.getSelectedFile());
	}
	
	/** Prompts user for a task tree to open and opens it. */
	public static void openTaskTree() {
		File path = TaskMistress.showPathDialog();
		if (path != null) openTaskTree(path, true);
	}

	/**
	 * Opens the specified task tree.
	 * @param path the directory path of the task tree to open
	 * @param secondary set to true if this is a secondary open done from an existing task tree window, false the
	 * program is just starting
	 */
	private static void openTaskTree(File path, boolean secondary) {
		boolean ignoreLock = false;
		while (true) {
			try {
				addToHistory(config, path);
				new TaskMistress(path, ignoreLock);

				/* if no default, make the used path default */
				if (!secondary && config.get(CONFIG_DEFAULT) == null) config.add(CONFIG_DEFAULT, path.getPath());
				
				/* if the tree was opened successfully, the loop is done */
				break;
			} catch (TaskTreeLockedException e) {
				/* the tree is locked; ask whether to open it anyway */
				int choice = JOptionPane.showConfirmDialog(null,
				                              "Warning: the task tree may already be open! Proceed anyway?",
				                              PROGRAM_NAME + " " + PROGRAM_VERSION,
				                              JOptionPane.YES_NO_OPTION);
				
				/* on yes, open the tree anyway, on no just quit the program */
				if (choice == JOptionPane.YES_OPTION) ignoreLock = true;
				else break;
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null,
				                              "Task tree load failed: " + e.getMessage(),
				                              PROGRAM_NAME + " " + PROGRAM_VERSION,
				                              JOptionPane.ERROR_MESSAGE);
				break;
			}
		}
	}

	
	/**
	 * Returns the configuration file. Three environment variables are examined for the directory that contains the
	 * configuration file in the following order: XDG_CONFIG_HOME, HOME and APPDATA. If HOME is used, a directory
	 * called ".config" is appended to it.
	 * 
	 * In all three cases the directory stored in the constant CONFIG_DIR is further appended to the used environment
	 * variable to get the directory. Then, finally, the actual configuration file name is appended from the constant
	 * CONFIG_FILE.
	 * 
	 * @return the configuration file or null if no suitable place for the configuration file was found
	 */
	private static File getConfigFile() {
		File path = null;
		
		/* array of tried env. variables and paths that are appended to them */
		String[][] envs = { { "XDG_CONFIG_HOME", null }, { "HOME", ".local" }, { "APPDATA", null } };
		for (int i = 0; i < envs.length; i++) {
			String var = System.getenv(envs[i][0]);
			if (var != null) {
				/* the environment variable exists; use it */
				path = new File(var);
				if (envs[i][1] != null) path = new File(path, envs[i][1]);
				break;
			}
		}
		
		/* if one of the environment variables existed, append the config file directory and name to it*/
		if (path != null) {
			path = new File(path, CONFIG_DIR);
			path = new File(path, CONFIG_FILE);
		}
		
		return path;
	}
	
	/** Opens the settings window, if not already open. */
	public static void showSettings() {
		SettingsWindow.open(config);
	}
	
	/**
	 * Adds a task tree path to the history.
	 * @param conf the configuration where the history is stored
	 * @param path the path to add
	 */
	private static void addToHistory(Configuration conf, File path) {
		int i;
		for (i = 0; i < HISTORY_SIZE; i++) {
			String name = CONFIG_HISTORY + i;
			if (conf.get(name) == null) break;
			if (conf.get(name).compareTo(path.getPath()) == 0) return; /* the path already exists, don't add */
		}
		
		if (i < HISTORY_SIZE) {
			String name = CONFIG_HISTORY + i;
			conf.add(name, path.getPath());
		}
	}
	
	/**
	 * Returns the task tree history.
	 * @return an array of task tree history paths
	 */
	public static String[] getHistory() {
		Vector<String> v = new Vector<String>();

		for (int i = 0; i < HISTORY_SIZE; i++) {
			String name = CONFIG_HISTORY + i;
			if (config.get(name) == null) continue;
			v.add(config.get(name));
		}
		
		String[] array = new String[v.size()];
		for (int i = 0; i < v.size(); i++) {
			array[i] = v.get(i);
		}
		
		return(array);
	}
	
	/** Saves the configuration. */
	public static void saveConfiguration() {
		File confFile = TaskMistress.getConfigFile();

		if (confFile != null && config != null) {
			/* create the conf file path if necessary */
			File path = confFile.getParentFile();
			if (!path.exists()) path.mkdirs();
			try { config.store(confFile); } catch (Exception e) {
				JOptionPane.showMessageDialog(null,
				                              "Could not save configuration: " + e.getMessage(),
				                              "Error!",
				                              JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	/**
	 * Runs the program.
	 * @param args command line arguments (unused)
	 */
	public static void main(String[] args) {
		/* set native look and feel if possible */
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,
			                              "Could not set native look and feel; using the default.",
			                              "Error!",
			                              JOptionPane.ERROR_MESSAGE);
		}

		/* get the configuration file */
		File confFile = TaskMistress.getConfigFile();
		File defaultPath = null;
		
		/* try to parse it if it exists and extract the default task tree */
		try { config = Configuration.parse(confFile); } catch (Exception e) {
			JOptionPane.showMessageDialog(null,
			                              "Failed to parse " + confFile.getPath() + ": " + e.getMessage(),
			                              "Warning",
			                              JOptionPane.WARNING_MESSAGE);
		}
		if (config != null && config.get(CONFIG_DEFAULT) != null) defaultPath = new File(config.get(CONFIG_DEFAULT));

		/* the path can also be set by command line argument, which overrides config */
		/* TODO add decent command line argument handling */
		if (args.length > 0) defaultPath = new File(args[0]);

		if (defaultPath == null || !defaultPath.exists()) {
			/* no default task tree or the default does not exist */
			defaultPath = TaskMistress.showPathDialog();
			if (defaultPath == null) {
				/* no directory chosen, show a message and terminate the program */
				JOptionPane.showMessageDialog(null,
				                              "No directory chosen. Terminating the program.",
				                              PROGRAM_NAME + " " + PROGRAM_VERSION,
				                              JOptionPane.INFORMATION_MESSAGE);
				System.exit(0);
			}
		}
		
		/* create a new config, if no config exists */
		if (config == null) config = new Configuration();

		/* open the task tree */
		openTaskTree(defaultPath, false);
		
		/* exited; save the configuration */
		saveConfiguration();
	}
}
