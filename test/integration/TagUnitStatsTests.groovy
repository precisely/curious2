import static org.junit.Assert.*

import us.wearecurio.model.TagUnitStats

import java.math.MathContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date;

import org.junit.After
import org.junit.Before
import org.junit.Test

import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.TimeZoneId;
import us.wearecurio.model.User
import us.wearecurio.model.Entry.TagStatsRecord
import us.wearecurio.services.DatabaseService

import org.joda.time.DateTimeZone
import org.junit.*
import grails.test.mixin.*
import us.wearecurio.utility.Utils

class TagUnitStatsTests extends GroovyTestCase {
	static transactional = true
	
	User user
	Long userId
	def dateFormat
	Date earlyBaseDate
	Date currentTime
	String timeZone // simulated server time zone
	String timeZone2 // simulated server time zone
	Date yesterdayBaseDate
	Date baseDate
	Date tomorrowBaseDate
	Date lateBaseDate
	Date veryLateBaseDate

	@Before
	void setUp() {
		Locale.setDefault(Locale.US)	// For to run test case in any country.
		TimeZoneId.clearCacheForTesting()
		
		def entryTimeZone = Utils.createTimeZone(-8 * 60 * 60, "GMTOFFSET8", true)
		timeZone = "America/Los_Angeles"
		timeZone2 = "America/New_York"
		dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		dateFormat.setTimeZone(entryTimeZone)
		earlyBaseDate = dateFormat.parse("June 25, 2010 12:00 am")
		currentTime = dateFormat.parse("July 1, 2010 3:30 pm")
		yesterdayBaseDate = dateFormat.parse("June 30, 2010 12:00 am")
		baseDate = dateFormat.parse("July 1, 2010 12:00 am")
		tomorrowBaseDate = dateFormat.parse("July 2, 2010 12:00 am")
		lateBaseDate = dateFormat.parse("July 3, 2010 12:00 am")
		veryLateBaseDate = dateFormat.parse("July 20, 2010 12:00 am")
		
		def params = [username:'y', sex:'F', \
			last:'y', email:'y@y.com', birthdate:'01/01/2001', \
			first:'y', password:'y', action:'doregister', \
			controller:'home']

		user = User.create(params)

		Utils.save(user, true)
		println "new user " + user
		
		userId = user.getId()
	}

	@After
	void tearDown() {
	}

	@Test
	void testEntryUnits() {
		/*def entry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 1 1pm", yesterdayBaseDate, true), null)
		//No units used when no usage history present
		assert entry.units == ''
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 3 km 3pm", yesterdayBaseDate, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 5 km 3pm", yesterdayBaseDate, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 4 kilometer 4pm", baseDate, true), null)
		//Using the most used unit when no unit is present
		entry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 5 4pm", baseDate, true), null)
		assert entry.units == 'km'	
		//Using the most used unit incase of spelling mistake
		entry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 4 kkk 4pm", baseDate, true), null)
		assert entry.units == 'km'	*/
	}
	
	@Test
	void testMultiTagUnits() {
		def z = TagUnitStats.mostUsedTagUnitStatsForTags([Tag.look("jog").getId(), Tag.look("run").getId()], userId)
		z = z
		def entry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 1 1pm", yesterdayBaseDate, true), null)
		//No units used when no usage history present
		z = TagUnitStats.mostUsedTagUnitStatsForTags([Tag.look("jog").getId(), Tag.look("run").getId()], userId)
		z = z
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 3 km 3pm", yesterdayBaseDate, true), null)
		z = TagUnitStats.mostUsedTagUnitStatsForTags([Tag.look("jog").getId(), Tag.look("run").getId()], userId)
		z = z
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 5 km 3pm", yesterdayBaseDate, true), null)
		z = TagUnitStats.mostUsedTagUnitStatsForTags([Tag.look("jog").getId(), Tag.look("run").getId()], userId)
		z = z
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 4 kilometer 4pm", baseDate, true), null)
		//Using the most used unit when no unit is present
		z = TagUnitStats.mostUsedTagUnitStatsForTags([Tag.look("jog").getId(), Tag.look("run").getId()], userId)
		z = z
		entry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 5 feet 4pm", baseDate, true), null)
		//Using the most used unit incase of spelling mistake
		
		entry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 5 minutes 4pm", baseDate, true), null)
		//Using the most used unit incase of spelling mistake
		
		z = TagUnitStats.mostUsedTagUnitStatsForTags([Tag.look("jog").getId(), Tag.look("run").getId()], userId)
		z = z
		entry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "run 5 miles 4pm", baseDate, true), null)

		z = TagUnitStats.mostUsedTagUnitStatsForTags([Tag.look("jog").getId(), Tag.look("run").getId()], userId)
		z = z
		
		entry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "run 5 lbs 4pm", baseDate, true), null)

		z = TagUnitStats.mostUsedTagUnitStatsForTags([Tag.look("jog").getId(), Tag.look("run").getId()], userId)
		assert z.unit == 'km'
		
		entry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "run 5 miles 5pm", baseDate, true), null)
		entry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "run 2 miles 7pm", baseDate, true), null)
		entry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "run 2 miles 8pm", baseDate, true), null)
		entry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "run 2 miles 2pm", baseDate, true), null)
		entry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "run 2 miles 1pm", baseDate, true), null)
		entry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "run 2 miles 3pm", baseDate, true), null)
		
		z = TagUnitStats.mostUsedTagUnitStatsForTags([Tag.look("jog").getId(), Tag.look("run").getId()], userId)
		assert z.unit == 'miles'

		z = TagUnitStats.mostUsedTagUnitStatsForTags([Tag.look("jog").getId()], userId)
		assert z.unit == 'km'

	}
}
