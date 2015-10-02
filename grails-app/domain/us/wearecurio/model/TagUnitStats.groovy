package us.wearecurio.model

import grails.compiler.GrailsTypeChecked
import org.apache.commons.logging.LogFactory

import org.springframework.aop.aspectj.RuntimeTestWalker.ThisInstanceOfResidueTestVisitor;
import org.springframework.transaction.annotation.Transactional

import us.wearecurio.cache.BoundedCache
import us.wearecurio.utility.Utils
import us.wearecurio.services.DatabaseService
import us.wearecurio.data.UnitGroupMap
import us.wearecurio.data.DataRetriever
import us.wearecurio.data.UnitGroupMap.UnitGroup
import us.wearecurio.data.DecoratedUnitRatio
import us.wearecurio.data.UnitRatio
import us.wearecurio.data.RepeatType
import us.wearecurio.data.TagUnitStatsInterface

class TagUnitStats implements TagUnitStatsInterface {
	
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
	
	UnitGroup getUnitGroup() {
		if (unitGroupId)
			return UnitGroup.get(unitGroupId)
			
		return null
	}
	
	String getUnit() {
		return unit
	}
	
	String lookupUnitString(boolean plural) {
		return getUnitGroup()?.lookupUnitString(unit, plural)
	}
	
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
		Utils.save(tagUnitStats, true)
		
		DataRetriever.addToTagUnitStatsCache(tagUnitStats)
		
		return tagUnitStats
	}
	
	/**
	 * Updates all TagUnitStats for given user
	 * @param user
	 * @return
	 */
	static def updateTagUnitStats(User user) {
		log.debug "TagStats.updateTagStats() userId:" + user.getId()
		
		def userId = user.getId()
		
		def tagStats = []
		
		def tags = Entry.getTags(user, Entry.ONLYIDS)
		
		for (tagId in tags) {
			createOrUpdateAllUnits(userId, tagId)
		}
	}
	
	@Transactional
	static TagUnitStats createOrUpdateAllUnits(Long userId, Long tagId) {
		def results = DatabaseService.get().sqlRows(
			"select units from Entry entry where entry.tag_id = :tagId and entry.user_id = :userId and units IS NOT NULL and length(units) > 0 and entry.date IS NOT NULL and (entry.repeat_type_id IS NULL or (entry.repeat_type_id & :ghostBit = 0)) group by units",
			[tagId:tagId, userId:userId, ghostBit:RepeatType.GHOST_BIT])

		if (results && results.size() > 0) {
			String units = results[0]['units']
			createOrUpdate(userId, tagId, units)
		}
	}
	

	@Transactional
	static TagUnitStats createOrUpdate(Long userId, Long tagId, String unit) {
		if (unit == null || unit == '') { // if blank unit, return null
			return null
		}
		
		// Find UnitRatio for the unit
		DecoratedUnitRatio unitRatio = UnitGroupMap.theMap.lookupDecoratedUnitRatio(unit)

		if (!unitRatio) {
			return createOrUpdateSingle(userId, tagId, unit, null, false)
		} else {
			TagUnitStats stats = createOrUpdateSingle(userId, tagId, unit, unitRatio.getGroupId(), true)
			if (unit != unitRatio.canonicalUnitString)
				createOrUpdateSingle(userId, tagId, unitRatio.canonicalUnitString, unitRatio.getGroupId(), false)
			
			return stats
		}
	}

	static TagUnitStatsInterface mostUsedTagUnitStats(Long userId, Long tagId) {
		return DataRetriever.get().mostUsedTagUnitStats(userId, tagId)
	}
	
	static TagUnitStatsInterface mostUsedTagUnitStatsForTags(Long userId, def tagIds) {
		return DataRetriever.get().mostUsedTagUnitStatsForTags(userId, tagIds)
	}
}
