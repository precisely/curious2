package us.wearecurio.model

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class GenericTagGroup {

	private static Log log = LogFactory.getLog(this)
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

	List getProps() {
		GenericTagGroupProperties.findAllByTagGroupId(this.id)
	}

	List getAssociatedGroups() {
		GenericTagGroupProperties.findAllByTagGroupIdAndGroupIdIsNotNull(this.id)*.group
	}
}
