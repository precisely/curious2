package us.wearecurio.model

class TagInputType {

	Long tagId // BaseTagId
	Long userId

	// Maximum allowed value for this TagInputType.
	Integer max

	// Minimum allowed value for this TagInputType.
	Integer min

	// Number of divisions the InputWidget on the client side shows up for this TagInputType.
	Integer noOfLevels

	InputType inputType
	ValueType valueType = ValueType.DISCRETE // Default ValueType

	static constraints = {
		userId nullable: true
		tagId unique: true
	}

	static mapping = {
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
	}

	static List recentTwoWeekTagsWithInputType(boolean recentTags = true) {
		return executeQuery(
				"SELECT new Map(t.id as tagId, t.description as description, tiy.inputType as inputType," +
				" tiy.min as min, tiy.max as max, tiy.noOfLevels as noOfLevels) " +
				"FROM Tag t, TagStats ts, TagInputType tiy " +
				"WHERE t.id = ts.tagId and t.id = tiy.tagId " +
				(recentTags ? "and ts.mostRecentUsage > :twoWeeksAgo " : "") + 
				"group by t.id ORDER BY t.description", recentTags ? [twoWeeksAgo: new Date() - 14] : [:])
	}
}

enum InputType {
	SLIDER(1),
	LEVEL(2),
	BOOLEAN(3),
	SMILEY(4),
	THUMBS(5)

	final int id

	InputType(int id) {
		this.id = id
	}
}

enum ValueType {
	CONTINUOUS(0),
	DISCRETE(1)

	final int id

	ValueType(int id) {
		this.id = id
	}
}