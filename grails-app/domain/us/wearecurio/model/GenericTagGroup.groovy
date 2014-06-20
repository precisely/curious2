package us.wearecurio.model

import org.apache.commons.logging.LogFactory

class GenericTagGroup {
	private static def log = LogFactory.getLog(this)
	public static final int MAXLENGTH = 100
	
	static constraints = { description(maxSize:MAXLENGTH) }
	
	static mapping = {
		version false
		table 'tag_group'
		description column:'description', index:'description_idx'
	}
	
	static hasMany = [subTagGroups:GenericTagGroup]
	static belongsTo = [parentTagGroup:GenericTagGroup]
	
	String description
	String name	// Will be short name
}
