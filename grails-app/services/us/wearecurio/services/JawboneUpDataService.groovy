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

	static final String BASE_URL = "https://jawbone.com"
	static final String COMMON_BASE_URL = "$BASE_URL/nudge/api%s"
	static final String COMMENT = "(Jawbone Up)"
	static final String SET_NAME = "JUP"

	JawboneUpTagUnitMap tagUnitMap = new JawboneUpTagUnitMap()

	JawboneUpDataService() {
		provider = "Jawboneup"
		typeId = ThirdParty.JAWBONE
		profileURL = String.format(BASE_URL + COMMON_BASE_URL, "/users/@me")
	}

	@Override
	Map getDataDefault(OAuthAccount account, Date startDate, boolean refreshAll) throws InvalidAccessTokenException {
		return null;
	}

	boolean isRequestSucceded(JSONObject response) {
		return response && response["meta"] && response["meta"]["code"] == 200
	}

	TimeZone getTimeZone(OAuthAccount account) {

	}

	Map getDataSleep(OAuthAccount account, Date forDay, boolean refreshAll) {
		log.debug "getDataSleep(): account ${account.id} forDay: $forDay refreshAll: $refreshAll"

		String requestUrl = String.format(BASE_URL + COMMON_BASE_URL, "/users/@me/sleeps")

		if (forDay) {
			Integer timeZoneIdNumber = getTimeZoneId(account)
			TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
			DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
			TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.US)
			formatter.setTimeZone(timeZone)

			String forDate = formatter.format(forDay)
			requestUrl += "?date=$forDate"
		}

		return getDataSleep(account, refreshAll, requestUrl)
	}

	// Overloaded method to support pagination
	Map getDataSleep(OAuthAccount account, boolean refreshAll, String requestURL) {

		Integer timeZoneIdNumber = getTimeZoneId(account)
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
		DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
		TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.US)
		formatter.setTimeZone(timeZone)

		String accountId = account.accountId
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		JSONObject sleepDataResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)

		if (!isRequestSucceded(sleepDataResponse)) {
			return [success: false, message: "Received non 200 response"]
		}

		sleepDataResponse["data"]["items"].find { sleepEntry ->
			JSONObject sleepDetails = sleepEntry["details"]

			if (!sleepDetails) {
				return false	// continue looping
			}

			String setName = SET_NAME + " " + sleepDetails["date"]
			Map args = [comment: COMMENT, setName: setName]

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

		if (sleepDataResponse["links"] && sleepDataResponse["links"]["next"]) {
			log.debug "Processing get sleep data for paginated URL"

			getDataSleep(account, refreshAll, sleepDataResponse["links"]["next"])
		}

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