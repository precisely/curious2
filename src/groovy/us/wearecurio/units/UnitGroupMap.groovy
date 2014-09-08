package us.wearecurio.units

import org.apache.commons.logging.LogFactory
import us.wearecurio.model.*
import us.wearecurio.parse.PatternScanner

import java.util.HashSet;
import java.util.regex.Matcher
import java.util.regex.Pattern

class UnitGroupMap {

	private static def log = LogFactory.getLog(this)

	public static final int RATIO = 0
	public static final int AFFINITY = 1
	public static final int CANONICALUNIT = 2
	public static final int SUFFIX = 3
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
	
	//Density
	public static final double KGPERM3 = 1
	public static final double MGPERDL = 0.01d
	public static final double KGPERL = 1000.0d
	public static final double GPERML = 1000.0d
	// TODO: Finish adding density units
	
	//Steps
	public static final double STEPSUNIT = 1
	
	//Percentage
	public static final double PERCENTAGEUNIT = 1
	
	//Frequency
	public static final double PERSECOND = 1.0d
	public static final double PERMINUTE = 60.0d
	public static final double PERHOUR = 60.0d * PERMINUTE
	public static final double PERDAY = 24.0d * PERHOUR
	public static final double PERWEEK = 7.0d * PERDAY
	public static final double PERMONTH = 30.4375d * PERDAY
	public static final double PERYEAR = 365.25d * PERDAY
	
	//Scale
	public static final double SCALEUNITY = 1
	public static final double SCALETEN = 10
	public static final double SCALEFIVE = 5
	public static final double SCALEFOUR = 4
	public static final double SCALETHREE = 4
	
	// other suffixes
	static Map suffixPriority = ['systolic' : 10, 'diastolic' : 20]
	
	static final Set<String> suffixes = new HashSet<String>()
	
	static {
		suffixPriority.each {
			suffixes.add(it.key)
		}
	}
	
	enum UnitGroup {
		
