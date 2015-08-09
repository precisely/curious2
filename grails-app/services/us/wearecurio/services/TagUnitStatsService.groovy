package us.wearecurio.services

import java.util.Collection

import us.wearecurio.units.UnitGroupMap
import us.wearecurio.model.TagUnitStats
import us.wearecurio.units.TagUnitStatsInterface
import us.wearecurio.units.TagUnitStatsServiceInterface

import org.apache.commons.logging.LogFactory

class TagUnitStatsService implements TagUnitStatsServiceInterface {

	static def service
	
	static def set(s) {
		service = s
	}

	static TagUnitStatsService get() { return service }
	
	@Override
	public TagUnitStatsInterface mostUsedTagUnitStatsForTags(Long userId,
			Collection<Long> tagIds) {
		return TagUnitStats.mostUsedTagUnitStatsForTags(userId, tagIds)
	}
	
	@Override
	public TagUnitStatsInterface mostUsedTagUnitStats(Long userId, Long tagId) {
		return TagUnitStats.mostUsedTagUnitStats(userId, tagId)
	}
	
	private static def log = LogFactory.getLog(this)
	static transactional = true
	
	TagUnitStatsService() {
		UnitGroupMap.setTagUnitStatsService(this)
	}
}
