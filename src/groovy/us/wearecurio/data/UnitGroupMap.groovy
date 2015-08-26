package us.wearecurio.data

import org.apache.commons.logging.LogFactory

import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher
import java.util.regex.Pattern

import us.wearecurio.data.DataRetriever

class UnitGroupMap {
	
	private static def log = LogFactory.getLog(this)
	
	static final Pattern twoWordUnitPattern = ~/(?i)^(([^0-9\(\)@\s\.:][^\(\)@\s:]*)(\s([^0-9\(\)@\s\.:][^\(\)@\s:]*)))\s(([^0-9\(\)@\s\.:][^\(\)@\s:]*)(\s([^0-9\(\)@\s\.:][^\(\)@\s:]*))*)/
	static final Pattern oneWordUnitPattern = ~/(?i)^(([^0-9\(\)@\s\.:][^\(\)@\s:]*))\s(([^0-9\(\)@\s\.:][^\(\)@\s:]*)(\s([^0-9\(\)@\s\.:][^\(\)@\s:]*))*)/
	
	Map<String, UnitRatio> unitsToRatio

	//Index offsets of unit ratio initializer array
	static final int RATIO = 0
	static final int SUBRATIO = 1
	static final int AFFINITY = 2
	static final int CANONICALUNIT = 3
	static final int SUFFIX = 4
	static final int SUFFIXSUFFIX = 5
	
	//Duration
	static final long MILLISECOND = 1
	static final double PICOSECOND = MILLISECOND * 0.000000001d
	static final double MICROSECOND = MILLISECOND * 0.001d
	static final long SECOND = MILLISECOND * 1000
	static final long MINUTE = SECOND * 60
	static final long HOUR = MINUTE * 60
	static final long DAY = HOUR * 24
	static final long WEEK = DAY * 7
	static final double MONTH = DAY * 30.4375d
	static final double YEAR = MONTH * 12
	static final double CENTURY = YEAR * 100

	//Distance
	static final double METER = 1
	static final double CENTIMETER = 0.01d
	static final double MILLIMETER = 0.001d
	static final double FOOT = METER * 0.30480d
	static final double INCH = FOOT / 12.0d
	static final double YARD = METER * FOOT * 3.0d
	static final double MILE = METER * FOOT * 5280.0d
	static final double KILOMETER = METER * 1000

	//Weight
	static final double GRAM = 1
	static final double MILLIGRAM = 0.001d
	static final double MICROGRAM = 0.001d * MILLIGRAM
	static final double KILOGRAM = GRAM * 1000
	static final double POUND = GRAM * 453.6d
	static final double TON = POUND * 2000.0d
	static final double OUNCE = POUND / 16.0d
	
	//Area
	static final double SQMETER = 1
	static final double HECTARE = 10000 * SQMETER
	static final double ACRE = HECTARE / 2.471d
	static final double SQKILOMETER = KILOMETER * KILOMETER
	static final double SQMILE = MILE * MILE
	static final double SQFOOT = FOOT * FOOT
	
	//Volume
	static final double CC = 1
	static final double LITER = 1000.0d
	static final double GALLON = OUNCE * 128.0d
	static final double QUART = OUNCE * 32.0d
	static final double CUP = OUNCE * 8.0d
	static final double TEASPOON = LITER * 0.00492892d
	
	//Force
	static final double NEWTON = POUND / 4.44822162825d
	static final double DYNE = NEWTON / 100000.0d
	static final double KIP = POUND * 1000.0d
	
	//Power
	static final double WATT = 1
	static final double KILOWATT = 1000.0d * WATT
	static final double MILLIWATT = WATT / 1000.0d
	static final double HORSEPOWER = 745.699872d * WATT
	
	//Energy
	static final double JOULE = 1
	static final double ERG = 1.0e-7d
	static final double FOOTPOUND = 1.3558d
	static final double BTU = 1055.056d
	static final double HORSEPOWERHOUR = 2.6845e6d
	static final double MEGAJOULE = 1000000.0d
	static final double GIGAJOULE = 1.0e9d
	static final double TERAJOULE = 1.0e12d
	static final double PETAJOULE = 1.0e15d
	static final double KILOWATTHOUR = 3.6e6d
	static final double THERM = 105.5d * MEGAJOULE
	static final double CALORIE = 4.184d
	static final double KILOCALORIE = 1000.0d * CALORIE
	static final double KILOTON = 4.184e3d * GIGAJOULE
	static final double MEGATON = 1000.0d * KILOTON
	static final double EV = 1.60217653e-19d
	
	//Pressure
	static final double PASCAL = 1
	static final double BARYE = 0.1d
	static final double PSI = 6894.75729d
	static final double BAR = 100000.0d
	static final double DECIBAR = BAR / 10.0d
	static final double CENTIBAR = BAR / 100.0d
	static final double MILLIBAR = BAR / 1000.0d
	static final double MEGABAR = 1.0e6d * BAR
	static final double KILOBAR = 1.0e3d * BAR
	static final double MMHG = 133.3223684211d
	static final double INCHHG = 3386.389d
	static final double ATMOSPHERE = 98.0665e3d
	
	//Density
	static final double KGPERM3 = 1.0d/1000.0d
	static final double GPERM3 = 1/1000000.0d
	static final double MGPERDL = 0.00001d
	static final double KGPERL = 1.0d
	static final double GPERML = 1.0d
	static final double GPERL = 1.0d/1000.0d
	// TODO: Finish adding density units
	
	//Steps
	static final double STEPSUNIT = 1
	
	//Percentage
	static final double PERCENTAGEUNIT = 1
	
	//Frequency
	static final double PERSECOND = 1.0d
	static final double PERMINUTE = 60.0d
	static final double PERHOUR = 60.0d * PERMINUTE
	static final double PERDAY = 24.0d * PERHOUR
	static final double PERWEEK = 7.0d * PERDAY
	static final double PERMONTH = 30.4375d * PERDAY
	static final double PERYEAR = 365.25d * PERDAY
	
	//Scale
	static final double SCALEUNITY = 1
	static final double SCALETEN = 10
	static final double SCALEFIVE = 5
	static final double SCALEFOUR = 4
	static final double SCALETHREE = 4
	
	//Quantity
	static final double QUANTITYRATIO = 1
	
	static final int SINGULAR = 1 // mark unit as a singular sub unit
	static final int PLURAL = 2 // mark it as a plural sub unit
	static final int BOTH = SINGULAR | PLURAL // mark it as both singular and plural
	
	// other suffixes
	static Map suffixPriority = ['systolic' : 10, 'diastolic' : 20]
	
	static final Set<String> suffixes = new HashSet<String>()
	
	static {
		suffixPriority.each {
			suffixes.add(it.key)
		}
	}
	
