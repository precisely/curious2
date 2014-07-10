package us.wearecurio.model

import org.apache.commons.logging.LogFactory
import org.springframework.transaction.annotation.Transactional
import us.wearecurio.units.UnitGroupMap

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
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
	}
	
	static constraints = {
		unitGroupId(nullable:true)
	}

	@Transactional
	public static def createOrUpdate(Long userId, Long tagId, String unit) {
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
			log.debug ("TagUnitStats.createOrUpdate(): ${tagUnitStats.size()} unit stats found for user "+
					userId + " tag" + tagId)
			log.debug ("TagUnitStats.createOrUpdate(): ${unit} used in the past ")
			tagUnitStats = tagUnitStats[0]
		} else {
			// If this unit is being used for the first time for this tag
			def unitRatio = UnitGroupMap.theMap.unitRatioForUnit(unit)
			if (!unitRatio) {
				log.debug ("TagUnitStats.createOrUpdate(): ${unit} NOT found in the map ")
				//If we can't find it in our map we use the most used unit
				//for this tag
				tagUnitStats = mostUsedTagUnitStats(userId, tagId)
				if (!tagUnitStats) {
					//This unit is not in the map and this is the first entry for this tag
					//i.e. we don't have the most used unit for this tag
					tagUnitStats = new TagUnitStats(tagId: tagId, userId: userId,
							unit: unit)
				}
			} else {
				log.debug ("TagUnitStats.createOrUpdate(): ${unit} FOUND in the map ")
				tagUnitStats = new TagUnitStats(tagId: tagId, userId: userId,
						unit: unitRatio.unit, unitGroupId: unitRatio.getGroupId())
			}
		}
		
		log.debug ("TagUnitStats.createOrUpdate():" + tagUnitStats.dump())
		tagUnitStats.timesUsed+=1
		tagUnitStats.save()
		return tagUnitStats
	}
	
	public static def mostUsedTagUnitStats(Long userId, Long tagId) {
		def tagUnitStats = TagUnitStats.withCriteria {
			and {
				eq ('tagId', tagId)
				eq ('userId', userId)
			}
			order ('timesUsed', 'desc')
		}
		return tagUnitStats.size()>1 ? tagUnitStats[0] : null
	}
	
	public static def mostUsedTagUnitStatsForTags(Long userId, def tagIds) {
		def r = TagUnitStats.executeQuery("select tagStats.unitGroupId, sum(tagStats.timesUsed) as s from TagUnitStats tagStats where tagStats.tagId in (:tagIds) and tagStats.userId = :userId group by tagStats.unitGroupId order by s desc",
				[tagIds: tagIds, userId: userId], [max: 1])
		if ((!r) || (!r[0]))
			return null
			
		def unitGroupId = r[0][0]
		
		if (unitGroupId == null)
			return null
			
		r = TagUnitStats.executeQuery("select tagStats.unit, sum(tagStats.timesUsed) as s from TagUnitStats tagStats where tagStats.tagId in (:tagIds) and tagStats.userId = :userId and tagStats.unitGroupId = :unitGroupId group by tagStats.unit order by s desc",
				[tagIds: tagIds, userId: userId, unitGroupId: unitGroupId], [max: 1])
		
		if ((!r) || (!r[0]))
			return null
		
		return [unitGroupId: unitGroupId, unit: r[0][0]]
	}
}

enum UnitGroup {
	
	DURATION(1, "duration"),
	DISTANCE(2, "distance"),
	WEIGHT(3, "weight"),
	AREA(4, "area"),
	VOLUME(5, "volume"),
	FORCE(6, "force"),
	POWER(7, "power"),
	ENERGY(8, "energy"),
	TORQUE(9, "torque"),
	LUMINOSITY(10, "luminosity"),
	PRESSURE(11, "pressure"),
	DENSITY(12, "density")
	
	final int id
	final String groupName
	
	UnitGroup(int id, String groupName) {
		this.id = id
		this.groupName= groupName
	}
	
	private static final Map<Integer, UnitGroup> map = new HashMap<Integer, UnitGroup>()
	
	static {
		UnitGroup.each { unitGroup ->
			map.put(unitGroup.id, unitGroup)
		}
	}
	
	static UnitGroup get(int id) {
		if (id == null) return null
		map.get(id)
	}
}
