package us.wearecurio.thirdparty.human

import us.wearecurio.thirdparty.TagUnitMap

class HumanTagUnitMap extends TagUnitMap {

	static Map activityUnitMap, columnDetailMap = [:]

	static {
		//columnDetailMap.putAll(activityUnitMap)
	}

	@Override
	Map getBuckets() {
		null
	}

	@Override
	Map getTagUnitMappings() {
		columnDetailMap + commonTagMap
	}

}