		DURATION(1, "duration", 100, [
			'm':[MINUTE,1,'minute'], 'min':[MINUTE,5,'minute'], 'mins':[MINUTE,6,'minute'], 'minute':[MINUTE,10,'minute'],
			'minutes':[MINUTE,10,'minute'], 'h':[HOUR,1,'hour'], 'hours':[HOUR,10,'hour'], 'hrs':[HOUR,8,'hour'], 'hour':[HOUR,10,'hour'],
			'day':[DAY,10,'day'], 'days':[DAY,10,'day'], 'd':[DAY,1,'day'], 'week':[WEEK,10,'week'],
			'weeks':[WEEK,10,'week'], 'wks':[WEEK,5,'week'], 'wk':[WEEK,4,'week'],
			'month':[MONTH,10,'month'], 'months':[MONTH,10,'month'], 'mnths':[MONTH,7,'month'], 'year':[YEAR,10,'year'],
			'years':[YEAR,10,'year'], 'y':[YEAR,1,'year'], 'century':[YEAR,10,'century'], 'centuries':[CENTURY,10,'century'],
			'ms':[MILLISECOND,6,'millisecond'],'sec':[SECOND,5,'second'], 'secs':[SECOND,7,'second'], 'seconds': [SECOND,10,'second'],
			'second':[SECOND,10,'second'], 'millisecond':[MILLISECOND,10,'millisecond'], 'ms':[MILLISECOND,3,'millisecond'], 'milliseconds':[MILLISECOND,10,'millisecond'], 'microsecond':[MICROSECOND,10,'microsecond'],
			'microseconds':[MICROSECOND,10,'microsecond'], 'picosecond':[PICOSECOND,10,'picosecond'], 'picoseconds':[PICOSECOND,10,'picosecond'],
		]),
		DISTANCE(2, "distance", 200, [
			'cm':[CENTIMETER,5,'centimeter'], 'mm':[MILLIMETER,6,'millimeter'],
			'centimeter':[CENTIMETER,10,'centimeter'], 'centimetre':[CENTIMETER,10,'centimeter'],
			'millimeter':[MILLIMETER,10,'millimeter'], 'millimetre':[MILLIMETER,10,'millimeter'],
			'm':[METER,2,'meter'], 'meters':[METER,11,'meter'], 'meter':[METER,10,'meter'], 'metre':[METER,10,'meter'], 'metres':[METER,10,'meter'], 'foot':[FOOT,10,'foot'], 'feet':[FOOT,10,'foot'], 'ft':[FOOT,5,'foot'],
			'yard':[YARD,10,'yard'], 'yards':[YARD,10,'yard'], 'mi':[MILE,7,'mile'], 'miles':[MILE,10,'mile'], 'km':[KILOMETER,8,'kilometer'], 'kilometers':[KILOMETER,10,'kilometer'],
			'kilometers':[KILOMETER,10,'kilometer'], 'kilometer':[KILOMETER,1,'kilometer'],'kilometre':[KILOMETER,10,'kilometer'], 'kilometres':[KILOMETER,10,'kilometer'],
		]),
		WEIGHT(3, "weight", 300, [
			'g':[GRAM,2,'gram'], 'grams':[GRAM,10,'gram'], 'pound':[POUND,10,'pound'], 'lb':[POUND,5,'pound'], 'pounds':[POUND,10,'pound'], 'lbs':[POUND,10,'pound'],
			'kg':[KILOGRAM,8,'kilogram'], 'kgs':[KILOGRAM,4,'kilogram'], 'kilograms':[KILOGRAM,10,'kilogram'], 'kilogram':[KILOGRAM,10,'kilogram'], 'ounce':[OUNCE,10,'ounce'], 'oz':[OUNCE,4,'ounce'],
			'ounces':[OUNCE,10,'ounce'],
		]),
		AREA(4, "area", 400, [
			'sq m':[SQMETER,3,'square meter'], 'sqm':[SQMETER,1,'square meter'], 'square meter':[SQMETER,10,'square meter'], 'square metre':[SQMETER,10,'square meter'], 'sq meter':[SQMETER,7,'square meter'],
			'sq metre':[SQMETER,7,'square meter'],
			'sq ft':[SQFOOT,7,'square foot'], 'sqft':[SQFOOT,7,'square foot'], 'square foot':[SQFOOT,7,'square foot'], 'square feet':[SQFOOT,7,'square foot'], 'sq feet':[SQFOOT,7,'square foot'], 'sq foot':[SQFOOT,7,'square foot'],
			'hectare':[HECTARE,10,'hectare'], 'ha':[HECTARE,3,'hectare'], 'h':[HECTARE,1,'hectare'], 'hectares':[HECTARE,10,'hectare'],
			'acre':[ACRE,10,'acre'], 'acres':[ACRE,10,'acre'], 'ac':[ACRE,6,'acre'],
		]),
		VOLUME(5, "volume", 500, [
			'cc':[CC,7,'cubic centimeter'], 'cubic centimeter':[CC,10,'cubic centimeter'], 'cubic centimeters':[CC,10,'cubic centimeter'], 'cubic cm':[CC,10,'cubic centimeter'], 'cubic centimetre':[CC,10,'cubic centimeter'], 'cubic centimetres':[CC,10,'cubic centimeter'],
			'l':[LITER,2,'liter'], 'liter':[LITER,10,'liter'], 'liters':[LITER,10,'liter'], 'litre':[LITER,10,'liter'], 'litres':[LITER,10,'liter'],
			'gal':[GALLON,7,'gallon'], 'gl':[GALLON,1,'gallon'], 'g':[GALLON,1,'gallon'], 'gallon':[GALLON,10,'gallon'], 'gallons':[GALLON,10,'gallon'],
			'qt':[QUART,8,'quart'], 'quart':[QUART,10,'quart'], 'quarts':[QUART,10,'quart'],
		]),
		FORCE(6, "force", 600, [
			'n':[NEWTON,3,'newton'], 'newton':[NEWTON,10,'newton'], 'newtons':[NEWTON,10,'newton'],
			'dyn':[DYNE,7,'dyne'], 'dyne':[DYNE,10,'dyne'],
			'kip':[KIP,5,'kip'],
		]),
		POWER(7, "power", 700, [
			'w':[WATT,2,'watt'], 'watt':[WATT,10,'watt'], 'watts':[WATT,10,'watt'],
			'kw':[KILOWATT,5,'kilowatt'], 'kilowatt':[KILOWATT,10,'kilowatt'], 'kilowatts':[KILOWATT,10,'kilowatt'],
			'mw':[MILLIWATT,3,'milliwatt'], 'milliwatt':[MILLIWATT,10,'milliwatt'], 'milliwatts':[MILLIWATT,10,'milliwatt'],
			'hp':[HORSEPOWER,7,'horsepower'], 'horsepower':[HORSEPOWER,10,'horsepower'],
		]),
		ENERGY(8, "energy", 350, [
			'j':[JOULE,3,'joule'], 'joule':[JOULE,10,'joule'], 'joules':[JOULE,10,'joule'],
			'erg':[ERG,10,'erg'],
			'ft-lb':[FOOTPOUND,8,'foot-pound'], 'foot-pound':[FOOTPOUND,10,'foot-pound'], 'foot-pounds':[FOOTPOUND,10,'foot-pound'], 'foot pound':[FOOTPOUND,10,'foot-pound'], 'foot pounds':[FOOTPOUND,10,'foot-pound'], 'footpound':[FOOTPOUND,10,'foot-pound'], 'footpounds':[FOOTPOUND,10],
			'btu':[BTU,8,'btu'],
			'hph':[HORSEPOWERHOUR,5,'horsepower-hour'], 'horsepower-hour':[HORSEPOWERHOUR,10,'horsepower-hour'], 'horsepower-hours':[HORSEPOWERHOUR,10,'horsepower-hour'], 'horsepower hour':[HORSEPOWERHOUR,10,'horsepower-hour'], 'horsepower hours':[HORSEPOWERHOUR,10,'horsepower-hour'], 'horsepowerhour':[HORSEPOWERHOUR,10,'horsepower-hour'], 'horsepowerhours':[HORSEPOWERHOUR,10,'horsepower-hour'],
			'mj':[MEGAJOULE,3,'megajoule'], 'megajoule':[MEGAJOULE,10,'megajoule'], 'megajoules':[MEGAJOULE,10,'megajoule'],
			'gj':[GIGAJOULE,3,'gigajoule'], 'gigajoule':[GIGAJOULE,10,'gigajoule'], 'gigajoules':[GIGAJOULE,10,'gigajoule'],
			'tj':[TERAJOULE,3,'terajoule'], 'terajoule':[TERAJOULE,10,'terajoule'], 'terajoules':[TERAJOULE,10,'terajoule'],
			'pj':[PETAJOULE,3,'petajoule'], 'petajoule':[PETAJOULE,10,'petajoule'], 'petajoules':[PETAJOULE,10,'petajoule'],
			'kwh':[KILOWATTHOUR,7,'kilowatt-hour'], 'kilowatt-hour':[KILOWATTHOUR,10,'kilowatt-hour'], 'kilowatt-hours':[KILOWATTHOUR,10,'kilowatt-hour'], 'kilowatt hour':[KILOWATTHOUR,10,'kilowatt-hour'], 'kilowatt hours':[KILOWATTHOUR,10,'kilowatt-hour'], 'kilowatthour':[KILOWATTHOUR,10,'kilowatt-hour'], 'kilowatthours':[KILOWATTHOUR,10,'kilowatt-hour'],
			'therm':[THERM,10,'therm'], 'therms':[THERM,10,'therm'],
			'cal':[CALORIE,3,'calorie','calories'], 'cals':[CALORIE,2,'calorie','calories'], 'calorie':[CALORIE,10,'calorie','calories'], 'calories':[CALORIE,10,'calorie','calories'],
			'kcal':[KILOCALORIE,7,'kilocalorie','calories'], 'kcals':[KILOCALORIE,7,'kilocalorie','calories'], 'kcalorie':[KILOCALORIE,10,'kilocalorie','calories'], 'kcalories':[KILOCALORIE,10,'kilocalorie','calories'],
			'kiloton':[KILOTON,10,'kiloton'], 'kilotonne':[KILOTON,10,'kiloton'], 'kilotons':[KILOTON,10,'kiloton'], 'kilotonnes':[KILOTON,10,'kiloton'],
			'megaton':[MEGATON,10,'megatons'], 'megatons':[MEGATON,10,'megatons'], 'megatonne':[MEGATON,10,'megatons'], 'megatonnes':[MEGATON,10,'megatons'],
			'ev':[EV,5,'electron-volt'], 'evs':[EV,2,'electron-volt'], 'electron-volt':[EV,10,'electron-volt'], 'electron-volts':[EV,10,'electron-volt'], 'electron volt':[EV,10,'electron-volt'], 'electron volts':[EV,10,'electron-volt'],
		]),
		// TORQUE(9, "torque"),
		// LUMINOSITY(10, "luminosity"),
		PRESSURE(11, "pressure", 900, [
			'pa':[PASCAL,2,'pascal'], 'pascal':[PASCAL,10,'pascal'], 'pascals':[PASCAL,10,'pascal'],
			'ba':[BARYE,3,'barye'], 'barye':[BARYE,10,'barye'], 'baryes':[BARYE,10,'barye'],
			'psi':[PSI,8,'psi'],
			'bar':[BAR,9,'bar'], 'bars':[BAR,10,'bar'],
			'dbar':[DECIBAR,7,'decibar'], 'dbars':[DECIBAR,7,'decibar'], 'decibar':[DECIBAR,10,'decibar'], 'decibars':[DECIBAR,10,'decibar'],
			'cbar':[CENTIBAR,5,'centibar'], 'centibar':[CENTIBAR,10,'centibar'], 'cbars':[CENTIBAR,10,'centibar'], 'centibars':[CENTIBAR,10,'centibar'],
			'mbar':[MILLIBAR,7,'millibar'], 'mbars':[MILLIBAR,7,'millibar'], 'millibar':[MILLIBAR,10,'millibar'], 'millibars':[MILLIBAR,10,'millibar'],
			'Mbar':[MEGABAR,4,'megabar'], 'Mbars':[MEGABAR,5,'megabar'], 'megabar':[MEGABAR,10,'megabar'], 'megabars':[MEGABAR,10,'megabar'],
			'kbar':[KILOBAR,6,'kilobar'], 'kbars':[KILOBAR,7,'kilobar'], 'kilobar':[KILOBAR,10,'kilobar'], 'kilobars':[KILOBAR,10,'kilobar'],
			'mmHg':[MMHG,7,'mmHg'],
			'inHg':[INCHHG,6,'inHg'],
			'atm':[ATMOSPHERE,5,'atmosphere'], 'atmosphere':[ATMOSPHERE,10,'atmosphere'], 'atmospheres':[ATMOSPHERE,1,'atmosphere'],
		]),
		DENSITY(12, "density", 450, [
			'kg/m3':[KGPERM3,10,'kg/m3'], 'mg/dL':[MGPERDL,10,'mg/dL'], 'kg/L':[KGPERL,10,'kg/L'], 'g/mL':[GPERML,10,'g/mL'],
			'mg/dl':[MGPERDL,10,'mg/dL'], 'kg/l':[KGPERL,10,'kg/L'], 'g/ml':[GPERML,10,'g/mL'],
		]),
		STEPS(13, "steps", 5, [
			'steps':[STEPSUNIT,10,'steps'],
		]),
		PERCENTAGE(13, "percentage", 1000, [
			'%':[PERCENTAGEUNIT,10,'percent'], 'percent':[PERCENTAGEUNIT,10,'percent'], 'percentage':[PERCENTAGEUNIT,10,'percent'],
		]),
		FREQUENCY(13, "frequency", 90, [
			'per sec':[PERSECOND,10,'per second'], 'per second':[PERSECOND,10,'per second'], 'bps':[PERSECOND,8,'per second'],
			'/ s':[PERSECOND,5,'per second'], '/ sec':[PERSECOND,9,'per second'],
			'/s':[PERSECOND,4,'per second'], '/sec':[PERSECOND,10,'per second'],
			'per min':[PERMINUTE,10,'per minute'], 'per minute':[PERMINUTE,10,'per minute'], 'bpm':[PERMINUTE,8,'per minute'],
			'/ m':[PERMINUTE,9,'per minute'], '/ min':[PERMINUTE,9,'per minute'],
			'/m':[PERMINUTE,4,'per minute'], '/min':[PERMINUTE,10,'per minute'],
			'per hr':[PERHOUR,10,'per hour'], 'per hour':[PERHOUR,10,'per hour'],
			'/ h':[PERHOUR,8,'per hour'], '/ hr':[PERHOUR,5,'per hour'], '/ hour':[PERHOUR,9,'per hour'],
			'/h':[PERHOUR,8,'per hour'], '/hr':[PERHOUR,5,'per hour'], '/hour':[PERHOUR,9,'per hour'],
			'per day':[PERDAY,10,'per day'], 'per d':[PERDAY,10,'per day'],
			'/ d':[PERDAY,5,'per day'], '/ day':[PERDAY,9,'per day'],
			'/d':[PERDAY,5,'per day'], '/day':[PERDAY,9,'per day'],
			'per w':[PERWEEK,10,'per week'], 'per week':[PERWEEK,10,'per week'], 'per wk':[PERWEEK,10,'per week'],
			'/ week':[PERWEEK,8,'per week'], '/ w':[PERWEEK,5,'per week'], '/ wk':[PERWEEK,9,'per week'],
			'/week':[PERWEEK,8,'per week'], '/w':[PERWEEK,5,'per week'], '/wk':[PERWEEK,9,'per week'],
			'per mon':[PERMONTH,10,'per month'], 'per month':[PERMONTH,10,'per month'],
			'/ month':[PERMONTH,5,'per month'], '/ mnth':[PERMONTH,9,'per month'], '/ mon':[PERMONTH,4,'per month'],
			'/month':[PERMONTH,5,'per month'], '/mnth':[PERMONTH,9,'per month'], '/mon':[PERMONTH,4,'per month'],
			'per y':[PERYEAR,10,'per year'], 'per year':[PERYEAR,10,'per year'], 'per yr':[PERYEAR,8,'per year'],
			'/ y':[PERYEAR,5,'per year'], '/ year':[PERYEAR,9,'per year'], '/ yr':[PERYEAR,5,'per year'],
			'/y':[PERYEAR,5,'per year'], '/year':[PERYEAR,9,'per year'], '/yr':[PERYEAR,5,'per year'],
		]),
		SCALE(13, "scale", 3, [
			'scale':[SCALETEN,10,'to ten'], '':[SCALETEN,1,'to ten'], 'to ten':[SCALETEN,10,'to ten'],
			'unity':[SCALEUNITY,10,'to ten'], 'to one':[SCALEUNITY,10,'to one'],
			'to three':[SCALETHREE,10,'to three'], 'to four':[SCALEFOUR,10,'to four'], 'to five':[SCALEFIVE,10,'to five'],
		])
		public static final double PERSECOND = 1.0d
		public static final double PERMINUTE = 60.0d
		public static final double PERHOUR = 60.0d * PERMINUTE
		public static final double PERDAY = 24.0d * PERHOUR
		public static final double PERWEEK = 7.0d * PERDAY
		public static final double PERMONTH = 30.4375d * PERDAY
		public static final double PERYEAR = 365.25d * PERDAY
	
