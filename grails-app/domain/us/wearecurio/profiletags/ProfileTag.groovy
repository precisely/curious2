package us.wearecurio.profiletags

import groovy.transform.EqualsAndHashCode
import us.wearecurio.model.Tag
import us.wearecurio.model.User
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

	static ProfileTag add(Map params, boolean flush = false) {
		ProfileTag profileTag = new ProfileTag(params)

		if (!profileTag.validate() || !Utils.save(profileTag, flush)) {
			return
		}

		SearchService.get()?.index(User.get(params.userId))

		return profileTag
	}

	static ProfileTag add(Tag tag, Long userId, ProfileTagType type, ProfileTagStatus status, boolean flush = false) {
		return add([tag: tag, userId: userId, type: type, status: status], flush)
	}

	static ProfileTag addInterestTag(Tag tag, Long userId, ProfileTagStatus status, boolean flush = false) {
		return add(tag, userId, ProfileTagType.INTEREST, status, flush)
	}

	static ProfileTag addPublicInterestTag(Tag tag, Long userId, boolean flush = false) {
		return addInterestTag(tag, userId, ProfileTagStatus.PUBLIC, flush)
	}

	static ProfileTag addPrivateInterestTag(Tag tag, Long userId, boolean flush = false) {
		return addInterestTag(tag, userId, ProfileTagStatus.PRIVATE, flush)
	}

	static ProfileTag addGeneticTag(Tag tag, Long userId, ProfileTagStatus status, boolean flush = false) {
		return add(tag, userId, ProfileTagType.GENETIC, status, flush)
	}

	static ProfileTag addPublicGeneticTag(Tag tag, Long userId, boolean flush = false) {
		return addGeneticTag(tag, userId, ProfileTagStatus.PUBLIC, flush)
	}

	static ProfileTag addPrivateGeneticTag(Tag tag, Long userId, boolean flush = false) {
		return addGeneticTag(tag, userId, ProfileTagStatus.PRIVATE, flush)
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

	static List<ProfileTag> getTags(Long userId, ProfileTagType type, ProfileTagStatus status) {
		return withCriteria {
			eq('userId', userId)
			eq('type', type)
			eq('status', status)

			maxResults(1000)
		}
	}

	static List<ProfileTag> getInterestTags(Long userId, ProfileTagStatus status) {
		return getTags(userId, ProfileTagType.INTEREST, status)
	}

	static List<ProfileTag> getPublicInterestTags(Long userId) {
		return getInterestTags(userId, ProfileTagStatus.PUBLIC)
	}

	static List<ProfileTag> getPrivateInterestTags(Long userId) {
		return getInterestTags(userId, ProfileTagStatus.PRIVATE)
	}

	static List<ProfileTag> getGeneticTags(Long userId, ProfileTagStatus status) {
		return getTags(userId, ProfileTagType.GENETIC, status)
	}

	static List<ProfileTag> getPublicGeneticTags(Long userId) {
		return getGeneticTags(userId, ProfileTagStatus.PUBLIC)
	}

	static List<ProfileTag> getPrivateGeneticTags(Long userId) {
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