	static final Map<String, String> unitStrings = [
		'minute':'mins',
		'hour':'hours',
		'day':'days',
		'week':'weeks',
		'month':'months',
		'year':'years',
		'century':'centuries',
		'second':'secs',
		'millisecond':'ms',
		'microsecond':'microseconds',
		'picosecond':'picoseconds',
		'centimeter':'cm',
		'millimeter':'mm',
		'meter':'m',
		'foot':'ft',
		'yard':'yards',
		'mile':'miles',
		'kilometer':'km',
		'gram':'g',
		'milligram':'mg',
		'microgram':'mcg',
		'pound':'lbs',
		'kilogram':'kg',
		'ounce':'oz',
		'square meter':'sq m',
		'square foot':'sq ft',
		'hectare':'ha',
		'acre':'acres',
		'cubic centimeter':'cc',
		'liter':'l',
		'deciliter':'dl',
		'milliliter':'ml',
		'gallon':'gal',
		'quart':'qt',
		'newton':'n',
		'dyne':'dyne',
		'kip':'kip',
		'watt':'w',
		'kilowatt':'kw',
		'milliwatt':'mw',
		'horsepower':'hp',
		'joule':'joules',
		'erg':'erg',
		'foot-pound':'ft-lbs',
		'btu':'btu',
		'horsepower-hour':'hph',
		'joule':'j',
		'megajoule':'mj',
		'gigajoule':'gj',
		'terajoule':'tj',
		'petajoule':'pj',
		'kilowatt-hour':'kw-hr',
		'therm':'therms',
		'calories':'cal',
		'kiloton':'kilotons',
		'megatons':'megatons',
		'electron-volt':'ev',
		'pascal':'pa',
		'barye':'ba',
		'psi':'psi',
		'bar':'bar',
		'decibar':'dbar',
		'centibar':'cbar',
		'millibar':'mbar',
		'megabar':'megabars',
		'kilobar':'kbar',
		'mmHg':'mmHg',
		'inHg':'inHg',
		'atmosphere':'atm',
		'percent':'%',
		'cup':'cups',
	]
	
