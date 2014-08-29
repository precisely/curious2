package us.wearecurio.support

import java.util.ArrayList
import java.util.Date
import java.util.Map
import java.util.Set

import us.wearecurio.model.Entry
import us.wearecurio.model.EntryIdGroup
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.User


/*
 * Class to return entry creation statistics for later updating of entry and tag statistics
 */
class EntryStats {
	static mapWith = "none"
	
	Long userId
	Set<Long> tagIds = new HashSet<Long>()
	Integer timeZoneId = null
	boolean batchCreation = false
	
	EntryStats() {
	}

	EntryStats(boolean batchCreation, Long userId) {
		this.batchCreation = true
		this.userId = userId
	}

	EntryStats(boolean batchCreation) {
		this.batchCreation = true
	}

	EntryStats(Long userId) {
		this.userId = userId
	}

	void addTag(Tag tag) {
		tagIds.add(tag.getId())
	}
	
	void addTagId(Long tagId) {
		tagIds.add(tagId)
	}
	
	ArrayList<TagStats> finish() { // after finalizing all entry creation, call this to update statistics
		ArrayList<TagStats> tagStats = new ArrayList<TagStats>()
		
		for (Long tagId: tagIds) {
			tagStats.add(TagStats.createOrUpdate(userId, tagId))
		}

		if (timeZoneId != null)
			User.setTimeZoneId(userId, timeZoneId)
			
		return tagStats
	}
}

