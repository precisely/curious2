package us.wearecurio.services

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Token
import org.springframework.transaction.annotation.Transactional
import us.wearecurio.datetime.DateUtils
import us.wearecurio.model.DurationType
import us.wearecurio.model.Entry
import us.wearecurio.model.Identifier
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.services.DataService.DataRequestContext;
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.thirdparty.TagUnitMap
import us.wearecurio.thirdparty.moves.MovesTagUnitMap

import java.text.DateFormat
import java.text.SimpleDateFormat

class MovesDataService extends DataService {

	static transactional = false

	static final String BASE_URL = "https://api.moves-app.com/api/1.1%s"
	static final String COMMENT = "(Moves)"
	static final String SET_NAME = "Moves"
	static final String SOURCE_NAME = "Moves Data"
	MovesTagUnitMap tagUnitMap = new MovesTagUnitMap()

	MovesDataService() {
		provider = "Moves"
		typeId = ThirdParty.MOVES
		profileURL = String.format(BASE_URL, "/user/profile")
		TagUnitMap.addSourceSetIdentifier(SET_NAME, SOURCE_NAME)
	}

	@Override
	@Transactional
	Map getDataDefault(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataRequestContext context) throws
			InvalidAccessTokenException {
		log.debug("MovesDataService.getDataDefault(): account " + account.getId() + " startDate: " + startDate + " refreshAll: " + refreshAll)

		Long userId = account.userId

		startDate = startDate ?: earlyStartDate
		endDate = endDate ?: DateUtils.getEndOfTheDay()

		Integer timeZoneId = getTimeZoneId(account)

		TimeZone timeZoneInstance = TimeZoneId.getTimeZoneInstance(timeZoneId)

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd")
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
		SimpleDateFormat startEndTimeFormatWithTimezone = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ")
		SimpleDateFormat startEndTimeFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")

		format.setTimeZone(timeZoneInstance)
		formatter.setTimeZone(timeZoneInstance)

		log.info "Polling account with userId [$userId]"

		String setName = SET_NAME
		Map args = [setName: setName, comment: COMMENT]

		Token tokenInstance = account.tokenInstance

		String pollURL = String.format(BASE_URL, ("/user/activities/daily?from=" + formatter.format(startDate) +
				"&to=" + formatter.format(endDate)))

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)
		context.initEntrylist()

		def parsedResponse = getResponse(tokenInstance, pollURL)

		if (parsedResponse.getCode() != 200) {
			log.error "Error fetching data from moves api for userId [$account.userId]. Body: [$parsedResponse]"
			return [success: false]
		}

		parsedResponse.each { daySummary ->
			Date currentDate = format.parse(daySummary.date)
			log.debug "Processing moves api summary for userId [$account.userId] for date: $daySummary.date"

			if (!daySummary["segments"]?.asBoolean()) {
				return
			}

			List<JSONObject> activities = []
			List<String> allowedTypes = ["walking", "running", "cycling"]

			daySummary["segments"]?.each { currentSegment ->
				currentSegment["activities"].each { activityData ->
					if (allowedTypes.contains(activityData["activity"])) {
						activities.addAll(activityData)
					}
				}
			}

			long thresholdDuration = 15 * 60 * 1000		// 15 minutes or 900 seconds

			JSONObject previousActivity = null
			activities.each { currentActivity ->
				if (!previousActivity) {
					previousActivity = currentActivity
					return
				}

				// IF type of previous and current activity is same
				if ((previousActivity["activity"] == currentActivity["activity"])) {
					Date previousActivityEndTime = (previousActivity["endTime"].endsWith("Z") ? startEndTimeFormat :
							startEndTimeFormatWithTimezone).parse(previousActivity["endTime"])
					Date currentActivityStartTime = (currentActivity["startTime"].endsWith("Z") ? startEndTimeFormat :
							startEndTimeFormatWithTimezone).parse(currentActivity["startTime"])

					/*
					 * And if the duration between previous & current is less than 15 minutes,
					 * then merge all the data of current entry to previous entry
					 */
					if ((currentActivityStartTime.getTime() - previousActivityEndTime.getTime()) <= thresholdDuration) {
						// Not merging "duration" here as it is not used and the duration is calculated from start & end time
						["distance", "calories", "steps"].each { String key ->
							// First make sure there is no null values and values are integer for current entry
							currentActivity[key] = (currentActivity[key] ?: 0).toInteger()
							// Then make sure there is no null values and values are integer for previous entry
							previousActivity[key] = (previousActivity[key] ?: 0).toInteger()
							// And finally merge them
							previousActivity[key] += currentActivity[key]
						}

						previousActivity["endTime"] = currentActivity["endTime"]
						return
					}
				}

				processActivity(creationMap, stats, previousActivity, userId, timeZoneId,
						previousActivity["endTime"].endsWith("Z") ? startEndTimeFormat : startEndTimeFormatWithTimezone, args, context)
				previousActivity = currentActivity
			}

			// Make sure to process the last previous activity after the above loop finishes
			if (previousActivity) {
				processActivity(creationMap, stats, previousActivity, userId, timeZoneId,
						previousActivity["endTime"].endsWith("Z") ? startEndTimeFormat : startEndTimeFormatWithTimezone, args, context)
			}
		}

