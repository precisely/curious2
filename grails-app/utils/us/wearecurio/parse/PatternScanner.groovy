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
	int lastStateCount
	int stateCount
	int begin
	int endMatch
	Matcher matcher // current matcher
	LinkedList<ScannerTry> tryStack = new LinkedList<ScannerTry>() // pending tries
	LinkedList<ScannerTry> backtrackStack = new LinkedList<ScannerTry>() // failed tries, don't retry
	Set<Integer> alreadyFired = new HashSet<Integer>()
	
	static final int CONDITION_ANY = -1
	static final int CONDITION_ATEND = 0
	
	static class ScannerTry {
		PatternScanner scanner
		
		// this is the condition ID that is fired if this state is matched
		int conditionId
		
		int begin
		int endMatch
		boolean fired
		Matcher matcher
		Closure fireClosure
		
		// nextConditions are the allowed next matching conditions (must match these next condition ids to match this state
		List<ScannerPattern> nextPatterns
		
		ScannerTry(PatternScanner scanner, int conditionId, int begin, int endMatch, Matcher matcher, List<ScannerPattern> nextPatterns, Closure fireClosure) {
			this.scanner = scanner
			this.conditionId = conditionId
			this.begin = begin
			this.endMatch = endMatch
			this.matcher = matcher
			this.fireClosure = fireClosure
			this.nextPatterns = nextPatterns
			this.fired = false
		}
		
		int getBegin() {
			begin
		}
		
		int getEndMatch() {
			endMatch
		}
		
		Matcher getMatcher() {
			matcher
		}
		
		Closure getFireClosure() {
			fireClosure
		}
		
		void fire() {
			if (fireClosure != null)
				fireClosure()
			this.fired = true
			scanner.setConditionFired(conditionId)
		}
		
		boolean alreadyTried(int conditionId, int begin) {
			return this.begin == begin && this.conditionId == conditionId
		}
		
		boolean hasPostcondition(int conditionId) {
			if (conditionId == CONDITION_ANY) {
				return true
			}
			if (nextPatterns == null) { // unconditional try
				return true
			}
			
			for (ScannerPattern pattern : nextPatterns) {
				if (pattern.conditionId == conditionId) {
					return true
				}
			}
			
			return false
		}
		
		boolean equals(Object o) {
			return this.is(o)
		}
	}

	PatternScanner(String string) {
		this.text = string.toCharArray()
		begin = 0
		endMatch = 0
		// store match counts to determine whether pattern scanner has modified its state since the last call to ready()
		this.lastStateCount = -1
		this.stateCount = 0
	}
	
	boolean ready() {
		if (atEnd()) return false
		
		int lastStateCount = this.lastStateCount
		this.lastStateCount = this.stateCount
		
		return lastStateCount < this.stateCount
	}
	
	boolean resetReady() {
		if (atEnd()) return false
		this.lastStateCount = -1
		
		return true
	}
	
	protected void setConditionFired(int conditionId) {
		++stateCount
		alreadyFired.add(conditionId)
	}
	
	protected boolean backtrackTo(ScannerTry mark) {
		++this.stateCount
		
		while (!tryStack.isEmpty()) {
			ScannerTry s = tryStack.getFirst()
			
			if (s.is(mark)) {
				tryStack.removeFirst()
				while (!backtrackStack.isEmpty()) {
					ScannerTry b = backtrackStack.getFirst()
					if (b.begin > s.begin) {
						backtrackStack.removeFirst() // obsolete backtrack state
					} else
						break
				}
				backtrackStack.addFirst(s)
				
				this.begin = s.begin
				this.endMatch = s.begin
				this.matcher = null
				
				return true
			} else if (s.begin < mark.begin)
				return false
			else
				tryStack.removeFirst()
		}
		
		return false
	}
	
	protected ScannerTry pushTry(int conditionId, List<ScannerPattern> nextPatterns, Closure c) {
		++this.stateCount
			
		if (!tryStack.isEmpty()) {
			ScannerTry p = tryStack.getFirst()
			
			if (!p.hasPostcondition(conditionId)) {
				backtrackTo(p) // fail previous condition since this one does not match it
				return null
			}
		}
		
		ScannerTry t = new ScannerTry(this, conditionId, begin, endMatch, matcher, nextPatterns, c)
		tryStack.addFirst(t)
		
		return t
	}
	
	public boolean trying(int conditionId) {
		for (ScannerTry b : tryStack) {
			if (b.conditionId == conditionId)
				return true
		}
		
		return false
	}
	
	protected boolean alreadyTried(int conditionId) {
		for (ScannerTry b : backtrackStack) {
			if (b.alreadyTried(conditionId, begin))
				return true
		}
		
		return false
	}
	
	protected Closure fireTry(ScannerTry t) {
		this.begin = t.begin
		this.endMatch = t.endMatch
		this.matcher = t.matcher
		return t.fire()
	}
	
	protected CharSequence nextString() {
		if (endMatch >= 0) {
			begin = endMatch
		}
		return new Segment(text, begin, text.size() - begin)
	}
	
	String remainder() {
		int b = this.begin
		if (endMatch >= 0) {
			b = endMatch
		}
		return new String(text, b, text.size() - b)
	}
	
	boolean atEnd() {
		return begin >= text.size() || endMatch >= text.size()
	}
	
	protected boolean match(Pattern pattern) {
		if (atEnd())
			return false
		
		String s = nextString()
			
		matcher = pattern.matcher(s)
		boolean retVal = matcher.lookingAt()
		if (retVal) {
			++this.stateCount
			
			endMatch = begin + matcher.end()
			return retVal
		}

		return false
	}

	/**
	 * Start a conditional match --- this pattern matches only if a pattern (or multiple patterns) follow it
	 * 
	 * unique is true if the condition should only match once
	 */
	boolean tryMatch(ScannerPattern pattern, Closure additionalFireClosure = null) {
		if (pattern.conditionId == CONDITION_ANY) {
			return tryFiringCondition(pattern, additionalFireClosure)
		}
		
		if (pattern.unique && alreadyFired.contains(pattern.conditionId))
			return false
		
		if (pattern.conditionId == CONDITION_ATEND) {
			if (atEnd()) {
				return tryFiringCondition(pattern, additionalFireClosure)
			}
			return false
		}
		
		if (alreadyTried(pattern.conditionId))
			return false
			
		if (this.match(pattern.pattern)) {
			ScannerTry t = pushTry(pattern.conditionId, pattern.nextPatterns,
					additionalFireClosure == null ? pattern.fireClosure : {
						pattern.fire()
						additionalFireClosure()
					})
			
			if (t == null) {
				// already backtracked
				
				return false
			}
			
			if (atEnd()) {
				if (matchesCondition(CONDITION_ATEND)) {
					fireTries()
					
					return true
				}
				return false
			}
			
			for (ScannerPattern nextPattern : pattern.nextPatterns) {
				if (nextPattern.tryMatch(null))
					break
			}
			
			if (!t.fired) {
				backtrackTo(t)
				
				return false
			}
			
			return true
		}
		
		return false
	}
	
	protected boolean matchesCondition(int conditionId) {
		if (tryStack.isEmpty()) return true
		
		// check to see if all conditions have been met
		for (ScannerTry t in tryStack) {
			if (!t.hasPostcondition(conditionId)) {
				backtrackTo(t)
				
				return false
			}
			conditionId = t.conditionId
		}
		
		return true
	}
	
	/**
	 * fire condition --- do not execute closures until all current try conditions are satisfied
	 * @param conditionId
	 */
	protected boolean fireTries() {
		// save state
		int begin = this.begin
		int endMatch = this.endMatch
		Matcher matcher = this.matcher
				
		// fire all tries
		while (!tryStack.isEmpty()) {
			ScannerTry t = tryStack.removeLast()
			
			fireTry(t)
		}
		
		// restore state
		this.matcher = matcher
		this.begin = begin
		this.endMatch = endMatch
		
		return true
	}
	
	private boolean tryFiringCondition(ScannerPattern pattern, Closure additionalFireClosure) {
		if (tryStack.isEmpty()) {
			setConditionFired(pattern.conditionId)
		
			pattern.fire()
			if (additionalFireClosure != null)
				additionalFireClosure()
			
			return true
		}
		
		if (matchesCondition(pattern.conditionId)) {
			setConditionFired(pattern.conditionId)
		
			fireTries()
			
			pattern.fire()				
			if (additionalFireClosure != null)
				additionalFireClosure()
				
			return true
		} else {
			// backtracked
			return false
		}
	}
	
	boolean match(ScannerPattern pattern, Closure additionalFireClosure = null) {
		if (pattern.conditionId == CONDITION_ANY) {
			return tryFiringCondition(pattern, additionalFireClosure)
		}
		
		if (pattern.unique && alreadyFired.contains(pattern.conditionId))
			return false
		
		if (pattern.conditionId == CONDITION_ATEND) {
			if (atEnd()) {
				return tryFiringCondition(pattern, additionalFireClosure)
			}
			return false
		}
		
		if (this.match(pattern.pattern)) {
			return tryFiringCondition(pattern, additionalFireClosure)
		}
		
		return false
	}
	
	// Returns true if there is any remaining string to parse
	boolean remaining() {
		return !atEnd()
	}

	String group() {
		return matcher.group()
	}

	String group(int group) {
		return matcher.group(group)
	}
}
