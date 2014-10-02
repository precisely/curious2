package us.wearecurio.support

import java.util.ArrayList
import java.util.Date
import java.util.Map
import java.util.Set

import us.wearecurio.model.Entry
import us.wearecurio.model.EntryGroup
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.TagValueStats
import us.wearecurio.model.User


/*
 * Class to return entry creation statistics for later updating of entry and tag statistics
 */
class EntryStats {
	static mapWith = "none"
	
	Long userId
	Set<Long> baseTagIds = new HashSet<Long>()
	Set<Long> tagIds = new HashSet<Long>()
	Date startDate
	Integer timeZoneId = null
	boolean batchCreation = false
	
	EntryStats() {
	}

	EntryStats(boolean batchCreation, Long userId) {
		this.batchCreation = true
		this.userId = userId
		this.startDate = null
	}

	EntryStats(boolean batchCreation) {
		this.batchCreation = true
	}

	EntryStats(Long userId) {
		this.userId = userId
	}

	void addEntry(Entry entry) {
		tagIds.add(entry.tag.id)
		baseTagIds.add(entry.baseTag.id)
		if (startDate == null)
			startDate = entry.date
		else if (startDate > entry.date)
			startDate = entry.date
		timeZoneId = entry.getTimeZoneId()
	}
	
	ArrayList<TagStats> finish() { // after finalizing all entry creation, call this to update statistics
		ArrayList<TagStats> tagStats = new ArrayList<TagStats>()
		
		for (Long baseTagId: baseTagIds) {
			tagStats.add(TagStats.createOrUpdate(userId, baseTagId))
		}

		for (Long tagId: tagIds) {
			TagValueStats.createOrUpdate(userId, tagId, startDate)
		}

		if (timeZoneId != null)
			User.setTimeZoneId(userId, timeZoneId)

		return tagStats
	}
}

