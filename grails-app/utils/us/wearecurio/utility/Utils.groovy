package us.wearecurio.utility

import grails.plugin.mail.MailService

import java.text.DateFormat
import java.text.SimpleDateFormat

import org.apache.commons.logging.LogFactory

import us.wearecurio.model.User

/**
 * @author mitsu
 */
class Utils {

	public static final long THIRTY_SECONDS = 30000L;
	public static final long FIVE_MINUTES = 5 * 60000L;
	public static final long HOUR = 60 * 60000L;
	public static final long DAY = 24 * 60 * 60000L;

	private static def log = LogFactory.getLog(this)

	static def listJSONDesc(list) {
		def retVal = []
		for (obj in list) {
			if (obj instanceof Map)
				retVal.add(obj)
			else
				retVal.add(obj.getJSONDesc())
		}

		return retVal;
	}

	static def listJSONShortDesc(list) {
		def retVal = []
		for (obj in list) {
			retVal.add(obj.getJSONShortDesc())
		}

		return retVal;
	}

	static def save(obj) {
		return save(obj, false)
	}

	static boolean save(obj, boolean flush) {
		if (!obj.save(flush: flush)) {
			log.debug "Error saving $obj: $obj.errors"
			return false
		} else {
			log.debug "Object saved successfully $obj."
		}

		return true
	}

	static def testResetClosures
	
	public static void registerTestReset(Closure c) {
		if (!testResetClosures)
			testResetClosures = []
		testResetClosures.add(c)
	}
	
	public static void resetForTesting() {
		for (c in testResetClosures) {
			c()
		}
	}
	
	/**
	 * Simple utility method to do equals in a null-safe manner
	 */
	static boolean equals(a, b) {
		if (a == null) return b == null
		if (b == null) return false
		if (a == b) return true
		return a.equals(b)
	}

	/**
	 * Offset is in minutes
	 */
	private static def Map<String, TimeZone> timeZones = new HashMap<Integer, TimeZone>()

	// offset is in seconds, not milliseconds
	static TimeZone createTimeZone(int offset, String idStr, boolean dst) {
		Integer intOffset = offset;

		TimeZone tz = timeZones.get(idStr)

		if (tz != null) return tz;

		if (dst)
			tz = new SimpleTimeZone(offset * 1000, idStr,
					Calendar.APRIL, 1, -Calendar.SUNDAY,
					7200000,
					Calendar.OCTOBER, -1, Calendar.SUNDAY,
					7200000,
					3600000)
		else
			tz = new SimpleTimeZone(offset * 1000, idStr)

		timeZones.put(idStr, tz)

		return tz
	}

	static DateFormat gmtDateFormat

	static {
		Calendar gmtCalendar = Calendar.getInstance(new SimpleTimeZone(0, "GMT"))
		gmtDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
		gmtDateFormat.setCalendar(gmtCalendar)
	}

	static String dateToGMTString(Date date) {
		return date == null ? "null" : gmtDateFormat.format(date)
	}

	static MailService mailService

	public static setMailService(MailService service) { mailService = service }

	public static MailService getMailService() { return mailService }

	/**
	 * A generic utility method to get paginated results from one or more HQL queries.
	 * The basic function of this method is that, if there are two queries then first,
	 * it fetches data from first query and if it is not meets the required result count
	 * then it fetches next set of results from next query. Same looping runs until the
	 * required number of results fetched (unless records not exists in the db).
	 * 
	 * This method also returns the total number of results availble in the system for
	 * given queries which is used for generating pagination.
	 * 
	 * @param hqlDataList REQUIRED It is the list of map which contains three values.
	 * @param hqlDataList[each].query REQUIRED The HQL query used to fetch the actual results.
	 * @param hqlDataList[each].countQuery REQUIRED The equivalent HQL query used to fetch the total results count.
	 * @param hqlDataList[each].namedParameters REQUIRED Any named parameters needed in HQL queries.
	 * @param hqlDataList[each].params OPTIONAL Any additional parameter needs to pass to the query.
	 * @param maxResults Maximum results to be fetched. DEFAULT 10
	 * @param firstResult Specifies the offset for the results. DEFAULT 0
	 * @return This returns a map containing two values: first is <b>dataList</b> which is
	 * 			the actual result list for query. And second is <b>totalCount</b> which is
	 * 			the total count for matching queries existing the database.
	 * 
	 * @see http://grails.org/doc/2.2.x/ref/Domain%20Classes/executeQuery.html
	 * @see <code>getDiscussionsInfoForUser()</code> in <strong>UserGroup.groovy</strong>
	 */
	static Map paginateHQLs(List<Map> hqlDataList, def maxResults, def firstResult) {
		List dataList = []
		Long totalCount = 0

		int max = maxResults ? maxResults.toInteger() : 10
		int offset = firstResult ? firstResult.toInteger() : 0

		// Iterate through each hql query data.
		hqlDataList.eachWithIndex { hqlData, index ->
			Map params = hqlData["params"] ?: [:]
			params.putAll([max: max, offset: offset])

			int existingResultCount = dataList.size()

			// Do not calculate pagination parameter for first query data and if results are sufficient.
			if (index > 0 && existingResultCount < max) {
				int currentPage = (offset % max) + 1

				// Get next set of results from 
				params["max"] = max - existingResultCount
				if (existingResultCount == 0) {
					params["offset"] = offset ? max * (offset - 1) : 0
				} else {
					params["offset"] = 0
				}
			}

			totalCount += User.executeQuery(hqlData["countQuery"], hqlData["namedParameters"])[0]

			if (index == 0 || existingResultCount < max) {
				dataList.addAll User.executeQuery(hqlData["query"], hqlData["namedParameters"], params)
			}
		}

		[dataList: dataList, totalCount: totalCount]
	}
}