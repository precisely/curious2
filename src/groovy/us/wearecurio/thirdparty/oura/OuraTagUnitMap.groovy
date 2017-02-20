package us.wearecurio.thirdparty.oura

import us.wearecurio.thirdparty.TagUnitMap

class OuraTagUnitMap extends TagUnitMap {

	private static Map sleepUnitMap, activityUnitMap, exerciseUnitMap, readinessUnitMap, columnDetailMap = [:]

	Map buckets

	static {
		exerciseUnitMap = [
			classification_light: [tag: "light $EXERCISE", unit: "hours", convert: true, from: "mins"],
			classification_moderate: [tag: "moderate $EXERCISE", unit: "hours", convert: true, from: "mins"],
			classification_sedentary: [tag: "sedentary $EXERCISE", unit: "hours", convert: true, from: "mins"],
			classification_rest: [tag: "rest $EXERCISE", unit: "hours", convert: true, from: "mins"]
		]

		activityUnitMap = [
			non_wear: [tag: ACTIVITY, unit: "hours nonwear", convert: true, from: "minutes"],
			steps: [tag: ACTIVITY, unit: "steps"],
			daily_movement: [tag: ACTIVITY, unit: "miles walk", convert: true, from: "meters"],
			cal_active: [tag: ACTIVITY, unit: "kcal active"],
			cal_total: [tag: ACTIVITY, unit: "kcal total"],
		]

		sleepUnitMap = [
			total: [tag: SLEEP, unit: "hours total", convert: true, from: "seconds"],
			score: [tag: SLEEP, unit: "score"],
			awake: [tag: SLEEP, unit: "hours awake", convert: true, from: "seconds"],
			rem: [tag: SLEEP, unit: "hours rem", convert: true, from: "seconds"],
			light: [tag: SLEEP, unit: "hours light", convert: true, from: "seconds"],
			deep: [tag: SLEEP, unit: "hours deep", convert: true, from: "seconds"],

			// Present in new Oura API.
			wake_up_count: [tag: SLEEP, unit: 'wake up times'],
			got_up_count: [tag: SLEEP, unit: 'got up times'],
			efficiency: [tag: SLEEP, unit: '%'],
			hr_lowest: [tag: SLEEP, unit: 'bpm lowest'],
			restless: [tag: SLEEP, unit: '%'],
			score_total: [tag: SLEEP, unit: 'score total'],
			score_rem: [tag: SLEEP, unit: 'score rem'],
			score_deep: [tag: SLEEP, unit: 'score deep'],
			score_efficiency: [tag: SLEEP, unit: 'score efficiency'],
			score_latency: [tag: SLEEP, unit: 'score latency'],
			score_disturbances: [tag: SLEEP, unit: 'score disturbances'],
			score_alighment: [tag: SLEEP, unit: 'score alignment'],
		]

		readinessUnitMap = [
			readiness_score: [tag: READINESS, unit: 'score'],
		]

		columnDetailMap.putAll(sleepUnitMap)
		columnDetailMap.putAll(activityUnitMap)
		columnDetailMap.putAll(exerciseUnitMap)
		columnDetailMap.putAll(readinessUnitMap)
	}

	OuraTagUnitMap() {
		super("Oura", true)
		// First keeping commonTagMap so that unit map from here can override the common
		tagUnitMappings = initializeTagUnitMappings(commonTagMap + columnDetailMap)
	}

	@Override
	Map getBuckets() {
		return null
	}
}
