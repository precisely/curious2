package us.wearecurio.thirdparty.jawbone

import us.wearecurio.thirdparty.TagUnitMap

import us.wearecurio.model.Entry.DurationType

class JawboneTagUnitMap extends TagUnitMap {
	
	private static Map estimatedMealUnitMap, measuredMovementUnitMap, sleepUnitMap, napUnitMap, moodUnitMap, 
		columnDetailMap = [:]
		
	Map buckets

	static {
		estimatedMealUnitMap = [
			e_calcium: [tag: "$MEAL", unit: "mg calcium"],
			e_calories: [tag: "$MEAL", unit: "kcal"],
			e_carbs: [tag: "$MEAL", unit: "g carbs"],
			e_cholesterol: [tag: "$MEAL", unit: "mg cholesterol"],
			e_fiber: [tag: "$MEAL", unit: "g fiber"],
			e_protein: [tag: "$MEAL", unit: "g protein"],
			e_sat_fat : [tag: "$MEAL", unit: "g saturated fat"],
			e_sodium: [tag: "$MEAL", unit: "mg sodium"],
			e_sugar: [tag: "$MEAL", unit: "g sugar"],
			e_unsat_fat: [tag: "$MEAL", unit: "g unsaturated fat"]
		]

		measuredMovementUnitMap = [
			m_active_time: [tag: "$MOVEMENT", unit: "mins active", convert: true, from: "secs"],
			m_calories: [tag: "$MOVEMENT", unit: "cal"],
			m_distance: [tag: "$MOVEMENT", unit: "m"],
			m_lcat: [tag: "$MOVEMENT", unit: "secs longest active"],
			m_lcit: [tag: "$MOVEMENT", unit: "secs longest idle"],
			m_steps: [tag: "$MOVEMENT", unit: "steps"],
			m_workout_count: [tag: "$MOVEMENT workout", suffix: "count", unit: ""],
			m_workout_time: [tag: "$MOVEMENT workout", unit: "hours", convert: true, from: "secs"]
		]

		sleepUnitMap = [
			s_asleep_time: [tag: "$SLEEP", suffix: "start", unit: "ms", convert: true, from: "ms", durationType: DurationType.START],
			s_awake: [tag: "$SLEEP", unit: "hours awake", convert: true, from: "secs"],
			s_awake_time: [tag: "$SLEEP", suffix: "end", unit: "ms", convert: true, from: "ms", durationType: DurationType.END],
			s_awakenings: [tag: "$SLEEP awakenings", unit: ""],
			s_bedtime: [tag: "$SLEEP", suffix: "start", unit: "", durationType: DurationType.START],
			s_deep: [tag: "$SLEEP", unit: "hours deep", convert: true, from: "secs"],
			s_duration: [tag: "$SLEEP", unit: "hours", convert: true, from: "secs"],
			s_light: [tag: "$SLEEP", unit: "hours light", convert: true, from: "secs"],
			s_quality: [tag: "$SLEEP quality", unit: "", convert: true, from: "to one"]
		]

		napUnitMap = [
			n_asleep_time: [tag: "nap", suffix: "start", unit: "ms", convert: true, from: "ms", durationType: DurationType.START],
			n_awake: [tag: "$SLEEP", unit: "hours awake", convert: true, from: "secs"],
			n_awake_time: [tag: "nap", suffix: "end", unit: "ms", convert: true, from: "ms", durationType: DurationType.END],
			n_awakenings: [tag: "$SLEEP awakenings", unit: ""],
			n_bedtime: [tag: "$SLEEP", suffix: "start", unit: "", durationType: DurationType.START],
			n_deep: [tag: "$SLEEP", unit: "hours deep", convert: true, from: "secs"],
			n_duration: [tag: "$SLEEP", unit: "hours", convert: true, from: "secs"],
			n_light: [tag: "$SLEEP", unit: "hours light", convert: true, from: "secs"],
			n_quality: [tag: "$SLEEP quality", unit: "", convert: true, from: "to one"]
		]

		moodUnitMap = [
			o_count: [tag: "$MOOD workout", unit: "", bucketKey: "mood_average"],
			o_mood: [tag: "$MOOD count", unit: "", bucketKey: "mood_average"]
		]

		columnDetailMap.putAll(estimatedMealUnitMap)
		columnDetailMap.putAll(sleepUnitMap)
		columnDetailMap.putAll(measuredMovementUnitMap)
		columnDetailMap.putAll(napUnitMap)
		columnDetailMap.putAll(moodUnitMap)
	}
	
	public JawboneTagUnitMap() {
		buckets = [
			// don't forget to initialize a empty list named `values`.
			mood_average: [values: [], tag: "$MOOD average", unit: "", operation: AVERAGE],
		]
		tagUnitMappings = initializeTagUnitMappings(columnDetailMap)
	}
	
	/**
	 * Buckets are used to perform multiple operations on several columns. Note that
	 * those columns will be not stored in Entry domain, instead the final values from
	 * bucket will be used.
	 * For ex. Taking average of two columns and save it as a new column,
	 * @return
	 */
	public Map getBuckets() {
		return buckets
	}

}