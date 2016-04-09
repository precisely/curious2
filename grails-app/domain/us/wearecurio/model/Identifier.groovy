package us.wearecurio.model

import org.apache.commons.logging.LogFactory
import us.wearecurio.cache.BoundedCache
import us.wearecurio.utility.Utils

class Identifier {

	private static def log = LogFactory.getLog(this)
	
	public static final int MAXIDENTIFIERLENGTH = 100
	
	static constraints = {
		value(maxSize:MAXIDENTIFIERLENGTH, unique:true)
	}

	static mapping = {
		version false
		table 'identifier'
		value column:'value', index:'value_index'
	}
	
	String value
	
	/**
	* Create and save new entry
	*/
	static Identifier look(String value) {
		if (value == null) return null
		
		Identifier ident = Identifier.findByValue(value)
				
		if (ident) {
			return ident
		}
				
		ident = new Identifier(value:value)
				
		if (Utils.save(ident, true)) {
			return ident
		}
		
		return Identifier.findByValue(value)
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
