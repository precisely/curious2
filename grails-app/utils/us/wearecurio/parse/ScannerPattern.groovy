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

import grails.compiler.GrailsCompileStatic

/**
 *
 * @author mitsu
 */
@GrailsCompileStatic
class ScannerPattern {
	int conditionId
	Pattern pattern
	boolean unique
	List<ScannerPattern> nextPatterns
	Closure preconditionClosure
	Closure fireClosure
	
	ScannerPattern(int conditionId) {
		this.conditionId = conditionId
		this.pattern = null
		this.unique = false
		this.preconditionClosure = null
		this.fireClosure = null
	}
	
	ScannerPattern(int conditionId, Pattern pattern, boolean unique, Closure fireClosure, Closure preconditionClosure = null) {
		this.conditionId = conditionId
		this.pattern = pattern
		this.unique = unique
		this.preconditionClosure = preconditionClosure
		this.fireClosure = fireClosure
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
	
	boolean tryMatch(PatternScanner scanner, Closure fireClosure = null) {
		if (this.preconditionClosure != null) {
			if (!(this.preconditionClosure)(scanner, scanner.context))
				return false
		}
		if (this.nextPatterns == null) {
			return scanner.match(this, fireClosure)
		}
		return scanner.tryMatch(this, fireClosure)
	}
	
	boolean match(PatternScanner scanner, Closure fireClosure = null) {
		return scanner.match(this, fireClosure)
	}
	
	void fire(PatternScanner scanner) {
		if (fireClosure != null)
			fireClosure(scanner, scanner.context)
	}
}

