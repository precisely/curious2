package us.wearecurio.thirdparty.moves

import java.util.Map;

import us.wearecurio.thirdparty.TagUnitMap

class MovesTagUnitMap extends TagUnitMap {

	static final String BIKE = "bike"
	static final String RUN = "run"
	static final String WALK = "walk"

	static Map bikeUnitMap, runUnitMap, walkUnitMap, columnDetailMap = [:]

	static {
		bikeUnitMap = [
			bikeStep: [tag: "$BIKE", unit: "steps"],
			bikeDistance: [tag: "$BIKE distance", unit: "km", convert: true, type: METER_TO_KM],
			bikeCalories: [tag: "$BIKE calories", unit: "kcal"],
			bikeStart: [tag: "$BIKE start", unit: ""],
			bikeEnd: [tag: "$BIKE end", unit: ""],
		]

		runUnitMap = [
			runStep: [tag: "$RUN", unit: "steps"],
			runDistance: [tag: "$RUN distance", unit: "km", convert: true, type: METER_TO_KM],
			runCalories: [tag: "$RUN calories", unit: "Kcal"],
			runStart: [tag: "$RUN start", unit: ""],
			runEnd: [tag: "$RUN end", unit: ""],
		]

		walkUnitMap = [
			walkStep: [tag: "$WALK", unit: "steps"],
			walkDistance: [tag: "$WALK distance", unit: "km", convert: true, type: METER_TO_KM],
			walkCalories: [tag: "$WALK calories", unit: "kcal"],
			walkStart: [tag: "$WALK start", unit: ""],
			walkEnd: [tag: "$WALK end", unit: ""],
		]

		columnDetailMap.putAll(bikeUnitMap)
		columnDetailMap.putAll(runUnitMap)
		columnDetailMap.putAll(walkUnitMap)
	}

	@Override
	Map getBuckets() {
		[:]
	}

	@Override
	Map getTagUnitMappings() {
		columnDetailMap
	}

}