package us.wearecurio.thirdparty.oura

import us.wearecurio.thirdparty.TagUnitMap

class LegacyOuraTagUnitMap extends TagUnitMap {

	private static Map sleepUnitMap, activityUnitMap, exerciseUnitMap, columnDetailMap = [:]

	Map buckets

	static {
		exerciseUnitMap = [
			classification_light: [tag: "light $EXERCISE", unit: "hours", convert: true, from: "mins"],
			classification_moderate: [tag: "moderate $EXERCISE", unit: "hours", convert: true, from: "mins"],
			classification_sedentary: [tag: "sedentary $EXERCISE", unit: "hours", convert: true, from: "mins"],
			classification_rest: [tag: "rest $EXERCISE", unit: "hours", convert: true, from: "mins"]
		]

		activityUnitMap = [
			non_wear_m: [tag: ACTIVITY, unit: "hours nonwear", convert: true, from: "mins"],
			steps: [tag: ACTIVITY, unit: "steps"],
			eq_meters: [tag: ACTIVITY, unit: "miles walk", convert: true, from: "meters"],
			active_cal: [tag: ACTIVITY, unit: "kcal active", convert: true, from: "cal"],
			total_cal: [tag: ACTIVITY, unit: "kcal total", convert: true, from: "cal"],
		]

		sleepUnitMap = [
			bedtime_m: [tag: SLEEP, unit: "hours total", convert: true, from: "mins"],
			sleep_score: [tag: SLEEP, unit: "score"],
			awake_m: [tag: SLEEP, unit: "hours awake", convert: true, from: "mins"],
			rem_m: [tag: SLEEP, unit: "hours rem", convert: true, from: "mins"],
			light_m: [tag: SLEEP, unit: "hours light", convert: true, from: "mins"],
			deep_m: [tag: SLEEP, unit: "hours deep", convert: true, from: "mins"]
		]

		columnDetailMap.putAll(sleepUnitMap)
		columnDetailMap.putAll(activityUnitMap)
		columnDetailMap.putAll(exerciseUnitMap)
	}

	LegacyOuraTagUnitMap() {
		super("OuraLegacy", true)
		// First keeping commonTagMap so that unit map from here can override the common
		tagUnitMappings = initializeTagUnitMappings(commonTagMap + columnDetailMap)
	}

	@Override
	Map getBuckets() {
		return null
	}
}