		final int id
		final String name
		final int priority // priority for listing units in a sequence
		Map<String, UnitRatio> map = [:]
		Set<String> units = new HashSet<String>()
		
		UnitGroup(int id, String name, int priority, def initMap) {
			this.id = id
			this.name = name
			this.priority = priority
			
			UnitGroupMap.suffixPriority[name] = priority
			
			UnitGroupMap.suffixes.add(name)
			
			for (e in initMap) {
				String unit = e.key
				def val = e.value
				String suffix = name
				
				if (val.size() > SUFFIX)
					suffix = val[SUFFIX]
				
				map.put(unit, new UnitRatio(
					this, unit, val[RATIO], val[AFFINITY], val[CANONICALUNIT], suffix
				))
				units.add(unit)
			}
		}
		
		private static final Map<Integer, UnitGroup> unitGroups = new HashMap<Integer, UnitGroup>()
		
		static {
			UnitGroup.each { unitGroup ->
				unitGroups.put(unitGroup.id, unitGroup)
			}
		}
		
		static UnitGroup get(int id) {
			if (id == null) return null
			unitGroups.get(id)
		}

		public UnitRatio lookupUnitRatio(String unit) {
			return map[unit]
		}
		
		public UnitRatio lookupBestUnitRatio(String unit, UnitRatio prevUnitRatio) {
			UnitRatio unitRatio = map[unit]

			if (unitRatio == null) return prevUnitRatio
			
			return unitRatio.bestRatio(prevUnitRatio)
		}
		
