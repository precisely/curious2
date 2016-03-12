package us.wearecurio.model

import us.wearecurio.cache.BoundedCache
import us.wearecurio.utility.Utils

class WildcardTagGroup extends GenericTagGroup {

	static mapping = {
		version false
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
		// Cann't get list of associated tags for a user if user id is absent
		if (!userId) {
			return []
		}

		String wildcardTagGroupName = this.description

		List<String> terms = wildcardTagGroupName.tokenize()
		List<String> spaceTerms = terms.collectNested { " $it" }

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

		matchingTags as List
	}

	void removeFromCache(def tagOrTagGroup) {
		// do nothing for now, no longer maintaining cache
	}
	
	List<String> getTagsDescriptions(Long userId) {
		// Look for cached tag ids.
		// Fetch & cache all sub tags & return descriptions/names
		return getTags(userId)*.description
	}

	List<Long> getTagsIds(Long userId) {
		// Look for cached tag id's.
		// Fetch & cache all sub tags & return ids
		return getTags(userId)*.id
	}
}