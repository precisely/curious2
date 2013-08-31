package us.wearecurio.thirdparty

import java.util.Map

import us.wearecurio.model.Entry
import org.apache.commons.logging.LogFactory

public abstract class TagUnitMap {
	public final static String MEAL = "meal"
	public final static String MOVEMENT = "movement"
	public final static String MOOD = "mood"
	public final static String SLEEP = "sleep"
	public final static String NAP = "nap"

	public final static int MINUTES_TO_MS = 60 * 1000
	public final static float MS_TO_MINUTES = 0.001
	public final static int SECONDS_TO_HOURS = 3600
	public final static float SCALE_TO_0_10 = 10 / 100
	public final static int TO_MILLI = 1000

	public final static int AVERAGE = 1
	
	private static def log = LogFactory.getLog(this)
	
	static debug(str) {
		log.debug(str)
	}
	/**
	 * Generic method to convert a tag value from one unit to another if needed
	 * @param amount
	 * @param currentUnitMap
	 * @return
	 */
	public BigDecimal convert(BigDecimal amount,def currentUnitMapping) {
		def bucket = this.getBuckets();
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
			default:
				// Else means operation to be performed are on buckets, ex.: merging
				// Or type provided is a key in bucket[] to hold values
				debug "Adding to bucket: " + this.getBuckets()[currentUnitMapping.type]?.dump()
				this.getBuckets()[currentUnitMapping.type]?.values.add(amount)
				break
		}
		
		return amount
	}
	
	/**
	 * Buckets defined by the implementing class. These buckets will be specific to the type of
	 * importer
	 * @return
	 */
	public abstract Map getBuckets();
	
	public abstract Map getTagUnitMappings();
	
	public Entry buildEntry(def tagName, def amount, def userId, def date = new Date(), def args = [:]) {
		def currentMapping = this.getTagUnitMappings()[tagName]
		if (!currentMapping) {
			debug "No mapping found for tag name: " + tagName
			return
			
		}
		if(currentMapping.convert) {
			amount = this.convert(amount, currentMapping)
		}
		
		if (args.tagName) {
			this.createEntry(userId, amount, currentMapping.units, args.tagName, date, args)
		} else {
			this.createEntry(userId, amount, currentMapping.units, currentMapping.tag, date, args)
		}
	}
	
	/**
	 * Generic method to create an Entry domain object based on a Map
	 * @param userId
	 * @param amount
	 * @param units
	 * @param description
	 * @param date
	 * @return
	 */
	public Entry createEntry(userId, amount, units, description, date, Map args = [:]) {
		Map parsedEntry = [userId: userId, date: date,
			description: description, amount: amount, units: units,
			comment: "", timeZoneOffsetSecs: args.timeZoneOffsetSecs, tweetId: args.tweetId,
			repeatType: args.repeatType, setName: args.setName, amountPrecision: args.amountPrecision, 
			datePrecisionSecs: args.datePrecisionSecs
		]
		parsedEntry.putAll(args)

		def entry = Entry.create(userId, parsedEntry, date, false, null)
		return entry
	}
	
	/**
	 * Calculating entry amount based on the bucket operation and creating an entry for each of the buckets.
	 * @param userId
	 */
	public void buildBucketedEntries(def userId) {
		// Iterating through buckets & performing actions.
		this.getBuckets().each { bucketName, bucket ->
			if(bucket.operation == this.AVERAGE) {
				def totalAmount = bucket.values.sum()
				def averageAmount = totalAmount / bucket.values.size()
				def units = bucket.unit
				def description = bucket.tag
				createEntry(userId, averageAmount, units, description, new Date())
			}
		}
	}
	
	public void emptyBuckets() {
		this.getBuckets().each { bucketName, bucket ->  
			bucket.values.clear()
		}
	}
}
