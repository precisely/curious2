package us.wearecurio.model

import us.wearecurio.cache.BoundedCache

class WildcardTagGroup extends GenericTagGroup {

	static mapping = {
		version false
	}

	// Cache holder to cache list of tag id's for a wildcard tag group
	static BoundedCache<Long, List<Long>> tagCache = new BoundedCache<Long, List<Long>>(100000)
	// Cache holder to cache list of tag descriptions for a wildcard tag group
	static BoundedCache<Long, List<String>> tagIdCache = new BoundedCache<Long, List<String>>(100000)

	static addToCache(Long id, Tag tagInstance) {
		synchronized(tagCache) {
			List cachedIds = tagIdCache[id] ?: []
			cachedIds << tagInstance.id
			tagIdCache.put(id, cachedIds)

			List cachedDescriptions = tagCache[id] ?: []
			cachedDescriptions << tagInstance.description
			tagCache.put(id, cachedDescriptions)
		}
	}

	static addToCache(WildcardTagGroup tagGroupInstance, Tag tagInstance) {
		addToCache(tagGroupInstance.id, tagInstance)
	}

	static List<Tag> getTags(Long id, Long userId) {
		WildcardTagGroup.get(id).getTags(userId)
	}

	static List<Tag> getTags(Long id, String wildcardTagGroupName, Long userId) {
		if (tagIdCache[id]) {
			return Tag.fetchAll(tagIdCache[id])
		}

		// Cann't get list of associated tags for a user if user id is absent
		if (!userId) {
			return []
		}

		List<String> terms = wildcardTagGroupName.tokenize()
		List<String> spaceTerms = terms.collectAll { " $it" }

		Set<Tag> matchingTags = []
		List<Long> exclusions = []

		GenericTagGroupProperties propertyInstance = GenericTagGroupProperties.lookup(userId, id)
		if (propertyInstance) {
			exclusions = TagExclusion.withCriteria {
				projections {
					property("objectId")
				}
				eq("tagGroupPropertyId", propertyInstance.id)
				eq("type", ExclusionType.TAG)
			}
		}

		User.getTags(userId, true).each { Tag tagInstance ->
			boolean match = true
			String tagName = tagInstance.description

			terms.eachWithIndex { term, index ->
				if (term && (!tagName.startsWith(term) || tagName.contains(spaceTerms[index]))) {
					match = false
				}
			}

			if (match && !exclusions.contains(tagInstance.id)) {
				matchingTags << tagInstance
				addToCache(id, tagInstance)
			}
		}

		matchingTags as List
	}

	private static removeFromCache(GenericTagGroup tagGroupInstance, Tag tagInstance) {
		removeFromCache(tagGroupInstance.id, tagInstance)
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

	void addTagGroupToCache(GenericTagGroup childTagGroupInstance, Long userId) {
		if (hasCachedData()) {
			childTagGroupInstance.getTags(userId).each { tagInstance ->
				addTagToCache(tagInstance)
			}
		}
	}

	void addTagToCache(Tag tagInstance) {
		if (hasCachedData()) {
			addToCache(this, tagInstance)
		}
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

	List<Tag> getTags(Long userId) {
		getTags(this.id, this.description, userId)
	}

	List<String> getTagsDescriptions(Long userId) {
		// Look for cached tag ids.
		if (tagCache[this.id]) {
			return tagCache[this.id]
		}

		// Fetch & cache all sub tags & return descriptions/names
		return getTags(userId)*.description
	}

	List<Long> getTagsIds(Long userId) {
		// Look for cached tag id's.
		if (tagIdCache[this.id]) {
			return tagIdCache[this.id]
		}

		// Fetch & cache all sub tags & return ids
		return getTags(userId)*.id
	}

	boolean hasCachedData() {
		tagIdCache[this.id]
	}

	void removeTagFromCache(Tag tagInstance) {
		if (hasCachedData()) {
			removeFromCache(this, tagInstance)
		}
	}

	void removeTagGroupFromCache(GenericTagGroup subTagGroupInstance, Long userId) {
		if (hasCachedData()) {
			subTagGroupInstance.getTags(userId).each { tagInstance ->
				removeTagFromCache(tagInstance)
			}
		}
	}
}