/* Configuration.java - Part of Task Mistress
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
import java.io.FileReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Set;

/* TODO add support for sections, if needed */

/**
 * Configuration file parser class.
 * @author anonpds <anonpds@gmail.com>
 */
public class Configuration {
	/** The map of parsed configuration variables. */
	private HashMap<String,String> map;
	
	/** Constructs a new empty Configuration object. */
	public Configuration() {
		this.map = new HashMap<String,String>();
	}
	
	/**
	 * Adds a variable to the list of configuration variables.
	 * @param name the name of the variable
	 * @param value the value of the variable
	 * @return true if a variable with the same name already existed and was overwritten, false if a variable was added
	 */
	public boolean add(String name, String value) {
		return this.map.put(name, value) != null;
	}
	
	/**
	 * Returns the value of the named variable.
	 * @param name the name of variable to query
	 * @return the value of the variable or null if no such variable in the configuration
	 */
	public String get(String name) {
		return this.map.get(name);
	}
	
	/**
	 * Returns the set of all variable names in the configuration.
	 * @return set of all the variable names
	 */
	public Set<String> getAll() {
		return this.map.keySet();
	}
	
	/**
	 * Parses a configuration file and returns the Configuration class created from it.
	 * @param confFile the configuration file to parse
	 * @return the Configuration object parsed from the given file
	 * @throws Exception on errors
	 */
	public static Configuration parse(File confFile) throws Exception {
		Configuration conf = new Configuration();
		
		/* make sure the file exists */
		if (!confFile.exists()) throw new Exception(confFile.getName() + " does not exist.");
		
		/* read the file line at a time */
		BufferedReader reader = new BufferedReader(new FileReader(confFile));
		StringBuffer text;
		String string; 
		int line = 1;
		while ((string = reader.readLine()) != null) {
			text = new StringBuffer(string);
			
			/* read the first token of the line */
			Token token;
			try {
				token = Token.readToken(text);
			} catch (Exception e) {
				throw new Exception(confFile.getName() + ": line " + line + ": " + e.getMessage());
			}

			/* ignore comments and empty lines*/
			if (token.getType() == Token.COMMENT || token.getType() == Token.EMPTY_LINE) continue;
			
			/* expect either a word or a string for parameter name */
			String name = null;
			if (token.getType() == Token.STRING || token.getType() == Token.WORD) {
				name = token.getToken();
				if (token.getType() == Token.STRING) name = Token.flattenString(name);
			} else throw new Exception(confFile.getName() + ": line " + line + ": unexpected token " + token + " in '" + string + "'");
			
			/* expect a string after the parameter name */
			token = Token.readToken(text);
			String value = null;
			if (token.getType() == Token.STRING) {
				value = Token.flattenString(token.getToken());
			} else throw new Exception(confFile.getName() + ": line " + line + ": unexpected token " + token + " in '" + string + "'");
			
			/* finally, there must nothing but a comment or white space at the end */
			token = Token.readToken(text);
			if (token.getType() != Token.COMMENT && token.getType() != Token.EMPTY_LINE) 
				throw new Exception(confFile.getName() + ": line " + line + ": junk at the end of line " + string);
			
			/* add the read parameter to the configuration */
			conf.add(name, value);
		}
		
		return conf;
	}
	
	/**
	 * Stores the configuration variables in a file.
	 * @param file the file to write the configuration to
	 * @throws Exception on file IO error
	 */
	public void store(File file) throws Exception {
		PrintStream stream = new PrintStream(file);
		/* TODO this isn't quite safe, is it? If there's an exception, the stream will not be closed */
		try {
			this.store(new PrintStream(file));
			stream.close();
		} catch (Exception e) { throw e; }
	}
		
	/**
	 * Stores the configuration variables in a file.
	 * @param stream the PrintStream to use for writing
	 * @throws Exception on file IO error
	 */
	public void store(PrintStream stream) throws Exception {
		Set<String> names = this.getAll();
		for (String name : names) {
			/* make the value string */
			String value = "\"" + this.escapeString(this.get(name)) + "\"";
			
			/* if name isn't all word characters, make it into a string too */
			if (!this.isWord(name)) name = "\"" + this.escapeString(name) + "\"";
			
			/* write the name, value pair */
			stream.print(name + " " + value + "\n");
		}
	}
	
