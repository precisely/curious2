/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package us.wearecurio.parse

import org.apache.commons.logging.LogFactory

import java.util.regex.Pattern
import java.util.regex.Matcher

/**
 *
 * @author mitsu
 */
class PatternClosure {
	
	private static def log = LogFactory.getLog(this)

	Pattern pattern
	Closure closure

	public PatternClosure(Pattern p, Closure c) {
		this.pattern = p
		this.closure = c
	}
}
