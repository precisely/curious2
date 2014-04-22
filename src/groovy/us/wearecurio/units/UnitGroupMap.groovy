package us.wearecurio.units

import org.apache.commons.logging.LogFactory
import us.wearecurio.model.*
import java.util.regex.Pattern

class UnitGroupMap {

	private static def log = LogFactory.getLog(this)

	public static final int RATIO = 0
	public static final int AFFINITY = 1
	//Duration
	public static final long MILLISECOND = 1
	public static final double PICOSECOND = MILLISECOND * 0.000000001
	public static final double MICROSECOND = MILLISECOND * 0.001
	public static final long SECOND = MILLISECOND * 1000
	public static final long MINUTE = SECOND * 60
	public static final long HOUR = MINUTE * 60
	public static final long DAY = HOUR * 24
	public static final long WEEK = DAY * 7
	public static final double MONTH = DAY * 30.4375
	public static final double YEAR = MONTH * 12
	public static final double CENTURY = YEAR * 100

	//Distance
	public static final float METER = 1
	public static final float FOOT = METER * 0.3048
	public static final float YARD = METER * 0.9144
	public static final float MILE = METER * 1609.34
	public static final float KILOMETER = METER * 1000

	//Weight
	public static final float GRAM = 1
	public static final float KILOGRAM = GRAM * 1000
	public static final float POUND = GRAM * 1000
	

	//[groupName: 
	//		[ unitName: [relativeRatio, affinityVal], 
	//		[ unitName: [relativeRatio, affinityVal], 
	//]
	static Map groups = [
	(UnitGroup.DURATION): ['m':[MINUTE,0.2], 'min':[MINUTE,0.2], 'mins':[MINUTE,0.2], 'minute':[MINUTE,0.2], 
	'minutes':[MINUTE,0.2], 'h':[HOUR,0.2], 'hours':[HOUR,0.2], 'hrs':[HOUR,0.2], 'hour':[HOUR,0.2], 
	'day':[DAY,0.2], 'days':[DAY,0.2], 'd':[DAY,0.2], 'week':[WEEK,0.2],
	'weeks':[WEEK,0.2], 'wks':[WEEK,0.2], 'wk':[WEEK,0.2], 
	'month':[MONTH,0.2], 'months':[MONTH,0.2], 'mnths':[MONTH,0.2], 'year':[YEAR,0.2], 
	'years':[YEAR,0.2], 'y':[YEAR,0.2], 'century':[YEAR,0.2], 'centuries':[CENTURY,0.2], 
	'ms':[MILLISECOND,0.2],'sec':[SECOND,0.2], 'secs':[SECOND,0.2], 'seconds': [SECOND,0.2],
	'seccond':[SECOND,0.2], 'millisecond':[1,0.2], 'milliseconds':[1,0.2], 'microsecond':[MICROSECOND,0.2],
	'microseconds':[MICROSECOND,0.2], 'picosecond':[PICOSECOND,0.2], 'picoseconds':[PICOSECOND,0.2]
	],
	(UnitGroup.DISTANCE): [
	'm':[METER,0.2], 'meter':[METER,0.222], 'foot':[FOOT,0.2], 'feet':[FOOT,0.2], 'ft':[FOOT,0.2], 
	'yard':[YARD,0.2], 'yards':[YARD,0.2], 'miles':[MILE,0.2], 'km':[KILOMETER,0.2], 'kilometers':[KILOMETER,0.2],
	'kilometers':[KILOMETER,0.2], 'kilometer':[KILOMETER,0.2],'kilometre':[KILOMETER,0.2], 'kilometres':[KILOMETER,0.2]
	],
	(UnitGroup.WEIGHT): [
	'g':[GRAM,0.2], 'grams':[GRAM,0.2], 'pound':[POUND,0.2], 'pounds':[POUND,0.2], 'lbs':[POUND,0.2], 
	'kg':[KILOGRAM,0.2], 'kilograms':[KILOGRAM,0.2], 'kilogram':[KILOGRAM,0.2],
	]
	];

	/**
	 *	Compute which group a given unit falls under
	 *
	 */
	public static Map groupMapForUnit(String unit, def group = null) {
		def groupMap = null
		def unitKey
		if (unit == '') return groupMap
		if (group) {
			log.debug("UnitGroupMap.groupMapForUnit(): looking up groupMap for unit "
				+ unit + " in group " + group )
			unitKey = unitListContainsUnit(unit, groups[group])
			groupMap = groupMapFromUnitList(unitKey, groups[group], group)
		} else {
			log.debug("UnitGroupMap.groupMapForUnit(): looking up groupMap for unit " 
				+ unit + " in other groups")
			groups.each { groupAsKey, unitList ->
				unitKey = unitListContainsUnit(unit, unitList) 
				groupMap = groupMapFromUnitList(unitKey, unitList, groupAsKey, groupMap)
				log.debug("UnitGroupMap.groupMapForUnit(): groupMap " + groupMap?.dump())
			}
			log.debug("UnitGroupMap.groupMapForUnit(): unitKey " + unitKey)
		}
		return groupMap
	}

