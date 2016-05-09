package us.wearecurio.services.integration

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.scribe.model.Response
import us.wearecurio.data.UnitGroupMap
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.*
import us.wearecurio.services.MovesDataService
import us.wearecurio.services.DataService.DataRequestContext
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.TagUnitMap
import us.wearecurio.utility.Utils

import java.math.RoundingMode

class MovesDataServiceTests extends CuriousServiceTestCase {
	static transactional = true

	User user2
	OAuthAccount account

	MovesDataService movesDataService

	@Before
	void setUp() {
		super.setUp()

		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", name: "Mark Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true, hash: new DefaultHashIDGenerator().generate(12)])
		assert Utils.save(user2, true)

		account = new OAuthAccount([typeId: ThirdParty.MOVES, userId: userId, accessToken: "Z14DRUTWswu66GuptWqQR1b295DikZY77Bfwocqaduku9VKI2t0WTuOJQ7F72DSQ",
			accessSecret: "6b76f2ebd6e16b5bb5e1672d421241e4d9d1ce37122532f68b30dd735098", accountId: "65828076742279775", timeZoneId: TimeZoneId.look("America/Los_Angeles").id])
		assert Utils.save(account, true)
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	int testEntries(User user, String timeZoneName, Date baseDate, Date currentTime, Closure test) {
		def list = Entry.fetchListData(user, timeZoneName, baseDate, currentTime)

		int c = 0
		for (record in list) {
			test(record)
			++c
		}

		return c
	}

	@Test
	void testNormalizedData() {
		/**
		 * There are 5 activities in the mocked response, out of which four are valid & one is of type "trp" (invalid).
		 * Out of 4 valid entries, three of them having calories & others not. So according to code, 6 entries for each
		 * activity having calories & 5 entry for activity not having calories will be created. Total 23 entries will be created.
		 *
		 * @see this in http://jsoneditoronline.org/index.html
		 */

		String parsedResponse = """[{"date":"20121212","segments":[{"type":"move","startTime":"20121212T071430-0800","endTime":"20121212T074617-0800","activities":[{"activity":"running","startTime":"20121212T071430-0800","endTime":"20121212T072732-0800","duration":782,"distance":1251,"steps":1353}]},{"type":"place","startTime":"20121212T074617-0800","endTime":"20121212T100051-0800","activities":[{"activity":"walking","startTime":"20121212T074804-0800","endTime":"20121212T075234-0800","duration":270,"distance":227,"steps":303,"calories":99}]},{"type":"move","startTime":"20121212T100051-0800","endTime":"20121212T100715-0800","activities":[{"activity":"walking","startTime":"20121212T100051-0800","endTime":"20121212T100715-0800","duration":384,"distance":421,"steps":488,"calories":99}]},{"type":"move","startTime":"20121212T153638-0800","endTime":"20121212T160744-0800","activities":[{"activity":"transport","startTime":"20121212T153638-0800","endTime":"20121212T155321-0800","duration":1003,"distance":8058},{"activity":"cycling","startTime":"20121212T155322-0800","endTime":"20121212T160744-0800","duration":862,"distance":1086,"steps":1257,"calories":99}]}]}]"""

		movesDataService.oauthService = [getMovesResource: {token, url, param, header ->
				return new Response(new MockedHttpURLConnection(parsedResponse))
			}]

		Map response = movesDataService.getDataDefault(account, null, null, false, new DataRequestContext())
		assert response.success == true
		Collection entries = Entry.findAllByUserId(user.getId())
		Date date = null
		for (def entry in entries) {
			if (entry.getAmount().intValue() == 1353) {
				assert entry.toString().contains("datePrecisionSecs:180, timeZoneName:America/Los_Angeles, baseTag:run, description:run [steps], amount:1353.000000000, units:steps, amountPrecision:3, comment:(Moves), repeatType:null, repeatEnd:null")
			}
			if (date == null || entry.date < date)
				date = entry.date
		}
		assert Entry.count() == 15

		// Ensuring entries of the same day will be replaced with new entries.
		response = movesDataService.getDataDefault(account, null, null, false, new DataRequestContext())
		assert response.success == true

		testEntries(user, "America/Los_Angeles", date, date) {
			def i = it
			//assert !it.normalizedAmounts[0].sum
			i = i
		}
	}

	@Test
	void testMergedData() {
		String parsedResponse = new File("./test/integration/test-files/moves/default-data-1.json").text

		movesDataService.oauthService = [getMovesResource: {token, url, param, header ->
			return new Response(new MockedHttpURLConnection(parsedResponse))
		}]

		Map response = movesDataService.getDataDefault(account, null, null, false, new DataRequestContext())
		assert response.success == true
		List<Entry> entries = Entry.findAllByUserId(user.getId(), [sort: "id", order: "asc"])

		assert Entry.count() == 21

		/*
		 * Constructed this list of final values from the file "./test/integration/test-files/moves/default-data-1.json"
		 * which should be finally constructed as entries.
		 *
		 * These values are written in the order of Entries being created.
		 */
		List<Map> groupedAmounts = [
				[distance: 94, duration: 189],
				[calories: 120, duration: 241],
				[distance: 43, duration: 38],
				[steps: 1740, distance: 1337, duration: 2119],		// Merged entry
				[steps: 978, distance: 734, duration: 801],			// Merged entry
				[steps: 377, distance: 295, duration: 298],			// Merged entry
				[steps: 2304, distance: 1942, duration: 2201],		// Merged entry
				[steps: 91, distance: 45, duration: 61]
		]

		int index = 0
		UnitGroupMap theMap = UnitGroupMap.fetchTheMap()
		groupedAmounts.each { data ->
			data.each { key, amount ->
				Entry entry = entries[index]

				if (key == "distance") {
					BigDecimal ratio = theMap.fetchConversionRatio("meter", "miles")
					assert entry.amount.setScale(2, RoundingMode.HALF_UP) == TagUnitMap.convert(amount, ratio).setScale(2, RoundingMode.HALF_UP)
					assert entry.units == "miles"
					assert entry.tag.description == "walk [distance]"
				} else if (key == "duration") {
					if (amount >= 60) {
						BigDecimal ratio = theMap.fetchConversionRatio("seconds", "minutes")
						assert entry.amount.setScale(2, RoundingMode.HALF_UP) == TagUnitMap.convert(amount, ratio).setScale(2, RoundingMode.HALF_UP)
						assert entry.units == "mins"
					} else {
						assert entry.amount == amount
						assert entry.units == "secs"
					}
					assert entry.tag.description == "walk [time]"
				} else if (key == "steps") {
					assert entry.amount == amount
					assert entry.units == "steps"
					assert entry.tag.description == "walk [steps]"
				} else if (key == "calories") {
					assert entry.amount == amount
					assert entry.units == "kcal"
					assert entry.tag.description == "walk [calories]"
				}
				index++
			}
		}
	}

	@Test
	void "Test for records with start and end time format yyyyMMdd'T'HHmmss'Z'"() {
		/**
		 * All activities in the mocked response have end date and start date in the format yyyyMMdd'T'HHmmss'Z'
		 */

		String parsedResponse = new File("./test/integration/test-files/moves/default-data-2.json").text

		movesDataService.oauthService = [getMovesResource: {token, url, param, header ->
				return new Response(new MockedHttpURLConnection(parsedResponse))
			}]

		Map response = movesDataService.getDataDefault(account, null, null, false, new DataRequestContext())
		assert response.success == true
		Collection entries = Entry.findAllByUserId(user.getId())
		assert Entry.count() == 42
		assert entries[0].amount == 81.000000000
		assert entries[0].tag.description == "walk [steps]"
	}
}
