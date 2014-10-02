package us.wearecurio.model;

import org.apache.commons.logging.LogFactory

import java.math.MathContext;
import java.sql.ResultSet
import java.util.TreeSet
import us.wearecurio.utility.Utils
import us.wearecurio.services.DatabaseService

class TagValueStats {

	private static def log = LogFactory.getLog(this)

	Long userId
	Long tagId
	BigDecimal minimum
	BigDecimal maximum
	
	static constraints = {
		minimum(nullable:true)
		maximum(nullable:true)
	}
	static mapping = {
		version false
		table 'tag_value_stats'
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
	}

	TagValueStats() {
	}
	
	static final MathContext mc = new MathContext(9)
	
	static def createOrUpdate(Long userId, Long tagId, Date startDate) {
		log.debug "TagValueStats.createOrUpdate() userId:" + userId + ", tag:" + Tag.get(tagId) + ", startDate:" + startDate
		
		TagValueStats stats = TagValueStats.findByTagIdAndUserId(tagId, userId)
		
		if (!stats) {
			stats = new TagValueStats(tagId:tagId, userId:userId)
			startDate = null // start from scratch
		}
		
		if (startDate == null) {
			def minMax = Entry.executeQuery(
					"select min(entry.amount), max(entry.amount) from Entry as entry where entry.tag.id = :tagId and entry.userId = :userId and entry.date IS NOT NULL and (entry.repeatType IS NULL or (not entry.repeatType.id in (:ghostIds)))",
					[tagId:tagId, userId:userId, ghostIds:Entry.LONG_GHOST_IDS])
			
			if (minMax) {
				stats.minimum = minMax[0][0]
				stats.maximum = minMax[0][1]
			}
		} else {
			def minMax = Entry.executeQuery(
					"select min(entry.amount), max(entry.amount) from Entry as entry where entry.tag.id = :tagId and entry.userId = :userId and entry.date IS NOT NULL and (entry.date >= :startDate) and entry.amount IS NOT NULL and (entry.repeatType IS NULL or (not entry.repeatType.id in (:ghostIds)))",
					[tagId:tagId, userId:userId, startDate:startDate, ghostIds:Entry.LONG_GHOST_IDS])
			
			if (minMax) {
				BigDecimal min = minMax[0][0]
				BigDecimal max = minMax[0][1]
				
				if (min != null && min < stats.minimum)
					stats.minimum = min
				if (max != null && max > stats.maximum)
					stats.maximum = max
			}
		}
		
		Utils.save(stats, true)
		
		return stats
	}
	
	/**
	 * Updates all TagValueStats for given user
	 * @param user
	 * @return
	 */
	static def updateTagValueStats(User user) {
		log.debug "TagStats.updateTagStats() userId:" + user.getId()
		
		def userId = user.getId()

		def tagStats = []
		
		def tags = Entry.getTags(user, Entry.ONLYIDS)
		
		for (tagId in tags) {
			createOrUpdate(userId, tagId)
		}
	}
	
	String toString() {
		return "TagValueStats(userId:" + userId + ", tagId:" + tagId + ", minimum:" + minimum + ", maximum:" + maximum + ")"
	}
	
	def getJSONDesc() {
		return [tagId:tagId, minimum:minimum, maximum:maximum]
	}
	
	def getJSONShortDesc() {
		return [tagId, minimum, maximum]
	}
}
