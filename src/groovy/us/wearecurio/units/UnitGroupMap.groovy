package us.wearecurio.units

import org.apache.commons.logging.LogFactory
import us.wearecurio.model.*

class UnitGroupMap {

	private static def log = LogFactory.getLog(this)

	//[groupName: 
	//		[ unitName: [relativeScoreVal, affinityVal], 
	//		[ unitName: [relativeScoreVal, affinityVal], 
	//]
	static Map groups = [
		(UnitGroup.DURATION): ['m':[0.1,0.2], 'min':[0.1,0.2], 'mins':[0.1,0.2], 'minute':[0.1,0.2], 
			'minutes':[0.1,0.2], 'h':[0.1,0.2], 'hours':[0.1,0.2], 'hrs':[0.1,0.2], 'hour':[0.1,0.2], 
			'day':[0.1,0.2], 'days':[0.1,0.2], 'd':[0.1,0.2], 'week':[0.1,0.2],
			'weeks':[0.1,0.2], 'wks':[0.1,0.2], 'wk':[0.1,0.2], 'month':[0.1,0.2], 'months':[0.1,0.2],
			'mnths':[0.1,0.2], 'year':[0.1,0.2], 'years':[0.1,0.2], 'y':[0.1,0.2], 'century':[0.1,0.2],
			'centuries':[0.1,0.2], 'ms':[0.1,0.2],'sec':[1000,0.2], 'secs':[1000,0.2], 'seconds': [1000,0.2],
			'seccond':[1000.1,0.2], 'millisecond':[1,0.2], 'milliseconds':[1,0.2], 'microsecond':[0.001f,0.2],
			'microseconds':[0.001f,0.2], 'picosecond':[0.000000001f,0.2], 'picoseconds':[0.000000001f,0.2]
		],
		(UnitGroup.DISTANCE): [
			'm':[0.1,0.2], 'meter':[0.1,0.2], 'foot':[0.1,0.2], 'feet':[0.1,0.2], 'ft':[0.1,0.2], 
			'yard':[0.1,0.2], 'yards':[0.1,0.2], 'miles':[0.1,0.2], 'km':[0.1,0.2], 'kilometers':[0.1,0.2],
			'kilometers':[0.1,0.2], 'kilometer':[0.1,0.2],'kilometre':[0.1,0.2], 'kilometres':[0.1,0.2]
		],
		(UnitGroup.WEIGHT): [
			'g':[0.1,0.2], 'grams':[0.1,0.2], 'pound':[0.1,0.2], 'pounds':[0.1,0.2], 'lbs':[0.1,0.2], 
			'kg':[0.1,0.2], 'kilograms':[0.1,0.2], 'kilogram':[0.1,0.2],
		]
	];

	/**
	*	Compute which group a given unit falls under
	*
	*/
	public static groupForUnit(String unit) {
		def result = null

		if (unit == '') return result

		groups.each { group, unitMeta ->
			if (unitMeta.containsKey(unit)) {
				def ratioAndAffinity = unitMeta.get(unit)
				if (result == null || result.affinity < ratioAndAffinity[1]) {
					result = [group:group, affinity: ratioAndAffinity[1], relativeRatio: ratioAndAffinity[0]]
				}
					
			}
		}
		return result
	}

}
