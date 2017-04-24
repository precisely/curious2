package us.wearecurio.model

import us.wearecurio.cache.BoundedCache
import us.wearecurio.utility.Utils

class TagInputType {

	Long tagId
	Long userId

	String defaultUnit

	// Maximum allowed value for this TagInputType.
	BigDecimal max

	// Minimum allowed value for this TagInputType.
	BigDecimal min

	// Number of divisions the InputWidget on the client side shows up for this TagInputType.
	Integer noOfLevels

	InputType inputType
	ValueType valueType = ValueType.DISCRETE // Default ValueType

	boolean isDefault

	static Date cacheDate

	// Cache holder to cache Tags with its InputType, id, description, min, max and numberOfLevels.
	static BoundedCache<Long, Map> cachedTagInputTypes = Collections.synchronizedMap(new BoundedCache<Long,
			Map>(50000))

	static constraints = {
		userId nullable: true
		tagId unique: true
		defaultUnit nullable: true
	}

	static mapping = {
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
	}

	/*
	 * A method to cache various properties of Tag and TagInputType.
	 */
	static void cache(List tagInputTypes) {
		if (!tagInputTypes.size()) {
			Utils.reportError('[Curious Server] - TagInputType Cache Empty', 'Received empty list from database for ' +
					'TagInputType cache.')

			return
		}

		tagInputTypes.each { Map dataMap ->
			synchronized(cachedTagInputTypes) {
				cachedTagInputTypes.put(dataMap.tagId, dataMap)
			}
		}
		if (cachedTagInputTypes.values()) {
			cacheDate = new Date() // Updating cache date.
		}
	}

	/**
	 * A method to get Tags used withing last two weeks with it's input type and other properties such as description,
	 * noOfLevels, max, etc.
	 *
	 * @param userId Long Id of the User.
	 * @return List<Map> of result instances.
	 */
	static List getRecentTagsWithInputType(Long userId) {
		TreeSet tagInputTypes = new TreeSet<>([compare: { tagInputTypeMap1, tagInputTypeMap2 ->
			tagInputTypeMap1.description <=> tagInputTypeMap2.description
		}] as Comparator)

		tagInputTypes.addAll(fetchTagInputTypeInfo(new Date() - 14, null, userId))

		if (!tagInputTypes || tagInputTypes.size() < 8) {
			tagInputTypes.addAll(getDefaultTagInputTypes())
		}

		return tagInputTypes as List
	}

	static List getDefaultTagInputTypes() {
		return executeQuery("SELECT new Map(t.id as tagId, t.description as description," +
				" tiy.inputType as inputType, tiy.min as min, tiy.max as max, tiy.noOfLevels as noOfLevels," +
				" tiy.defaultUnit as defaultUnit) FROM Tag t, TagInputType tiy WHERE t.id = tiy.tagId" +
				" and tiy.isDefault = :isDefault", [isDefault: true, max: 8])
	}

	/**
	 * A method to get Tags with it's input type and other properties such as
	 * description, noOfLevels, max, etc. This method searches in the cache first based on the passed arguments and
	 * if no cached values are found then a database query is done to get TagInputType instances.
	 * userId is an optional parameter.
	 *
	 * @param clientCacheDate
	 * @param userId
	 * @return List<Map> of result instances.
	 */
	static Map getAllTagsWithInputType(Date clientCacheDate) {
		if (!clientCacheDate || (cacheDate > clientCacheDate)) {
			if (!cachedTagInputTypes.size() || !cacheDate) {
				// Trying to update the cache if cache is empty.
				cache(fetchTagInputTypeInfo())
			}

			return [cacheDate: cacheDate?.time, tagsWithInputTypeList: cachedTagInputTypes.values() as List]
		}

		return [:]
	}

	static void initializeCachedTagWithInputTypes() {
		// Adding query result to cache.
		cache(fetchTagInputTypeInfo())
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
	static List getTagsWithInputTypeForDateRange(Date startDate, Date endDate, Long userId = null) {
		return fetchTagInputTypeInfo(startDate, endDate, userId)
	}

	/**
	 * A method to get all Tags with it's input type and other properties such as
	 * description, noOfLevels, max, etc. Result list is cached before response. userId is an optional parameter.
	 *
	 * @param userId
	 * @return List<Map> of result instances.
	 */
	static List getAllTagsWithInputTypeForUser(Long userId) {
		return fetchTagInputTypeInfo(null, null, userId)
	}

	/**
	 * A method to fetch TagInputType info for caching. If the result size is more than 35000 then an email is sent to
	 * the support team with the actual count of matching entries.
	 *
	 * @param startDate
	 * @param endDate
	 * @param userId
	 * @return List<Map> of result instances.
	 */
	static List fetchTagInputTypeInfo(Date startDate = null, Date endDate =  null, Long userId = null,
			boolean isDefault = false) {
		Map namedParams = [max: 50000]

		String query = "SELECT new Map(t.id as tagId, t.description as description, tiy.inputType as inputType," +
				" tiy.min as min, tiy.max as max, tiy.noOfLevels as noOfLevels, tiy.defaultUnit as defaultUnit," +
				" ts.lastUnits as lastUnits) FROM Tag t, TagStats ts, TagInputType tiy " +
				"WHERE t.id = ts.tagId and t.id = tiy.tagId "

		if (startDate) {
			query += "and ts.mostRecentUsage > :startDate "
			namedParams.startDate = startDate
		}

		if (endDate) {
			query += "and ts.mostRecentUsage < :endDate "
			namedParams.endDate = endDate
		}

		if (userId) {
			namedParams.userId = userId
			query += "and ts.userId = :userId "
		}

		if (isDefault) {
			namedParams.isDefault = isDefault
			query += "and tiy.isDefault = :isDefault "
		}

		query += "group by t.id ORDER BY t.description"

		List resultInstanceList = executeQuery(query, namedParams)

		if (resultInstanceList.size() > 35000) {
			String countQuery = "SELECT count(*) as totalCount from TagInputType as tiy1 where tiy1.tagId in (" +
					"SELECT tiy.tagId FROM Tag t, TagStats ts, TagInputType tiy WHERE t.id = ts.tagId and " +
					"t.id = tiy.tagId group by t.id)"

			int totalCount = executeQuery(countQuery, namedParams)[0]

			String title = '[Curious] - TagInputType Total Count'
			String message = "TagInputType cache size has increased significantly, current size is ${totalCount}. " +
					'Please take necessary steps to cache from blowing up.'

			Utils.reportError(title, message)
		}

		return resultInstanceList
	}

	/**
	 * A method to remove entry from the cache for a given tagId.
	 */
	static void removeFromCache(String tagId) {
		cachedTagInputTypes.remove(tagId)
	}

	/**
	 * A method to clear cache data.
	 */
	static void clearCache() {
		cachedTagInputTypes.clear()
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