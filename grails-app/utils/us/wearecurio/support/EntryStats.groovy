package us.wearecurio.support

import java.util.ArrayList
import java.util.Date
import java.util.Set

import us.wearecurio.model.Entry
import us.wearecurio.model.EntryGroup
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.TagValueStats
import us.wearecurio.model.User

import us.wearecurio.services.AlertGenerationService

/*
 * Class to return entry creation statistics for later updating of entry and tag statistics
 */
class EntryStats {
	static mapWith = "none"
	
	Long userId
	Set<Long> baseTagIds = new HashSet<Long>()
	Set<Long> tagIds = new HashSet<Long>()
	boolean alertEdited = false
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
		
		if (entry.isAlert())
			alertEdited = true
	}
	
	Date getLastDate() {
		startDate
	}
	
	ArrayList<TagStats> finish() { // after finalizing all entry creation, call this to update statistics
		ArrayList<TagStats> tagStats = new ArrayList<TagStats>()
		
		boolean recache = false
		
		for (Long baseTagId: baseTagIds) {
			if (!User.hasCachedTagId(userId, baseTagId))
				recache = true
			TagStats stats = TagStats.createOrUpdate(userId, baseTagId)
			if (stats.isEmpty())
				recache = true
			tagStats.add(stats)
		}

		for (Long tagId: tagIds) {
			if (!User.hasCachedTagId(userId, tagId))
				recache = true
			TagValueStats stats = TagValueStats.createOrUpdate(userId, tagId, startDate)
			if (stats.isEmpty())
				recache = true
		}

		if (timeZoneId != null)
			User.setTimeZoneId(userId, timeZoneId)
		
		if (alertEdited) {
			AlertGenerationService.get().regenerate(userId, new Date())
		}
		
		if (recache) {
			User.recacheTagIds(userId)
		}
		
		if (tagStats.size() > 0)
			return tagStats
			
		return null
	}
}

