package us.wearecurio.model

import us.wearecurio.cache.BoundedCache

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

	// Cache holder to cache Tags with its InputType, id, description, min, max and numberOfLevels.
	static BoundedCache<Long, Map> tagsWithInputTypeCache = Collections.synchronizedMap(new BoundedCache<Long,
			List<String>>(50000))

	/*
	 * A method to cache various properties of Tag and TagInputType.
	 */
	static void cache(List instanceList) {
		instanceList.each { instance ->
			synchronized(tagsWithInputTypeCache) {
				tagsWithInputTypeCache.put(instance.tagId, [tagId: instance.tagId,
						description: instance.description, max: instance.max, inputType: instance.inputType,
						min: instance.min, noOfLevels: instance.noOfLevels, cacheDate: new Date()])
			}
		}
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
	 * A method to get recent Tags with it's input type and other properties such as description, noOfLevels, max, etc.
	 * Results can be filtered passing startDate and endDate in the argument.
	 * If values are present in cache then they are returned directly, if not then they are fetched from the database
	 * and before returning added to BoundedCache.
	 * If lastInputTypeUpdate Date is sent then only the data which was cached later to lastInputTypeUpdate Date are
	 * returned. If no values are found then method returns an empty List.
	 *
	 * @param startDate
	 * @param endDate
	 * @param lastInputTypeUpdate
	 *
	 * @return List<Map> of matching data.
	 */
	static List recentTagsWithInputType(Date startDate = null, Date endDate = null, Date lastInputTypeUpdate = null) {
		List resultInstanceList = []
		Map cachedInstances = [:]

		if (lastInputTypeUpdate) {
			cachedInstances << tagsWithInputTypeCache.findAll { k, v ->
				v.cacheDate > lastInputTypeUpdate
			}

			cachedInstances.each { k, v ->
				resultInstanceList.add(v)
			}

			return resultInstanceList
		}

		if (tagsWithInputTypeCache) {
			tagsWithInputTypeCache.each { k, v ->
				resultInstanceList.add(v)
			}

			return resultInstanceList
		}

		Map namedParams = startDate != null ? (endDate != null ? [startDate: startDate, endDate: endDate] :
				[startDate: startDate]) : (endDate != null ? [endDate: endDate] : [:])

		String query = "SELECT new Map(t.id as tagId, t.description as description, tiy.inputType as inputType," +
				" tiy.min as min, tiy.max as max, tiy.noOfLevels as noOfLevels) " +
				"FROM Tag t, TagStats ts, TagInputType tiy " +
				"WHERE t.id = ts.tagId and t.id = tiy.tagId "
		if (startDate) {
			query += "and ts.mostRecentUsage > :startDate "
		}
		if (endDate) {
			query += "and ts.mostRecentUsage < :endDate "
		}

		query += "group by t.id ORDER BY t.description"

		resultInstanceList = executeQuery(query, namedParams)

		// Adding results to cached data.
		cache(resultInstanceList)

		return resultInstanceList
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