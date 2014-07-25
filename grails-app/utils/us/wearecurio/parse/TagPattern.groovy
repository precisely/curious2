/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package us.wearecurio.parse

import org.apache.commons.logging.LogFactory

import java.util.regex.Pattern
import java.util.regex.Matcher

/**
 * Class that matches a tag against a syntax pattern
 * 
 * @author mitsu
 */
abstract class TagPattern {
	
	private static def log = LogFactory.getLog(this)

	/**
	 * Pass in tag, and value, or collection of values
	 * 
	 * Returns match if a multi-dimensional tag
	 * 
	 * @param tag
	 * @return
	 */
	public abstract def match(String tag, def values)
}
