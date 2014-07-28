package us.wearecurio.model;

import org.apache.commons.logging.LogFactory

import grails.converters.*
import us.wearecurio.cache.BoundedCache;
import us.wearecurio.utility.Utils
import java.util.Date
import java.util.Calendar
import java.util.TimeZone

class Tag {

	// TODO: Turn tags() into an interface called Taggables that both TagGroup and Tag implement.
	//	This will allow us to iterate on both TagGroups and Tags.
	def tags() {
		[this]
	}

	private static def log = LogFactory.getLog(this)

	public static final int MAXLENGTH = 100

	public static BoundedCache<String, Tag> tagCache = new BoundedCache<String, Tag>(100000)
	public static BoundedCache<String, Tag> tagIdCache = new BoundedCache<Long, Tag>(100000)
	
	static constraints = { description(maxSize:MAXLENGTH) }

	static mapping = {
		version false
		table 'tag'
		description column:'description', index:'description_idx'
	}

	static {
		Utils.registerTestReset {
			tagCache = new BoundedCache<String, Tag>()
			tagIdCache = new BoundedCache<String, Tag>()
		}
	}

	static def create(String d) {
		log.debug "Tag.create() description:'" + d + "'"
		def tag = new Tag(description:d)
		Utils.save(tag, true)
		synchronized(tagCache) {
			tagCache.put(d, tag)
			tagIdCache.put(tag.getId(), tag)
		}
		return tag
	}
	
	static def fetch(Long id) {
		def tag = tagIdCache.get(id)
		
		if (tag != null) return tag
		
		return Tag.get(id)
	}

	static def look(String d) {
		log.debug "Tag.look() description:'" + d + "'"
		def tag = tagCache.get(d)
		if (tag) return tagIdCache.get(tag.id)
		
		tag = Tag.findByDescription(d)

		if (tag != null) {
			synchronized(tagCache) {
				tagCache.put(d, tag)
				tagIdCache.put(tag.id, tag)
				return tag
			}
		}

		return Tag.create(d)
	}

	def getPropertiesForUser(Long userId) {
		return TagProperties.lookup(userId, getId())
	}

	String description

	String toString() {
		return "Tag(id:" + id + ", description:" + description + ")"
	}
}
