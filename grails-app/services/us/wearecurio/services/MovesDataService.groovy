package us.wearecurio.services

import static us.wearecurio.model.OAuthAccount.*
import grails.converters.JSON

import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.web.json.JSONArray
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.thirdparty.moves.MovesTagUnitMap

class MovesDataService {

	static final String MOVES_SET_NAME = "moves import"

	def oauthService

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

	void poll() {
		OAuthAccount.findAllByTypeId(MOVES_ID).each {
			poll(it)
		}
	}

	Map poll(OAuthAccount account) {
		Long userId = account.userId
		log.info "Polling account with userId [$userId]"

		Date today = new Date()

		Map args = [setName: MOVES_SET_NAME, comment: "(Moves)"]

		Token tokenInstance = account.tokenInstance

		MovesTagUnitMap tagUnitMap = new MovesTagUnitMap()

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd")
		SimpleDateFormat startEndTimeFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")
		// format.setTimeZone(TimeZone.getTimeZone(""))		// TODO Provide time-zone, if needed

		String pollURL = "https://api.moves-app.com/api/v1/user/activities/daily/" + today.format("yyyy-MM-dd")

		Response response = oauthService.getMovesResource(tokenInstance, pollURL)
		log.info "Summary of moves data for userId [$account.userId] with code: [$response.code]"

		if (response.code != 200) {
			log.error "Error fetching data from moves api for userId [$account.userId] with code: [$response.code] & body: [$response.body]"
			return
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

					String baseType
					String activity = currentActivity.activity
					Date startTime = startEndTimeFormat.parse(currentActivity.startTime)
					Date endTime = startEndTimeFormat.parse(currentActivity.endTime)

					switch(activity) {
						case "cyc":
							baseType = "bike"
							break
						case "run":
							baseType = "run"
							break
						case "wlk":
							baseType = "walk"
							break
						case "trp":
							baseType = ""
							break
					}
					if (baseType) {
						if (currentActivity.steps) {
							tagUnitMap.buildEntry("${baseType}Step", currentActivity.steps.toBigDecimal(), userId, startTime, args)
						}
						tagUnitMap.buildEntry("${baseType}Distance", currentActivity.distance.toBigDecimal(), userId, startTime, args)
						if (currentActivity.calories) {
							tagUnitMap.buildEntry("${baseType}Calories", currentActivity.calories.toBigDecimal(), userId, startTime, args)
						}

						tagUnitMap.buildEntry("${baseType}Start", 1 as BigDecimal, userId, startTime, args.plus([amountPrecision: -1]))
						tagUnitMap.buildEntry("${baseType}End", 1 as BigDecimal, userId, endTime, args.plus(amountPrecision: -1))
					}
				}
			}
		}
		return [success: true]
	}

}