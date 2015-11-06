package us.wearecurio.thirdparty

import java.math.BigDecimal

import us.wearecurio.data.UnitRatio
import us.wearecurio.data.DecoratedUnitRatio

import java.math.MathContext
import java.math.RoundingMode
import org.apache.commons.logging.LogFactory

import us.wearecurio.model.DurationType
import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.TimeZoneId
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats
import us.wearecurio.services.*
import us.wearecurio.services.EntryParserService.ParseAmount
import us.wearecurio.data.UnitGroupMap

abstract class TagUnitMap {

	final static String ACTIVITY = "activity"
	final static String MEAL = "meal"
	final static String MOVEMENT = "movement"
	final static String MOOD = "mood"
	final static String NAP = "nap"
	final static String SLEEP = "sleep"
	final static String MEASUREMENT = "measurement"
	// The above constants are used for common string across various tag maps.

	final static int AVERAGE = 1
	
	/**
	 * @return A map containing tag names as key and their entry description as value.
	 * Used for creating entries from data coming from third party APIs.
	 */	
	Map tagUnitMappings = null

	static Map commonTagMap = [:]

	private static def log = LogFactory.getLog(this)
	
	UnitGroupMap unitGroupMap
	
	public TagUnitMap() {
		unitGroupMap = UnitGroupMap.theMap
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
	
	static Map initializeTagUnitMappings(Map map) {
		for (Map value : map.values()) {
			value['unitRatio'] = UnitGroupMap.theMap.lookupDecoratedUnitRatio(value['unit'])
			if (value['convert'])
				value['ratio'] = UnitGroupMap.theMap.fetchConversionRatio(value['from'], value['unit'])
		}
		
		return map
	}

	/**
	 * Buckets defined by the implementing class. These buckets will be specific to the type of
	 * importer
	 * @return
	 */
	abstract Map getBuckets();

	Entry buildEntry(EntryCreateMap creationMap, EntryStats stats, String tagName, BigDecimal amount, Long userId,
			Integer timeZoneId, Date date, String comment, String setName, Map args = [:]) {

		Map currentMapping = tagUnitMappings[tagName]

		if (!currentMapping) {
			log.warn "No mapping found for tag name: [$tagName]"
			return null
		}

		log.debug "The tag map is: $currentMapping"
		
		if (currentMapping.convert) {
			amount = (amount * currentMapping.ratio).setScale(100, BigDecimal.ROUND_HALF_UP)
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
