package us.wearecurio.model

import us.wearecurio.cache.BoundedCache;

class TagGroup extends GenericTagGroup {

	static hasMany = [tags: Tag]

	static mapping = {
		version false
	}

	// Cache holder to cache list of tag ids for a tag group
	static BoundedCache<Long, List<Long>> tagCache = new BoundedCache<Long, List<Long>>(100000)
	// Cache holder to cache list of tag descriptions for a tag group
	static BoundedCache<Long, List<String>> tagIdCache = new BoundedCache<Long, List<String>>(100000)

	/*
	 * A helper method to cache id and description to cache list.
	 * This method must be called internally inside a loop which 
	 * is iterating over all tags of a tag group otherwise there
	 * will be incomplete caching data for a tag group.
	 */
	private static addToCache(Long id, Tag tagInstance) {
		synchronized(tagCache) {
			List cachedIds = tagIdCache[id] ?: []
			cachedIds << tagInstance.id
			tagIdCache.put(id, cachedIds)

			List cachedDescriptions = tagCache[id] ?: []
			cachedDescriptions << tagInstance.description
			tagCache.put(id, cachedDescriptions)
		}
	}

	private static addToCache(GenericTagGroup tagGroupInstance, Tag tagInstance) {
		addToCache(tagGroupInstance.id, tagInstance)
	}

	private static removeFromCache(Long id, Tag tagInstance) {
		synchronized(tagCache) {
			List cachedIds = tagIdCache[id] ?: []
			cachedIds.remove(tagInstance.id)
			tagIdCache.put(id, cachedIds)

			List cachedDescriptions = tagCache[id] ?: []
			cachedDescriptions.remove(tagInstance.description)
			tagCache.put(id, cachedDescriptions)
		}
	}

	private static removeFromCache(GenericTagGroup tagGroupInstance, Tag tagInstance) {
		removeFromCache(tagGroupInstance.id, tagInstance)
	}

	boolean hasCachedData() {
		tagIdCache[this.id]
	}

	void cacheTag(Tag tagInstance) {
		if (hasCachedData()) {
			addToCache(this, tagInstance)
		}
	}

	void removeTagFromCache(Tag tagInstance) {
		if (hasCachedData()) {
			removeFromCache(this, tagInstance)
		}
	}

	/*
	 * Get list of all tag instances associated with this tag group instance.
	 * This will get all nested tags which are sub tags of any sub tag group.
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

		filteredSubTagList.each { Tag tagInstance ->
			addToCache(this, tagInstance)
		}

		return filteredSubTagList
	}

	List<Long> getTagsIds(Long userId) {
		// Look for cached sub tag ids.
		if (tagIdCache[this.id]) {
			return tagIdCache[this.id]
		}

		// Fetch & cache all sub tags & return ids
		return getTags(userId)*.id
	}

	List<String> getTagsDescriptions(Long userId) {
		// Look for cached sub tag ids.
		if (tagCache[this.id]) {
			return tagCache[this.id]
		}

		// Fetch & cache all sub tags & return descriptions/names
		return getTags(userId)*.description
	}

	boolean containsTag(Tag tag, Long userId) {
		if (!tag) {
			return false
		}
		tag.id in getTagsIds(userId)
	}

	boolean containsTagString(String tagString, Long userId) {
		tagString in getTagsDescriptions(userId)
	}
}