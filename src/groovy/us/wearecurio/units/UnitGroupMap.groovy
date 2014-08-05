package us.wearecurio.units

import org.apache.commons.logging.LogFactory
import us.wearecurio.model.*
import java.util.regex.Pattern

class UnitGroupMap {

	private static def log = LogFactory.getLog(this)

	public static final int RATIO = 0
	public static final int AFFINITY = 1
	public static final int CANONICALUNIT = 2
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
	
	static Map suffixPriority = ['systolic' : 10, 'diastolic' : 20]
	
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
			'cal':[CALORIE,3,'calorie'], 'cals':[CALORIE,2,'calorie'], 'calorie':[CALORIE,10,'calorie'], 'calories':[CALORIE,10,'calorie'],
			'kcal':[KILOCALORIE,7,'kilocalorie'], 'kcals':[KILOCALORIE,7,'kilocalorie'], 'kcalorie':[KILOCALORIE,10,'kilocalorie'], 'kcalories':[KILOCALORIE,10,'kilocalorie'],
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
		])

		final int id
		final String name
		final int priority // priority for listing units in a sequence
		Map<String, UnitRatio> map = [:]
		Set<String> units = new HashSet<String>()
		
		public static final Set<String> unitGroupNames = new HashSet<String>()
		
		UnitGroup(int id, String name, int priority, def initMap) {
			this.id = id
			this.name = name
			this.priority = priority
			
			suffixPriority[name] = priority
			
			unitGroupNames.add(name)
			for (e in initMap) {
				String unit = e.key
				def val = e.value
				map.put(unit, new UnitRatio(
					unitMap:this,
					unit: unit,
					ratio: val[RATIO],
					affinity:val[AFFINITY],
					canonicalUnit:val[CANONICALUNIT]
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
	
	public static class UnitSuffix {
		UnitGroup group
		String suffix
	}
	
	public static final UnitGroupMap theMap = new UnitGroupMap()
	
	Map<String, UnitRatio> unitToRatio = new HashMap<String, UnitRatio>()

	def UnitGroupMap() {
		Set<String> allUnits = new HashSet<String>()
		
		// cache best unit ratio for each unit
		for (UnitGroup group : UnitGroup.values()) {
			allUnits.addAll(group.fetchUnits())
		}
		
		for (String units : allUnits) {
			unitToRatio.put(unit, calculateUnitRatioForUnits(units))
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
	
	/**
	 * Returns generic closest-matching UnitRatio for the given unit string
	 */
	public UnitRatio unitRatioForUnits(String unit) {
		return unitToRatio.get(unit)
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
				return unitGroup.lookupUnitRatio(unit)
		}
		// lookup cached unit ratio for the unit
		return unitToRatio.get(mostUsed.unit)
	}
	
	/**
	 * Return unit suffix corresponding to unit string. For now, just use simple algorithm
	 */
	public String getSuffixForUnits(String tagDescription, String units) {
		UnitRatio unitRatio = unitRatioForUnits(units)
		if (unitRatio) {
			return unitRatio.getUnitGroup().getName()
		}
		return units
	}
	
	/**
	 * Return set of all suffixes for parsing
	 */
	public static Set<String> getSuffixes() {
		return UnitGroup.unitGroupNames
	}
	
	public static int getSuffixPriority(String suffix) {
		return suffixPriority.get(suffix)
	}
}

