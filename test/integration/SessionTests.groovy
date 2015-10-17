import java.text.DateFormat
import java.util.Date;

import grails.test.*
import us.wearecurio.server.Session
import us.wearecurio.services.DatabaseService
import us.wearecurio.integration.CuriousTestCase;
import us.wearecurio.model.User
import us.wearecurio.utility.Utils
import static org.junit.Assert.*

import org.junit.*

import grails.test.mixin.*

class SessionTests extends CuriousTestCase {
	static transactional = true

	User user1
	User user2
	Date longAgo
	Date currentTime
	Date laterTime
	Date futureTime
	DatabaseService databaseService
	
	@Before
	void setUp() {
		super.setUp()
		
		def entryTimeZone = Utils.createTimeZone(-8 * 60 * 60, "GMTOFFSET8", true)
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		dateFormat.setTimeZone(entryTimeZone)
		longAgo = dateFormat.parse("July 1, 2009 3:30 pm")
		currentTime = dateFormat.parse("July 1, 2015 3:30 pm")
		laterTime = dateFormat.parse("July 1, 2015 3:40 pm")
		futureTime = dateFormat.parse("July 1, 2020 3:40 pm")
		
		Map params = new HashMap()
		params.put("username", "testuser")
		params.put("email", "test@test.com")
		params.put("password", "eiajrvaer")
		params.put("first", "first")
		params.put("last", "last")
		params.put("sex", "F")
		params.put("location", "New York, NY")
		params.put("birthdate", "12/1/1990")

		user1 = User.create(params)
		Utils.save(user1, true)

		params = new HashMap()
		params.put("username", "testuser2")
		params.put("email", "test2@test.com")
		params.put("password", "eiajrvaer")
		params.put("first", "first2")
		params.put("last", "last2")
		params.put("sex", "M")
		params.put("location", "New York, NY")
		params.put("birthdate", "12/1/1991")

		user2 = User.create(params)
		Utils.save(user2, true)
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testCreateNewSession() {
		def session = Session.createOrGetSession(user1, currentTime)
		assert session != null
		assert (session.fetchUuid() != null) && session.fetchUuid().length() > 10 && session.fetchUuid().length() < 50

		def session2 = Session.createOrGetSession(user2, currentTime)
		assert session2 != null
		assert (session2.fetchUuid() != null) && session2.fetchUuid().length() > 10 && session2.fetchUuid().length() < 50
		assert session.fetchUuid() != session2.fetchUuid()

		def lookupUser = Session.lookupSessionUser(session.fetchUuid(), laterTime)
		assert lookupUser.getId() == session.getUserId()
	}
	
	@Test
	void testExpireSession() {
		def session = Session.createOrGetSession(user1, currentTime)
		assert session != null
		assert (session.fetchUuid() != null) && session.fetchUuid().length() > 10 && session.fetchUuid().length() < 50

		User lookupUserFuture = Session.lookupSessionUser(session.fetchUuid(), futureTime)
		assert lookupUserFuture == null
	}
	
	
	@Test
	void testCleanupSession() {
		def session = Session.createOrGetSession(user2, longAgo)
		long sessionId = session.id
		assert session != null
		assert (session.fetchUuid() != null) && session.fetchUuid().length() > 10 && session.fetchUuid().length() < 50

		def results = databaseService.sqlRows("select * from session where session.id = :sessionId", [sessionId:sessionId])

		assert results.size() == 1
		
		Session.deleteStale()
		
		results = databaseService.sqlRows("select * from session where session.id = :sessionId", [sessionId:sessionId])

		assert !results
	}
}