		public Set<String> fetchUnits() {
			return map.keySet()
		}
	}
	
	public static class UnitRatio {
		UnitGroup unitGroup
		String unit 
		double ratio
		def affinity
		String canonicalUnit
		String suffix
		
		public UnitRatio(UnitGroup unitGroup, String unit, double ratio, def affinity, String canonicalUnit, String suffix) {
			this.unitGroup = unitGroup
			this.unit = unit
			this.ratio = ratio
			this.affinity = affinity
			this.canonicalUnit = canonicalUnit
			this.suffix = suffix
		}
		
		public UnitRatio(UnitRatio other, String newSuffix) {
			this.unitGroup = other.unitGroup
			this.unit = other.unit
			this.ratio = other.ratio
			this.affinity = other.affinity
			this.canonicalUnit = other.canonicalUnit
			this.suffix = newSuffix
		}
		
		UnitRatio bestRatio(UnitRatio other) {
			if (other == null)
				return this
			return this.affinity > other.affinity ? this : other
		}
		
		public int getGroupId() {
			return unitGroup.getId()
		}
		
		public UnitGroup getGroup() {
			return unitGroup
		}
		
		public int getGroupPriority() {
			return unitGroup.getPriority()
		}
	}
	
	public static final UnitGroupMap theMap = new UnitGroupMap()
	
