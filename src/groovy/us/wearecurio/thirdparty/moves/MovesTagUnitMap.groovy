package us.wearecurio.thirdparty.moves

import java.util.Map;

import us.wearecurio.model.DurationType

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
			bikeStart: [tag: "$BIKE", suffix: "start", unit: "", durationType: DurationType.START],
			bikeEnd: [tag: "$BIKE", suffix: "end", unit: "", durationType: DurationType.END],
		]

		runUnitMap = [
			runStep: [tag: "$RUN", unit: "steps"],
			runDistance: [tag: "$RUN", unit: "km", convert: true, from: "meter"],
			runCalories: [tag: "$RUN", unit: "kcal"],
			runStart: [tag: "$RUN", suffix: "start", unit: "", durationType: DurationType.START],
			runEnd: [tag: "$RUN", suffix: "end", unit: "", durationType: DurationType.END],
		]

		walkUnitMap = [
			walkStep: [tag: "$WALK", unit: "steps"],
			walkDistance: [tag: "$WALK", unit: "km", convert: true, from: "meter"],
			walkCalories: [tag: "$WALK", unit: "kcal"],
			walkStart: [tag: "$WALK", suffix: "start", unit: "", durationType: DurationType.START],
			walkEnd: [tag: "$WALK", suffix: "end", unit: "", durationType: DurationType.END],
		]

		columnDetailMap.putAll(bikeUnitMap)
		columnDetailMap.putAll(runUnitMap)
		columnDetailMap.putAll(walkUnitMap)
	}

	MovesTagUnitMap() {
		tagUnitMappings = initializeTagUnitMappings(columnDetailMap)
	}

	@Override
	Map getBuckets() {
		[:]
	}
}