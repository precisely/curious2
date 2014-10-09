package us.wearecurio.thirdparty.jawbone

import us.wearecurio.thirdparty.TagUnitMap

class JawboneUpTagUnitMap extends TagUnitMap {

	private static Map estimatedMealUnitMap, measuredMovementUnitMap, sleepUnitMap, napUnitMap, moodUnitMap,
	columnDetailMap = [:]

	Map buckets

	static {
		sleepUnitMap = [
			duration: [tag: "$SLEEP", unit: "mins", convert: true, from: "second"],
			awakeningsCount: [tag: "$SLEEP interruptions", unit: ""],
			minutesAsleep: [tag: "$SLEEP", unit: "hours", convert: true, from: "ms"],
			startTime: [tag: "$SLEEP startTime", unit: ""],
		]

		columnDetailMap.putAll(sleepUnitMap)
	}

	JawboneUpTagUnitMap() {
		tagUnitMappings = initializeTagUnitMappings(columnDetailMap + commonTagMap)
	}

	@Override
	Map getBuckets() {
		return null
	}
}