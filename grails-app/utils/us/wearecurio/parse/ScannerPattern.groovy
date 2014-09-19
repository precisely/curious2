/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package us.wearecurio.parse

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import groovy.lang.Closure
import groovy.transform.TypeChecked

import java.util.List
import java.util.regex.Pattern
import java.util.regex.Matcher

import javax.swing.text.Segment

/**
 *
 * @author mitsu
 */
@TypeChecked
class ScannerPattern {
	PatternScanner scanner
	
	int conditionId
	Pattern pattern
	boolean unique
	List<ScannerPattern> nextPatterns
	Closure preconditionClosure
	Closure fireClosure
	
	ScannerPattern(PatternScanner scanner, int conditionId) {
		this.scanner = scanner
		this.conditionId = conditionId
		this.pattern = null
		this.unique = false
		this.preconditionClosure = null
		this.fireClosure = null
	}
	
	ScannerPattern(PatternScanner scanner, int conditionId, Pattern pattern, boolean unique, Closure fireClosure, Closure preconditionClosure = null) {
		this.scanner = scanner
		this.conditionId = conditionId
		this.pattern = pattern
		this.unique = unique
		this.fireClosure = fireClosure
		this.preconditionClosure = preconditionClosure
	}
	
	ScannerPattern followedBy(List<ScannerPattern> nextPatterns) {
		this.nextPatterns = nextPatterns
		
		return this
	}
	
	ScannerPattern addFollowedBy(ScannerPattern following) {
		if (this.nextPatterns == null)
			this.nextPatterns = []
		
		this.nextPatterns.add(following)
		
		return this
	}
	
	boolean tryMatch(Closure fireClosure = null) {
		if (this.preconditionClosure != null) {
			if (!(this.preconditionClosure)())
				return false
		}
		if (this.nextPatterns == null) {
			return scanner.match(this, fireClosure)
		}
		return scanner.tryMatch(this, fireClosure)
	}
	
	boolean match(Closure fireClosure = null) {
		return scanner.match(this, fireClosure)
	}
	
	void fire() {
		if (fireClosure != null)
			fireClosure()
	}
}
	
