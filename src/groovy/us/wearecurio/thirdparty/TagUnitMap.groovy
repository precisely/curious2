package us.wearecurio.thirdparty

import java.math.BigDecimal;

import org.apache.commons.logging.LogFactory

import us.wearecurio.model.Entry

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
	public final static int SECONDS_TO_HOURS = 3600
	public final static float SCALE_TO_0_10 = 10 / 100
	public final static int TO_MILLI = 1000

	public final static int AVERAGE = 1
	public final static int METER_TO_KM = 2

	static Map commonTagMap = [:]

	private static def log = LogFactory.getLog(this)

	static {
		commonTagMap = [
			bpDiastolic: [tag: "blood pressure diastolic", unit: "mmHg"],
			bpSystolic: [tag: "blood pressure systolic", unit: "mmHg"],
			fatFreeMass: [tag: "fat free mass", unit: "lbs", amountPrecision: 2, convert: true, type: KG_TO_POUNDS],
			fatRatio: [tag: "fat ratio", unit: "%"],
			fatMassWeight: [tag: "fat mass weight", unit: "lbs", amountPrecision: 2, convert: true, type: KG_TO_POUNDS],
			heartRate: [tag: "heart rate", unit: "bpm"],
			height: [tag: "height", unit: "feet", amountPrecision: 5, convert: true, type: KG_TO_POUNDS],
			steps: [tag: "$ACTIVITY steps", unit: ""],
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
			case this.SECONDS_TO_HOURS:
				amount = amount / this.SECONDS_TO_HOURS
				break
			case this.MINUTES_TO_MS:
				amount = amount * this.MINUTES_TO_MS
				break
			case this.MS_TO_MINUTES:
				amount = amount * this.MS_TO_MINUTES
				break
			case this.SCALE_TO_0_10:
				amount = amount * this.SCALE_TO_0_10
				break
			case this.TO_MILLI:
				amount = amount * this.TO_MILLI
				break
			case this.METER_TO_KM:
				amount = amount / 1000
				break
			case M_TO_FEET:
				amount = amount * M_TO_FEET
				break
			case KG_TO_POUNDS:
				amount = amount * KG_TO_POUNDS
				break
			default:
			// Else means operation to be performed are on buckets, ex.: merging
			// Or type provided is a key in bucket[] to hold values
				log.debug "Adding to bucket: " + getBuckets()[currentUnitMapping.type]?.dump()
				getBuckets()[currentUnitMapping.type]?.values.add(amount)
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

		String description = args["tagName"] ?: currentMapping["tag"]
		createEntry(userId, timeZoneId, amount, currentMapping["unit"], description, date, comment, setName, args)
	}

	/**
	 * Generic method to create an Entry domain object based on a Map.
	 * @param userId
	 * @param amount
	 * @param units
	 * @param description
	 * @param date
	 * @return
	 */
	Entry createEntry(Long userId, Integer timeZoneId, BigDecimal amount, String units, String description, Date date, String comment, String setName, Map args = [:]) {
		if (amount != null) {
			amount = amount.setScale(args["amountPrecision"] ?: Entry.DEFAULT_AMOUNTPRECISION, BigDecimal.ROUND_HALF_UP)
		}

		Map parsedEntry = [userId: userId, date: date, description: description, amount: amount, units: units,
			comment: comment, setName: setName, timeZoneName: args.timeZoneName ?: "America/Los_Angeles",
			timeZoneId: timeZoneId]
		parsedEntry.putAll(args)

		Entry.create(userId, parsedEntry, null)
	}

	/**
	 * Calculating entry amount based on the bucket operation and creating an entry for each of the buckets.
	 * @param userId
	 */
	void buildBucketedEntries(def userId) {
		// Iterating through buckets & performing actions.
		this.getBuckets().each { bucketName, bucket ->
			if (bucket.operation == this.AVERAGE) {
				def totalAmount = bucket.values.sum()
				def averageAmount = totalAmount / bucket.values.size()
				def units = bucket.unit
				def description = bucket.tag
				createEntry(userId, averageAmount, units, description, new Date())
			}
		}
	}

	void emptyBuckets() {
		this.getBuckets().each { bucketName, bucket ->
			bucket.values.clear()
		}
	}

}