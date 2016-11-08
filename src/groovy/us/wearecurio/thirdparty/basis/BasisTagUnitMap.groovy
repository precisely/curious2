package us.wearecurio.thirdparty.basis

import us.wearecurio.thirdparty.TagUnitMap

class BasisTagUnitMap  extends TagUnitMap {
	private static Map sleepUnitMap, activityUnitMap, exerciseUnitMap, columnDetailMap = [:]

	Map buckets

	static {
		sleepUnitMap = [
				light: [tag: SLEEP, unit: "hours total", convert: true, from: "mins"],
				unknown: [tag: SLEEP, unit: "hours unknown", convert: true, from: "mins"],
				interruption: [tag: SLEEP, unit: "hours interrupted", convert: true, from: "mins"],
				rem: [tag: SLEEP, unit: "hours rem", convert: true, from: "mins"],
				deep: [tag: SLEEP, unit: "hours deep", convert: true, from: "mins"],
				total: [tag: SLEEP, unit: "hours total", convert: true, from: "mins"],
				tosses: [tag: SLEEP, unit: "times tossed"]
		]

		activityUnitMap = [
				light_activity_steps: [tag: ACTIVITY, unit: "steps light"],
				inactive_calories: [tag: ACTIVITY, unit: "kcal inactive", convert: true, from: "cal"],
				light_activity_calories: [tag: ACTIVITY, unit: "kcal light", convert: true, from: "cal"],
				off_wrist_calories: [tag: ACTIVITY, unit: "kcal offwrist", convert: true, from: "cal"],
				inactive_heart_rate: [tag: "heart rate", unit: "bpm inactive"],
				light_activity_heart_rate: [tag: "heart rate", unit: "bpm light"],
		]

		columnDetailMap.putAll(sleepUnitMap)
		columnDetailMap.putAll(activityUnitMap)
	}

	BasisTagUnitMap() {
		super("Basis", true)
		// First keeping commonTagMap so that unit map from here can override the common
		tagUnitMappings = initializeTagUnitMappings(commonTagMap + columnDetailMap)
	}

	@Override
	Map getBuckets() {
		return null
	}
}