	static Pattern twoWordUnitPattern = ~/(?i)^(([^0-9\(\)@\s\.:][^\(\)@\s:]*)(\s([^0-9\(\)@\s\.:][^\(\)@\s:]*)))\s(([^0-9\(\)@\s\.:][^\(\)@\s:]*)(\s([^0-9\(\)@\s\.:][^\(\)@\s:]*))*)/
	static Pattern oneWordUnitPattern = ~/(?i)^(([^0-9\(\)@\s\.:][^\(\)@\s:]*))\s(([^0-9\(\)@\s\.:][^\(\)@\s:]*)(\s([^0-9\(\)@\s\.:][^\(\)@\s:]*))*)/
	
	Map<String, UnitRatio> unitsToRatio = new HashMap<String, UnitRatio>()

	def UnitGroupMap() {
		Set<String> allUnits = new HashSet<String>()
		
		// cache best unit ratio for each unit
		for (UnitGroup group : UnitGroup.values()) {
			allUnits.addAll(group.fetchUnits())
		}
		
		for (String units : allUnits) {
			unitsToRatio.put(units, calculateUnitRatioForUnits(units))
		}
	}
	
	/**
	 *	Compute which group a given unit falls under, looking through UnitGroups for best match
	 *
	 */
	protected UnitRatio calculateUnitRatioForUnits(String units) {
		if (!units) return null
		
		def unitRatio = null
		for (UnitGroup group : UnitGroup.values()) {
			unitRatio = group.lookupBestUnitRatio(units, unitRatio)
		}
		return unitRatio
	}
	
