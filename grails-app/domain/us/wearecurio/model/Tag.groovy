package us.wearecurio.model;

import org.apache.commons.logging.LogFactory

import grails.converters.*
import us.wearecurio.utility.Utils
import java.util.Date
import java.util.Calendar
import java.util.TimeZone

class Tag {
	
	private static def log = LogFactory.getLog(this)

	public static final int MAXLENGTH = 100

	static constraints = { description(maxSize:MAXLENGTH) }

	static mapping = {
		table 'tag'
		description column:'description', index:'description_idx'
	}
	
	static def create(String d) {
		log.debug "Tag.create() description:'" + d + "'"
		def tag = new Tag(description:d)
		Utils.save(tag)
		return tag
	}

	static def look(d) {
		log.debug "Tag.look() description:'" + d + "'"
		def tag = Tag.findByDescription(d)

		if (tag != null) {
			return tag
		}

		return Tag.create(d)
	}
	
	def getPropertiesForUser(Long userId) {
		return TagProperties.lookup(userId, getId())
	}

	String description

	def String toString() {
		return "Tag(id:" + id + ", description:" + description + ")"
	}
}
