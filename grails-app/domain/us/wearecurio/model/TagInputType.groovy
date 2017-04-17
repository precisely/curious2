package us.wearecurio.model

import us.wearecurio.cache.BoundedCache
import us.wearecurio.utility.Utils

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
	static void cache(List resultList) {
		if (!resultList.size()) {
			Utils.reportError('[Curious Server] - TagInputType Cache Empty', 'Received empty list from database for ' +
					'TagInputType cache.')

			return
		}

		resultList.each { Map dataMap ->
			synchronized(tagsWithInputTypeCache) {
				tagsWithInputTypeCache.put(dataMap.tagId, dataMap)
			}
		}
		cacheDate = new Date() // Updating cache date.
	}

	static constraints = {
		userId nullable: true
		tagId unique: true
		defaultUnit nullable: true
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
		return fetchTagInputTypeInfo(new Date() - 14, null, userId)
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
		Map tagsWithInputTypeData = [:]

		if (!clientCacheDate || (cacheDate > clientCacheDate)) {
			if (!tagsWithInputTypeCache.size() || !cacheDate) {
				// Trying to update the cache if cache is empty.
				cache(fetchTagInputTypeInfo())
			}

			if (cacheDate && tagsWithInputTypeCache.values()) {
				tagsWithInputTypeData.cacheDate = cacheDate.time
				tagsWithInputTypeData.tagsWithInputTypeList = tagsWithInputTypeCache.values() as List
			}
		}

		return tagsWithInputTypeData
	}

	static void initializeTagsWithInputTypeCache() {
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
	static List fetchTagInputTypeInfo(Date startDate = null, Date endDate =  null, Long userId = null) {
		Map namedParams = startDate ? (endDate ? [startDate: startDate, endDate: endDate] :
				[startDate: startDate]) : (endDate ? [endDate: endDate] : [:])

		namedParams.max = 50000

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