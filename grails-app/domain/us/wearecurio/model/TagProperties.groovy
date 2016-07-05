package us.wearecurio.model


import us.wearecurio.utility.Utils
import org.apache.commons.logging.LogFactory

class TagProperties {
	
	private static def log = LogFactory.getLog(this)

	static enum DataType {
		UNSPECIFIED(0), CONTINUOUS(1), EVENT(2)
		final int id
		DataType(Integer val) { this.id = val }
		String toString() { name() }
		Integer getValue() { id }
	}
	static final DataType CONTINUOUS	= DataType.CONTINUOUS
	static final DataType EVENT = DataType.EVENT
	static final DataType UNSPECIFIED = DataType.UNSPECIFIED

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
		tagId unique: 'userId'
	}

	static mapping = {
		version false
		table 'tag_properties'
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
		dataTypeManual column: 'data_type_manual', defaultValue: DataType.UNSPECIFIED
		dataTypeComputed column: 'data_type_computed', defaultValue: DataType.UNSPECIFIED
	}

	// Put reg. expressions  here so that we don't have to keep recompiling them.
	static REG_PATTERN = ~/^\/(.*)\/$/

	static EVENT_PATTERNS = ["ate", "/.*ache\\b/", "bread", "pain", "eat", "jog", "ran", "run", "running", "jogged", "jogging", "sleep", "slept", "duration", "/\\bexercise.*\\b/"]
	static EVENT_EXCLUSION_PATTERNS = ["constant .* pain"]

	static CONTINUOUS_PATTERNS = ["weight", "rate", "pulse", "cholesterol", "temp", "temperature", "pressure", "air quality", "mood", "density", "glucose", "blood", "cholesterol",
			"hdl", "ldl", "thyroid", "compounded", "cortisol", "serum", "creatinine", "/bacteria\\b/", "ratio", "bmi", "body mass index", "heart rate", "hr", "hrv",
			"heart rate variability", "co2", "platelet count", "fat", "body fat", "/\\bhips\\b.*\\[distance\\]/", "/\\bwaist\\b.*\\[distance\\]/", "/\\bchest\\b.*\\[distance\\]/",
			"/\\bbiceps\\b.*\\[distance\\]/", "/\\bbicep\\b.*\\[distance\\]/", "/\\bthighs\\b.*\\[distance\\]/", "/\\bthigh\\b.*\\[distance\\]/", "/\\bcalves\\b.*\\[distance\\]/", "bdt", "respiration", "hrv", "heart-rate", "bp", "cbc",
			"blood sugar", "glucose", "fbs", "fbg", "pulse"]
	static CONTINUOUS_EXCLUSION_PATTERNS = ["weight watchers", "blood orange"]

	TagProperties() {
		// Default values.
		dataTypeManual = UNSPECIFIED
		dataTypeComputed = UNSPECIFIED
	}

	static TagProperties createOrLookup(long userId, long tagId) {
		TagProperties props
		
		userId = userId // seems to be needed to avoid strange Groovy compilation error?
		
		TagProperties.withTransaction {
			props = TagProperties.findByTagIdAndUserId(tagId, userId)
	
			if (!props) {
				props = new TagProperties(tagId:tagId, userId:userId)
				props.classifyAsEvent()
				if (!Utils.save(props, true)) {
					props = null
				}
			}
		}

		if (!props) {
			props = TagProperties.findByTagIdAndUserId(tagId, userId)
		}
		
		return props
	}

	def percentEntriesWithoutValues() {
		def numEntriesWithNoValue = Entry.countSeries(userId, tagId, -1)
		def totalEntriesInSeries = Entry.countSeries(userId, tagId, null)
		if (totalEntriesInSeries > 0) {
			return 1.0 * numEntriesWithNoValue / totalEntriesInSeries
		} else {
			return null
		}
	}

	static def toReg(String s) {
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
			result = ~("\\b" + s + "\\b")
		}
		result
	}

	static def match(patterns, not_patterns, description) {
		def result = false
		patterns.each { pattern ->
			if (toReg(pattern).matcher(description).find()) {
				result = true
				return true // break out of find loop.
			}
			return false
		}
		not_patterns.each { pattern ->
			if (toReg(pattern).matcher(description).find()) {
				result = false
				return true // break out of find loop.
			}
			return false
		}
		result
	}

	static def isEventWord(description) {
		match(EVENT_PATTERNS, EVENT_EXCLUSION_PATTERNS, description)
	}

	static def isContinuousWord(description) {
		match(CONTINUOUS_PATTERNS, CONTINUOUS_EXCLUSION_PATTERNS, description)
	}

	// A simple algorithm to determine if the tag is event-like
	//	(ie. happens in pulses, where the base value is 0) or
	//	continuous (most likely hovers around some non-zero mean
	//	value).

	TagProperties classifyAsEvent() {
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

		def the_tag = Tag.get(tagId)
		def description = null
		if (the_tag) {
			description = the_tag.description
		}

		// If a tag description matches the list of continuous patterns,
		//	then classify it as continuous.
		if (description && TagProperties.isContinuousWord(description)) {
			dataTypeComputed = CONTINUOUS
			return this
		}
		// If the tag description matches a list of event words,
		//	then classify it as an event.
		if (description && TagProperties.isEventWord(description)) {
			dataTypeComputed = EVENT
			return this
		}

		// The default value is that the tag series is continuous.
		dataTypeComputed = EVENT
		return this
	}
	
	static reclassifyAll() {
		log.debug "Reclassifying all TagProperties..."
		
		TagProperties.executeUpdate("delete TagProperties tp where tp.dataTypeManual = :unspecified",
				[unspecified:DataType.UNSPECIFIED])
		
		TagProperties.withTransaction {
			def allTP = TagProperties.getAll()
			
			for (TagProperties tp : allTP) {
				log.debug "Reclassifying " + tp
				
				tp.classifyAsEvent()
				
				log.debug "TP now: " + tp
				
				Utils.save(tp, true)
			}
		}
	}

	static TagProperties lookup(Long uid, Long tid) {
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
		//	 invoked frequently and it will keep dataTypeComputed up-to-date.
		classifyAsEvent()
	}

	def fetchDataType() {
		dataTypeComputed
	}

	def setIsContinuous(x) {
		setDataType(x)
	}

	def getIsContinuous() {
		dataTypeComputed == CONTINUOUS
	}

	// Note: Changes here should also be made to lookupJSONDesc()
	def getJSONDesc() {
		return [userId: userId,
			tagId: tagId,
			isContinuous: isContinuous,
			showPoints: showPoints]
	}

	static def lookupJSONDesc(long userId, long tagId) {
		def props = TagProperties.findByTagIdAndUserId(tagId, userId)

		if (!props) { // return default settings, don't create new TagProperties
			return [userId: userId,
				tagId: tagId,
				isContinuous: false,
				showPoints: false]
		}
		return props.getJSONDesc()
	}

	String toString() {
		return "TagProperties(userId:" + userId + ", tag:" + Tag.fetch(tagId).description + ", dataTypeComputed:" \
				+ dataTypeComputed + ", showPoints:" \
				+ showPoints + ")"
	}
}