	public double fetchConversionRatio(String fromUnits, String toUnits) {
		UnitRatio fromUnitRatio = unitsToRatio.get(fromUnits)
		UnitRatio toUnitRatio = unitsToRatio.get(toUnits)
		
		double fromRatio = fromUnitRatio == null ? 1.0d : fromUnitRatio.ratio
		double toRatio = toUnitRatio == null ? 1.0d : toUnitRatio.ratio
		
		return fromRatio / toRatio
	}
	
	/**
	 * Returns generic closest-matching UnitRatio for the given unit string
	 */
	public UnitRatio unitRatioForUnits(String units) {
		UnitRatio ratio = unitsToRatio.get(units)
		
		if (ratio != null) return ratio
		
		Matcher matcher = twoWordUnitPattern.matcher(units)
		
		if (matcher.lookingAt()) {
			String units2 = matcher.group(1)
			ratio = unitsToRatio.get(units2)
			if (ratio != null) {
				String suffix = matcher.group(3)
				return new UnitRatio(ratio, suffix)
			}
		}
		
		matcher = oneWordUnitPattern.matcher(units)
		
		if (matcher.lookingAt()) {
			String units1 = matcher.group(1)
			ratio = unitsToRatio.get(units1)
			if (ratio != null) {
				String suffix = matcher.group(5)
				return new UnitRatio(ratio, suffix)
			}
		}
		
		return null
	}

