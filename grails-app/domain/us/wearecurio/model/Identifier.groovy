package us.wearecurio.model;

import groovy.time.*
import grails.converters.*

import org.apache.commons.logging.LogFactory

import us.wearecurio.datetime.LocalTimeRepeater
import us.wearecurio.parse.PatternScanner
import us.wearecurio.services.DatabaseService
import us.wearecurio.utility.Utils
import us.wearecurio.model.Tag

import java.util.Map;
import java.util.regex.Pattern
import java.math.MathContext
import java.text.DateFormat
import java.text.SimpleDateFormat

import org.joda.time.*
import org.junit.Before;

class Identifier {

	private static def log = LogFactory.getLog(this)
	
	public static final int MAXIDENTIFIERLENGTH = 100
	
	public static Map<String,Identifier> map = new HashMap<String, Identifier>()
	
	static constraints = {
		value(maxSize:MAXIDENTIFIERLENGTH, unique:true)
	}

	static mapping = {
		version false
		table 'identifier'
		value column:'value', index:'value_index'
	}
	
	static {
		Utils.registerTestReset() {
			map = new HashMap<String, Identifier>()
		}
	}

	String value
	
	/**
	* Create and save new entry
	*/
	static Identifier look(String value) {
		if (value == null) return null
		
		Identifier ident = map.get(value)
		
		if (ident != null) return ident
		
		while (true) {
			ident = Identifier.findByValue(value)
			
			if (ident) {
				map.put(value, ident)
				return ident
			}
			
			ident = new Identifier(value:value)
			
			if (Utils.save(ident, true))
				return ident
		}
	}
	
	public int hashCode() {
		return (int) id;
	}

	boolean equals(Object o) {
		if (!o instanceof Identifier)
			return false
		return ((Identifier)o).value == value
	}
	
	String toString() {
		return value
	}
}
