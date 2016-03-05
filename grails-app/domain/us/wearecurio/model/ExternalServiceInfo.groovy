package us.wearecurio.model

import java.util.Date
import java.util.Locale
import java.util.Map;
import org.apache.commons.logging.LogFactory

import us.wearecurio.utility.Utils

class ExternalServiceInfo {
	
	private static def log = LogFactory.getLog(this)
	
	String name
	
	static constraints = {
		name(nullable:true, unique:true)
	}
	
	static mapping = {
		version false
		name column: 'name', index:'name_index'
	}
	
	static ExternalServiceInfo create(String name) {
		ExternalServiceInfo desc = new ExternalServiceInfo(name)
		
		Utils.save(desc, true)
		
		return desc
	}
	
	static ExternalServiceInfo look(String name) {
		ExternalServiceInfo desc = ExternalServiceInfo.findByName(name)
		
		if (desc) return desc
		
		return create(name)
	}
	
	ExternalServiceInfo(String name) {
		this.name = name
	}
}
