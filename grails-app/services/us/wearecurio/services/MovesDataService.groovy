package us.wearecurio.services

import static us.wearecurio.model.OAuthAccount.*
import grails.converters.JSON

import java.text.DateFormat
import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.Entry
import us.wearecurio.model.User
import us.wearecurio.model.OAuthAccount
import us.wearecurio.thirdparty.moves.MovesTagUnitMap

class MovesDataService {

	static final String MOVES_SET_NAME = "moves import"

	def oauthService

	MovesTagUnitMap tagUnitMap = new MovesTagUnitMap()

	void poll() {
		OAuthAccount.findAllByTypeId(MOVES_ID).each {
			poll(it)
		}
	}

	Map poll(OAuthAccount account) {
		Long userId = account.userId
		
		Integer timeZoneId = User.getTimeZoneId(userId)
		
		log.info "Polling account with userId [$userId]"

		Date today = new Date()

		Map args = [setName: MOVES_SET_NAME, comment: "(Moves)"]

		Token tokenInstance = account.tokenInstance

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd")
		SimpleDateFormat startEndTimeFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")
		// format.setTimeZone(TimeZone.getTimeZone(""))		// TODO Provide time-zone, if needed

		String pollURL = "https://api.moves-app.com/api/v1/user/activities/daily/" + today.format("yyyy-MM-dd")

		Response response = oauthService.getMovesResource(tokenInstance, pollURL)

		switch(response.code) {
			case 200:
				log.info "Summary of moves data for userId [$account.userId] with code: [$response.code]"
				break
			case 401:
				log.error "Token expired for userId [$account.userId] with code: [$response.code] & body: [$response.body]"
				return [success: false]
			default:
				log.error "Error fetching data from moves api for userId [$account.userId] with code: [$response.code] & body: [$response.body]"
				return [success: false]
		}

		JSONArray parsedResponse = JSON.parse(response.body)

		parsedResponse.each { daySummary ->
			Date currentDate = format.parse(daySummary.date)
			log.debug "Processing moves api summary for userId [$account.userId] for date: $daySummary.date"

			if(!daySummary["segments"]?.asBoolean()) {
				return
			}

			/**
			 * Checking date > start date-time (exa. 2013-12-13 00:00:00) and date < (start date-tim + 1)
			 * instead of checking for date <= end data-time (exa. 2013-12-13 23:59:59)
			 */
			Entry.findAllByUserIdAndSetNameAndDateBetween(userId, MOVES_SET_NAME, currentDate, currentDate + 1)*.delete()

			daySummary["segments"]?.each { currentSegment ->
				log.debug "Processing segment for userId [$account.userId] of type [$currentSegment.type]"

				currentSegment["activities"]?.each { currentActivity ->
					log.debug "Processing activity for userId [$account.userId] of type [$currentActivity.activity]"

					processActivity(currentActivity, userId, timeZoneId, currentActivity.activity, startEndTimeFormat, args)
				}
			}
		}
		return [success: true]
	}

	void processActivity(JSONObject currentActivity, Long userId, Integer timeZoneId, String activityType, DateFormat timeFormat, Map args) {
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
			if (currentActivity.steps) {
				tagUnitMap.buildEntry("${baseType}Step", currentActivity.steps.toBigDecimal(), userId, timeZoneId, startTime, args)
			}
			tagUnitMap.buildEntry("${baseType}Distance", currentActivity.distance.toBigDecimal(), userId, timeZoneId, startTime, args)
			if (currentActivity.calories) {
				tagUnitMap.buildEntry("${baseType}Calories", currentActivity.calories.toBigDecimal(), userId, timeZoneId, startTime, args)
			}

			tagUnitMap.buildEntry("${baseType}Start", 1 as BigDecimal, userId, timeZoneId, startTime, args.plus([amountPrecision: -1]))
			tagUnitMap.buildEntry("${baseType}End", 1 as BigDecimal, userId, timeZoneId, endTime, args.plus(amountPrecision: -1))
		}
	}

	Map unSubscribe(Long userId) {
		log.debug "unSubscribe() account:" + userId

		OAuthAccount account = OAuthAccount.findByUserIdAndTypeId(userId, MOVES_ID)
		if (!account) {
			log.info "No moves subscription found for userId [$userId]"
			return [success: false, message: "No subscription found"]
		}

		account.delete()
		[success: true]
	}

}