		account.markLastPolled(stats.lastDate)
		stats.finish()

		return [success: true]
	}

	@Transactional
	String getTimeZoneName(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException {
		getUserProfile(account).profile.currentTimeZone.id
	}

	@Override
	@Transactional
	List<ThirdPartyNotification> notificationHandler(String notificationData) {
	}

	@Transactional
	void processActivity(EntryCreateMap creationMap, EntryStats stats, JSONObject activity, Long userId,
				Integer timeZoneId, DateFormat timeFormat, Map args, DataRequestContext context = new DataRequestContext()) {
		processActivity(creationMap, stats, activity, userId, timeZoneId, activity["activity"].toString(), timeFormat, args, context)
	}

	@Transactional
	void processActivity(EntryCreateMap creationMap, EntryStats stats, JSONObject currentActivity, Long userId,
				Integer timeZoneId, String activityType, DateFormat timeFormat, Map args, DataRequestContext context = new DataRequestContext()) {
		String baseType

		Date startTime = timeFormat.parse(currentActivity.startTime)
		Date endTime = timeFormat.parse(currentActivity.endTime)

		switch(activityType) {
			case "cycling":		// Support type for HumanAPI
			case "cyc":			// Support type for MovesAPI
				baseType = "bike"
				break
			case "running":
			case "run":
				baseType = "run"
				break
			case "walking":
			case "wlk":
				baseType = "walk"
				break
			default:
				baseType = ""
				break
		}
		if (baseType) {
			String comment = args.comment
			String setName = args.setName
			if (currentActivity.steps) {
				tagUnitMap.buildEntry(creationMap, stats, "${baseType}Step", currentActivity.steps, userId,
						timeZoneId, startTime, comment, setName, args, context)
			}
			if (currentActivity.distance) {
				tagUnitMap.buildEntry(creationMap, stats, "${baseType}Distance", currentActivity.distance, userId,
						timeZoneId, startTime, comment, setName, args, context)
			}
			if (currentActivity.calories) {
				tagUnitMap.buildEntry(creationMap, stats, "${baseType}Calories", currentActivity.calories, userId,
						timeZoneId, startTime, comment, setName, args, context)
			}

			tagUnitMap.buildEntry(creationMap, stats, "${baseType}Start", 1, userId, timeZoneId, startTime,
					comment, setName, args.plus([amountPrecision: -1, durationType: DurationType.START]), context)

			tagUnitMap.buildEntry(creationMap, stats, "${baseType}End", 1, userId, timeZoneId, endTime,
					comment, setName, args.plus([amountPrecision: -1, durationType: DurationType.END]), context)
		}
	}
}