	public static String unitListContainsUnit(String unit, def unitList) {
		String unitKey = null 
		def singularUnit = unit.minus(Pattern.compile("s\$"))
		log.debug("UnitGroupMap.unitListContainsUnit(): checking for unit " + unit +
		" in list " + unitList.dump())
		if (unitList.containsKey(unit)) {
			unitKey = unit
			log.debug("UnitGroupMap.unitListContainsUnit(): unit found " + unitKey)
		} else if (singularUnit && unitList.containsKey(singularUnit)) {
			unitKey = singularUnit
			log.debug("UnitGroupMap.unitListContainsUnit(): singular unit found " + unitKey)
		}
		log.debug("UnitGroupMap.unitListContainsUnit(): returning unitKey " + unitKey)
		return unitKey	
	}

	public static Object groupContainsUnit(String unit, def group) {
		return unitListContainsUnit(unit, groups[group])
	}

	public static Map groupMapFromUnitList(String unitKey, def unitList, def group, def prevGroupMap= null) {
		def groupMap = null
		if (unitKey == null)
			return prevGroupMap
		def ratioAndAffinity = unitList[unitKey]
		log.debug("UnitGroupMap.groupMapFromUnitList(): meta " + ratioAndAffinity?.dump())
		if (prevGroupMap == null || prevGroupMap?.affinity < ratioAndAffinity[AFFINITY]) {
			log.debug("UnitGroupMap.groupMapFromUnitList(): creating group map for unit " + unitKey)
			groupMap = [group:group, meta: ratioAndAffinity, unit: unitKey]
		} else {
			return prevGroupMap
		}
		log.debug("UnitGroupMap.groupMapFromUnitList(): created group map for unit " + unitKey + " " + groupMap.dump())
		return groupMap
	}

	/**
	 * This method returns the meta information for a unit
	 * with a relative ration 1 for a particular unit group
	 * 
	 */
	public static Object getStandardUnitMeta(group) {
		def unitList = groups[group]
		def metaInfo = null
		unitList.each { unit, meta ->
			if (meta[RATIO] == 1) {
				log.debug("UnitGroupMap.getStandardUnitMeta(): Unit with ratio 1 " + unit)
				metaInfo = [unit: unit, meta: meta]	
			}
		}
		return metaInfo
	}

	public static Object convertToMostUsedUnit(Long tagId, Long userId, BigDecimal amount, String unit) {
		def mostUsedTagUnitStats = TagUnitStats.mostUsedTagUnitStats(tagId, userId)	
		if (unit.equals(mostUsedTagUnitStats?.unit))
			return null
		log.debug("UnitGroupMap.convertToMostUsedUnit(): converting unit " + unit)
		def mostUsedGroupMap = UnitGroupMap.groupMapForUnit(mostUsedTagUnitStats?.unit)
		log.debug("UnitGroupMap.convertToMostUsedUnit(): most used groupMap " + mostUsedGroupMap?.dump())
		def unitGroupMap = groupMapForUnit(unit, mostUsedGroupMap?.group)
		log.debug("UnitGroupMap.convertToMostUsedUnit(): group map for the unit to be converted" + unitGroupMap?.dump())
		if (!mostUsedGroupMap || !unitGroupMap || mostUsedGroupMap.group != unitGroupMap.group) {
			//No need to normalize as there is no tag + unit usage history
			//Or the unit used has not been grouped and hence can't be
			//converted. Or the two units belong to two different groups
			// and can't be converted
			log.debug("UnitGroupMap.convertToMostUsedUnit(): no normalization needed for unit " + unit)
			return null
		}
		def standardUnitMeta = getStandardUnitMeta(mostUsedGroupMap.group)
		log.debug("UnitGroupMap.convertToMostUsedUnit(): standard unit meta info " + standardUnitMeta?.dump()) 
		def valueWithRatioOne = amount * standardUnitMeta.meta[RATIO]
		log.debug("UnitGroupMap.convertToMostUsedUnit(): standard value " + valueWithRatioOne +
			' '+ standardUnitMeta.unit)
		def convertedValue = valueWithRatioOne/mostUsedGroupMap.meta[RATIO]
		log.debug("UnitGroupMap.convertToMostUsedUnit(): converted value " + convertedValue +
			' '+ mostUsedGroupMap.unit)
		return [unit:mostUsedGroupMap.unit, amount: convertedValue,
			group: mostUsedGroupMap.group]

	}

}
