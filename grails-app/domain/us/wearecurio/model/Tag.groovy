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

	/*public static BoundedCache<String, Tag> tagCache = Collections.synchronizedMap(new BoundedCache<String, Tag>(100000))
	public static BoundedCache<String, Tag> tagIdCache = Collections.synchronizedMap(new BoundedCache<Long, Tag>(100000))*/
	
	static constraints = { description(maxSize:MAXLENGTH) }

	static mapping = {
		version false
		table 'tag'
		description column:'description', index:'description_idx'
	}

	static Tag create(String d) {
		log.debug "Tag.create() description:'" + d + "'"
		def tag = new Tag(description:d)
		if (!Utils.save(tag, true)) {
			Thread.sleep(1000)
			return Tag.findByDescription(d)
		}
		return tag
	}

	static Tag fetch(Long id) {
		return Tag.get(id)
	}

	static List<Tag> fetchAll(Collection<Long> ids) {
		List<Tag> retVal = new ArrayList<Tag>()
		
		ids.each { id ->
			retVal.add(Tag.get(id))
		}
		
		retVal
	}

	static Tag look(String d) {
		log.debug "Tag.look() description:'" + d + "'"
		
		if (d.length() > MAXLENGTH)
			d = d.substring(0, MAXLENGTH)
		
		Tag tag = Tag.findByDescription(d)
		
		if (tag != null) return tag

		return Tag.create(d)
	}
	
	Long fetchId() { return this.id }
	
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
	
	String getDescription() {
		return description;
	}

	String toString() {
		return "Tag(id:" + id + ", description:" + description + ")"
	}
	
	public int hashCode() {
		return (int)this.id
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof Tag))
			return false
		return this.id == other.id
	}

	@Override
	public int compareTo(Object o) {
		if (!(o instanceof Tag))
			return -1;
		
		return description.compareTo(((Tag)o).description)
	}
}
