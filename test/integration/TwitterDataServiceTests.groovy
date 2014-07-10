import grails.test.*

import us.wearecurio.integration.CuriousTestCase;
import us.wearecurio.model.*
import us.wearecurio.services.TwitterDataService

import us.wearecurio.utility.Utils

import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.ResponseList
import twitter4j.DirectMessage
import twitter4j.auth.RequestToken
import twitter4j.auth.AccessToken
import twitter4j.Status
import twitter4j.Paging

import grails.util.GrailsUtil

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

class TwitterDataServiceTests extends CuriousTestCase {
	static transactional = true

	TwitterDataService twitterDataService

	@Before
	void setUp() {
		super.setUp()
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testTweetEntry() {
		return; // disable this test for now, as we've disabled the twitter data service polling
		
		if (!GrailsUtil.environment.equals('test'))
			return; // only run in test environment

		Twitter twitter = new TwitterFactory().getInstance()
		twitter.setOAuthConsumer("yF321ZO2dVOHz2We7Y2Edw", "1GLW8TUO8NzWsoVHqI7zRu9ZSjuzpXZEmfLTq6Wv9U")
		twitter.setOAuthAccessToken(new AccessToken("327824932-ho9VCIyzH3icfIT8SjvAOqFpl8eNO75qjuDSwVzX", "h9J4thJB1Y0lGIHIFvcr2VY8CKLjzxk2nAi0DPgapU"))

		Map params = new HashMap()
		params.put("username", "twitterTestUserXYZ")
		params.put("email", "twitterTestUserXYZ@test.com")
		params.put("password", "eiajrvaer")
		params.put("first", "first")
		params.put("last", "last")
		params.put("sex", "F")
		params.put("location", "New York, NY")
		params.put("birthdate", "12/1/1990")

		def user = User.create(params)

		assertTrue(Utils.save(user, true))

		user.setTwitterAccountName("testcurious")

		assertTrue(Utils.save(user, true))

		int r = new Random().nextInt()
		r = r < 0 ? -r : r

		twitter.sendDirectMessage("trkcs", "9am ran 2 miles comment " + r)

		Utils.save(user, true)

		Date dayAgo = new Date(new Date().getTime() - 24L * 60L * 60000L)

		boolean found = false

		while (!found) {
			try {
				twitterDataService.poll()
			} catch (Throwable t) {
				t.printStackTrace()
				fail()
				return
			}

			def results = Entry.findAllByUseridAndDateGreaterThan(user.getId(), dayAgo)

			for (Entry result: results) {
				println "Searching " + result + " trying to match " + r
				if (result.getDescription().equals("ran") && (result.getAmount().intValue() == 2) \
						&& result.getDateString().endsWith("T08:00:00") \
						&& result.getComment().equals("comment " + r))
					found = true
			}

			if (!found) Thread.sleep(1000)
		}

		assertTrue(found)
	}
}
