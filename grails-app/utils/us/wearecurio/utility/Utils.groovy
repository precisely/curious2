package us.wearecurio.utility

import grails.util.GrailsUtil
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.joda.time.DateTime
import us.wearecurio.model.User
import us.wearecurio.services.EmailService
import us.wearecurio.services.SearchService

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * @author mitsu
 */
class Utils {

	public static final long THIRTY_SECONDS = 30000L;
	public static final long FIVE_MINUTES = 5 * 60000L;
	public static final long HOUR = 60 * 60000L;
	public static final long DAY = 24 * 60 * 60000L;

	private static Log log = LogFactory.getLog(this)

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

	static String zip(String s) {
		ByteArrayOutputStream targetStream = new ByteArrayOutputStream()
		def zipStream = new GZIPOutputStream(targetStream)
		zipStream.write(s.getBytes())
		zipStream.close()
		def zipped = targetStream.toByteArray()
		targetStream.close()
		return zipped.encodeBase64()
	}

	static String unzip(String compressed) {
		def inflaterStream = new GZIPInputStream(new ByteArrayInputStream(compressed.decodeBase64()))
		def uncompressedStr = inflaterStream.getText('UTF-8')
		return uncompressedStr
	}
	
	static reportError(String title, Exception e) {
		ByteArrayOutputStream os = new ByteArrayOutputStream()
		e.printStackTrace(new PrintStream(os))
		String output = os.toString("UTF8")
		
		log.error "Exception during operation " + title + ": " + e
		e.printStackTrace()
		def messageBody = "Error:\n" + output
		def messageSubject = title + GrailsUtil.environment
		EmailService.get().sendMail {
			to "server@wearecurio.us"
			from "server@wearecurio.us"
			subject messageSubject
			body messageBody
		}
	}

	static def save(obj) {
		return save(obj, false)
	}

	static boolean save(obj, boolean flush) {
		//quick workaround to avoid saving empty string sprint description
		//alternative is to create an unanalyzed index for sprints so empty strings can be filtered
		if (obj instanceof us.wearecurio.model.Sprint && obj.description == "") {
			obj.description = null
		}
		if (!obj.save(flush: flush)) {
			log.debug "Error saving $obj: $obj.errors"
		   	def messageBody = "Error saving while executing Curious app:\n" + obj.errors + "\n" + Arrays.toString(new Exception().getStackTrace())
			def messageSubject = "CURIOUS SERVER SAVE ERROR: " + GrailsUtil.environment
			EmailService.get().sendMail {
				to "server@wearecurio.us"
				from "server@wearecurio.us"
				subject messageSubject
				body messageBody
			}
			
			return false
		} else {
			log.debug "Object saved successfully $obj."
			SearchService.get()?.index(obj)
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
	
	public static String elasticSearchDate(long ms) {
		def f = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
		f.setTimeZone(TimeZone.getTimeZone("GMT"))
		return f.format(elasticSearchRoundMs(ms))
	}
	
	public static String orifyList(def l) {
		String orified = l?.collect{ it.toString() }?.join(" OR ")
		if (l?.size() > 1) {
			orified = "(" + orified + ")"
		}

		return orified
	}
	
	//For some reason, there is a compile error whenever I try to call this
	public static String elasticSearchDate(Date d) {
		return elasticSearchDate(d.getTime())
	}
	
    public static long elasticSearchRoundMs(long ms) {
        return 1000*((long)((ms+500)/1000))
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
	 */
	static Map paginateHQLs(List<Map> hqlDataList, def maxResults, def firstResult) {
		List dataList = []
		Long totalCount = 0

		log.debug "Paginate HQLs with max $maxResults & offset $firstResult"
		int max = maxResults ? maxResults.toInteger() : 10
		int offset = firstResult ? firstResult.toInteger() : 0

		// Iterate through each hql query data.
		hqlDataList.eachWithIndex { hqlData, index ->
			Map params = hqlData["params"] ?: [:]
			params.putAll([max: max, offset: offset])

			int existingResultCount = dataList.size()

			// Do not calculate pagination parameter for first query data and if results are sufficient.
			if (index > 0 && existingResultCount < max) {
				int currentPage = (offset / max) + 1
				int offsetAdjustment = 0
				long previousPagesCount = 0
				int previousPaginatedPagesCount = 0

				((index - 1)..0).each { previousQueryIndex ->
					previousPagesCount += User.executeQuery(hqlDataList[previousQueryIndex]["countQuery"], hqlDataList[previousQueryIndex]["namedParameters"])[0]
				}
				if ((previousPagesCount % max) != 0) {
					offsetAdjustment = max - (previousPagesCount % max)
				}
				previousPaginatedPagesCount = previousPagesCount / max

				// Get next set of results from current query
				params["max"] = max - existingResultCount

				if (existingResultCount == 0) {
					// Calculate offset for current HQL from current page
					params["offset"] = (max * (currentPage - previousPaginatedPagesCount - 1)) + offsetAdjustment
				} else {
					params["offset"] = 0
				}
			}
			log.debug "Paginating HQL for query number [${index + 1}], returning result count: $existingResultCount, total count: $totalCount with params $params."

			totalCount += User.executeQuery(hqlData["countQuery"], hqlData["namedParameters"])[0]

			if (index == 0 || existingResultCount < max) {
				List result = User.executeQuery(hqlData["query"], hqlData["namedParameters"], params)
				dataList.addAll result
				log.debug "Paginated HQL for query number [${index + 1}], returning result count: ${dataList.size()}, total count: $totalCount, fetched result count: ${result.size()}."
			} else {
				log.debug "Paginated HQL for query number [${index + 1}], returning result count: ${dataList.size()}, total count: $totalCount."
			}
		}

		[dataList: dataList, totalCount: totalCount]
	}

	static Date getStartOfDay(Date dateInstance) {
		DateTime dt = new DateTime(dateInstance).withTimeAtStartOfDay();
		return dt.toDate();
	}
}
