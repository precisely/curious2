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
			bikeDistance: [tag: "$BIKE", unit: "km", convert: true, from: "meter"],
			bikeCalories: [tag: "$BIKE", unit: "kcal"],
			bikeStart: [tag: "$BIKE start", unit: ""],
			bikeEnd: [tag: "$BIKE end", unit: ""],
		]

		runUnitMap = [
			runStep: [tag: "$RUN", unit: "steps"],
			runDistance: [tag: "$RUN", unit: "km", convert: true, from: "meter"],
			runCalories: [tag: "$RUN", unit: "kcal"],
			runStart: [tag: "$RUN start", unit: ""],
			runEnd: [tag: "$RUN end", unit: ""],
		]

		walkUnitMap = [
			walkStep: [tag: "$WALK", unit: "steps"],
			walkDistance: [tag: "$WALK", unit: "km", convert: true, from: "meter"],
			walkCalories: [tag: "$WALK", unit: "kcal"],
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