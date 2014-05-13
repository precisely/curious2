package us.wearecurio.model;

import org.apache.commons.logging.LogFactory

import us.wearecurio.utility.Utils

class TagProperties {

	private static def log = LogFactory.getLog(this)

	public static enum ContinuousType {
		UNSPECIFIED,
		CONTINUOUS,
		EVENT
	}

	// The user specified something.
	//	If set to false, it means that the user wants to have
	//	it automatically computer, or that they never specified
	//	anything.

	// The user specified that it was continuous.
	ContinuousType isContinuousManuallySet
	ContinuousType isContinuous
	Boolean showPoints
	Long tagId
	Long userId

	static constraints = {
		isContinuousManuallySet(nullable:true)
		isContinuous(nullable:true)
		showPoints(nullable:true)
	}

	static mapping = {
		version false
		table 'tag_properties'
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
		isContinuousManuallySet defaultValue: ContinuousType.UNSPECIFIED
		isContinuous defaultValue:  ContinuousType.UNSPECIFIED
	}

	public static EVENT_PATTERNS = ["ache", "pain", "\\bate", "\\beat", "exercise", "\\bjog", "sleep"]

	public static CONTINUOUS_PATTERNS = ["\\brate", "pulse", "cholesterol"]

	public TagProperties() {
			// Default values.
			isContinuousManuallySet = ContinuousType.UNSPECIFIED
			isContinuous = ContinuousType.UNSPECIFIED
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

	public static def match(patterns, description) {
		def result = false
		patterns.each { pattern ->
			if ((~pattern).matcher(description).find()) {
				result = true
			}
		}
		return result
	}

	public static def isEventWord(description) {
		match(EVENT_PATTERNS, description)
	}

	public static def isContinuousWord(description) {
		match(CONTINUOUS_PATTERNS, description)
	}

	// A simple algorithm to determine if the tag is event-like
	//	(ie. happens in pulses, where the base value is 0) or
	//	continuous (most likely hovers around some non-zero mean
	//	value).
	public def classifyAsEvent() {
		// If user specified something, use what the user specified.
		//	The user-specified value is isContinuous.
		if (isContinuousManuallySet != ContinuousType.UNSPECIFIED) {
			isContinuous = isContinuousManuallySet
			return this
		}

		// If a significant portion of the entries don't have values,
		//	then it should be treated as an event.
		def p = percentEntriesWithoutValues()
		if (p && p > 0.10) {
			isContinuous = ContinuousType.EVENT
			return this
		}

		def description = Tag.get(tagId).description

		// If a tag description matches the list of continuous patterns,
		//	then classify it as continuous.
		if (TagProperties.isContinuousWord(description)) {
			isContinuous = ContinuousType.CONTINUOUS
			return this
		}

		// If the tag description matches a list of event words,
		//	then classify it as an event.
		if (TagProperties.isEventWord(description)) {
			isContinuous = ContinuousType.EVENT
			return this
		}

		// The default value is that the tag series is continuous.
		isContinuous = ContinuousType.CONTINUOUS
		return this
	}

	public static def lookup(uid, tid) {
		return TagProperties.findWhere(tagId:tid, userId:uid)
	}

	static public def lookupJSONDesc(long userId, long tagId) {
		def props = TagProperties.findByTagIdAndUserId(tagId, userId)

		if (!props) { // return default settings, don't create new TagProperties
			return [userId:userId,
				tagId:tagId,
				isContinuous:false,
				showPoints:false]
		}

		return props.getJSONDesc()
	}

	// note: changes here should also be made to lookupJSONDesc()
	def getJSONDesc() {
		return [userId:userId,
			tagId:tagId,
			isContinuous:isContinuous,
			showPoints:showPoints]
	}

	public String toString() {
		return "TagProperties(userId:" + userId + ", tagId:" + tagId + ", isContinuous:" \
				+ isContinuous + ", showPoints:" \
				+ showPoints + ")"
	}
}
