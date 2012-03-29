/* Util.java - Part of Task Mistress
 * Written in 2012 by anonymous.
 * 
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighbouring rights to
 * this software to the public domain worldwide. This software is distributed without any warranty.
 * 
 * Full license at <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package anonpds.TaskMistress;

import java.awt.Dimension;

/**
 * Static utility functions for Task Mistress.
 * @author anonpds <anonpds@gmail.com>
 */
public class Util {
	/**
	 * Returns a string that defines a Dimension. The string consists of two integer values separated by an 'x'.
	 * @param width the width of the Dimension
	 * @param height the height of the Dimension
	 * @return the string that defines the dimension
	 */
	public static String dimensionString(int width, int height) {
		return width + "x" + height;
	}

	/**
	 * Parses a dimension from a string. 
	 * @param value the string to parse
	 * @return the Dimension parsed from the string or null on error
	 */
	/* TODO throw an Exception on errors */
	public static Dimension parseDimension(String value) {
		if (value == null) return null;

		/* find the separating 'x' in the string */
		int i = value.indexOf('x');
		if (i == -1) return null;
		
		/* separate to two strings (catch IndexOutOfBoundsExceptions) */
		String wString, hString;
		try {
			wString = value.substring(0, i);
			hString = value.substring(i+1);
		} catch (Exception e) { return null; }
		
		Dimension d = null;
		try {
			int w = Integer.parseInt(wString);
			int h = Integer.parseInt(hString);
			d = new Dimension(w, h);
		} catch (Exception e) { }
		
		return d;
	}
}
