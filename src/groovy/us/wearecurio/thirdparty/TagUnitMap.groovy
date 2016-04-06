package us.wearecurio.thirdparty

import org.apache.commons.logging.LogFactory
import us.wearecurio.data.DecoratedUnitRatio
import us.wearecurio.data.UnitGroupMap
import us.wearecurio.model.DurationType
import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.services.DatabaseService
import us.wearecurio.services.EntryParserService
import us.wearecurio.services.EntryParserService.ParseAmount
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats

abstract class TagUnitMap {
	
	String name

	final static String ACTIVITY = "activity"
	final static String MEAL = "meal"
	final static String MOVEMENT = "movement"
	final static String MOOD = "mood"
	final static String NAP = "nap"
	final static String SLEEP = "sleep"
	final static String MEASUREMENT = "measurement"
	final static String EXERCISE = "exercise"
	// The above constants are used for common string across various tag maps.

	final static int AVERAGE = 1
	
	/**
	 * @return A map containing tag names as key and their entry description as value.
	 * Used for creating entries from data coming from third party APIs.
	 */	
	Map tagUnitMappings = null

	static Map commonTagMap = [:]
	static Map<String, TagUnitMap> theMaps = Collections.synchronizedMap([:])
	static Map<String, String> theSourceSetIdentifiers = Collections.synchronizedMap([:]) // map from device set identifier to device group name in UI
	
	static void addSourceSetIdentifier(String setIdentifier, String deviceGroupName) {
		theSourceSetIdentifiers.put(setIdentifier, deviceGroupName)
		log.debug "Source set identifier added. $theSourceSetIdentifiers"
	}
	
	static String setIdentifierToSource(String setIdentifier) {
		if (setIdentifier == null) return null

		/*
		 * Set name defined in the data service of each device are prefix for the actual set name or set identifier
		 * used in the entries. So searching the source name based on the matching initials of the set identifier.
		 *
		 * TODO Move this to ThirdParty enum
		 */
		return theSourceSetIdentifiers.find { setIdentifier.startsWith(it.key) }?.value
	}

	private static def log = LogFactory.getLog(this)
	
	UnitGroupMap unitGroupMap
	
	boolean active
	
	Set<String> baseTagNames = new HashSet<String>()
	
	TagUnitMap(String name, boolean active = true) {
		this.name = name
		
		unitGroupMap = UnitGroupMap.fetchTheMap()
		
		if (active)
			theMaps[name] = this
		
		this.active = active
	}

	static {
		commonTagMap = [
			activityCalorie: [tag: "$ACTIVITY", unit: "cal"],
			activityDistance: [tag: "$ACTIVITY", unit: "miles", convert: true, from:"meters"],
			activityElevation: [tag: "$ACTIVITY", unit: "meters elevation"],
			activitySteps: [tag: "$ACTIVITY", unit: "steps"],
			activityDuration: [tag: "$ACTIVITY", unit: "min", convert: true, from:"seconds"],

			bpDiastolic: [tag: "blood pressure", suffix: "diastolic", unit: "mmHg"],
			bpSystolic: [tag: "blood pressure", suffix: "systolic", unit: "mmHg"],
			fatFreeMass: [tag: "fat free mass", unit: "lbs", amountPrecision: 2, convert: true, from:"kg"],
			fatRatio: [tag: "fat ratio", unit: "%"],
			fatMassWeight: [tag: "fat mass weight", unit: "lbs", amountPrecision: 2, convert: true, from: "kg"],
			heartRate: [tag: "heart rate", unit: "bpm"],
			height: [tag: "height", unit: "feet", amountPrecision: 5, convert: true, from: "meters"],
			steps: [tag: "$ACTIVITY", unit: "steps"],	// @Deprecated. Use `activitySteps` instead.
			weight: [tag: "weight", unit: "lbs", amountPrecision: 2, convert: true, from: "kg"],
		]
	}
	