	enum UnitGroup {
		DURATION(1, "duration", 100, [
			'm':[MINUTE,SECOND,1,'minute'], 'min':[MINUTE,SECOND,5,'minute'], 'mins':[MINUTE,SECOND,6,'minute',BOTH], 'minute':[MINUTE,SECOND,10,'minute'], 'minutes':[MINUTE,SECOND,10,'minute'],
			'h':[HOUR,MINUTE,1,'hour'], 'hours':[HOUR,MINUTE,10,'hour'], 'hr':[HOUR,MINUTE,8,'hour',SINGULAR], 'hr.':[HOUR,MINUTE,8,'hour'], 'hrs':[HOUR,MINUTE,8,'hour',PLURAL], 'hrs.':[HOUR,MINUTE,8,'hour'], 'hour':[HOUR,MINUTE,10,'hour'],
			'day':[DAY,HOUR,10,'day',SINGULAR], 'days':[DAY,HOUR,10,'day',PLURAL], 'd':[DAY,HOUR,1,'day',SINGULAR],
			'week':[WEEK,DAY,10,'week',SINGULAR], 'weeks':[WEEK,DAY,10,'week',PLURAL], 'wks':[WEEK,DAY,5,'week'], 'wk':[WEEK,DAY,4,'week'],
			'month':[MONTH,DAY,10,'month',SINGULAR], 'months':[MONTH,DAY,10,'month',PLURAL], 'mon':[MONTH,DAY,2,'month'], 'mo':[MONTH,DAY,2,'month'], 'mo.':[MONTH,DAY,2,'month'], 'mos.':[MONTH,DAY,2,'month'], 'mnths':[MONTH,DAY,7,'month'],
			'year':[YEAR,DAY,10,'year',SINGULAR], 'years':[YEAR,DAY,10,'year',PLURAL], 'yrs':[YEAR,DAY,10,'year'], 'yrs.':[YEAR,DAY,10,'year'], 'y':[YEAR,DAY,1,'year'],
			'century':[CENTURY,YEAR,10,'century',SINGULAR], 'centuries':[CENTURY,YEAR,10,'century',PLURAL],
			'sec':[SECOND,0,5,'second'],'secs':[SECOND,0,7,'second',BOTH], 'seconds': [SECOND,0,10,'second'], 'second':[SECOND,0,10,'second'],
			'millisecond':[MILLISECOND,0,10,'millisecond'], 'ms':[MILLISECOND,0,3,'millisecond',BOTH], 'milliseconds':[MILLISECOND,0,10,'millisecond'],
			'us':[MICROSECOND,0,1,'microsecond',BOTH], 'microsecond':[MICROSECOND,0,10,'microsecond'], 'microseconds':[MICROSECOND,0,10,'microsecond',BOTH],
			'ps':[PICOSECOND,0,2,'picosecond',BOTH], 'picosecond':[PICOSECOND,0,10,'picosecond'], 'picoseconds':[PICOSECOND,0,10,'picosecond',BOTH],
		]),
		DISTANCE(2, "distance", 200, [
			'cm':[CENTIMETER,0,5,'centimeter',BOTH], 'mm':[MILLIMETER,0,6,'millimeter'],
			'centimeter':[CENTIMETER,0,10,'centimeter'], 'centimetre':[CENTIMETER,0,10,'centimeter'],
			'millimeter':[MILLIMETER,0,10,'millimeter'], 'millimetre':[MILLIMETER,0,10,'millimeter'],
			'm':[METER,0,2,'meter',BOTH], 'meters':[METER,0,11,'meter'], 'meter':[METER,0,10,'meter'], 'metre':[METER,0,10,'meter'], 'metres':[METER,0,10,'meter'],
			'foot':[FOOT,INCH,10,'foot'], 'feet':[FOOT,INCH,10,'foot'], 'ft':[FOOT,INCH,5,'foot',BOTH],
			'inch':[INCH,0,10,'inch'], 'in':[INCH,0,10,'inch',BOTH], 'inches':[INCH,0,5,'inch'],
			'yard':[YARD,INCH,10,'yard'], 'yards':[YARD,INCH,10,'yard'], 'yd':[YARD,INCH,10,'yard',BOTH],
			'mi':[MILE,0,7,'mile',BOTH], 'mile':[MILE,0,10,'mile'], 'miles':[MILE,0,10,'mile'],
			'km':[KILOMETER,0,8,'kilometer',BOTH], 'kilometers':[KILOMETER,0,10,'kilometer'], 'kilometers':[KILOMETER,0,10,'kilometer'],
			'kilometer':[KILOMETER,0,1,'kilometer'],'kilometre':[KILOMETER,0,10,'kilometer'], 'kilometres':[KILOMETER,0,10,'kilometer'],
		]),
		WEIGHT(3, "weight", 300, [
			'g':[GRAM,0,2,'gram'], 'grams':[GRAM,0,10,'gram'], 'pound':[POUND,OUNCE,10,'pound'], 'lb':[POUND,OUNCE,5,'pound',BOTH], 'pounds':[POUND,OUNCE,10,'pound'], 'lbs':[POUND,OUNCE,10,'pound'],
			'kg':[KILOGRAM,0,8,'kilogram',BOTH], 'kgs':[KILOGRAM,0,4,'kilogram'], 'kilograms':[KILOGRAM,0,10,'kilogram'], 'kilogram':[KILOGRAM,0,10,'kilogram'],
			'ounce':[OUNCE,0,10,'ounce'], 'oz':[OUNCE,0,4,'ounce',BOTH], 'ounces':[OUNCE,0,10,'ounce'],
			'mg':[MILLIGRAM,0,8,'milligram',BOTH], 'mgs':[MILLIGRAM,0,8,'milligram'], 'milligram':[MILLIGRAM,0,8,'milligram'], 'milligrams':[MILLIGRAM,0,8,'milligram'],
			'ug':[MICROGRAM,0,8,'microgram',BOTH], 'ugs':[MICROGRAM,0,8,'microgram'], 'mcg':[MICROGRAM,0,2,'microgram'], 'mcgs':[MICROGRAM,0,8,'microgram'], 'microgram':[MICROGRAM,0,8,'microgram'], 'micrograms':[MICROGRAM,0,8,'microgram'],
			'pg':[MICROGRAM / 1000000.0d,0,8,'picogram',BOTH], 'pgs':[MICROGRAM / 1000000.0d,0,8,'picogram'], 'picogram':[MICROGRAM / 1000000.0d,0,8,'picogram'], 'picograms':[MICROGRAM / 1000000.0d,0,8,'picogram'],
			'ng':[MICROGRAM / 1000.0d,0,8,'nanogram',BOTH], 'ngs':[MICROGRAM / 1000.0d,0,8,'nanogram'], 'nanogram':[MICROGRAM / 1000.0d,0,8,'nanogram'], 'nanograms':[MICROGRAM / 1000.0d,0,8,'nanogram'],
		]),
		AREA(4, "area", 400, [
			'sq m':[SQMETER,0,3,'square meter',BOTH], 'sqm':[SQMETER,0,1,'square meter'], 'square meter':[SQMETER,0,10,'square meter'], 'square metre':[SQMETER,0,10,'square meter'], 'sq meter':[SQMETER,0,7,'square meter'],
			'sq metre':[SQMETER,0,7,'square meter'], 'm2':[SQMETER,0,4,'square meter'], 
			'sq ft':[SQFOOT,0,7,'square foot',BOTH], 'sqft':[SQFOOT,0,7,'square foot'], 'square foot':[SQFOOT,0,7,'square foot'], 'square feet':[SQFOOT,0,7,'square foot'], 'sq feet':[SQFOOT,0,7,'square foot'], 'sq foot':[SQFOOT,0,7,'square foot'],
			'hectare':[HECTARE,0,10,'hectare'], 'ha':[HECTARE,0,3,'hectare',BOTH], 'h':[HECTARE,0,1,'hectare'], 'hectares':[HECTARE,0,10,'hectare'],
			'acre':[ACRE,0,10,'acre',SINGULAR], 'acres':[ACRE,0,10,'acre',PLURAL], 'ac':[ACRE,0,6,'acre'],
		]),
		VOLUME(5, "volume", 500, [
			'cc':[CC,0,7,'cubic centimeter',BOTH], 'ccs':[CC,0,3,'cubic centimeter'], 'cubic centimeter':[CC,0,10,'cubic centimeter'], 'cubic centimeters':[CC,0,10,'cubic centimeter'], 'cubic cm':[CC,0,10,'cubic centimeter'], 'cubic centimetre':[CC,0,10,'cubic centimeter'], 'cubic centimetres':[CC,0,10,'cubic centimeter'],
			'mm3':[CC / 1000.0d,0,7,'cubic millimeter',BOTH], 'mm3s':[CC / 1000.0d,0,3,'cubic millimeter'], 'cubic millimeter':[CC / 1000.0d,0,10,'cubic millimeter'], 'cubic millimeters':[CC / 1000.0d,0,10,'cubic millimeter'], 'cubic mm':[CC / 1000.0d,0,10,'cubic millimeter'], 'cubic millimetre':[CC / 1000.0d,0,10,'cubic millimeter'], 'cubic millimetres':[CC / 1000.0d,0,10,'cubic millimeter'],
			'm3':[CC * 1000000.0d,0,7,'cubic meter',BOTH], 'm3s':[CC * 1000000.0d,0,3,'cubic meter'], 'cubic meter':[CC * 1000000.0d,0,10,'cubic meter'], 'cubic meters':[CC * 1000000.0d,0,3,'cubic meter'], 'cubic m':[CC * 1000000.0d,0,3,'cubic meter'], 'cubic metre':[CC * 1000000.0d,0,3,'cubic meter'], 'cubic metres':[CC * 1000000.0d,0,3,'cubic meter'],
			'l':[LITER,0,2,'liter',BOTH], 'L':[LITER,0,2,'liter'], 'liter':[LITER,0,10,'liter'], 'liters':[LITER,0,10,'liter'], 'litre':[LITER,0,10,'liter'], 'litres':[LITER,0,10,'liter'],
			'dl':[LITER/10.0d,0,2,'deciliter',BOTH], 'dL':[LITER/10.0d,0,2,'deciliter'], 'deciliter':[LITER/10.0d,0,10,'deciliter'], 'deciliters':[LITER/10.0d,0,10,'deciliter'], 'decilitre':[LITER/10.0d,0,10,'deciliter'], 'decilitres':[LITER/10.0d,0,10,'deciliter'],
			'cl':[LITER/100.0d,0,2,'centiliter',BOTH], 'cL':[LITER/100.0d,0,2,'centiliter'], 'centiliter':[LITER/100.0d,0,10,'centiliter'], 'centiliters':[LITER/100.0d,0,10,'centiliter'], 'centilitre':[LITER/100.0d,0,10,'centiliter'], 'centilitres':[LITER/100.0d,0,10,'centiliter'],
			'ml':[LITER/1000.0d,0,2,'milliliter',BOTH], 'mL':[LITER/1000.0d,0,2,'milliliter'], 'milliliter':[LITER/1000.0d,0,10,'milliliter'], 'milliliters':[LITER/1000.0d,0,10,'milliliter'], 'millilitre':[LITER/1000.0d,0,10,'milliliter'], 'millilitres':[LITER/1000.0d,0,10,'milliliter'],
			'gal':[GALLON,OUNCE,7,'gallon',BOTH], 'gl':[GALLON,OUNCE,1,'gallon'], 'g':[GALLON,OUNCE,1,'gallon'], 'gallon':[GALLON,OUNCE,10,'gallon'], 'gallons':[GALLON,OUNCE,10,'gallon'],
			'yd3':[CC * 764554.858d,0,8,'yd3',BOTH], 'cu yd':[CC * 764554.858d,0,8,'yd3'], 'cubic yd':[CC * 764554.858d,0,8,'yd3'], 'cubic yard':[CC * 764554.858d,0,8,'yd3'], 'cubic yards':[CC * 764554.858d,0,8,'yd3'],
			'ft3':[CC * 28316.8466d,0,8,'ft3',BOTH], 'cu ft':[CC * 28316.8466d,0,8,'ft3'], 'cubic ft':[CC * 28316.8466d,0,8,'ft3'], 'cubic foot':[CC * 28316.8466d,0,8,'ft3'], 'cubic feet':[CC * 28316.8466d,0,8,'ft3'],
			'in3':[CC * 16.387064d,0,8,'in3',BOTH], 'cu in':[CC * 16.387064d,0,8,'in3'], 'cubic in':[CC * 16.387064d,0,8,'in3'], 'cubic inch':[CC * 16.387064d,0,8,'in3'], 'cubic inches':[CC * 16.387064d,0,8,'in3'],
			'oz':[OUNCE,0,1,'ounce',BOTH], 'ounce':[OUNCE,0,1,'ounce'], 'ounces':[OUNCE,0,1,'ounce'],
			'qt':[QUART,OUNCE,8,'quart',BOTH], 'quart':[QUART,OUNCE,10,'quart'], 'quarts':[QUART,OUNCE,10,'quart'],
			'cup':[CUP,OUNCE,8,'cup',SINGULAR], 'cups':[CUP,OUNCE,10,'cup',PLURAL],
			'tsp':[TEASPOON,0,8,'tsp',BOTH], 'tsps':[TEASPOON,0,10,'tsp'], 'tbsp':[TEASPOON,0,8,'tsp'], 'tbsp.':[TEASPOON,0,10,'tsp'],
			'tbs.':[TEASPOON,0,8,'tsp'], 'teaspoon':[TEASPOON,0,8,'tsp'], 'teaspoons':[TEASPOON,0,10,'tsp'],
		]),
		FORCE(6, "force", 600, [
			'n':[NEWTON,0,3,'newton'], 'N':[NEWTON,0,3,'newton',BOTH], 'newton':[NEWTON,0,10,'newton'], 'newtons':[NEWTON,0,10,'newton'],
			'dyn':[DYNE,0,7,'dyne',BOTH], 'dyne':[DYNE,0,10,'dyne'],
			'kip':[KIP,0,5,'kip',BOTH],
		]),
		POWER(7, "power", 700, [
			'w':[WATT,0,2,'watt',BOTH], 'watt':[WATT,0,10,'watt'], 'watts':[WATT,0,10,'watt'],
			'kw':[KILOWATT,0,5,'kilowatt',BOTH], 'kilowatt':[KILOWATT,0,10,'kilowatt'], 'kilowatts':[KILOWATT,0,10,'kilowatt'],
			'mw':[MILLIWATT,0,3,'milliwatt',BOTH], 'milliwatt':[MILLIWATT,0,10,'milliwatt'], 'milliwatts':[MILLIWATT,0,10,'milliwatt'],
			'hp':[HORSEPOWER,0,7,'horsepower',BOTH], 'horsepower':[HORSEPOWER,0,10,'horsepower'],
		]),
		ENERGY(8, "energy", 350, [
			'j':[JOULE,0,3,'joule',BOTH], 'joule':[JOULE,0,10,'joule'], 'joules':[JOULE,0,10,'joule'],
			'erg':[ERG,0,10,'erg',BOTH], 'ergs':[ERG,0,10,'erg'],
			'ft-lb':[FOOTPOUND,0,8,'foot-pound',BOTH], 'ftlb':[FOOTPOUND,0,8,'foot-pound'], 'ft-lbs':[FOOTPOUND,0,8,'foot-pound'], 'foot-pound':[FOOTPOUND,0,10,'foot-pound'],
			'foot-pounds':[FOOTPOUND,0,10,'foot-pound'], 'foot pound':[FOOTPOUND,0,10,'foot-pound'], 'foot pounds':[FOOTPOUND,0,10,'foot-pound'],
			'footpound':[FOOTPOUND,0,10,'foot-pound'], 'footpounds':[FOOTPOUND,0,10,'foot-pound'],
			'btu':[BTU,0,8,'btu',BOTH], 'BTU':[BTU,0,8,'btu'],
			'hph':[HORSEPOWERHOUR,0,5,'horsepower-hour',BOTH], 'horsepower-hour':[HORSEPOWERHOUR,0,10,'horsepower-hour'],
			'horsepower-hours':[HORSEPOWERHOUR,0,10,'horsepower-hour'], 'horsepower hour':[HORSEPOWERHOUR,0,10,'horsepower-hour'],
			'horsepower hours':[HORSEPOWERHOUR,0,10,'horsepower-hour'], 'horsepowerhour':[HORSEPOWERHOUR,0,10,'horsepower-hour'],
			'horsepowerhours':[HORSEPOWERHOUR,0,10,'horsepower-hour'],
			'mj':[MEGAJOULE,0,3,'megajoule'], 'megajoule':[MEGAJOULE,0,10,'megajoule'], 'megajoules':[MEGAJOULE,0,10,'megajoule'],
			'gj':[GIGAJOULE,0,3,'gigajoule'], 'gigajoule':[GIGAJOULE,0,10,'gigajoule'], 'gigajoules':[GIGAJOULE,0,10,'gigajoule'],
			'tj':[TERAJOULE,0,3,'terajoule'], 'terajoule':[TERAJOULE,0,10,'terajoule'], 'terajoules':[TERAJOULE,0,10,'terajoule'],
			'pj':[PETAJOULE,0,3,'petajoule'], 'petajoule':[PETAJOULE,0,10,'petajoule'], 'petajoules':[PETAJOULE,0,10,'petajoule'],
			'kwh':[KILOWATTHOUR,0,7,'kilowatt-hour'], 'kilowatt-hour':[KILOWATTHOUR,0,10,'kilowatt-hour'], 'kilowatt-hours':[KILOWATTHOUR,0,10,'kilowatt-hour'],
			'kilowatt hour':[KILOWATTHOUR,0,10,'kilowatt-hour'], 'kilowatt hours':[KILOWATTHOUR,0,10,'kilowatt-hour'], 'kilowatthour':[KILOWATTHOUR,0,10,'kilowatt-hour'],
			'kilowatthours':[KILOWATTHOUR,0,10,'kilowatt-hour'],
			'therm':[THERM,0,10,'therm'], 'therms':[THERM,0,10,'therm'],
			'cal':[CALORIE,0,3,'calorie','calories'], 'cals':[CALORIE,0,2,'calorie','calories'],
			'calorie':[CALORIE,0,10,'calorie','calories'], 'calories':[CALORIE,0,10,'calorie','calories'],
			'kcal':[KILOCALORIE,0,7,'kilocalorie','calories'], 'kcals':[KILOCALORIE,0,7,'kilocalorie','calories'],
			'kcalorie':[KILOCALORIE,0,10,'kilocalorie','calories'], 'kcalories':[KILOCALORIE,0,10,'kilocalorie','calories'],
			'kiloton':[KILOTON,0,10,'kiloton'], 'kilotonne':[KILOTON,0,10,'kiloton'], 'kilotons':[KILOTON,0,10,'kiloton'],
			'kilotonnes':[KILOTON,0,10,'kiloton'],
			'megaton':[MEGATON,0,10,'megatons'], 'megatons':[MEGATON,0,10,'megatons'], 'megatonne':[MEGATON,0,10,'megatons'],
			'megatonnes':[MEGATON,0,10,'megatons'],
			'ev':[EV,0,5,'electron-volt'], 'evs':[EV,0,2,'electron-volt'], 'electron-volt':[EV,0,10,'electron-volt'],
			'electron-volts':[EV,0,10,'electron-volt'], 'electron volt':[EV,0,10,'electron-volt'],
			'electron volts':[EV,0,10,'electron-volt'],
		]),
		// TORQUE(9, "torque"),
		// LUMINOSITY(10, "luminosity"),
		PRESSURE(11, "pressure", 900, [
			'pa':[PASCAL,0,2,'pascal'], 'pascal':[PASCAL,0,10,'pascal'], 'pascals':[PASCAL,0,10,'pascal'],
			'ba':[BARYE,0,3,'barye'], 'barye':[BARYE,0,10,'barye'], 'baryes':[BARYE,0,10,'barye'],
			'psi':[PSI,0,8,'psi'],
			'bar':[BAR,0,9,'bar'], 'bars':[BAR,0,10,'bar'],
			'dbar':[DECIBAR,0,7,'decibar'], 'dbars':[DECIBAR,0,7,'decibar'], 'decibar':[DECIBAR,0,10,'decibar'],
			'decibars':[DECIBAR,0,10,'decibar'],
			'cbar':[CENTIBAR,0,5,'centibar'], 'centibar':[CENTIBAR,0,10,'centibar'], 'cbars':[CENTIBAR,0,10,'centibar'],
			'centibars':[CENTIBAR,0,10,'centibar'],
			'mbar':[MILLIBAR,0,7,'millibar'], 'mbars':[MILLIBAR,0,7,'millibar'], 'millibar':[MILLIBAR,0,10,'millibar'],
			'millibars':[MILLIBAR,0,10,'millibar'],
			'Mbar':[MEGABAR,0,4,'megabar'], 'Mbars':[MEGABAR,0,5,'megabar'], 'megabar':[MEGABAR,0,10,'megabar'],
			'megabars':[MEGABAR,0,10,'megabar'],
			'kbar':[KILOBAR,0,6,'kilobar'], 'kbars':[KILOBAR,0,7,'kilobar'], 'kilobar':[KILOBAR,0,10,'kilobar'],
			'kilobars':[KILOBAR,0,10,'kilobar'],
			'mmHg':[MMHG,0,7,'mmHg'],
			'over':[MMHG,0,5,'over'],
			'inHg':[INCHHG,0,6,'inHg'],
			'atm':[ATMOSPHERE,0,5,'atmosphere'], 'atmosphere':[ATMOSPHERE,0,10,'atmosphere'],
			'atmospheres':[ATMOSPHERE,0,1,'atmosphere'],
		]),
		DENSITY(12, "density", 450, [
			'kg/m3':[KGPERM3,0,10,'kg/m3'], 'mg/dL':[MGPERDL,0,10,'mg/dL'], 'kg/L':[KGPERL,0,10,'kg/L'], 'g/mL':[GPERML,0,10,'g/mL'],
			'mg/dl':[MGPERDL,0,10,'mg/dL'], 'kg/l':[KGPERL,0,10,'kg/L'], 'g/ml':[GPERML,0,10,'g/mL'],
			'pg/mL':[GPERL * (1.0e-9d),0,10,'pg/mL'], 'pg/ml':[GPERL * (1.0e-9d),0,10,'pg/mL'],
			'kg/L':[GPERL * 1000.0d,0,10,'kg/L'], 'kg/l':[GPERL * 1000.0d,0,10,'kg/L'],
			'mg/m3':[GPERM3 / 1000.0d,0,10,'kg/L'],
			'g/L':[GPERL,0,10,'g/L'], 'g/l':[GPERL,0,10,'g/L'],
			'g/m3':[GPERM3,0,10,'g/m3'],
			'g/cm3':[GPERM3 * (1000000.0d),0,10,'g/cm3'],
			'mg/L':[GPERL * (1.0e-3d),0,10,'mg/L'], 'mg/l':[GPERL * (1.0e-3d),0,10,'mg/L'],
			'mg/mL':[GPERL,0,10,'mg/mL'], 'mg/ml':[GPERL,0,10,'mg/mL'],
			'mg/tsp':[GPERL / TEASPOON,0,10,'mg/tsp'],
			'ug/uL':[GPERL,0,10,'ug/uL'], 'ug/ul':[GPERL,0,10,'ug/uL'],
			'pg/uL':[GPERL * (1.0e-6d),0,10,'pg/uL'], 'pg/ul':[GPERL * (1.0e-6d),0,10,'pg/uL'],
			'ng/uL':[GPERL * (1.0e-3d),0,10,'ng/uL'], 'ng/ul':[GPERL * (1.0e-3d),0,10,'ng/uL'],
			'ppm':[1.0e-6d,0,10,'ppm'], 'ppb':[1.0e-9d,0,10,'ppb'],
		]),
		STEPS(13, "steps", 5, [
			'steps':[STEPSUNIT,0,10,'steps'],
		]),
		PERCENTAGE(13, "percentage", 1000, [
			'%':[PERCENTAGEUNIT,0,10,'percent'], 'percent':[PERCENTAGEUNIT,0,10,'percent'],
			'percentage':[PERCENTAGEUNIT,0,10,'percent'],
		]),
		FREQUENCY(13, "frequency", 90, [
			'per sec':[PERSECOND,0,10,'per second'], 'per second':[PERSECOND,0,10,'per second'], 'bps':[PERSECOND,0,8,'per second'],
			'/ s':[PERSECOND,0,5,'per second'], '/ sec':[PERSECOND,0,9,'per second'],
			'/s':[PERSECOND,0,4,'per second'], '/sec':[PERSECOND,0,10,'per second'],
			'per min':[PERMINUTE,0,10,'per minute'], 'per minute':[PERMINUTE,0,10,'per minute'], 'bpm':[PERMINUTE,0,8,'per minute'],
			'/ m':[PERMINUTE,0,9,'per minute'], '/ min':[PERMINUTE,0,9,'per minute'],
			'/m':[PERMINUTE,0,4,'per minute'], '/min':[PERMINUTE,0,10,'per minute'],
			'per hr':[PERHOUR,0,10,'per hour'], 'per hour':[PERHOUR,0,10,'per hour'],
			'/ h':[PERHOUR,0,8,'per hour'], '/ hr':[PERHOUR,0,5,'per hour'], '/ hour':[PERHOUR,0,9,'per hour'],
			'/h':[PERHOUR,0,8,'per hour'], '/hr':[PERHOUR,0,5,'per hour'], '/hour':[PERHOUR,0,9,'per hour'],
			'per day':[PERDAY,0,10,'per day'], 'per d':[PERDAY,0,10,'per day'],
			'/ d':[PERDAY,0,5,'per day'], '/ day':[PERDAY,0,9,'per day'],
			'/d':[PERDAY,0,5,'per day'], '/day':[PERDAY,0,9,'per day'],
			'per w':[PERWEEK,0,10,'per week'], 'per week':[PERWEEK,0,10,'per week'], 'per wk':[PERWEEK,0,10,'per week'],
			'/ week':[PERWEEK,0,8,'per week'], '/ w':[PERWEEK,0,5,'per week'], '/ wk':[PERWEEK,0,9,'per week'],
			'/week':[PERWEEK,8,'per week'], '/w':[PERWEEK,5,'per week'], '/wk':[PERWEEK,9,'per week'],
			'per mon':[PERMONTH,0,10,'per month'], 'per month':[PERMONTH,0,10,'per month'],
			'/ month':[PERMONTH,0,5,'per month'], '/ mnth':[PERMONTH,0,9,'per month'], '/ mon':[PERMONTH,0,4,'per month'],
			'/month':[PERMONTH,0,5,'per month'], '/mnth':[PERMONTH,0,9,'per month'], '/mon':[PERMONTH,0,4,'per month'],
			'per y':[PERYEAR,0,10,'per year'], 'per year':[PERYEAR,0,10,'per year'], 'per yr':[PERYEAR,0,8,'per year'],
			'/ y':[PERYEAR,0,5,'per year'], '/ year':[PERYEAR,0,9,'per year'], '/ yr':[PERYEAR,0,5,'per year'],
			'/y':[PERYEAR,0,5,'per year'], '/year':[PERYEAR,0,9,'per year'], '/yr':[PERYEAR,0,5,'per year'],
		]),
		SCALE(13, "scale", 3, [
			'scale':[SCALETEN,0,10,'to ten'], '':[SCALETEN,0,1,'to ten'], 'to ten':[SCALETEN,0,10,'to ten'],
			'unity':[SCALEUNITY,0,10,'to one'], 'to one':[SCALEUNITY,0,10,'to one'],
			'to three':[SCALETHREE,0,10,'to three'], 'to four':[SCALEFOUR,0,10,'to four'], 'to five':[SCALEFIVE,0,10,'to five'],
		]),
		// quantity groups
		TABLET(14, "tablets", 2, [
			'tablet':[QUANTITYRATIO,0,10,'tablets',SINGULAR], 'tablets':[QUANTITYRATIO,0,10,'tablets',PLURAL],
		]),
		CAPSULE(15, "capsules", 2, [
			'capsule':[QUANTITYRATIO,0,10,'capsules',SINGULAR], 'capsules':[QUANTITYRATIO,0,10,'capsules',PLURAL], 'cap':[QUANTITYRATIO,0,3,'capsules'], 'caps':[QUANTITYRATIO,0,3,'capsules'],
		]),
		SLICE(16, "slices", 2, [
			'slice':[QUANTITYRATIO,0,10,'slices',SINGULAR], 'slices':[QUANTITYRATIO,0,10,'slices',PLURAL],
		]),
		DRINK(17, "drinks", 2, [
			'drink':[QUANTITYRATIO,0,10,'glasses',SINGULAR], 'drinks':[QUANTITYRATIO,0,10,'glasses',PLURAL],
		]),
		GLASS(18, "glasses", 2, [
			'glass':[QUANTITYRATIO,0,5,'glasses',SINGULAR], 'glasses':[QUANTITYRATIO,0,10,'glasses',PLURAL],
		]),
		K(19, "k", 2, [
			'K':[QUANTITYRATIO,0,2,'k',BOTH], 'k':[QUANTITYRATIO,0,2,'k'],
		]),
		CLAW(20, "claws", 2, [
			'claw':[QUANTITYRATIO,0,10,'claws',SINGULAR], 'claw':[QUANTITYRATIO,0,10,'claws',PLURAL],
		]),
		UNIT(21, "units", 2, [
			'u':[QUANTITYRATIO,0,2,'units'], 'unit':[QUANTITYRATIO,0,10,'units', SINGULAR], 'units':[QUANTITYRATIO,0,10,'units',PLURAL],
			'mU':[QUANTITYRATIO / 1000.0d,0,2,'milliunits',SINGULAR], 'mu':[QUANTITYRATIO / 1000.0d,0,10,'milliunits',PLURAL],
			'milliunit':[QUANTITYRATIO / 1000.0d,0,10,'milliunits'], 'milliunits':[QUANTITYRATIO / 1000.0d,0,10,'milliunits'],
		]),
		INTERNATIONALUNIT(23, "international unit", 2, [
			'iu':[QUANTITYRATIO,0,2,'iu', BOTH], 'ius':[QUANTITYRATIO,0,10,'iu'],
		]),
		DOLLAR(24, "dollars", 2, [
			'dollar':[1.0d,0,10,'dollars',SINGULAR], 'dollars':[1.0d,0,10,'dollars',PLURAL],
			'cent':[0.01d,0,10,'dollars',SINGULAR], 'cents':[0.01d,0,10,'dollars',PLURAL],
		]),
		BEER(25, "beers", 2, [
			'beer':[QUANTITYRATIO,0,5,'beers',SINGULAR], 'beers':[QUANTITYRATIO,0,10,'beers',PLURAL],
		]),
		QUANTITY(26, "quantity", 2, [
			'quantity':[QUANTITYRATIO,0,10,'quantity',BOTH], 'qty':[QUANTITYRATIO,0,10,'quantity'],
		]),
		static final double PERSECOND = 1.0d
		static final double PERMINUTE = 60.0d
		static final double PERHOUR = 60.0d * PERMINUTE
		static final double PERDAY = 24.0d * PERHOUR
		static final double PERWEEK = 7.0d * PERDAY
		static final double PERMONTH = 30.4375d * PERDAY
		static final double PERYEAR = 365.25d * PERDAY
	
