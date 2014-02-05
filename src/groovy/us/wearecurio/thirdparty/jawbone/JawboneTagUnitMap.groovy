package us.wearecurio.thirdparty.jawbone

import us.wearecurio.thirdparty.TagUnitMap

class JawboneTagUnitMap extends TagUnitMap {
	
	private static Map estimatedMealUnitMap, measuredMovementUnitMap, sleepUnitMap, napUnitMap, moodUnitMap, 
		columnDetailMap = [:]
		
	Map buckets

	static {
		estimatedMealUnitMap = [
			e_calcium: [tag: "$MEAL calcium", unit: "mg"],
			e_calories: [tag: "$MEAL calories", unit: "kcal"],
			e_carbs: [tag: "$MEAL carbs", unit: "g"],
			e_cholesterol: [tag: "$MEAL cholesterol", unit: "mg"],
			e_fiber: [tag: "$MEAL fiber", unit: "g"],
			e_protein: [tag: "$MEAL protein", unit: "g"],
			e_sat_fat : [tag: "$MEAL saturated fat", unit: "g"],
			e_sodium: [tag: "$MEAL sodium", unit: "mg"],
			e_sugar: [tag: "$MEAL sugar", unit: "g"],
			e_unsat_fat: [tag: "$MEAL unsaturated fat", unit: "g"]
		]

		measuredMovementUnitMap = [
			m_active_time: [tag: "$MOVEMENT active time", unit: "hours", convert: true, type: SECONDS_TO_HOURS],
			m_calories: [tag: "$MOVEMENT calories", unit: "cal"],
			m_distance: [tag: "$MOVEMENT distance", unit: "m"],
			m_lcat: [tag: "$MOVEMENT longest active", unit: "secs"],
			m_lcit: [tag: "$MOVEMENT longest idle", unit: "secs"],
			m_steps: [tag: "$MOVEMENT steps", unit: ""],
			m_workout_count: [tag: "$MOVEMENT workout count", unit: ""],
			m_workout_time: [tag: "$MOVEMENT workout duration", unit: "hours", convert: true, type: SECONDS_TO_HOURS]
		]

		sleepUnitMap = [
			s_asleep_time: [tag: "sleeping start", unit: "", convert: true, type: MINUTES_TO_MS],
			s_awake: [tag: "$SLEEP awake", unit: "seconds", convert: true, type: SECONDS_TO_HOURS],
			s_awake_time: [tag: "sleeping end", unit: ""],
			s_awakenings: [tag: "$SLEEP awakenings", unit: ""],
			s_bedtime: [tag: "$SLEEP mode start", unit: ""],
			s_deep: [tag: "deep $SLEEP", unit: "hours", convert: true, type: SECONDS_TO_HOURS],
			s_duration: [tag: "$SLEEP", unit: "hours", convert: true, type: SECONDS_TO_HOURS],
			s_light: [tag: "light $SLEEP", unit: "hours", convert: true, type: SECONDS_TO_HOURS],
			s_quality: [tag: "$SLEEP quality", unit: "score", convert: true, type: SCALE_TO_0_10]
		]

		napUnitMap = [
			n_asleep_time: [tag: "napping start", unit: "", convert: true, type: MINUTES_TO_MS],
			n_awake: [tag: "$SLEEP awake", unit: "hours", convert: true, type: SECONDS_TO_HOURS],
			n_awake_time: [tag: "napping end", unit: "", convert: true, type: MINUTES_TO_MS],
			n_awakenings: [tag: "$SLEEP awakenings", unit: ""],
			n_bedtime: [tag: "$SLEEP mode start", unit: "", convert: true, type: MINUTES_TO_MS],
			n_deep: [tag: "$SLEEP deep", unit: "hours", convert: true, type: SECONDS_TO_HOURS],
			n_duration: [tag: "$SLEEP", unit: "hours", convert: true, type: SECONDS_TO_HOURS],
			n_light: [tag: "$SLEEP light", unit: "hours", convert: true, type: SECONDS_TO_HOURS],
			n_quality: [tag: "$SLEEP quality", unit: "", convert: true, type: SCALE_TO_0_10]
		]

		moodUnitMap = [
			o_count: [tag: "$MOOD workout", unit: "number", convert: true, type: BUCKET, bucketKey: "mood_average"],
			o_mood: [tag: "$MOOD count", unit: "number", convert: true, type: BUCKET, bucketKey: "mood_average"]
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
	}
	
	public Map getTagUnitMappings() {
		return columnDetailMap
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