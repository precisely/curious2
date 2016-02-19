package us.wearecurio.model;

import org.apache.commons.logging.LogFactory
import us.wearecurio.utility.Utils

class TextIdentifier {

	private static def log = LogFactory.getLog(this)
	
	public static final int MAXIDENTIFIERLENGTH = 10000
	
	static constraints = {
		value(maxSize:MAXIDENTIFIERLENGTH, unique:false)
	}

	static mapping = {
		version false
		table 'text_identifier'
	}
	
	String value
	
	/**
	* Create and save new entry
	*/
	static TextIdentifier create(String value) {
		if (value == null) return null

		TextIdentifier identifier = new TextIdentifier(value:value)
		
		Utils.save(identifier, true)
		
		return identifier
	}
	
	String toString() {
		return value
	}
}
