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
class PatternScanner {

	private static def log = LogFactory.getLog(this)

	String str // current un-parsed part of the string
	Matcher matcher // current matcher
	static Pattern whitespacePattern = ~/(?i)^\s*/

	public PatternScanner(String str) {
		this.str = str
	}

	public boolean match(Pattern pattern) {
		if (str == null)
			return false

		matcher = pattern.matcher(str)
		log.debug "Trying to match " + pattern.pattern() + " on string: '" + str + "'"
		boolean retVal = matcher.lookingAt()
		if (retVal) {
			str = str.substring(matcher.end())
			log.debug "Matched pattern, string now: " + str
			return retVal
		} else
			log.debug "Failed to match pattern"

		return false
	}

	public boolean skip(Pattern pattern) {
		if (str == null)
			return false

		def matcher = pattern.matcher(str)
		boolean retVal = matcher.lookingAt()
		if (retVal) {
			str = str.substring(matcher.end())
			return retVal
		}

		return false
	}

	public boolean matchField(Pattern pattern) {
		if (match(pattern)) {
			skipWhite()
			return true
		}

		return false
	}

	public boolean match(Pattern pattern, Closure c) {
		if (this.match(pattern)) {
			c()
			return true
		}

		return false
	}

	public boolean matchField(Pattern pattern, Closure c) {
		if (this.matchField(pattern)) {
			c()
			return true
		}

		return false
	}

	public boolean match(PatternClosure patternClosure) {
		if (this.match(patternClosure.getPattern())) {
			patternClosure.getClosure().call()
			return true
		}

		return false
	}

	public boolean matchField(PatternClosure patternClosure) {
		if (this.matchField(patternClosure.getPattern())) {
			patternClosure.getClosure().call()
			return true
		}

		return false
	}

	public boolean match(PatternClosure patternClosure, Closure c) {
		if (this.match(patternClosure.getPattern())) {
			patternClosure.getClosure().call()
			c()
			return true
		}

		return false
	}

	public boolean matchField(PatternClosure patternClosure, Closure c) {
		if (this.matchField(patternClosure.getPattern())) {
			patternClosure.getClosure().call()
			c()
			return true
		}

		return false
	}

	public boolean skipWhite() {
		return skip(whitespacePattern)
	}

	public boolean matchWhite() {
		return match(whitespacePattern)
	}

	// Returns true if there is any remaining string to parse
	public boolean remaining() {
		return str == null || str.length() == 0
	}

	public String group() {
		return matcher.group()
	}

	public String group(int group) {
		return matcher.group(group)
	}
}
