package us.wearecurio.thirdparty.human

import us.wearecurio.thirdparty.TagUnitMap

class HumanTagUnitMap extends TagUnitMap {

	static Map activityUnitMap, columnDetailMap = [:]

	static {
		//columnDetailMap.putAll(activityUnitMap)
	}

	HumanTagUnitMap() {
		tagUnitMappings = initializeTagUnitMappings(columnDetailMap + commonTagMap)
	}

	@Override
	Map getBuckets() {
		null
	}
}
