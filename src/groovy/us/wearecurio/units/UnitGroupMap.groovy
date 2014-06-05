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
	public static final double PICOSECOND = MILLISECOND * 0.000000001d
	public static final double MICROSECOND = MILLISECOND * 0.001d
	public static final long SECOND = MILLISECOND * 1000
	public static final long MINUTE = SECOND * 60
	public static final long HOUR = MINUTE * 60
	public static final long DAY = HOUR * 24
	public static final long WEEK = DAY * 7
	public static final double MONTH = DAY * 30.4375d
	public static final double YEAR = MONTH * 12
	public static final double CENTURY = YEAR * 100

	//Distance
	public static final double METER = 1
	public static final double CENTIMETER = 0.01d
	public static final double MILLIMETER = 0.001d
	public static final double FOOT = METER * 0.30480d
	public static final double YARD = METER * FOOT * 3.0d
	public static final double MILE = METER * FOOT * 5280.0d
	public static final double KILOMETER = METER * 1000

	//Weight
	public static final double GRAM = 1
	public static final double MILLIGRAM = 0.001
	public static final double KILOGRAM = GRAM * 1000
	public static final double POUND = GRAM * 453.6
	public static final double OUNCE = POUND / 16.0d
	
	//Area
	public static final double SQMETER = 1
	public static final double HECTARE = 10000 * SQMETER
	public static final double ACRE = HECTARE / 2.471d
	public static final double SQKILOMETER = KILOMETER * KILOMETER
	public static final double SQMILE = MILE * MILE
	public static final double SQFOOT = FOOT * FOOT
	
	//Volume
	public static final double CC = 1
	public static final double LITER = 1000.0d
	public static final double GALLON = OUNCE * 16.0d
	public static final double QUART = OUNCE * 4.0d
	
	//Force
	public static final double NEWTON = POUND / 4.44822162825d
	public static final double DYNE = NEWTON / 100000.0d
	public static final double KIP = POUND * 1000.0d
	
	//Power
	public static final double WATT = 1
	public static final double KILOWATT = 1000.0d * WATT
	public static final double MILLIWATT = WATT / 1000.0d
	public static final double HORSEPOWER = 745.699872d * WATT
	
	//Energy
	public static final double JOULE = 1
	public static final double ERG = 1.0e-7d
	public static final double FOOTPOUND = 1.3558d
	public static final double BTU = 1055.056d
	public static final double HORSEPOWERHOUR = 2.6845e6d
	public static final double MEGAJOULE = 1000000.0d
	public static final double GIGAJOULE = 1.0e9d
	public static final double TERAJOULE = 1.0e12d
	public static final double PETAJOULE = 1.0e15d
	public static final double KILOWATTHOUR = 3.6e6d
	public static final double THERM = 105.5d * MEGAJOULE
	public static final double CALORIE = 4.184d
	public static final double KILOCALORIE = 1000.0d * CALORIE
	public static final double KILOTON = 4.184e3d * GIGAJOULE
	public static final double MEGATON = 1000.0d * KILOTON
	public static final double EV = 1.60217653e-19d
	
	//Pressure
	public static final double PASCAL = 1
	public static final double BARYE = 0.1d
	public static final double PSI = 6894.75729d
	public static final double BAR = 100000.0d
	public static final double DECIBAR = BAR / 10.0d
	public static final double CENTIBAR = BAR / 100.0d
	public static final double MILLIBAR = BAR / 1000.0d
	public static final double MEGABAR = 1.0e6d * BAR
	public static final double KILOBAR = 1.0e3d * BAR
	public static final double MMHG = 133.3223684211d
	public static final double INCHHG = 3386.389d
	public static final double ATMOSPHERE = 98.0665e3d
	
	//[groupName: 
	//		[ unitName: [relativeRatio, affinityVal], 
	//		[ unitName: [relativeRatio, affinityVal], 
	//]
	static Map groups = [
		(UnitGroup.DURATION): [
			'm':[MINUTE,1], 'min':[MINUTE,5], 'mins':[MINUTE,6], 'minute':[MINUTE,10], 
			'minutes':[MINUTE,10], 'h':[HOUR,1], 'hours':[HOUR,10], 'hrs':[HOUR,8], 'hour':[HOUR,10], 
			'day':[DAY,10], 'days':[DAY,10], 'd':[DAY,1], 'week':[WEEK,10],
			'weeks':[WEEK,10], 'wks':[WEEK,5], 'wk':[WEEK,4], 
			'month':[MONTH,10], 'months':[MONTH,10], 'mnths':[MONTH,7], 'year':[YEAR,10], 
			'years':[YEAR,10], 'y':[YEAR,1], 'century':[YEAR,10], 'centuries':[CENTURY,10], 
			'ms':[MILLISECOND,6],'sec':[SECOND,5], 'secs':[SECOND,7], 'seconds': [SECOND,10],
			'second':[SECOND,10], 'millisecond':[MILLISECOND,10], 'ms':[MILLISECOND,3], 'milliseconds':[MILLISECOND,10], 'microsecond':[MICROSECOND,10],
			'microseconds':[MICROSECOND,10], 'picosecond':[PICOSECOND,10], 'picoseconds':[PICOSECOND,10],
		],
		(UnitGroup.DISTANCE): [
			'cm':[CENTIMETER,5], 'mm':[MILLIMETER,6],
			'centimeter':[CENTIMETER,10], 'centimetre':[CENTIMETER,10],
			'millimeter':[MILLIMETER,10], 'millimetre':[MILLIMETER,10],
			'm':[METER,2], 'meter':[METER,10], 'metre':[METER,10], 'foot':[FOOT,10], 'feet':[FOOT,10], 'ft':[FOOT,5], 
			'yard':[YARD,10], 'yards':[YARD,10], 'miles':[MILE,10], 'km':[KILOMETER,8], 'kilometers':[KILOMETER,10],
			'kilometers':[KILOMETER,10], 'kilometer':[KILOMETER,10],'kilometre':[KILOMETER,10], 'kilometres':[KILOMETER,10],
		],
		(UnitGroup.WEIGHT): [
			'g':[GRAM,2], 'grams':[GRAM,10], 'pound':[POUND,10], 'lb':[POUND,5], 'pounds':[POUND,10], 'lbs':[POUND,10], 
			'kg':[KILOGRAM,8], 'kilograms':[KILOGRAM,10], 'kilogram':[KILOGRAM,10], 'ounce':[OUNCE,10], 'oz':[OUNCE,4],
			'ounces':[OUNCE,10],
			],
		(UnitGroup.AREA): [
			'sq m':[SQMETER,3], 'sqm':[SQMETER,1], 'square meter':[SQMETER,10], 'square metre':[SQMETER,10], 'sq meter':[SQMETER,7],
			'sq metre':[SQMETER,7],
			'hectare':[HECTARE,10], 'ha':[HECTARE,3], 'h':[HECTARE,1], 'hectares':[HECTARE,10],
			'acre':[ACRE,10], 'acres':[ACRE,10], 'ac':[ACRE,6],
		],
		(UnitGroup.VOLUME): [
			'cc':[CC,7], 'cubic centimeter':[CC,10], 'cubic centimeters':[CC,10], 'cubic cm':[CC,10], 'cubic centimetre':[CC,10], 'cubic centimetres':[CC,10],
			'l':[LITER,2], 'liter':[LITER,10], 'liters':[LITER,10], 'litre':[LITER,10], 'litres':[LITER,10],
			'gal':[GALLON,7], 'gl':[GALLON,1], 'g':[GALLON,1], 'gallon':[GALLON,10], 'gallons':[GALLON,10],
			'qt':[QUART,8], 'quart':[QUART,10], 'quarts':[QUART,10],
		],
		(UnitGroup.FORCE): [
			'n':[NEWTON,3], 'newton':[NEWTON,10], 'newtons':[NEWTON,10],
			'dyn':[DYNE,7], 'dyne':[DYNE,10],
			'kip':[KIP,5],
		],
		(UnitGroup.POWER): [
			'w':[WATT,2], 'watt':[WATT,10], 'watts':[WATT,10],
			'kw':[KILOWATT,5], 'kilowatt':[KILOWATT,10], 'kilowatts':[KILOWATT,10],
			'mw':[MILLIWATT,3], 'milliwatt':[MILLIWATT,10], 'milliwatts':[MILLIWATT,10],
			'hp':[HORSEPOWER,7], 'horsepower':[HORSEPOWER,10],
		],
		(UnitGroup.ENERGY): [
			'j':[JOULE,3], 'joule':[JOULE,10], 'joules':[JOULE,10],
			'erg':[ERG,10],
			'ft-lb':[FOOTPOUND,8], 'foot-pound':[FOOTPOUND,10], 'foot-pounds':[FOOTPOUND,10], 'foot pound':[FOOTPOUND,10], 'foot pounds':[FOOTPOUND,10], 'footpound':[FOOTPOUND,10], 'footpounds':[FOOTPOUND,10],
			'btu':[BTU,8],
			'hph':[HORSEPOWERHOUR,5], 'horsepower-hour':[HORSEPOWERHOUR,10], 'horsepower-hours':[HORSEPOWERHOUR,10], 'horsepower hour':[HORSEPOWERHOUR,10], 'horsepower hours':[HORSEPOWERHOUR,10], 'horsepowerhour':[HORSEPOWERHOUR,10], 'horsepowerhours':[HORSEPOWERHOUR,10],
			'mj':[MEGAJOULE,3], 'megajoule':[MEGAJOULE,10], 'megajoules':[MEGAJOULE,10],
			'gj':[GIGAJOULE,3], 'gigajoule':[GIGAJOULE,10], 'gigajoules':[GIGAJOULE,10],
			'tj':[TERAJOULE,3], 'terajoule':[TERAJOULE,10], 'terajoules':[TERAJOULE,10],
			'pj':[PETAJOULE,3], 'petajoule':[PETAJOULE,10], 'petajoules':[PETAJOULE,10],
			'kwh':[KILOWATTHOUR,7], 'kilowatt-hour':[KILOWATTHOUR,10], 'kilowatt-hours':[KILOWATTHOUR,10], 'kilowatt hour':[KILOWATTHOUR,10], 'kilowatt hours':[KILOWATTHOUR,10], 'kilowatthour':[KILOWATTHOUR,10], 'kilowatthours':[KILOWATTHOUR,10], 
			'therm':[THERM,10], 'therms':[THERM,10],
			'cal':[CALORIE,3], 'cals':[CALORIE,2], 'calorie':[CALORIE,10], 'calories':[CALORIE,10],
			'kcal':[KILOCALORIE,7], 'kcals':[KILOCALORIE,7], 'kcalorie':[KILOCALORIE,10], 'kcalories':[KILOCALORIE,10],
			'kiloton':[KILOTON,10], 'kilotonne':[KILOTON,10], 'kilotons':[KILOTON,10], 'kilotonnes':[KILOTON,10],
			'megaton':[MEGATON,10], 'megatons':[MEGATON,10], 'megatonne':[MEGATON,10], 'megatonnes':[MEGATON,10],
			'ev':[EV,5], 'evs':[EV,2], 'electron-volt':[EV,10], 'electron-volts':[EV,10], 'electron volt':[EV,10], 'electron volts':[EV,10],
		],
		(UnitGroup.PRESSURE): [
			'pa':[PASCAL,2], 'pascal':[PASCAL,10], 'pascals':[PASCAL,10],
			'ba':[BARYE,3], 'barye':[BARYE,10], 'baryes':[BARYE,10],
			'psi':[PSI,8],
			'bar':[BAR,9], 'bars':[BAR,10],
			'dbar':[DECIBAR,7], 'dbars':[DECIBAR,7], 'decibar':[DECIBAR,10], 'decibars':[DECIBAR,10],
			'cbar':[CENTIBAR,5], 'centibar':[CENTIBAR,10], 'cbars':[CENTIBAR,10], 'centibars':[CENTIBAR,10],
			'mbar':[MILLIBAR,7], 'mbars':[MILLIBAR,7], 'millibar':[MILLIBAR,10], 'millibars':[MILLIBAR,10],
			'Mbar':[MEGABAR,4], 'Mbars':[MEGABAR,5], 'megabar':[MEGABAR,10], 'megabars':[MEGABAR,10],
			'kbar':[KILOBAR,6], 'kbars':[KILOBAR,7], 'kilobar':[KILOBAR,10], 'kilobars':[KILOBAR,10],
			'mmHg':[MMHG,7],
			'inHg':[INCHHG,6],
			'atm':[ATMOSPHERE,5], 'atmosphere':[ATMOSPHERE,10], 'atmospheres':[ATMOSPHERE,10],
		]	
	]

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
		if (unit == null || unit == '')
			return null
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
		def convertedValue = amount/mostUsedGroupMap.meta[RATIO]
		log.debug("UnitGroupMap.convertToMostUsedUnit(): converted value " + convertedValue +
			' '+ mostUsedGroupMap.unit)
		return [unit:mostUsedGroupMap.unit, amount: convertedValue,
			group: mostUsedGroupMap.group]

	}

}