	/**
	 * Tests if a string is made entirely of word characters.
	 * @param string the string to test
	 * @return true if the string only contains word characters, false otherwise
	 */
	public boolean isWord(String string) {
		for (int i = 0; i < string.length(); i++)
			if (!Token.isWordCharacter(string.charAt(i))) return false;
		return true;
	}
	
	/**
	 * Replaces certain characters in the input string with escape sequences. The escaped characters and their escape
	 * sequences are: line feed ("\n"), double quote ("\"") and backslash ("\\").
	 * @param orig the original string to escape
	 * @return the escaped string
	 */
	public String escapeString(String orig) {
		StringBuffer escaped = new StringBuffer();
		
		/* go through the characters one at a time and escape the needed ones */
		for (int i = 0; i < orig.length(); i++) {
			switch (orig.charAt(i)) {
			case '"': escaped.append("\\\""); break;
			case '\n': escaped.append("\\n"); break;
			case '\\': escaped.append("\\\\"); break;
			default: escaped.append(orig.charAt(i));
			}
		}
		
		return(escaped.toString());
	}
	
	/**
	 * Class for tokenizing configuration files. There are four possible types of tokens, each denoted by a integer
	 * constant in the class:
	 * 
	 * <ul>
	 *   <li>COMMENT: any hash ('#') character not enclosed in string (in double quotes) starts a comment, which spans
	 *     the rest of the line</li>
	 *   <li>EMPTY_LINE: line with nothing but whitespace characters (see below)</li>
	 *   <li>WORD: one or more "word characters" (see below)</li>
	 *   <li>STRING: a run of characters enclosed by double quotes; can be empty (just a pair of quotes);
	 *    strings can contain double quote character by escaping them with a backslash</li>
	 *  </ul>
	 *  
	 *  Escaping: double quotes, backslashes and line feeds can be escaped (C/Java escape codes) in strings, but they
	 *  are not automatically "flattened". Call flattenString for that.
	 *  
	 *  Whitespace: space and tab characters are considered white space and are ignored before and after tokens.
	 *  
	 *  Word characters: letters in the ASCII range, full stops and underscores are considered word characters and can
	 *  form a WORD token.
	 *   
	 * @author anonpds <anonpds@gmail.com>
	 */
	static class Token {
		/** Identifier of an unitialized token. */
		public static final int NONE = 0;
				
		/** Identifier of a comment token. */
		public static final int COMMENT = 1;

		/** Identifier of an empty (line) token. */
		public static final int EMPTY_LINE = 2;

		/** Identifier of a word token. */
		public static final int WORD = 3;

		/** Identifier of a string token. */
		public static final int STRING = 4;

		/** The type of this token. */
		private int type;
		
		/** The token contents (a word or a string). */
		private String token;
		
		/** Constructs an empty token. */
		public Token() {
			this.type = NONE;
		}
		
		/**
		 * Tests if a character is considered whitespace. Only spaces and tabs in the ASCII range  are considered
		 * whitespace.
		 * @param ch the character to test
		 * @return true if the character is whitespace character, false if not
		 */
		public static boolean isWhitespace(char ch) {
			if (ch == ' ' || ch == '\t') return true;
			return false;
		}
		
		/**
		 * Tests if a character is a word character. Underscore, full stop and letters (all in the ASCII range) are
		 * considered word characters.
		 * @param ch the character to test
		 * @return true if the character is a word character, false if not
		 */
		public static boolean isWordCharacter(char ch) {
			if (ch == '_' || ch == '.') return true;
			if (ch < 128 && Character.isLetter(ch)) return true;
			return false;
		}
		
