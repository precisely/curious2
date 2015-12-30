package us.wearecurio.thirdparty.oura

import us.wearecurio.thirdparty.TagUnitMap

class OuraTagUnitMap extends TagUnitMap {

	private static Map sleepUnitMap, activityUnitMap, exerciseUnitMap, columnDetailMap = [:]

	Map buckets

	static {
		exerciseUnitMap = [
			classification_light: [tag: "light $EXERCISE", unit: "mins"],
			classification_moderate: [tag: "moderate $EXERCISE", unit: "mins"],
			classification_sedentary: [tag: "sedentary $EXERCISE", unit: "mins"],
			classification_rest: [tag: "rest $EXERCISE", unit: "mins"]
		]

		activityUnitMap = [
			non_wear_m: [tag: "$ACTIVITY non wear", unit: "mins"],
			steps: [tag: "$ACTIVITY steps", unit: ""],
			eq_meters: [tag: ACTIVITY, unit: "miles", convert: true, from: "meters"],
			active_cal: [tag: "active $ACTIVITY", unit: "kcal", convert: true, from: "cal"],
			total_cal: [tag: "total $ACTIVITY", unit: "kcal", convert: true, from: "cal"],
		]

		sleepUnitMap = [
			bedtime_m: [tag: "$SLEEP bed time", unit: "mins"],
			sleep_score: [tag: "$SLEEP score", unit: ""],
			awake_m: [tag: "$SLEEP awake", unit: "mins"],
			rem_m: [tag: "$SLEEP rem", unit: "mins"],
			light_m: [tag: "$SLEEP light", unit: "mins"],
			deep_m: [tag: "$SLEEP deep", unit: "mins"]
		]

		columnDetailMap.putAll(sleepUnitMap)
		columnDetailMap.putAll(activityUnitMap)
		columnDetailMap.putAll(exerciseUnitMap)
	}

	OuraTagUnitMap() {
		// First keeping commonTagMap so that unit map from here can override the common
		tagUnitMappings = initializeTagUnitMappings(commonTagMap + columnDetailMap)
	}

	@Override
	Map getBuckets() {
		return null
	}
}
