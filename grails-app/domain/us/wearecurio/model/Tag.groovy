package us.wearecurio.model;

import grails.converters.*

import org.apache.commons.logging.LogFactory

import us.wearecurio.cache.BoundedCache
import us.wearecurio.utility.Utils

class Tag implements Serializable, Comparable {

	// TODO: Turn tags() into an interface called Taggables that both TagGroup and Tag implement.
	//	This will allow us to iterate on both TagGroups and Tags.
	Iterable<Tag> tags() {
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

	static Tag create(String d) {
		log.debug "Tag.create() description:'" + d + "'"
		def tag = new Tag(description:d)
		Utils.save(tag, true)
		addToCache(tag)
		return tag
	}

	static void addToCache(Tag tagInstance) {
		synchronized(tagCache) {
			tagCache.put(tagInstance.description, tagInstance)
			tagIdCache.put(tagInstance.id, tagInstance)
		}
	}

	
	static Tag fetch(Long id) {
		def tag = tagIdCache.get(id)
		
		if (tag != null) return tag
		
		return Tag.get(id)
	}

	static List<Tag> fetchAll(Collection<Long> ids) {
		Map cachedTagData = tagIdCache.findAll { id, instance ->
			id in ids
		}

		List<Long> cachedTagIds = cachedTagData.keySet() as List
		List<Tag> cachedTagInstances = cachedTagData.values() as List

		List<Long> tagIdsNotInCache = (ids - cachedTagIds) as List

		if (tagIdsNotInCache) {
			Tag.getAll(tagIdsNotInCache).each { tagInstance ->
				if (tagInstance != null) {
					addToCache(tagInstance)
					cachedTagInstances << tagInstance
				}
			}
		}

		cachedTagInstances
	}

	static Tag look(String d) {
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

	// Checks if a user has an entry for this tag.
	boolean hasEntry(Long userId) {
		Entry.withCriteria {
			eq("tag", this)
			eq("userId", userId)
			isNotNull("date")
			maxResults(1)
		}[0] != null
	}

	TagProperties getPropertiesForUser(Long userId) {
		return TagProperties.lookup(userId, getId())
	}

	String description

	String toString() {
		return "Tag(id:" + id + ", description:" + description + ")"
	}
	
	public int hashCode() {
		return (int)getId()
	}
	
	public boolean equals(Object other) {
		if (!other instanceof Tag)
			return false
		
		return getId() == other.getId()
	}

	@Override
	public int compareTo(Object o) {
		if (!(o instanceof Tag))
			return -1;
		
		return description.compareTo(((Tag)o).description)
	}
}
