package us.wearecurio.thirdparty.withings

import us.wearecurio.thirdparty.TagUnitMap

class WithingsTagUnitMap extends TagUnitMap {

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