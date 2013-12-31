package us.wearecurio.services.integration

import static org.junit.Assert.*
import static us.wearecurio.model.OAuthAccount.*

import org.junit.*
import org.scribe.model.Response

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User
import us.wearecurio.services.IHealthDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.TagUnitMap;

class IHealthDataServiceTests {

	OAuthAccount account1

	IHealthDataService IHealthDataService

	User userInstance1

	@Before
	void setUp() {
		Entry.list()*.delete()
		OAuthAccount.findAllByTypeIdNotEqual(TWENTY_3_AND_ME_ID)*.delete()

		userInstance1 = new User([username: "dummy1", email: "dummy1@curious.test", sex: "M", first: "John", last: "Day",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: false])
		assert userInstance1.save()

		String accessToken = "some-token"
		account1 = OAuthAccount.createOrUpdate(IHEALTH_ID, userInstance1.id, "accountId", accessToken, "")
	}

	@After
	void tearDown() {
	}

	@Test
	void testPollForBloodGlucose() {
		IHealthDataService.oauthService = [getIHealthResource: {token, url ->
				String data = "[]"
				if(url.contains("glucose.json")) {
					data = """{"BGDataList":[{"BG":60,"DataID":"5f4fd34c37d1497a8dc1*****","DinnerSituation":"Before_breakfast","DrugSituation":"Before_breakfast","Lat":-1,"Lon":-1,"MDate":1355157900,"Note":""}],"BGUnit":0,"CurrentRecordCount":50,"NextPageUrl":"some-url","PageLength":50,"PageNumber":1,"PrevPageUrl":"","RecordCount":336}"""
				}
				return new Response(new MockedHttpURLConnection(data))
			}]

		IHealthDataService.poll(account1)

		assert Entry.count() == 1
	}

	void testPollForBloodOxygen() {
		IHealthDataService.oauthService = [getIHealthResource: {token, url ->
				String data = "[]"
				if(url.contains("spo2.json")) {
					data = """{"BODataList":[{"BO":188,"DataID":"01b34452f13049d387c*****","HR":72,"Lat":65.76911313588224,"Lon":151.35644438413738,"MDate":1362490426,"Note":"03-06-2013 01:25:46"}],"CurrentRecordCount":50,"NextPageUrl":"next-page-url","PageLength":50,"PageNumber":1,"PrevPageUrl":"","RecordCount":296}"""
				}
				return new Response(new MockedHttpURLConnection(data))
			}]

		IHealthDataService.poll(account1)

		assert Entry.count() == 1
	}

	void testPollForBloodPressure() {
		IHealthDataService.oauthService = [getIHealthResource: {token, url ->
				String data = "[]"
				if(url.contains("bp.json")) {
					data = """{"BPDataList":[{"BPL":5,"DataID":"6deacbc373e74e1794a7*****","HP":124,"HR":82,"IsArr":-1,"LP":82,"LastChangeTime":1373362486,"Lat":-1,"Lon":-1,"MDate":1373392309,"Note":""}],"BPUnit":0,"CurrentRecordCount":50,"NextPageUrl":"next-page-url","PageLength":50,"PageNumber":1,"PrevPageUrl":"","RecordCount":335}"""
				}
				return new Response(new MockedHttpURLConnection(data))
			}]

		IHealthDataService.poll(account1)

		assert Entry.count() == 2	// One for systolic & other for diastolic
		assert Entry.list()*.description == [TagUnitMap.BLOOD_PRESSURE_SYSTOLIC, TagUnitMap.BLOOD_PRESSURE_DIASTOLIC]
	}

	void testPollForSleep() {
		IHealthDataService.oauthService = [getIHealthResource: {token, url ->
				String data = "[]"
				if(url.contains("sleep.json")) {
					data = """{"CurrentRecordCount":50,"NextPageUrl":"","PageLength":50,"PageNumber":1,"PrevPageUrl":"","RecordCount":288,"SRDataList":[{"Awaken":5,"DataID":"6ca9e6f4c8c34e4d9a9eccc*****","EndTime":1372103460,"FellSleep":25,"HoursSlept":260,"Lat":28.584006583590064,"Lon":69.43493275457757,"Note":"","SleepEfficiency":92,"StartTime":1372085160}]}"""
				}
				return new Response(new MockedHttpURLConnection(data))
			}]

		IHealthDataService.poll(account1)

		assert Entry.count() == 3
		assert Entry.first().amount == (260 + 25 + 5)	// From above mocked data
		assert Entry.last().description == "sleep efficiency"
	}

	void testPollForWeight() {
		IHealthDataService.oauthService = [getIHealthResource: {token, url ->
				String data = "[]"
				if(url.contains("weight.json")) {
					data = """{"CurrentRecordCount":50,"NextPageUrl":"","PageLength":50,"PageNumber":1,"PrevPageUrl":"","RecordCount":272,"WeightDataList":[{"BMI":16.538315869208432,"BoneValue":2,"DCI":2,"DataID":"24abfb4d2a344f3aad571*****","FatValue":0.02,"LastChangeTime":1348879373,"MDate":1347460380,"MuscaleValue":2,"Note":"test900000000000000000","WaterValue":0.02,"WeightValue":52.4}],"WeightUnit":0}"""
				}
				return new Response(new MockedHttpURLConnection(data))
			}]

		IHealthDataService.poll(account1)

		assert Entry.count() == 1
	}

	void testPollWithAPI() {
		return
		IHealthDataService.poll(account1)
		assert Entry.count() != 0
	}

}