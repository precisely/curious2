package us.wearecurio.services

import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.scribe.model.Token

import us.wearecurio.model.Entry
import us.wearecurio.model.Identifier
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.thirdparty.jawbone.JawboneUpTagUnitMap

class JawboneUpDataService extends DataService {

	static final String BASE_URL = "https://jawbone.com/nudge/api%s"
	static final String COMMENT = "(Jawbone Up)"
	static final String SET_NAME = "JUP"

	JawboneUpTagUnitMap tagUnitMap = new JawboneUpTagUnitMap()

	JawboneUpDataService() {
		provider = "Jawboneup"
		typeId = ThirdParty.JAWBONE
		profileURL = String.format(BASE_URL, "/users/@me")
	}

	@Override
	Map getDataDefault(OAuthAccount account, Date startDate, boolean refreshAll) throws InvalidAccessTokenException {
		return null;
	}

	boolean isRequestSucceded(JSONObject response) {
		return !response || !response["meta"] || response["meta"] != 200
	}

	Map getDataSleep(OAuthAccount account, Date forDay, boolean refreshAll) {
		log.debug "getDataSleep(): account ${account.id} forDay: $forDay refreshAll: $refreshAll"

		Integer timeZoneIdNumber = getTimeZoneId(account)
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
		DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
		TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.US)
		formatter.setTimeZone(timeZone)
		SimpleDateFormat dateTimeParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
		dateTimeParser.setTimeZone(timeZone)

		String accountId = account.accountId
		String forDate = formatter.format(forDay)
		String setName = SET_NAME + " " + forDate
		String requestUrl = String.format(BASE_URL, "/users/@me/sleeps")
		Long userId = account.userId

		Map args = [setName: setName, comment: COMMENT]

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		JSONObject sleepData = getResponse(account.tokenInstance, requestUrl, "get", [date: forDate])

		if (!isRequestSucceded(sleepData)) {
			return [success: false, message: "Received non 200 response"]
		}

		sleepData["data"]["items"].find { sleepEntry ->
			if (!sleepEntry["details"]) {
				return false	// continue looping
			}

			JSONObject sleepDetails = sleepEntry["details"]

			Date entryDate = new Date(sleepDetails["asleep_time"])
			entryDate = new DateTime(entryDate.time).withZoneRetainFields(dateTimeZoneInstance).toDate()

			Entry.executeUpdate("""UPDATE Entry e SET e.userId = null WHERE e.setIdentifier = :setIdentifier AND
					e.userId = :userId""", [setIdentifier: Identifier.look(setName), userId: userId])

			if (sleepDetails["duration"]) {
				tagUnitMap.buildEntry(creationMap, stats, "duration", sleepDetails["duration"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)
			}
			if (sleepDetails["awakenings"]) {
				tagUnitMap.buildEntry(creationMap, stats, "awakeningsCount", sleepDetails["awakenings"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)
			}
		}

		stats.finish()

		return [success: true]
	}

	/**
	 * Overriding default implementation so to send accept header to all requests
	 */
	@Override
	JSONElement getResponse(Token tokenInstance, String requestUrl, String method = "get", Map queryParams = [:],
			Map requestHeaders = [:]) {
		requestHeaders << ["Accept": "application/json"]
		// TODO Remove this. This should work automatically.
		requestHeaders << ["Authorization": "Bearer ${tokenInstance?.token}"]
		super.getResponse(tokenInstance, requestUrl, method, queryParams, requestHeaders)
	}

	@Override
	String getTimeZoneName(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void notificationHandler(String notificationData) {
		// TODO Auto-generated method stub
	}
}