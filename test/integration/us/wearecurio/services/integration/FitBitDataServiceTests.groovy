package us.wearecurio.services.integration

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.scribe.model.Response

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.UrlService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.utility.Utils

class FitBitDataServiceTests extends CuriousServiceTestCase {

	UrlService urlService
	FitBitDataService fitBitDataService
	OAuthAccount account
	User user2

	@Override
	void setUp() {
		super.setUp()

		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert user2.save()

		account = new OAuthAccount([typeId: ThirdParty.FITBIT, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id", timeZoneId: TimeZoneId.look("America/New_York").id])

		Utils.save(account, true)
	}

	@Override
	void tearDown() {
	}

	void testSubscribeIfSuccess() {
		fitBitDataService.oauthService = [
			postFitBitResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection("{}"))
			}
		]
		Map result = fitBitDataService.subscribe(userId)
		assert result.success
	}

	void testSubscribeIfFail() {
		fitBitDataService.oauthService = [
			postFitBitResource:{ token, url, p, header ->
				return new Response(new MockedHttpURLConnection("{}", 202))
			}
		]
		Map result = fitBitDataService.subscribe(userId)
		assert !result.success
		assert OAuthAccount.findByUserId(userId).accessToken == ""
	}

	void testSubscribeIfNoAuthAccount() {
		shouldFail(MissingOAuthAccountException) {
			fitBitDataService.subscribe(user2.id)
		}
	}

	void testUnsubscribeIfNoOAuthAccount() {
		shouldFail(MissingOAuthAccountException) {
			fitBitDataService.unsubscribe(user2.id)
		}
	}

	void testUnsubscribeWithOAuthAccountExists() {
		fitBitDataService.oauthService = [
			deleteFitBitResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection("{}", 204))
			}
		]

		assert OAuthAccount.count() == 1
		fitBitDataService.unsubscribe(userId)
		assert OAuthAccount.count() == 0
	}

	void testNotificationHandler() {
		String notificationString = """[{"collectionType":"foods","date":"2010-03-01","ownerId":"228S74","ownerType":"user","subscriptionId":"1234"},{"collectionType":"foods","date":"2010-03-02","ownerId":"228S74","ownerType":"user","subscriptionId":"1234"},{"collectionType":"activities","date":"2010-03-01","ownerId":"184X36","ownerType":"user","subscriptionId":"2345"}]"""
		fitBitDataService.notificationHandler(notificationString)
		assert ThirdPartyNotification.count() == 3
		assert ThirdPartyNotification.first().typeId == ThirdParty.FITBIT
	}

	void testGetDataDefaultWithFailureStatusCode() {
		fitBitDataService.oauthService = [
			getFitBitResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(401))
			}
		]
		// When fitbit returns a non-zero response code
		try {
			fitBitDataService.getDataDefault(account, new Date(), false)
		} catch(e) {
			assert e instanceof InvalidAccessTokenException
		}
	}

	void testGetDataDefault() {
		String mockedResponseData = """{"someKey": "someValue"}"""
		fitBitDataService.oauthService = [
			getFitBitResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		Map result = fitBitDataService.getDataDefault(account, new Date(), false)

		assert result.success == true
	}

	void testGetDataSleep() {
		Date now = new Date()

		String mockedResponseData = """{"sleep":[{"isMainSleep":true,"logId":29767,"efficiency":98,"startTime":"${now.format("yyyy-MM-dd'T'HH:mm:ss.SSS")}","duration":6000000,"minutesToFallAsleep":0,"minutesAsleep":47,"minutesAwake":24,"awakeningsCount":10,"timeInBed":100}]}"""
		fitBitDataService.oauthService = [
			getFitBitResource: { token, url, p, header ->
				assert url == "http://api.fitbit.com/1/user/${account.accountId}/sleep/date/${now.format('yyyy-MM-dd')}.json"
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		Map result = fitBitDataService.getDataSleep(account, now, false)
		assert result.success == true

		// Fetch entry with tag 'sleep'
		Entry entryInstance = Entry.withCriteria(uniqueResult: true) {
			tag {
				eq("description", "sleep")
			}
		}

		Integer timeZoneIdNumber = account.timeZoneId
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
		DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()

		Date savedDate = new DateTime(now.time).withZoneRetainFields(DateTimeZone.getDefault()).toDate()

		assert entryInstance != null
		// Checking if received local date-time got saved in UTC.
		assert entryInstance.date == savedDate
	}

}