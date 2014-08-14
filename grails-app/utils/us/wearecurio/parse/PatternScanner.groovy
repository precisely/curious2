/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package us.wearecurio.parse

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import groovy.transform.TypeChecked

import java.util.regex.Pattern
import java.util.regex.Matcher

import javax.swing.text.Segment

/**
 *
 * @author mitsu
 */
@TypeChecked
class PatternScanner {

	char[] text
	int begin
	int endMatch
	Matcher matcher // current matcher
	LinkedList<ScannerState> stateStack = new LinkedList<ScannerState>()
	
	static Pattern whitespacePattern = ~/(?i)^\s*/
	
	static class ScannerState {
		int begin
		int endMatch
		Matcher matcher
		Closure closure
		
		public ScannerState(int begin, int endMatch, Matcher matcher, Closure c) {
			this.begin = begin
			this.endMatch = endMatch
			this.matcher = matcher
			this.closure = c
		}
		
		public int getBegin() {
			begin
		}
		
		public int getEndMatch() {
			endMatch
		}
		
		public Matcher getMatcher() {
			matcher
		}
		
		public Closure getClosure() {
			closure
		}
	}

	public PatternScanner(String string) {
		this.text = string.toCharArray()
		begin = 0
		endMatch = 0
	}
	
	protected void pushState(Closure c) {
		stateStack.addFirst(new ScannerState(begin, endMatch, matcher, c))
	}
	
	protected Closure pullState() {
		ScannerState state = stateStack.removeLast()
		this.begin = state.begin
		this.endMatch = state.endMatch
		this.matcher = state.matcher
		return state.closure
	}
	
	/**
	 * Used to retrieve current scanner state for later reset
	 * @return
	 */
	public ScannerState fetchScannerState() {
		return new ScannerState(begin, endMatch, matcher, null)
	}
	
	/**
	 * Used to reset current scanner state and backtrack to a location
	 * @return
	 */
	public void backtrackTo(ScannerState state) {
		this.begin = state.begin
		this.endMatch = state.endMatch
		this.matcher = state.matcher
		
		stateStack.clear()
	}
	
	protected CharSequence nextString() {
		if (endMatch >= 0) {
			begin = endMatch
			endMatch = -1
		}
		return new Segment(text, begin, text.size() - begin)
	}
	
	public String remainder() {
		int b = this.begin
		if (endMatch >= 0) {
			b = endMatch
		}
		return new String(text, b, text.size() - b)
	}
	
	public boolean atEnd() {
		return begin >= text.size() || endMatch >= text.size()
	}
	
	public boolean match(Pattern pattern) {
		if (atEnd())
			return false
		
		matcher = pattern.matcher(nextString())
		boolean retVal = matcher.lookingAt()
		if (retVal) {
			endMatch = begin + matcher.end()
			return retVal
		}

		return false
	}

	public boolean skip(Pattern pattern) {
		if (atEnd())
			return false

		Matcher matcher = pattern.matcher(nextString())
		boolean retVal = matcher.lookingAt()
		if (retVal) {
			endMatch = begin + matcher.end()
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
	
	/**
	 * Start a conditional match --- this pattern matches only if a pattern (or multiple patterns) follow it
	 * 
	 * Usage:
	 * 
	 * if (scanner.tryMatch(pattern, { ... })) {
	 * 		if (!scanner.followedBy(nextPattern, { ... }))
	 * 		if (!scanner.followedBy(nextPattern2, { ... }))
	 * 		scanner.endTry()
	 * }
	 * 
	 * @param pattern
	 * @param c
	 * @return
	 */
	public boolean tryField(Pattern pattern, Closure c = null) {
		if (this.match(pattern)) {
			pushState(c)
			skipWhite()
			
			return true
		}
		
		return false
	}
	
	public boolean tryMatch(Pattern pattern, Closure c = null) {
		if (this.match(pattern)) {
			pushState(c)
			
			return true
		}
		
		return false
	}

	public boolean followedBy(Pattern pattern, Closure c = null) {
		if (this.match(pattern)) {
			pushState(c)
			confirmTry()
			
			return true
		}
		
		return false
	}
	
	public boolean followedByField(Pattern pattern, Closure c = null) {
		if (this.match(pattern)) {
			pushState(c)
			confirmTry()
			skipWhite()
			
			return true
		}
		
		return false
	}
	
	public boolean followedByEnd() {
		if (atEnd()) {
			confirmTry()

			return true
		}
		
		return false
	}

	public void confirmTry() {
		while (!stateStack.isEmpty()) {
			Closure closure = pullState()
			if (closure != null)
				closure()
		}
	}
	
	public void backtrackTry() {
		if (stateStack.isEmpty()) return
		
		ScannerState state = stateStack.getFirst()
		
		this.matcher = null
		this.begin = state.begin
		this.endMatch = -1
		
		stateStack.clear()
	}

	public boolean matchField(Pattern pattern, Closure c) {
		if (this.matchField(pattern)) {
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
		return !atEnd()
	}

	public String group() {
		return matcher.group()
	}

	public String group(int group) {
		return matcher.group(group)
	}
}
