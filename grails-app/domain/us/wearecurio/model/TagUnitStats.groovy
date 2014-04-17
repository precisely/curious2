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
	Long timesUsed 

	static mapping = {
		version false
		table 'tag_unit_stats'
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
	}
	

	@Transactional
	public static def createOrUpdate(Long userId, Long tagId, String unit) {
		def unitGroup
		// Find if the user has used this for any tag
		def tagUnitStats = TagUnitStats.withCriteria {
			and {
				like ('unit', unit)
				eq ('tagId', tagId)
				eq ('userId', userId)
			}
		}

		if (tagUnitStats.size() < 1) {
			log.debug ("TagUnitStats.createOrUpdate(): 0 unit stats found for user "+
			userId + " tag" + tagId + " and unit " + unit)
			def unitGroupMap = UnitGroupMap.groupForUnit(unit)
			unitGroup = unit == ''?:unitGroupMap.group
			tagUnitStats = new TagUnitStats(tagId: tagId, userId: userId, 
				unitGroup: unitGroup, unit: unit, timesUsed: 1)
		} else {
			log.debug ("TagUnitStats.createOrUpdate(): ${tagUnitStats.size()} unit stats found for user "+
				userId + " tag" + tagId + " and unit " + unit)
			tagUnitStats = tagUnitStats[0]
			tagUnitStats.timesUsed+=1
		}
		log.debug ("TagUnitStats.createOrUpdate():" + tagUnitStats.dump())
		tagUnitStats.save()
	}
}

enum UnitGroup {

	DURATION(1, "duration"),
	DISTANCE(2, "distance"),
	WEIGHT(3, "weight")

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
