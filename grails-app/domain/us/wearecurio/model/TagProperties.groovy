package us.wearecurio.model;

import org.apache.commons.logging.LogFactory

import us.wearecurio.utility.Utils

class TagProperties {

	private static def log = LogFactory.getLog(this)

	public static enum DataType {
		UNSPECIFIED("U"), CONTINUOUS("C"), EVENT("E")
		final String value
		DataType(String val) { this.value = val }
		String toString() { value }
		String getKey() { name() }
		String getValue() { value }
	}
	public static CONTINUOUS  = DataType.CONTINUOUS
	public static EVENT       = DataType.EVENT
	public static UNSPECIFIED = DataType.UNSPECIFIED

	// The user specified something.
	//	If set to false, it means that the user wants to have
	//	it automatically computer, or that they never specified
	//	anything.

	// The user specified that it was continuous or discrete.
	DataType dataTypeManual
	DataType dataTypeComputed

	Boolean showPoints
	Long tagId
	Long userId

	static constraints = {
		dataTypeManual(nullable:true)
		dataTypeComputed(nullable:true)
		showPoints(nullable:true)
	}

	static mapping = {
		version false
		table 'tag_properties'
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
		dataTypeManual column: 'data_type_manual', defaultValue: DataType.UNSPECIFIED
		dataTypeComputed column: 'data_type_computed', defaultValue:	DataType.UNSPECIFIED
	}

	// Put reg. expressions  here so that we don't have to keep recompiling them.
	public static REG_PATTERN = ~/^\/(.*)\/$/

	public static EVENT_PATTERNS = ["/ache/", "pain", "ate", "eat", "/exercise/", "jogging", "sleep"]
	public static EVENT_EXCLUSION_PATTERNS = ["constant .* pain"]

	public static CONTINUOUS_PATTERNS = ["weight", "rate", "pulse", "cholesterol"]
	public static CONTINUOUS_EXCLUSION_PATTERNS = ["weight watchers meal"]

	public TagProperties() {
			// Default values.
			dataTypeManual = UNSPECIFIED
			dataTypeComputed = UNSPECIFIED
	}

	public static def createOrLookup(long userId, long tagId) {
		log.debug "TagProperties.createOrLookup() userId:" + userId + ", tag:" + Tag.get(tagId)

		def props = TagProperties.findByTagIdAndUserId(tagId, userId)

		if (!props) {
			props = new TagProperties(tagId:tagId, userId:userId)
		}

		if (Utils.save(props, true))
			return props

		return null
	}

	public def percentEntriesWithoutValues() {
		def tag = Tag.get(tagId)
		def numEntriesWithNoValue = Entry.findAllWhere(userId: userId, tag: tag, amountPrecision: -1).size
		def totalEntriesInSeries = Entry.findAllWhere(userId: userId, tag: tag).size
		1.0 * numEntriesWithNoValue / totalEntriesInSeries
	}

	public static def toReg(String s) {
		// Convert a string to a regular expression.
		// If it looks like "/stuff.*/" then convert it to a raw regular expression.
		// Otherwise, assume it's a word, so match word boundaries on the beginning
		//	 and end of the string.
		// Either way, return a regular expression.
		def m = REG_PATTERN.matcher(s)
		def result = null
		if (m.matches()) {
			result = ~(m[0][1])
		} else {
			result = ~"\\b${s}\\b"
		}
		result
	}

	public static def match(patterns, not_patterns, description) {
		def result = false
		patterns.find { pattern ->
			if (toReg(pattern).matcher(description).find()) {
				result = true
				return true // break out of find loop.
			}
		}
		not_patterns.find { pattern ->
			if (toReg(pattern).matcher(description).find()) {
				result = false
				return true // break out of find loop.
			}
		}
		result
	}

	public static def isEventWord(description) {
		match(EVENT_PATTERNS, EVENT_EXCLUSION_PATTERNS, description)
	}

	public static def dataTypeComputedWord(description) {
		match(CONTINUOUS_PATTERNS, CONTINUOUS_EXCLUSION_PATTERNS, description)
	}

	// A simple algorithm to determine if the tag is event-like
	//	(ie. happens in pulses, where the base value is 0) or
	//	continuous (most likely hovers around some non-zero mean
	//	value).
	public def classifyAsEvent() {
		// If user specified something, use what the user specified.
		//	The user-specified value is dataTypeComputed.
		if (dataTypeManual != null && dataTypeManual != UNSPECIFIED) {
			dataTypeComputed = dataTypeManual
			return this
		}

		// If a significant portion of the entries don't have values,
		//	then it should be treated as an event.
		def p = percentEntriesWithoutValues()
		if (p && p > 0.10) {
			dataTypeComputed = EVENT
			return this
		}

		def description = Tag.get(tagId).description

		// If a tag description matches the list of continuous patterns,
		//	then classify it as continuous.
		if (TagProperties.dataTypeComputedWord(description)) {
			dataTypeComputed = CONTINUOUS
			return this
		}

		// If the tag description matches a list of event words,
		//	then classify it as an event.
		if (TagProperties.isEventWord(description)) {
			dataTypeComputed = EVENT
			return this
		}

		// The default value is that the tag series is continuous.
		dataTypeComputed = CONTINUOUS
		return this
	}

	public static def lookup(uid, tid) {
		return TagProperties.findWhere(tagId:tid, userId:uid)
	}

	def setDataType(p) {
		if (p == true || p == "true" || p == "continuous" || p == CONTINUOUS) {
			dataTypeManual = CONTINUOUS
		} else if (p == false || p == "false" || p == "event" || p == EVENT) {
			dataTypeManual = EVENT
		} else if (p == null || p == "unspecified" || p == UNSPECIFIED) {
			dataTypeManual = UNSPECIFIED
		} else {
			// The user passed in a ContinuousType other than EVENT or CONTINUOUS.
			dataTypeManual = p
		}

		// This is an expensive operation, but it's probably not
		//   invoked frequently and it will keep dataTypeComputed up-to-date.
		classifyAsEvent()
	}

	def fetchDataType() {
	  dataTypeComputed
	}

	// Note: Changes here should also be made to lookupJSONDesc()
	def getJSONDesc() {
		return [userId: userId,
			tagId: tagId,
			dataType: fetchDataType(),
			showPoints: showPoints]
	}

	static public def lookupJSONDesc(long userId, long tagId) {
		def props = TagProperties.findByTagIdAndUserId(tagId, userId)

		if (!props) { // return default settings, don't create new TagProperties
			return [userId: userId,
				tagId: tagId,
				dataType: false,
				showPoints: false]
		}
		return props.getJSONDesc()
	}

	public String toString() {
		return "TagProperties(userId:" + userId + ", tagId:" + tagId + ", dataTypeComputed:" \
				+ dataTypeComputed + ", showPoints:" \
				+ showPoints + ")"
	}
}
