package us.wearecurio.model;

import org.apache.commons.logging.LogFactory

import java.math.MathContext
import java.math.RoundingMode
import java.sql.ResultSet
import java.util.TreeSet
import us.wearecurio.utility.Utils
import us.wearecurio.services.DatabaseService
import us.wearecurio.units.UnitGroupMap
import us.wearecurio.units.UnitGroupMap.UnitGroup
import us.wearecurio.units.UnitGroupMap.UnitRatio

class TagValueStats {

	private static def log = LogFactory.getLog(this)

	Long userId
	Long tagId
	String units
	BigDecimal minimum
	BigDecimal maximum
	
	static constraints = {
		minimum(nullable:true)
		maximum(nullable:true)
		units(nullable:true)
	}
	static mapping = {
		version false
		table 'tag_value_stats'
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
	}

	TagValueStats() {
	}
	
	static final MathContext mc = new MathContext(9, RoundingMode.HALF_UP)
	
	static def look(Long userId, Long tagId) {
		log.debug "TagValueStats.lookup() userId:" + userId + ", tag:" + Tag.get(tagId)
		
		TagValueStats stats = TagValueStats.findByTagIdAndUserId(tagId, userId)
		
		if (!stats)
			return createOrUpdate(userId, tagId, null)
		
		return stats
	}

	static def createOrUpdate(Long userId, Long tagId, Date startDate) {
		log.debug "TagValueStats.createOrUpdate() userId:" + userId + ", tag:" + Tag.get(tagId) + ", startDate:" + startDate
		
		TagValueStats stats = TagValueStats.findByTagIdAndUserId(tagId, userId)
		
		if (!stats) {
			stats = new TagValueStats(tagId:tagId, userId:userId)
			startDate = null // start from scratch
		}
		
		def minMax
		
		if (startDate == null) {
			minMax = Entry.executeQuery(
					"select min(entry.amount), max(entry.amount), units from Entry as entry where entry.tag.id = :tagId and entry.userId = :userId and entry.date IS NOT NULL and (entry.repeatType IS NULL or (not entry.repeatType.id in (:ghostIds))) group by units",
					[tagId:tagId, userId:userId, ghostIds:Entry.LONG_GHOST_IDS])
		} else {
			minMax = Entry.executeQuery(
					"select min(entry.amount), max(entry.amount), units from Entry as entry where entry.tag.id = :tagId and entry.userId = :userId and entry.date IS NOT NULL and (entry.date >= :startDate) and entry.amount IS NOT NULL and (entry.repeatType IS NULL or (not entry.repeatType.id in (:ghostIds))) group by units",
					[tagId:tagId, userId:userId, startDate:startDate, ghostIds:Entry.LONG_GHOST_IDS])
		}

		if (minMax && minMax.size() > 0) {
			UnitRatio mostUsedUnitRatio = UnitGroupMap.theMap.mostUsedUnitRatioForTagIds(userId, [tagId])

			if (mostUsedUnitRatio == null) {
				stats.minimum = minMax[0][0]
				stats.maximum = minMax[0][1]
			} else for (int i = 0; i < minMax.size(); ++i) {
				String units = minMax[i][2]
				BigDecimal ratio = mostUsedUnitRatio.conversionRatio(units)
				
				BigDecimal min
				BigDecimal max
				
				if (ratio == null) {
					min = minMax[i][0]
					max = minMax[i][1]
				} else {
					min = minMax[i][0].multiply(ratio).round(mc)
					max = minMax[i][1].multiply(ratio).round(mc)
				}
				
				if (stats.minimum == null || (min != null && min < stats.minimum))
					stats.minimum = min
				if (stats.maximum == null || (max != null && max > stats.maximum))
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
			createOrUpdate(userId, tagId, null)
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
