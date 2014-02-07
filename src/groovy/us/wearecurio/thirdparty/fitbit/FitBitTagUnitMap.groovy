package us.wearecurio.thirdparty.fitbit

import us.wearecurio.thirdparty.TagUnitMap

class FitBitTagUnitMap extends TagUnitMap {

	static Map sleepUnitMap, activityUnitMap, columnDetailMap = [:]

	static {
		sleepUnitMap = [
			minutesAsleep: [tag: "$SLEEP minutes asleep", unit: "mins", convert: true, type: MS_TO_MINUTES],
			duration: [tag: "$SLEEP", unit: "mins", convert: true, type: MS_TO_MINUTES],
			efficiency: [tag: "$SLEEP efficiency", unit: "", convert: true, type: SCALE_TO_0_10],
			awakeningsCount: [tag: "$SLEEP interruptions", unit: ""],
			startTime: [tag: "$SLEEP startTime", unit: ""],
		]

		activityUnitMap = [
			miles: [tag: "$ACTIVITY", unit: "miles"],
			minutes: [tag: "$ACTIVITY", unit: "mins", convert: true, type: MS_TO_MINUTES],
			total: [tag: "$ACTIVITY total ", unit: "miles"],
			tracker: [tag: "$ACTIVITY tracker", unit: "miles"],
			veryActive: [tag: "very high $ACTIVITY distance", unit: "miles"],
			moderatelyActive: [tag: "moderate $ACTIVITY distance", unit: "miles"],
			lightlyActive: [tag: "light $ACTIVITY distance", unit: "miles"],
			sedentaryActive: [tag: "sedentary $ACTIVITY distance", unit: "miles"],
			fairlyActiveMinutes: [tag: "fairly active", unit: "mins", convert: true, type: MS_TO_MINUTES],
			lightlyActiveMinutes: [tag: "light $ACTIVITY", unit: "mins", convert: true, type: MS_TO_MINUTES],
			sedentaryMinutes: [tag: "sedentary $ACTIVITY", unit: "mins", convert: true, type: MS_TO_MINUTES],
			veryActiveMinutes: [tag: "very high $ACTIVITY", unit: "mins", convert: true, type: MS_TO_MINUTES],
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