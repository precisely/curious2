package us.wearecurio.model

import org.apache.commons.logging.LogFactory
import org.springframework.transaction.annotation.Transactional
import us.wearecurio.units.UnitGroupMap

class TagUnitStats {

	private static def log = LogFactory.getLog(this)

	String unit
	UnitGroup unitGroup
	Long tagId
	Long userId
	Long timesUsed = 0l

	static mapping = {
		version false
		table 'tag_unit_stats'
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
	}


	@Transactional
	public static def createOrUpdate(Long userId, Long tagId, String unit) {
		// Verify if the user has used this unit for this tag in the past
		def tagUnitStats = TagUnitStats.withCriteria {
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
			def unitGroupMap = UnitGroupMap.groupMapForUnit(unit)
			if (!unitGroupMap) {
				log.debug ("TagUnitStats.createOrUpdate(): ${unit} NOT found in the map ")
				//If we can't find it in our map we use the most used unit
				//for this tag
				tagUnitStats = mostUsedTagUnitStats(tagId, userId) 
				if (!tagUnitStats) {
					//This unit is not in the map and this is the first entry for this tag
					//i.e. we don't have the most used unit for this tag
					tagUnitStats = new TagUnitStats(tagId: tagId, userId: userId, 
						unit: unit)
				} 
			} else {
				log.debug ("TagUnitStats.createOrUpdate(): ${unit} FOUND in the map ")
				tagUnitStats = new TagUnitStats(tagId: tagId, userId: userId, 
					unit: unitGroupMap.unit, unitGroup: unitGroupMap.group)
			}
		}

		log.debug ("TagUnitStats.createOrUpdate():" + tagUnitStats.dump())
		tagUnitStats.timesUsed+=1
		tagUnitStats.save()
		return tagUnitStats
	}

	public static def mostUsedTagUnitStats(Long tagId, Long userId) {
		def tagUnitStats = TagUnitStats.withCriteria {
			and {
				eq ('tagId', tagId)
				eq ('userId', userId)
			}
			order ('timesUsed', 'desc')
		}
		return tagUnitStats.size()>1 ? tagUnitStats[0] : null
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
	PRESSURE(11, "pressure")
	
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

	static UnitGroup get(Integer id) {
		map.get(id)
	}
}
