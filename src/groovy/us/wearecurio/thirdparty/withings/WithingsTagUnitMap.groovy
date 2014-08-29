package us.wearecurio.thirdparty.withings

import us.wearecurio.thirdparty.TagUnitMap

class WithingsTagUnitMap extends TagUnitMap {

	static Map activityUnitMap, columnDetailMap = [:]
	
	static {
		//columnDetailMap.putAll(activityUnitMap)
	}
	
	WithingsTagUnitMap() {
		tagUnitMappings = initializeTagUnitMappings(columnDetailMap + commonTagMap)
	}

	@Override
	Map getBuckets() {
		null
	}
}