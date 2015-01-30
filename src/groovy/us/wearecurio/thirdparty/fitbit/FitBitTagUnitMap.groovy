package us.wearecurio.thirdparty.fitbit

import us.wearecurio.thirdparty.TagUnitMap

class FitBitTagUnitMap extends TagUnitMap {

	static Map sleepUnitMap, activityUnitMap, columnDetailMap = [:]

	static {
		sleepUnitMap = [
			minutesAsleep: [tag: "$SLEEP", unit: "hours", convert: true, from: "ms"],
			duration: [tag: "$SLEEP", unit: "mins", convert: true, from: "ms"],
			efficiency: [tag: "$SLEEP efficiency", unit: "", convert: true, from: "to one"],
			awakeningsCount: [tag: "$SLEEP interruptions", unit: ""],
			startTime: [tag: "$SLEEP startTime", unit: ""],
		]

		activityUnitMap = [
			miles: [tag: "$ACTIVITY", unit: "miles"],
			minutes: [tag: "$ACTIVITY", unit: "mins"],
			total: [tag: "$ACTIVITY total ", unit: "miles"],
			tracker: [tag: "$ACTIVITY tracker", unit: "miles"],
			veryActive: [tag: "high $ACTIVITY", unit: "miles"],
			moderatelyActive: [tag: "moderate $ACTIVITY", unit: "miles"],
			lightlyActive: [tag: "light $ACTIVITY", unit: "miles"],
			sedentaryActive: [tag: "sedentary $ACTIVITY", unit: "miles"],
			fairlyActiveMinutes: [tag: "moderate $ACTIVITY", unit: "mins"],
			lightlyActiveMinutes: [tag: "light $ACTIVITY", unit: "mins"],
			sedentaryMinutes: [tag: "sedentary $ACTIVITY", unit: "mins"],
			veryActiveMinutes: [tag: "high $ACTIVITY", unit: "mins"],
		]

		columnDetailMap.putAll(sleepUnitMap)
		columnDetailMap.putAll(activityUnitMap)
	}
	
	FitBitTagUnitMap() {
		tagUnitMappings = initializeTagUnitMappings(columnDetailMap + commonTagMap)
	}

	@Override
	Map getBuckets() {
		return null
	}
}