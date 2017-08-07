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
	 * A method to cache various TagInputTypes.
	 */
	static void cache(List tagInputTypes) {
		if (!tagInputTypes.size()) {
			Utils.reportError('[precise.ly Server] - TagInputType Cache Empty', 'Received empty list from database for ' +
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
	 * A method to get all TagInputTypes for auto complete. This method searches in the cache first based on the passed 
	 * client cache date, if current cache date is greater than client cache date then new cache date and updated 
	 * list is sent in the response, otherwise empty map is sent. If the cache is empty, a database call is done to 
	 * update the cache.
	 *
	 * @param clientCacheDate
	 * @return List<Map> of result instances.
	 */
	static Map getAllTagsWithInputType(Date clientCacheDate) {
		if (!clientCacheDate || (cacheDate > clientCacheDate)) {
			if (!cachedTagInputTypes.size() || !cacheDate) {
				// Trying to update the cache if cache is empty.
				cache(getAllTagInputTypes())
			}

			return [cacheDate: cacheDate?.time, tagsWithInputTypeList: cachedTagInputTypes.values() as List]
		}

		return [:]
	}

	static void initializeCachedTagWithInputTypes() {
		// Adding query result to cache.
		cache(getAllTagInputTypes())
	}

	/**
	 * A method to get all TagInputTypes caching. If the result size is more than 35000 then an email is 
	 * sent to the support team with the actual count of matching entries.
	 * 
	 * @return List<Map> of result instances.
	 */
	static List getAllTagInputTypes() {
		String query = "SELECT new Map(t.id as tagId, t.description as description," +
				" tiy.inputType as inputType, tiy.min as min, tiy.max as max, tiy.noOfLevels as noOfLevels," +
				" tiy.defaultUnit as defaultUnit) FROM Tag t, TagInputType tiy WHERE t.id = tiy.tagId" +
				" GROUP BY t.id ORDER BY t.description"

		List resultInstanceList = executeQuery(query, [max: 50000])

		int totalCount = resultInstanceList.size()

		if (totalCount > 35000) {
				String title = '[precise.ly] - TagInputType Total Count'
			String message = "TagInputType cache size has increased significantly, current size is ${totalCount}. " +
					'Please take necessary steps to prevent cache from blowing up.'

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
	THUMBS(5),
	DEFAULT(6)

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