		final int id
		final String name
		final int priority // priority for listing units in a sequence
		Map<String, UnitRatio> map = [:]
		Set<String> units = new HashSet<String>()
		Map<Double, UnitRatio> singularSubUnit = [:]
		Map<Double, UnitRatio> pluralSubUnit = [:]
		Map<String, UnitGroup> numeratorMap = null
		Map<String, UnitGroup> denominatorMap = null
		
		UnitGroup(int id, String name, int priority, def initMap) {
			this.id = id
			this.name = name
			this.priority = priority
			
			UnitGroupMap.suffixPriority[name] = priority
			
			UnitGroupMap.suffixes.add(name)
			
			// first pass, collect unit information
			for (e in initMap) {
				String unit = e.key
				def val = e.value
				String suffix = name
				
				int subUnitType = 0
				
				if (val.size() > SUFFIX) {
					def s = val[SUFFIX]
					
					if (s instanceof String) {
						suffix = s
					} else
						subUnitType = s
				}
				if (val.size() > SUFFIXSUFFIX) {
					suffix = val[SUFFIX]
				}

				UnitRatio unitRatio = new UnitRatio(
					this, unit, val[RATIO], val[AFFINITY], val[CANONICALUNIT], unitStrings[val[CANONICALUNIT]], suffix)
				map.put(unit, unitRatio)
				
				if (subUnitType & SINGULAR) {
					singularSubUnit.put((Double)val[RATIO], unitRatio)
				}
				if (subUnitType & PLURAL)
					pluralSubUnit.putAt((Double)val[RATIO], unitRatio)
				
				units.add(unit)
			}
			
			// second pass, link up unit display maps
			for (e in initMap) {
				String unit = e.key
				def val = e.value
				
				double subRatio = val[SUBRATIO]
				
				UnitRatio unitRatio = map.get(unit)
				
				unitRatio.subRatio = subRatio
				
				if (subRatio > 0.0d) {
					unitRatio.singularSubUnitRatio = singularSubUnit.get((Double)subRatio)
					unitRatio.pluralSubUnitRatio = pluralSubUnit.get((Double)subRatio)
				}
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
		
		static UnitGroup get(Long id) {
			if (id == null) return null
			return get((int) id)
		}
		
		void addNumeratorGroups(UnitGroup... groups) {
			if (numeratorMap == null)
				numeratorMap = [:]
			groups.each {
				numeratorMap.putAll(it.map)
			}
		}
		
		void addDenominatorGroups(UnitGroup... groups) {
			if (denominatorMap == null)
				denominatorMap = [:]
			groups.each {
				denominatorMap.putAll(it.map)
			}
		}
		
		
		String lookupUnitString(String unit, boolean plural) {
			UnitRatio unitRatio = lookupUnitRatio(unit)
			
			if (!unitRatio)
				return unit
			
			double ratio = unitRatio.ratio
			
			if (plural) {
				UnitRatio pluralRatio = pluralSubUnit?.get(ratio)
				return (pluralRatio ?: unitRatio).unit
			}
			
			UnitRatio singularRatio = singularSubUnit?.get(ratio)
			return (singularRatio ?: unitRatio).unit
		}

		UnitRatio lookupUnitRatio(String unit) {
			UnitRatio unitRatio = map[unit]
			if (unitRatio) return unitRatio
			unit = unit.toLowerCase()
			unitRatio = map[unit.toLowerCase()]
			if (unitRatio) return unitRatio
			if (numeratorMap != null && denominatorMap != null) {
				int slashPosition = unit.indexOf('/')
				if (slashPosition > 0) {
					String leftUnits = unit.substring(0, slashPosition)
					String rightUnits = unit.substring(slashPosition +1)
					
					UnitRatio numeratorRatio = numeratorMap.get(leftUnits)
					UnitRatio denominatorRatio = denominatorMap.get(rightUnits)
					
					if (numeratorRatio != null && denominatorRatio != null) {
						// create new hybrid UnitRatio
						String unitName = numeratorRatio.canonicalUnitString + '/' + denominatorRatio.canonicalUnitString
						UnitRatio newDensityRatio = new UnitRatio(this, unit, numeratorRatio.ratio / denominatorRatio.ratio, 10, unitName, unitName, name)
						map.put(unit, newDensityRatio)
						map.put(unitName, newDensityRatio)
						
						return newDensityRatio
					}
				}
			}
			
			return null
		}
		
		UnitRatio lookupBestUnitRatio(String unit, UnitRatio prevUnitRatio) {
			UnitRatio unitRatio = lookupUnitRatio(unit)

			if (unitRatio == null) return prevUnitRatio
			
			return unitRatio.bestRatio(prevUnitRatio)
		}
		
		Set<String> fetchUnits() {
			return map.keySet()
		}
	}
	
	static class UnitRatio {
		UnitGroup unitGroup
		String unit 
		double ratio
		def affinity
		String canonicalUnit
		String canonicalUnitString
		String suffix
		double subRatio
		UnitRatio singularSubUnitRatio
		UnitRatio pluralSubUnitRatio
		
		UnitRatio(UnitGroup unitGroup, String unit, double ratio, def affinity, String canonicalUnit, String canonicalUnitString, String suffix) {
			this.unitGroup = unitGroup
			this.unit = unit
			this.ratio = ratio
			this.affinity = affinity
			this.canonicalUnit = canonicalUnit
			this.canonicalUnitString = canonicalUnitString ?: canonicalUnit
			if (!suffix.startsWith('['))
				suffix = '[' + suffix + ']'
			this.suffix = suffix
			this.subRatio = 0.0d
			this.singularSubUnitRatio = null
			this.pluralSubUnitRatio = null
		}
		
		UnitRatio(UnitRatio other, String newSuffix) {
			this.unitGroup = other.unitGroup
			this.unit = other.unit
			this.ratio = other.ratio
			this.affinity = other.affinity
			this.canonicalUnit = other.canonicalUnit
			this.canonicalUnitString = other.canonicalUnitString
			if (!newSuffix.startsWith('['))
				newSuffix = '[' + newSuffix + ']'
			this.suffix = newSuffix
			this.subRatio = 0.0d
			this.singularSubUnitRatio = null
			this.pluralSubUnitRatio = null
		}
		
		UnitRatio bestRatio(UnitRatio other) {
			if (other == null)
				return this
			return this.affinity > other.affinity ? this : other
		}
		
		int getGroupId() {
			return unitGroup.getId()
		}
		
		UnitGroup getGroup() {
			return unitGroup
		}
		
		int getGroupPriority() {
			return unitGroup.getPriority()
		}
		
		UnitRatio lookupUnitRatio(String unit) {
			return unitGroup.lookupUnitRatio(unit)
		}
		
		BigDecimal conversionRatio(String unit) {
			if (!unit)
				return null
			
			UnitRatio otherRatio = lookupUnitRatio(unit)
			
			if (otherRatio == null)
				return null
				
			if (otherRatio == this)
				return null
			
			return new BigDecimal(otherRatio.ratio / this.ratio)
		}
		
		void getJSONAmounts(Map amounts, BigDecimal amount, int amountPrecision) {
			UnitRatio subUnitRatio = null
			if (subRatio) {
				if (amount.compareTo(BigDecimal.ONE) == 0) {
					subUnitRatio = singularSubUnitRatio
				} else
					subUnitRatio = pluralSubUnitRatio
			}
			if (subUnitRatio == null || amountPrecision < 0) {
				amounts.put(amounts.size(), [amount:amount, amountPrecision:(Integer)amountPrecision, units:this.unit])
			} else {
				long primeVal = (long)amount
				amounts.put(amounts.size(), [amount:new BigDecimal(primeVal), amountPrecision:(Integer)amountPrecision, units:this.unit])
				double remainder = (amount.doubleValue() - ((double)primeVal)) * ratio / subRatio
				if (remainder != 0.0d && (remainder > 1.0e-5d || remainder < -1.0e-5d))
					subUnitRatio.getJSONAmounts(amounts, new BigDecimal(remainder).setScale(2, BigDecimal.ROUND_HALF_UP), amountPrecision)
			}
		}
	}
	
	static final UnitGroupMap theMap = new UnitGroupMap()
	
	def UnitGroupMap() {
		UnitGroup.DENSITY.addNumeratorGroups(UnitGroup.WEIGHT, UnitGroup.TABLET, UnitGroup.CAPSULE, UnitGroup.K,
				UnitGroup.UNIT, UnitGroup.QUANTITY)
		UnitGroup.DENSITY.addDenominatorGroups(UnitGroup.VOLUME)

		Set<String> allUnits = new HashSet<String>()
		
		// cache best unit ratio for each unit
		for (UnitGroup group : UnitGroup.values()) {
			allUnits.addAll(group.fetchUnits())
		}
		
		unitsToRatio = new ConcurrentHashMap<String, UnitRatio>()

		for (String units : allUnits) {
			UnitRatio baseRatio = simpleLookupUnitRatioForUnits(units)
			if (baseRatio)
				unitsToRatio.put(units.toLowerCase(), baseRatio)
		}
		
		for (String units : allUnits) {
			unitsToRatio.put(units.toLowerCase(), unitRatioForUnits(units))
		}
	}
	
	/**
	 *	Compute which group a given unit falls under, looking through UnitGroups for best match
	 *
	 */
	protected UnitRatio simpleLookupUnitRatioForUnits(String units) {
		UnitRatio ratio = unitsToRatio.get(units)
		
		if (ratio)
			return ratio
		
		for (UnitGroup group : UnitGroup.values()) {
			ratio = group.lookupBestUnitRatio(units, ratio)
		}
		
		if (ratio)
			unitsToRatio.put(units, ratio)
		
		return ratio
	}
	
	protected double lookupRatioForUnits(String units) {
		UnitRatio unitRatio = unitRatioForUnits(units.toLowerCase())
		if (unitRatio) return unitRatio.ratio
		
		return 1.0d
	}
	
	double fetchConversionRatio(String fromUnits, String toUnits) {
		return lookupRatioForUnits(fromUnits) / lookupRatioForUnits(toUnits)
	}
	
	/**
	 * Returns generic closest-matching UnitRatio for the given unit string
	 * 
	 * Also look for units that contain an additional suffix ("meters climbed") and records it as "miles" plus suffix of "climbed"
	 */
	UnitRatio unitRatioForUnits(String units) {
		UnitRatio ratio = simpleLookupUnitRatioForUnits(units.toLowerCase())
		
		if (ratio != null) return ratio
		
		Matcher matcher = twoWordUnitPattern.matcher(units)
		
		if (matcher.lookingAt()) {
			String units2 = matcher.group(1)
			ratio = unitsToRatio.get(units2.toLowerCase())
			if (ratio != null) {
				String suffix = matcher.group(3)
				ratio = new UnitRatio(ratio, suffix)
				unitsToRatio.put(units, ratio)
				
				return ratio
			}
		}
		
		matcher = oneWordUnitPattern.matcher(units)
		
		if (matcher.lookingAt()) {
			String units1 = matcher.group(1)
			ratio = unitsToRatio.get(units1.toLowerCase())
			if (ratio != null) {
				String suffix = matcher.group(4)
				ratio = new UnitRatio(ratio, suffix)
				unitsToRatio.put(units, ratio)
				
				return ratio
			}
		}
		
		return null
	}
	
	UnitRatio unitRatioForTagIdUnits(Long userId, Long tagId, String units) {
		TagUnitStatsInterface mostUsed = DataRetriever.get().mostUsedTagUnitStats(userId, tagId)
		
		UnitGroup unitGroup = mostUsed?.getUnitGroup()
		if (unitGroup != null) {
			return unitGroup.lookupUnitRatio(units)
		}
		
		// lookup generic unit ratio for the unit
		return unitRatioForUnits(units)
	}

	void getJSONAmounts(Long userId, Long tagId, Map amounts, BigDecimal amount, int amountPrecision, String units) {
		UnitRatio unitRatio = unitRatioForTagIdUnits(userId, tagId, units)
		if (unitRatio == null) {
			amounts.put(amounts.size(), [amount:amount, amountPrecision:(Integer)amountPrecision, units:units])
		} else {
			unitRatio.getJSONAmounts(amounts, amount, amountPrecision)
		}
	}
	
	/**
	 * Look through UnitGroups, or just use most used UnitGroup
	 */
	UnitRatio mostUsedUnitRatioForTagIds(Long userId, def tagIds) {
		TagUnitStatsInterface mostUsed = DataRetriever.get().mostUsedTagUnitStatsForTags(userId, tagIds)
		if (mostUsed == null)
			return null
			
		UnitGroup unitGroup = mostUsed?.getUnitGroup()
		if (unitGroup != null) {
			return unitGroup.lookupUnitRatio(mostUsed.getUnit())
		}
		

		// lookup cached unit ratio for the unit
		return unitRatioForUnits(mostUsed.getUnit())
	}
	
	/**
	 * Return tag with suffix for given units and offset. Hack blood pressure for now.
	 */
	String suffixForUnits(String units) {
		if (!units) {
			return ""
		}
		
		UnitRatio unitRatio = unitRatioForUnits(units)
		String suffix
		if (unitRatio)
			suffix = unitRatio.getSuffix()
		else
			suffix = units
		
		return suffix
	}

	/**
	 * Return set of all suffixes for parsing
	 */
	static Set<String> getSuffixes() {
		return suffixes
	}
	
	static int getSuffixPriority(String suffix) {
		return suffixPriority.get(suffix)
	}
}