	Map initializeTagUnitMappings(Map map) {
		for (Map value : map.values()) {
			UnitGroupMap theMap = UnitGroupMap.fetchTheMap()
			value['unitRatio'] = theMap.lookupDecoratedUnitRatio(value['unit'])
			if (value['convert'])
				value['ratio'] = theMap.fetchConversionRatio(value['from'], value['unit'])
			baseTagNames.add(value['tag'])
		}
		
		tagUnitMappings = map
	}

	/**
	 * Buckets defined by the implementing class. These buckets will be specific to the type of
	 * importer
	 * @return
	 */
	abstract Map getBuckets();

	static BigDecimal convert(BigDecimal amount, BigDecimal ratio) {
		return (amount * ratio).setScale(100, BigDecimal.ROUND_HALF_UP)
	}

	Entry buildEntry(EntryCreateMap creationMap, EntryStats stats, String tagName, BigDecimal amount, Long userId,
			Integer timeZoneId, Date date, String comment, String setName, Map args = [:]) {

		Map currentMapping = tagUnitMappings[tagName]

		if (!currentMapping) {
			log.warn "No mapping found for tag name: [$tagName]"
			return null
		}

		log.debug "The tag map is: $currentMapping"
		
		if (currentMapping.convert) {
			amount = convert(amount, currentMapping.ratio)
		}
		if (currentMapping.bucketKey) {
			log.debug "Adding to bucket: " + getBuckets()[currentMapping.bucketKey]
			getBuckets()[currentMapping.bucketKey].values.add(amount)
		}
		DurationType durationType
		if (currentMapping.durationType) {
			durationType = currentMapping.durationType
		}

		int amountPrecision = currentMapping.amountPrecision ?: Entry.DEFAULT_AMOUNTPRECISION
		args["timeZoneName"] = args["timeZoneName"] ?: "America/Los_Angeles"

		String description = args["tagName"] ?: currentMapping["tag"]
		
		Tag baseTag = Tag.look(description)
		
		Tag tag
		
		String units = currentMapping.unit
		
		if (currentMapping["suffix"]) {
			tag = Tag.look(description + ' ' + currentMapping["suffix"])
		} else {
			tag = EntryParserService.get().tagWithSuffixForUnits(baseTag, units, 0)
		}
		
		if (args["isSummary"]) {
			description += " summary" 
			args['datePrecisionSecs'] = Entry.VAGUE_DATE_PRECISION_SECS
		} else
			args['datePrecisionSecs'] = Entry.DEFAULT_DATEPRECISION_SECS
		
		ParseAmount parseAmount = new ParseAmount(tag, baseTag, amount, amountPrecision, units, currentMapping.unitRatio, durationType)
		
		DecoratedUnitRatio unitRatio = currentMapping.unitRatio
		
		Map parsedEntry = [userId: userId, date: date, description: description, amount: parseAmount, comment: comment, setName: setName, timeZoneId: timeZoneId, durationType:durationType]
		
		parsedEntry.putAll(args)
		
		Entry e = Entry.updatePartialOrCreate(userId, parsedEntry, creationMap.groupForDate(date), stats)
		
		creationMap.add(e)
		
		return e
	}

	/**
	 * Calculating entry amount based on the bucket operation and creating an entry for each of the buckets.
	 * @param userId
	 */
	void buildBucketedEntries(EntryCreateMap creationMap, EntryStats stats, Long userId, Map args) {
		// Iterating through buckets & performing actions.
		Date date = new Date()
		this.getBuckets().each { bucketName, bucket ->
			if (bucket.operation == this.AVERAGE) {
				def totalAmount = bucket.values.sum()
				def averageAmount = totalAmount / bucket.values.size()

				Map parsedEntry = [userId: userId, date: date, description: bucket.tag, amount: averageAmount,
					comment: args.comment, setName: args.setName, timeZoneId: args.timeZoneId, units: bucket.unit, datePrecisionSecs:Entry.DEFAULT_DATEPRECISION_SECS]

				DatabaseService.retry(args) {
					Entry e = Entry.updatePartialOrCreate(userId, parsedEntry, creationMap.groupForDate(date), bucket.unitRatio, stats)
					creationMap.add(e)
				}
			}
		}
	}

	void emptyBuckets() {
		this.getBuckets().each { bucketName, bucket ->
			bucket.values.clear()
		}
	}

}
