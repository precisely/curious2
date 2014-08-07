package us.wearecurio.model

import us.wearecurio.cache.BoundedCache

class WildcardTagGroup extends GenericTagGroup {

	static mapping = {
		version false
	}

	// Cache holder to cache list of tag descriptions for a wildcard tag group
	static BoundedCache<Long, List<String>> tagDescriptionCache = new BoundedCache<Long, List<String>>(100000)
	// Cache holder to cache list of tag id's for a wildcard tag group
	static BoundedCache<Long, List<Long>> tagIdCache = new BoundedCache<Long, List<Long>>(100000)

	void addToCache(GenericTagGroup childTagGroupInstance, Long userId) {
		if (hasCachedData()) {
			childTagGroupInstance.getTags(userId).each { tagInstance ->
				addToCache(tagInstance)
			}
		}
	}

	void addToCache(Tag tagInstance) {
		if (hasCachedData()) {
			cache([tagInstance])
		}
	}

	void cache(List<Tag> tagInstanceList) {
		List cachedIds = tagIdCache[id] ?: []
		List cachedDescriptions = tagDescriptionCache[id] ?: []

		tagInstanceList.each { tagInstance ->
			synchronized(tagDescriptionCache) {
				cachedIds << tagInstance.id
				tagIdCache.put(id, cachedIds)

				cachedDescriptions << tagInstance.description
				tagDescriptionCache.put(id, cachedDescriptions)
			}
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
		if (tagIdCache[id]) {
			return Tag.fetchAll(tagIdCache[id])
		}

		// Cann't get list of associated tags for a user if user id is absent
		if (!userId) {
			return []
		}

		String wildcardTagGroupName = this.description

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

		User.getTags(userId).each { Tag tagInstance ->
			boolean match = true
			String tagName = tagInstance.description

			terms.eachWithIndex { term, index ->
				if (term && (!tagName.startsWith(term) || tagName.contains(spaceTerms[index]))) {
					match = false
				}
			}

			if (match && !exclusions.contains(tagInstance.id)) {
				matchingTags << tagInstance
			}
		}

		cache(matchingTags as List)

		matchingTags as List
	}

	List<String> getTagsDescriptions(Long userId) {
		// Look for cached tag ids.
		if (tagDescriptionCache[this.id]) {
			return tagDescriptionCache[this.id] as List
		}

		// Fetch & cache all sub tags & return descriptions/names
		return getTags(userId)*.description
	}

	List<Long> getTagsIds(Long userId) {
		// Look for cached tag id's.
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