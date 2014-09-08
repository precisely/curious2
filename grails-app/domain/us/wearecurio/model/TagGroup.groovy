package us.wearecurio.model

import us.wearecurio.cache.BoundedCache
import us.wearecurio.utility.Utils

class TagGroup extends GenericTagGroup {

	static hasMany = [tags: Tag]

	static mapping = {
		version false
	}

	// Cache holder to cache list of tag descriptions for a tag group
	static BoundedCache<Long, List<String>> tagDescriptionCache = new BoundedCache<Long, List<String>>(100000)
	// Cache holder to cache list of tag ids for a tag group
	static BoundedCache<Long, List<Long>> tagIdCache = new BoundedCache<Long, List<Long>>(100000)

	static {
		Utils.registerTestReset {
			tagDescriptionCache = new BoundedCache<Long, List<String>>(100000)
			tagIdCache = new BoundedCache<Long, List<Long>>(100000)
		}
	}

	void addToCache(GenericTagGroup childTagGroupInstance, Long userId) {
		if (hasCachedData()) {
			childTagGroupInstance.getTags(userId).each { tagInstance ->
				this.addToCache(tagInstance)
			}
		}
	}

	/*
	 * Method to add an instance of tag to the cache.
	 * This method first check if the cache of current tag group is empty or not.
	 * This method will only cache the tag instance if its cache is not empty.
	 * This check is needed to prevent returning incomplete data from getTags() method.
	 */
	void addToCache(Tag tagInstance) {
		if (hasCachedData()) {
			cache([tagInstance])
		}
	}

	/*
	 * A method to cache the list of tags for a given tag group.
	 */
	void cache(List<Tag> tagInstanceList) {
		Set cachedIds = tagIdCache[id] ?: []
		Set cachedDescriptions = tagDescriptionCache[id] ?: []

		tagInstanceList.each { tagInstance ->
			synchronized(tagDescriptionCache) {
				cachedIds << tagInstance.id
				tagIdCache.put(id, cachedIds)

				cachedDescriptions << tagInstance.description
				tagDescriptionCache.put(id, cachedDescriptions)
			}
		}
	}


	boolean containsTag(Tag tag, Long userId = null) {
		if (!tag) {
			return false
		}
		tag.id in getTagsIds(userId)
	}

	boolean containsTagString(String tagString, Long userId = null) {
		tagString in getTagsDescriptions(userId)
	}

	/*
	 * Get list of all tag instances associated with this tag group instance.
	 * This will get all nested tags which are sub tags of any sub tag group.
	 * 
	 * Passing user id will exclude the tags & tag groups for the current tag group.
	 */
	List<Tag> getTags(Long userId) {
		if (tagIdCache[this.id]) {
			return Tag.fetchAll(tagIdCache[this.id])
		}

		List subTagList = this.tags as List
		List subTagGroupList = this.subTagGroups as List

		List filteredSubTagList = [], filteredSubTagGroupList = []

		GenericTagGroupProperties property = GenericTagGroupProperties.lookup(userId, id)

		// If there is property there may be the exclusion
		if (property) {
			List<TagExclusion> exclusions = TagExclusion.findAllByTagGroupPropertyId(property.id)*.properties

			// Iterate each tag & see if it is excluded
			subTagList.each { Tag tagInstance ->
				// See if sub tag has not been excluded by current user for this tag group
				if (!exclusions.find { it.type == ExclusionType.TAG && it.objectId == tagInstance.id}) {
					filteredSubTagList << tagInstance
				}
			}

			// Iterate each tag group & see if it is excluded
			subTagGroupList.each { tagGroupInstance ->
				// See if sub tag group has not been excluded by current user for this tag group
				if (!exclusions.find { it.type == ExclusionType.TAG_GROUP && it.objectId == tagGroupInstance.id}) {
					filteredSubTagGroupList << tagGroupInstance
				}
			}
		} else {
			filteredSubTagList = subTagList
			filteredSubTagGroupList = subTagGroupList
		}

		filteredSubTagGroupList.each { TagGroup tagGroupInstance ->
			filteredSubTagList.addAll(tagGroupInstance.getTags(userId))
		}

		cache(filteredSubTagList)

		return filteredSubTagList
	}

	// Helper method to get list of descriptions of all tags.
	List<String> getTagsDescriptions(Long userId) {
		// Look for cached sub tag ids.
		if (tagDescriptionCache[this.id]) {
			return tagDescriptionCache[this.id] as List
		}

		// Fetch & cache all sub tags & return descriptions/names
		return getTags(userId)*.description
	}

	// Helper method to get list of id of all tags.
	List<Long> getTagsIds(Long userId) {
		// Look for cached sub tag ids.
		if (tagIdCache[this.id]) {
			return tagIdCache[this.id] as List
		}

		// Fetch & cache all sub tags & return ids
		return getTags(userId)*.id
	}

	boolean hasCachedData() {
		tagIdCache[this.id]
	}

	void removeFromCache(GenericTagGroup subTagGroupInstance, Long userId = null) {
		if (hasCachedData()) {
			subTagGroupInstance.getTags(userId).each { tagInstance ->
				removeFromCache(tagInstance)
			}
		}
	}

	void removeFromCache(Tag tagInstance) {
		synchronized(tagDescriptionCache) {
			Set cachedIds = tagIdCache[id] ?: []
			cachedIds.remove(tagInstance.id)
			tagIdCache.put(id, cachedIds)

			Set cachedDescriptions = tagDescriptionCache[id] ?: []
			cachedDescriptions.remove(tagInstance.description)
			tagDescriptionCache.put(id, cachedDescriptions)
		}
	}
}