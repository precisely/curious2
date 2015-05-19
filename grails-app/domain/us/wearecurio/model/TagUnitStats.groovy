package us.wearecurio.model

import org.apache.commons.logging.LogFactory
import org.springframework.aop.aspectj.RuntimeTestWalker.ThisInstanceOfResidueTestVisitor;
import org.springframework.transaction.annotation.Transactional

import us.wearecurio.cache.BoundedCache
import us.wearecurio.units.UnitGroupMap
import us.wearecurio.units.UnitGroupMap.UnitGroup
import us.wearecurio.units.UnitGroupMap.UnitRatio

class TagUnitStats {
	
	private static def log = LogFactory.getLog(this)
	
	String unit
	Long unitGroupId
	Long tagId
	Long userId
	Long timesUsed = 0l
	
	static mapping = {
		version false
		table 'tag_unit_stats'
		unit column:'unit', index:'unit_index'
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
	}
	
	static constraints = {
		unitGroupId(nullable:true)
	}
	
	void incrementTimesUsed(int increment) {
		this.timesUsed += increment
	}
	
	static class UserTagId {
		Long userId
		Long tagId
		
		UserTagId(Long userId, Long tagId) {
			this.userId = userId
			this.tagId = tagId
		}
		
		int hashCode() {
			return (int) (this.userId + this.tagId)
		}
		
		boolean equals(Object o) {
			if (o instanceof UserTagId) {
				return (((UserTagId)o).userId == this.userId) && (((UserTagId)o).tagId == this.tagId)
			}
			
			return false
		}
	}
	
	UnitGroup getUnitGroup() {
		if (unitGroupId)
			return UnitGroup.get(unitGroupId)
			
		return null
	}
	
	String lookupUnitString(boolean plural) {
		return getUnitGroup().lookupUnitString(unit, plural)
	}
	
	protected static BoundedCache cache = new BoundedCache<UserTagId, TagUnitStats>(10000)
	
	protected static def createOrUpdateSingle(Long userId, Long tagId, String unit, Long unitGroupId, boolean increment) {
		def tagUnitStats
		
		if (unit == null || unit == '') { // if blank unit, return null
			return null
		}
		
		// Verify if the user has used this unit for this tag in the past
		tagUnitStats = TagUnitStats.withCriteria {
			and {
				eq ('unit', unit)
				eq ('tagId', tagId)
				eq ('userId', userId)
			}
		}
		
		// If yes then update then we have the correct record to update
		if (tagUnitStats.size() > 0) {
			tagUnitStats = tagUnitStats[0]
		} else {
			tagUnitStats = new TagUnitStats(tagId: tagId, userId: userId,
					unit: unit, unitGroupId: unitGroupId)
			if (increment)
				tagUnitStats.incrementTimesUsed(2)
		}
		
		tagUnitStats.timesUsed+=1
		log.debug ("TagUnitStats.createOrUpdate():" + tagUnitStats.dump())
		tagUnitStats.save()
		
		cache.putAt(new UserTagId(userId, tagId), tagUnitStats)
		return tagUnitStats
	}
	
	

	@Transactional
	static TagUnitStats createOrUpdate(Long userId, Long tagId, String unit) {
		if (unit == null || unit == '') { // if blank unit, return null
			return null
		}
		
		// Find UnitRatio for the unit
		UnitRatio unitRatio = UnitGroupMap.theMap.unitRatioForUnits(unit)

		if (!unitRatio) {
			return createOrUpdateSingle(userId, tagId, unit, null, false)
		} else {
			TagUnitStats stats = createOrUpdateSingle(userId, tagId, unit, unitRatio.getGroupId(), true)
			if (unit != unitRatio.canonicalUnitString)
				createOrUpdateSingle(userId, tagId, unitRatio.canonicalUnitString, unitRatio.getGroupId(), false)
			
			return stats
		}
	}
	
	static def mostUsedTagUnitStats(Long userId, Long tagId) {
		TagUnitStats stats = cache.getAt(new UserTagId(userId, tagId))
		if (stats != null) {
			if (!stats.isAttached()) {
				stats.attach()
			}
			
			return stats
		}
		def tagUnitStats = TagUnitStats.withCriteria {
			and {
				eq ('tagId', tagId)
				eq ('userId', userId)
			}
			order ('timesUsed', 'desc')
		}
		return tagUnitStats.size() > 0 ? tagUnitStats[0] : null
	}
	
	static def mostUsedTagUnitStatsForTags(Long userId, def tagIds) {
		def r = TagUnitStats.executeQuery("select tagStats.unitGroupId, sum(tagStats.timesUsed) as s from TagUnitStats tagStats where tagStats.tagId in (:tagIds) and tagStats.userId = :userId group by tagStats.unitGroupId order by s desc",
				[tagIds: tagIds, userId: userId], [max: 1])
		if ((!r) || (!r[0]))
			return null
			
		def unitGroupId = r[0][0]
		
		if (unitGroupId == null)
			return null
			
		r = TagUnitStats.executeQuery("select tagStats.unit, sum(tagStats.timesUsed) as s from TagUnitStats tagStats where tagStats.tagId in (:tagIds) and tagStats.userId = :userId and tagStats.unitGroupId = :unitGroupId group by tagStats.unit order by s desc",
				[tagIds: tagIds, userId: userId, unitGroupId: unitGroupId], [max: 2])
		
		if ((!r) || (!r[0]))
			return null
		
		return [unitGroupId: unitGroupId, unit: r[0][0]]
	}
}
