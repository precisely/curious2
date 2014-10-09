package us.wearecurio.thirdparty.jawbone

import us.wearecurio.thirdparty.TagUnitMap

class JawboneUpTagUnitMap extends TagUnitMap {

	private static Map sleepUnitMap, activityUnitMap, workOutUnitMap, columnDetailMap = [:]

	Map buckets

	static {
		sleepUnitMap = [
			duration: [tag: "$SLEEP", unit: "mins", convert: true, from: "seconds"],
			awakeningsCount: [tag: "$SLEEP interruptions", unit: ""],
			minutesAsleep: [tag: "$SLEEP", unit: "hours", convert: true, from: "ms"],
			startTime: [tag: "$SLEEP startTime", unit: ""],
		]

		activityUnitMap = [
			miles: [tag: "$ACTIVITY", unit: "miles", convert: true, from: "m"],
			minutes: [tag: "$ACTIVITY", unit: "mins", convert: true, from: "seconds"],
			calories: [tag: "$MOVEMENT", unit: "cal"],
			workoutCalories: [tag: ACTIVITY, unit: "cal"],
			workoutMinutes: [tag: ACTIVITY, unit: "mins", convert: true, from: "seconds"]
		]

		columnDetailMap.putAll(sleepUnitMap)
		columnDetailMap.putAll(activityUnitMap)
	}

	JawboneUpTagUnitMap() {
		tagUnitMappings = initializeTagUnitMappings(columnDetailMap + commonTagMap)
	}

	@Override
	Map getBuckets() {
		return null
	}
}