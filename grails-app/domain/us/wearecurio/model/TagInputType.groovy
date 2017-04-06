package us.wearecurio.model

import us.wearecurio.cache.BoundedCache

class TagInputType {

	Long tagId // BaseTagId
	Long userId

	String defaultUnit

	// Maximum allowed value for this TagInputType.
	Integer max

	// Minimum allowed value for this TagInputType.
	Integer min

	// Number of divisions the InputWidget on the client side shows up for this TagInputType.
	Integer noOfLevels

	InputType inputType
	ValueType valueType = ValueType.DISCRETE // Default ValueType

	static Date cacheDate

	// Cache holder to cache Tags with its InputType, id, description, min, max and numberOfLevels.
	static BoundedCache<Long, Map> tagsWithInputTypeCache = Collections.synchronizedMap(new BoundedCache<Long,
			Map>(50000))

	/*
	 * A method to cache various properties of Tag and TagInputType.
	 */
	static void cache(List instanceList) {
		instanceList.each { Map instance ->
			synchronized(tagsWithInputTypeCache) {
				tagsWithInputTypeCache.put(instance.tagId, instance)
			}
		}
		cacheDate = new Date() // Updating cache date.
	}

	static constraints = {
		userId nullable: true
		tagId unique: true
	}

	static mapping = {
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
	}

	/**
	 * A method to get Tags used withing last two weeks with it's input type and other properties such as description,
	 * noOfLevels, max, etc. userId is an optional parameter.
	 *
	 * @param userId: optional
	 * @return List<Map> of result instances.
	 */
	static List getRecentTagsWithInputType(Long userId = null) {
		return getQueryResults(new Date() - 14, null, userId)
	}

	/**
	 * A method to get Tags used withing a particular date range with it's input type and other properties such as
	 * description, noOfLevels, max, etc. userId is an optional parameter.
	 *
	 * @param startDate
	 * @param endDate
	 * @param userId : optional
	 * @return List<Map> of result instances.
	 */
	static List getTagsWithInputType(Date startDate, Date endDate, Long userId = null) {
		return getQueryResults(startDate, endDate, userId)
	}

	/**
	 * A method to get Tags with it's input type and other properties such as
	 * description, noOfLevels, max, etc. This method searches in the cache first based on the passed arguments and
	 * if no cached values are found then a database query is done to get TagInputType instances.
	 * userId is an optional parameter.
	 *
	 * @param lastInputTypeUpdate
	 * @param userId
	 * @return List<Map> of result instances.
	 */
	static List getTagsWithInputType(Date lastInputTypeUpdate, Long userId = null) {
		List tagsWithInputTypeList = []

		if (tagsWithInputTypeCache || lastInputTypeUpdate) {
			tagsWithInputTypeList.addAll(tagsWithInputTypeCache.values())

			if (lastInputTypeUpdate) {
				return (cacheDate > lastInputTypeUpdate) ? tagsWithInputTypeList : []
			}

			return tagsWithInputTypeList
		}

		return userId ? getTagsWithInputType(userId) : getTagsWithInputType()
	}

	/**
	 * A method to get  All Tags with it's input type and other properties such as
	 * description, noOfLevels, max, etc. Result list is cached before response. userId is an optional parameter.
	 *
	 * @param userId
	 * @return List<Map> of result instances.
	 */
	static List getTagsWithInputType(Long userId = null) {
		List resultInstanceList = getQueryResults(null, null, userId)

		// Adding query result to cache.
		cache(resultInstanceList)

		return resultInstanceList
	}

	/**
	 * A method which performs query on database and gets the desired result based on passed arguments.
	 *
	 * @param startDate
	 * @param endDate
	 * @param userId
	 * @return List<Map> of result instances.
	 */
	static List getQueryResults(Date startDate, Date endDate, Long userId = null) {
		Map namedParams = startDate ? (endDate ? [startDate: startDate, endDate: endDate] :
				[startDate: startDate]) : (endDate ? [endDate: endDate] : [:])

		if (userId) {
			namedParams.userId = userId
		}

		String query = "SELECT new Map(t.id as tagId, t.description as description, tiy.inputType as inputType," +
				" tiy.min as min, tiy.max as max, tiy.noOfLevels as noOfLevels, ts.lastUnits as lastUnits, " +
				"tiy.defaultUnit as defaultUnit) FROM Tag t, TagStats ts, TagInputType tiy " +
				"WHERE t.id = ts.tagId and t.id = tiy.tagId "

		if (startDate) {
			query += "and ts.mostRecentUsage > :startDate "
		}

		if (endDate) {
			query += "and ts.mostRecentUsage < :endDate "
		}

		if (userId) {
			query += "and ts.userId = :userId and tiy.userId = :userId "
		}

		query += "group by t.id ORDER BY t.description"

		List resultInstanceList = executeQuery(query, namedParams)

		return resultInstanceList
	}

	/**
	 * A method to remove entry from the cache for a given tagId.
	 */
	static void removeFromCache(String tagId) {
		tagsWithInputTypeCache.remove(tagId)
	}

	/**
	 * A method to clear cache data.
	 */
	static void clearCache() {
		tagsWithInputTypeCache.clear()
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