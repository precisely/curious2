package us.wearecurio.profiletags

import groovy.transform.EqualsAndHashCode
import us.wearecurio.model.Tag
import us.wearecurio.services.SearchService
import us.wearecurio.utility.Utils

@EqualsAndHashCode
class ProfileTag {

	Long userId
	Tag tag

	ProfileTagType type
	ProfileTagStatus status

	static constraints = {
		userId unique: ['type', 'status', 'tag']
	}

	static mapping = {
		userId index: 'user_id_index'
		type index: 'type_index'
		status index: 'status_index'
	}

	static ProfileTag add(Map params) {
		ProfileTag profileTag = new ProfileTag(params)

		if (!profileTag.validate() || !Utils.save(profileTag, true)) {
			return
		}

		return profileTag
	}

	static ProfileTag add(Tag tag, Long userId, ProfileTagType type, ProfileTagStatus status) {
		return add([tag: tag, userId: userId, type: type, status: status])
	}

	static ProfileTag addInterestTag(Tag tag, Long userId, ProfileTagStatus status) {
		return add(tag, userId, ProfileTagType.INTEREST, status)
	}

	static ProfileTag addPublicInterestTag(Tag tag, Long userId) {
		return addInterestTag(tag, userId, ProfileTagStatus.PUBLIC)
	}

	static ProfileTag addPrivateInterestTag(Tag tag, Long userId) {
		return addInterestTag(tag, userId, ProfileTagStatus.PRIVATE)
	}

	static ProfileTag addGeneticTag(Tag tag, Long userId, ProfileTagStatus status) {
		return add(tag, userId, ProfileTagType.GENETIC, status)
	}

	static ProfileTag addPublicGeneticTag(Tag tag, Long userId) {
		return addGeneticTag(tag, userId, ProfileTagStatus.PUBLIC)
	}

	static ProfileTag addPrivateGeneticTag(Tag tag, Long userId) {
		return addGeneticTag(tag, userId, ProfileTagStatus.PRIVATE)
	}

	static void delete(Tag tag, Long userId, ProfileTagType type, ProfileTagStatus status) {
		findByTagAndUserIdAndTypeAndStatus(tag, userId, type, status)?.delete()
	}

	static void deleteInterestTag(Tag tag, Long userId, ProfileTagStatus status) {
		delete(tag, userId, ProfileTagType.INTEREST, status)
	}

	static void deletePublicInterestTag(Tag tag, Long userId) {
		deleteInterestTag(tag, userId, ProfileTagStatus.PUBLIC)
	}

	static void deletePrivateInterestTag(Tag tag, Long userId) {
		deleteInterestTag(tag, userId, ProfileTagStatus.PRIVATE)
	}

	static void deleteGeneticTag(Tag tag, Long userId, ProfileTagStatus status) {
		delete(tag, userId, ProfileTagType.GENETIC, status)
	}

	static void deletePublicGeneticTag(Tag tag, Long userId) {
		deleteGeneticTag(tag, userId, ProfileTagStatus.PUBLIC)
	}

	static void deletePrivateGeneticTag(Tag tag, Long userId) {
		deleteGeneticTag(tag, userId, ProfileTagStatus.PRIVATE)
	}

	static boolean hasTag(Tag tag, Long userId, ProfileTagType type, ProfileTagStatus status) {
		return findByTagAndUserIdAndTypeAndStatus(tag, userId, type, status) ? true : false
	}

	static boolean hasInterestTag(Tag tag, Long userId, ProfileTagStatus status) {
		return hasTag(tag, userId, ProfileTagType.INTEREST, status)
	}

	static boolean hasPublicInterestTag(Tag tag, Long userId) {
		return hasInterestTag(tag, userId, ProfileTagStatus.PUBLIC)
	}

	static boolean hasPrivateInterestTag(Tag tag, Long userId) {
		return hasInterestTag(tag, userId, ProfileTagStatus.PRIVATE)
	}

	static boolean hasGeneticTag(Tag tag, Long userId, ProfileTagStatus status) {
		return hasTag(tag, userId, ProfileTagType.GENETIC, status)
	}

	static boolean hasPublicGeneticTag(Tag tag, Long userId) {
		return hasGeneticTag(tag, userId, ProfileTagStatus.PUBLIC)
	}

	static boolean hasPrivateGeneticTag(Tag tag, Long userId) {
		return hasGeneticTag(tag, userId, ProfileTagStatus.PRIVATE)
	}

	static List<Tag> getTags(Long userId, ProfileTagType type, ProfileTagStatus status) {
		return withCriteria {
			eq('userId', userId)
			eq('type', type)
			eq('status', status)

			projections {
				property('tag')
			}

			maxResults(1000)
		}
	}

	static List<Tag> getInterestTags(Long userId, ProfileTagStatus status) {
		return getTags(userId, ProfileTagType.INTEREST, status)
	}

	static List<Tag> getPublicInterestTags(Long userId) {
		return getInterestTags(userId, ProfileTagStatus.PUBLIC)
	}

	static List<Tag> getPrivateInterestTags(Long userId) {
		return getInterestTags(userId, ProfileTagStatus.PRIVATE)
	}

	static List<Tag> getGeneticTags(Long userId, ProfileTagStatus status) {
		return getTags(userId, ProfileTagType.GENETIC, status)
	}

	static List<Tag> getPublicGeneticTags(Long userId) {
		return getGeneticTags(userId, ProfileTagStatus.PUBLIC)
	}

	static List<Tag> getPrivateGeneticTags(Long userId) {
		return getGeneticTags(userId, ProfileTagStatus.PRIVATE)
	}
}

enum ProfileTagType {
	INTEREST(1),
	GENETIC(2)

	final int id

	ProfileTagType(int id) {
		this.id = id
	}

	public int value() { 
		return id 
	}
}

enum ProfileTagStatus {
	PUBLIC(1),
	PRIVATE(2)

	final int id

	ProfileTagStatus(int id) {
		this.id = id
	}

	public int value() {
		return id
	}
}
