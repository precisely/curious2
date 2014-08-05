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
			minutes: [tag: "$ACTIVITY", unit: "mins", convert: true, from: "ms"],
			total: [tag: "$ACTIVITY total ", unit: "miles"],
			tracker: [tag: "$ACTIVITY tracker", unit: "miles"],
			veryActive: [tag: "high $ACTIVITY", unit: "miles"],
			moderatelyActive: [tag: "moderate $ACTIVITY", unit: "miles"],
			lightlyActive: [tag: "light $ACTIVITY", unit: "miles"],
			sedentaryActive: [tag: "sedentary $ACTIVITY", unit: "miles"],
			fairlyActiveMinutes: [tag: "moderate $ACTIVITY", unit: "mins", convert: true, from: "ms"],
			lightlyActiveMinutes: [tag: "light $ACTIVITY", unit: "mins", convert: true, from: "ms"],
			sedentaryMinutes: [tag: "sedentary $ACTIVITY", unit: "mins", convert: true, from: "ms"],
			veryActiveMinutes: [tag: "high $ACTIVITY", unit: "mins", convert: true, from: "ms"],
		]

		columnDetailMap.putAll(sleepUnitMap)
		columnDetailMap.putAll(activityUnitMap)
	}

	@Override
	Map getBuckets() {
		return null
	}

	@Override
	Map getTagUnitMappings() {
		columnDetailMap + commonTagMap
	}
}