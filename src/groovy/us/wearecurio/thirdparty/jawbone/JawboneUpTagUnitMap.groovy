package us.wearecurio.thirdparty.jawbone

import us.wearecurio.thirdparty.TagUnitMap

class JawboneUpTagUnitMap extends TagUnitMap {

	private static Map sleepUnitMap, activityUnitMap, workOutUnitMap, measurementUnitMap, columnDetailMap = [:]

	static {
		sleepUnitMap = [
			duration: [tag: SLEEP, unit: "mins", convert: true, from: "seconds"],
			awake: [tag: SLEEP, unit: "mins awake", convert: true, from: "secs"],
			awakeningsCount: [tag: SLEEP, suffix: "interruptions", unit: ""],
			quality: [tag: SLEEP, suffix: "quality", unit: "%"]
		]

		activityUnitMap = [
			miles: [tag: ACTIVITY, unit: "miles", convert: true, from: "meters"],
			minutes: [tag: ACTIVITY, unit: "mins", convert: true, from: "seconds"],
			steps: [tag: ACTIVITY, unit: "steps"],

			lightlyActiveDistance: [tag: "light $ACTIVITY", unit: "miles", convert: true, from: "meters"],
			lightlyActiveMinutes: [tag: "light $ACTIVITY", unit: "mins", convert: true, from: "seconds"],
			lightlyActiveSteps: [tag: "light $ACTIVITY", unit: "steps"],
			lightlyActiveCalories: [tag: "light $ACTIVITY", unit: "cal"],

			highActiveDistance: [tag: "high $ACTIVITY", unit: "miles", unit: "miles", convert: true, from: "meters"],
			highActiveMinutes: [tag: "high $ACTIVITY", unit: "mins", convert: true, from: "seconds"],
			highActiveSteps: [tag: "high $ACTIVITY", unit: "steps"],
			highActiveCalories: [tag: "high $ACTIVITY", unit: "cal"],
		]

		measurementUnitMap = [
			weight: [tag: MEASUREMENT, unit: "lbs", amountPrecision: 2, convert: true, from: "kg"],
			fatRatio: [tag: MEASUREMENT, suffix: "fat ratio", unit: "%"]
		]

		columnDetailMap.putAll(sleepUnitMap)
		columnDetailMap.putAll(activityUnitMap)
		columnDetailMap.putAll(measurementUnitMap)
	}

	JawboneUpTagUnitMap() {
		super("Jawbone Up")
		// First keeping commonTagMap so that unit map from here can override the common
		tagUnitMappings = initializeTagUnitMappings(commonTagMap + columnDetailMap)
	}

	@Override
	Map getBuckets() {
		return null
	}
}