	/**
	 * Look through UnitGroups, or just use most used UnitGroup
	 */
	public UnitRatio mostUsedUnitRatioForTagIds(Long userId, def tagIds) {
		def mostUsed = TagUnitStats.mostUsedTagUnitStatsForTags(userId, tagIds)
		if (mostUsed == null)
			return null
		if (mostUsed.unitGroupId) {
			UnitGroup unitGroup = UnitGroup.get((int)mostUsed.unitGroupId)
			if (unitGroup)
				return unitGroup.lookupUnitRatio(mostUsed.unit)
		}
		// lookup cached unit ratio for the unit
		return unitsToRatio.get(mostUsed.unit)
	}
	
	/**
	 * Return unit suffix corresponding to unit string. For now, just use simple algorithm
	 */
	public String getSuffixForUnits(String tagDescription, String units) {
		UnitRatio unitRatio = unitRatioForUnits(units)
		if (unitRatio) {
			return unitRatio.getSuffix()
		}
		return units
	}
	
	static HashSet<Tag> bloodPressureTags = new HashSet<Tag>()
	
	static {
		bloodPressureTags.add(Tag.look("blood pressure"))
		bloodPressureTags.add(Tag.look("bp"))
		bloodPressureTags.add(Tag.look("blood"))
	}
	
	/**
	 * Return tag with suffix for given units and offset. Hack blood pressure for now.
	 */
	Tag tagWithSuffixForUnits(Tag baseTag, String units, int index) {
		if (bloodPressureTags.contains(baseTag)) {
			if (units) {
				String suffix = getSuffixForUnits(baseTag.getDescription(), units)
				if (suffix) {
					if (suffix.equals("pressure")) {
						if (index == 0) suffix = "systolic"
						else suffix = "diastolic"
					}
					return Tag.look(baseTag.getDescription() + ' ' + suffix)
				}
			}
		}
		if (units) {
			String suffix = UnitGroupMap.theMap.getSuffixForUnits(baseTag.getDescription(), units)
			if (suffix)
				return Tag.look(baseTag.getDescription() + ' ' + suffix)
		}
		
		return baseTag
	}

	/**
	 * Return set of all suffixes for parsing
	 */
	public static Set<String> getSuffixes() {
		return suffixes
	}
	
	public static int getSuffixPriority(String suffix) {
		return suffixPriority.get(suffix)
	}
}