		/**
		 * Reads the next token from the provided StringBuffer and removes the characters that are part of the token
		 * from it.
		 * @param text the text to tokenize
		 * @return the read token
		 * @throws Exception on parse error
		 */
		public static Token readToken(StringBuffer text) throws Exception {
			/* TODO what to do if there are line feeds in the text? */
			Token token = new Token();

			/* find the first non-whitespace character */
			while (text.length() > 0 && isWhitespace(text.charAt(0))) text.deleteCharAt(0);
			
			/* if end reached, the token is EMPTY_LINE */
			if (text.length() == 0) {
				token.setType(EMPTY_LINE);
				return token;
			}
			
			/* if hash, the token is COMMENT */
			if (text.charAt(0) == '#') {
				token.setType(COMMENT);
				return token;
			}
			
			/* if word character, the token is WORD */
			if (isWordCharacter(text.charAt(0))) {
				int i;
				for (i = 1; i < text.length() && isWordCharacter(text.charAt(i)); i++) ;
				token.setType(WORD);
				token.setToken(text.substring(0, i));
				text.delete(0, i);
				return token;
			}
			
			/* otherwise, expect a string */
			if (text.charAt(0) == '\"') {
				int i;
				for (i = 1; i < text.length(); i++) {
					/* string ends at double quote that is not preceded by a backslash */
					if (text.charAt(i) == '\"' && text.charAt(i-1) != '\\') break;
				}
				if (i == text.length()) throw new Exception("Unenclosed double quote in: " + text);
				
				token.setType(STRING);
				token.setToken(text.substring(1, i));
				text.delete(0, i+1);
				return token;
			}
			
			/* unrecognised token */
			throw new Exception("Bad token '" + text + "'.");
		}
		
		/**
		 * Returns the type of a token.
		 * @return the type of the token
		 */
		public int getType() {
			return this.type;
		}
		
		/**
		 * Sets the type of a token.
		 * @param type the token type
		 */
		public void setType(int type) {
			this.type = type;
		}
		
		/**
		 * Returns the token value.
		 * @return the token value or null if the token does not have a value
		 */
		public String getToken() {
			return this.token;
		}
		
		/**
		 * Sets the token value.
		 * @param token the new token value
		 */
		public void setToken(String token) {
			this.token = token;
		}
		
		/**
		 * "Flattens" a string that may contain escaped characters by replacing the escape sequences with the
		 * characters they represent. 
		 * @param string the string to flatten
		 * @return the flattened string
		 */
		public static String flattenString(String string) {
			StringBuffer result = new StringBuffer();
			for (int i = 0; i < string.length(); i++) {
				if (string.charAt(i) != '\\') result.append(string.charAt(i));
				else {
					i++;
					/* escape character */
					/* TODO write down this escape character handling somewhere */ 
					switch (string.charAt(i)) {
					case 'n': result.append('\n'); break;
					case '"': result.append('"'); break;
					case '\\': result.append('\\'); break;
					default: result.append(string.charAt(i)); break;
					}
				}
			}
			return result.toString();
		}
		
		/**
		 * Returns the string representation of the token.
		 * @return the string representation of the token
		 */
		public String toString() {
			String text;
			
			/* the token type */
			switch (this.getType()) {
			case COMMENT: text = "COMMENT"; break;
			case EMPTY_LINE: text = "EMPTY_LINE"; break;
			case WORD: text = "WORD"; break;
			case STRING: text = "STRING"; break;
			default: text = "UNKNOWN";
			}
			
			/* add the token contents, if they exist */
			if (this.getToken() != null) {
				text = text + "[" + this.getToken() + "]";
			}
			
			return text;
		}
	}
	
	/* DEBUG temporary test program */
	/*
	@SuppressWarnings("javadoc")
	public static void main(String[] args) {
		File file = new File("test.cfg");
		Configuration conf = null;
		
		try {
			conf = Configuration.parse(file);
		} catch (Exception e) {
			System.out.println(file.getPath() + ": error: " + e.getMessage());
			System.exit(1);
		}
		
		System.out.println("List of configuration variables:");
		Set<String> names = conf.getAll();
		for (String name : names) {
			System.out.println("  * " + name + " : " + conf.get(name));
		}
		
		System.out.println("\n\nThe saved configuration:");
		try {
			conf.store(System.out);
		} catch (Exception e) {
			System.out.println("Write error: " + e.getMessage());
		}
	}
	*/
}
