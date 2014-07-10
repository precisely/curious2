package us.wearecurio.thirdparty

import org.apache.commons.logging.LogFactory

import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.TimeZoneId
import us.wearecurio.services.*

abstract class TagUnitMap {

	final static String ACTIVITY = "activity"
	final static String MEAL = "meal"
	final static String MOVEMENT = "movement"
	final static String MOOD = "mood"
	final static String NAP = "nap"
	final static String SLEEP = "sleep"
	// The above constants are used for common string across various tag maps.

	static final BigDecimal KG_TO_POUNDS = new BigDecimal(220462, 5)
	static final BigDecimal M_TO_FEET = new BigDecimal(328084, 5)
	public final static int MINUTES_TO_MS = 60 * 1000
	public final static float MS_TO_MINUTES = 0.00001667
	public final static float SECONDS_TO_MINUTES = 1 / 60
	public final static float SECONDS_TO_HOURS = 1 / 3600
	public final static float SCALE_TO_0_10 = 10 / 100
	public final static int TO_MILLI = 1000
	public final static float METER_TO_KM = 1 / 1000
	public final static float METER_TO_MILE = 0.000621371

	final static int AVERAGE = 1
	final static int BUCKET = 2

	static Map commonTagMap = [:]

	private static def log = LogFactory.getLog(this)

	static {
		commonTagMap = [
			activityCalorie: [tag: "$ACTIVITY calories", unit: "cal"],
			activityDistance: [tag: "$ACTIVITY distance", unit: "miles", convert: true, type: METER_TO_MILE],
			activityElevation: [tag: "$ACTIVITY elevation", unit: "meters"],
			activitySteps: [tag: "$ACTIVITY move", unit: "steps"],
			activityDuration: [tag: "$ACTIVITY duration", unit: "min", convert: true, type: SECONDS_TO_MINUTES],

			bpDiastolic: [tag: "blood pressure diastolic", unit: "mmHg"],
			bpSystolic: [tag: "blood pressure systolic", unit: "mmHg"],
			fatFreeMass: [tag: "fat free mass", unit: "lbs", amountPrecision: 2, convert: true, type: KG_TO_POUNDS],
			fatRatio: [tag: "fat ratio", unit: "%"],
			fatMassWeight: [tag: "fat mass weight", unit: "lbs", amountPrecision: 2, convert: true, type: KG_TO_POUNDS],
			heartRate: [tag: "heart rate", unit: "bpm"],
			height: [tag: "height", unit: "feet", amountPrecision: 5, convert: true, type: KG_TO_POUNDS],
			steps: [tag: "$ACTIVITY steps", unit: ""],	// @Deprecated. Use `activitySteps` instead.
			weight: [tag: "weight", unit: "lbs", amountPrecision: 2, convert: true, type: KG_TO_POUNDS],
		]
	}

	/**
	 * Generic method to convert a tag value from one unit to another if needed
	 * @param amount Value to convert.
	 * @param currentUnitMap Tag map for conversion.
	 * @return Returns converted value.
	 */
	BigDecimal convert(BigDecimal amount, Map currentUnitMapping) {
		switch(currentUnitMapping.type) {
			// Operation to be performed are on buckets, ex.: merging
			// Or type provided is a key in bucket[] to hold values
			case BUCKET:
				log.debug "Adding to bucket: " + getBuckets()[currentUnitMapping.bucketKey]
				getBuckets()[currentUnitMapping.bucketKey].values.add(amount)
				break
			default:
				amount = amount * currentUnitMapping.type
				break
		}

		return amount
	}

	/**
	 * Buckets defined by the implementing class. These buckets will be specific to the type of
	 * importer
	 * @return
	 */
	abstract Map getBuckets();

	/**
	 * @return Returns a map containing tag names as key and their entry description as value.
	 * Used for creating entries from data coming from third party APIs.
	 */
	abstract Map getTagUnitMappings();
	
	Entry buildEntry(String tagName, def amount, Long userId, Integer timeZoneId, Date date, String comment, String setName, Map args = [:]) {
		Map currentMapping = getTagUnitMappings()[tagName]

		if (!currentMapping) {
			log.warn "No mapping found for tag name: [$tagName]"
			return null
		}

		log.debug "The tag map is: $currentMapping"

		if (amount != null) {
			amount = amount.toBigDecimal()
		}
		if (currentMapping.convert) {
			amount = convert(amount, currentMapping)
		}

		args["amountPrecision"] = args["amountPrecision"] ?: currentMapping["amountPrecision"]
		args["timeZoneName"] = args["timeZoneName"] ?: "America/Los_Angeles"

		String description = args["tagName"] ?: currentMapping["tag"]
		
		if (args["isSummary"]) {
			description += " summary" 
			args['datePrecisionSecs'] = Entry.VAGUE_DATE_PRECISION_SECS
		} else
			args['datePrecisionSecs'] = Entry.DEFAULT_DATEPRECISION_SECS

		if (amount != null) {
			amount = amount.setScale(args["amountPrecision"] ?: Entry.DEFAULT_AMOUNTPRECISION, BigDecimal.ROUND_HALF_UP)
		}

		Map parsedEntry = [userId: userId, date: date, description: description, amount: amount, units: currentMapping["unit"],
			comment: comment, setName: setName, timeZoneId: timeZoneId]
		
		parsedEntry.putAll(args)
		Entry.updatePartialOrCreate(userId, parsedEntry, null)
	}

	/**
	 * Calculating entry amount based on the bucket operation and creating an entry for each of the buckets.
	 * @param userId
	 */
	void buildBucketedEntries(Long userId, Map args) {
		// Iterating through buckets & performing actions.
		this.getBuckets().each { bucketName, bucket ->
			if (bucket.operation == this.AVERAGE) {
				def totalAmount = bucket.values.sum()
				def averageAmount = totalAmount / bucket.values.size()

				Map parsedEntry = [userId: userId, date: new Date(), description: bucket.tag, amount: averageAmount,
					comment: args.comment, setName: args.setName, timeZoneId: args.timeZoneId, units: bucket.unit, datePrecisionSecs:Entry.DEFAULT_DATEPRECISION_SECS]

				DatabaseService.retry(args) {
					Entry.updatePartialOrCreate(userId, parsedEntry, null)
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
