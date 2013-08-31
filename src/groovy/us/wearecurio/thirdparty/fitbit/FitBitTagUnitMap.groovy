package us.wearecurio.thirdparty.fitbit

import java.util.Map;
import us.wearecurio.thirdparty.TagUnitMap;


class FitBitTagUnitMap extends TagUnitMap {
	public final static String ACTIVITY = "activity"
	static Map sleepUnitMap, activityUnitMap, columnDetailMap = [:]

	static {
		sleepUnitMap = [
			minutesAsleep: [tag: "$SLEEP minutes asleep", unit: "mins", convert: true, type: MS_TO_MINUTES],
			duration: [tag: "$SLEEP duration", unit: "mins", convert: true, type: MS_TO_MINUTES],
			efficiency: [tag: "$SLEEP efficiency", unit: "", convert: true, type: SCALE_TO_0_10],
			awakeningsCount: [tag: "$SLEEP interruptions", unit: ""],
			startTime: [tag: "$SLEEP startTime", unit: ""],
		]
		
		activityUnitMap = [
			miles: [tag: "$ACTIVITY", unit: "miles"],
			minutes: [tag: "$ACTIVITY", unit: "mins", convert: true, type: MS_TO_MINUTES],
			total: [tag: "$ACTIVITY total ", unit: "miles"],
			tracker: [tag: "$ACTIVITY tracker", unit: "miles"],
			veryActive: [tag: "$ACTIVITY veryActive", unit: "miles"],
			moderatelyActive: [tag: "$ACTIVITY moderately active", unit: "miles"],
			lightlyActive: [tag: "$ACTIVITY lightly active", unit: "miles"],
			sedentaryActive: [tag: "$ACTIVITY sedentary active", unit: "miles"],
			steps: [tag: "$ACTIVITY steps", unit: ""],
			fairlyActiveMinutes: [tag: "$ACTIVITY active", unit: "mins", convert: true, type: MS_TO_MINUTES],
			lightlyActiveMinutes: [tag: "$ACTIVITY light", unit: "mins", convert: true, type: MS_TO_MINUTES],
			sedentaryMinutes: [tag: "$ACTIVITY sedentary", unit: "mins", convert: true, type: MS_TO_MINUTES],
			veryActiveMinutes: [tag: "$ACTIVITY very active", unit: "mins", convert: true, type: MS_TO_MINUTES],
		]
		
		columnDetailMap.putAll(sleepUnitMap)
		columnDetailMap.putAll(activityUnitMap)
	}
	
	@Override
	public Map getBuckets() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Map getTagUnitMappings() {
	
		return columnDetailMap
	}
}
