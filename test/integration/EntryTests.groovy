import static org.junit.Assert.*

import java.math.MathContext
import java.text.DateFormat

import org.junit.After
import org.junit.Before
import org.junit.Test

import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.model.Entry.TagStatsRecord
import us.wearecurio.services.DatabaseService

import org.joda.time.DateTimeZone
import org.junit.*
import grails.test.mixin.*
import us.wearecurio.utility.Utils

class EntryTests extends GroovyTestCase {
	static transactional = true

	DateFormat dateFormat
	Date earlyBaseDate
	Date earlyBaseDate2
	Date currentTime
	Date slightDifferentCurrentTime
	Date endTime
	String timeZone // simulated server time zone
	String timeZone2 // simulated server time zone
	TimeZoneId timeZoneId
	TimeZoneId timeZoneId2
	DateTimeZone dateTimeZone
	Date baseDate
	Date baseDate2
	Date tomorrowBaseDate
	Date tomorrowCurrentTime
	Date dayAfterTomorrowBaseDate
	Date dayAfterDayAfterTomorrowBaseDate
	Date lateBaseDate
	Date timeZone2BaseDate
	Date lateCurrentTime
	Date microlateCurrentTime
	Date veryLateBaseDate
	User user
	Long userId
	
	@Before
	void setUp() {
		Locale.setDefault(Locale.US)	// For to run test case in any country.
		TimeZoneId.clearCacheForTesting()
		
		def entryTimeZone = Utils.createTimeZone(-8 * 60 * 60, "GMTOFFSET8", true)
		timeZone = "America/Los_Angeles"
		timeZone2 = "America/New_York"
		timeZoneId = TimeZoneId.look(timeZone)
		timeZoneId2 = TimeZoneId.look(timeZone2)
		dateTimeZone = timeZoneId.toDateTimeZone()
		dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		dateFormat.setTimeZone(entryTimeZone)
		earlyBaseDate = dateFormat.parse("June 25, 2010 12:00 am")
		earlyBaseDate2 = dateFormat.parse("June 24, 2010 11:00 pm")
		currentTime = dateFormat.parse("July 1, 2010 3:30 pm")
		slightDifferentCurrentTime = dateFormat.parse("July 1, 2010 4:00 pm")
		lateCurrentTime = dateFormat.parse("July 3, 2010 3:30 pm")
		endTime = dateFormat.parse("July 1, 2010 5:00 pm")
		baseDate = dateFormat.parse("July 1, 2010 12:00 am")
		baseDate2 = dateFormat.parse("July 1, 2010 1:00 am") // switch time zone
		tomorrowBaseDate = dateFormat.parse("July 2, 2010 12:00 am")
		tomorrowCurrentTime = dateFormat.parse("July 2, 2010 2:15 pm")
		dayAfterTomorrowBaseDate = dateFormat.parse("July 3, 2010 12:00 am")
		dayAfterDayAfterTomorrowBaseDate = dateFormat.parse("July 4, 2010 12:00 am")
		lateBaseDate = dateFormat.parse("July 3, 2010 12:00 am")
		timeZone2BaseDate = dateFormat.parse("July 3, 2010 3:00 am")
		microlateCurrentTime = dateFormat.parse("July 3, 2010 1:58 pm")
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
	void testParse() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 slice", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet pinned", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:pinned, repeatType:768)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet repeat daily", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat daily, repeatType:1025)")
		
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet @4pm repeat weekly", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:1026)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet 4pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null)")

		// test assume recent time
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet 3:00", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet at 4:00 pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "mood seven 4pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin one tablet 4pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet at 16h00", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet at 16:00", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "9am aspirin 1 tablet repeat weekly", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:1026)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg @9h00 after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg at 9:00 after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg at 09h00 after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg at 9pm after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "craving sweets/starches/carbohydrates 3 at 9pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:craving sweets/starches/carbohydrates, amount:3.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		// decimal value
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "9am aspirin 1.2 mg repeat weekly", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.200000000, units:mg, amountPrecision:3, comment:repeat weekly, repeatType:1026)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "9am aspirin .7 mg repeat weekly", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:0.700000000, units:mg, amountPrecision:3, comment:repeat weekly, repeatType:1026)")

		// test metadata entry parsing

		entry = Entry.create(userId, Entry.parseMeta("fast caffeine metabolizer"), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:null, datePrecisionSecs:0, timeZoneName:Etc/UTC, description:fast caffeine metabolizer, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		// test odd tweets

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "what's going on???", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:what's going on???, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "snack#3: 2 bags of chips", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:snack#3, amount:2.000000000, units:bags, amountPrecision:3, comment:of chips, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "snack#3", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:snack#3, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "weight: 310#", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:weight, amount:310.000000000, units:#, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "weight: 310lbs", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:weight, amount:310.000000000, units:lbs, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "headache 8 at 4pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:headache, amount:8.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		// test current time for late base date

		entry = Entry.create(userId, Entry.parse(dateFormat.parse("July 1, 2010 6:00 pm"), timeZone, "just a tag", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:just a tag, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		// test time before amount

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "heart rate at 4pm 72bpm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "heart rate at 4pm = 72bpm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null)")

		// number in tag

		entry = Entry.create(userId, Entry.parse(dateFormat.parse("July 1, 2010 6:00 pm"), timeZone, "methyl b-12", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		// yes/no values

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 yes at 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12: yes at 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 no at 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:0.000000000, units:, amountPrecision:0, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 4pm yes", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 4pm: yes", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 yes we can at 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12 yes we can, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 yes we can 4 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12 yes we can, amount:4.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 yes we can 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12 yes we can, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		// none value
		
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 4pm: none", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:null, units:, amountPrecision:-1, comment:, repeatType:512)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 - 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:null, units:, amountPrecision:-1, comment:, repeatType:512)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 none we can at 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12 none we can, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		// test time parsing with unambiguous formats

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg 9pm after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg 9:00 after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "heart rate 4pm 72bpm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet repeat weekly at 9", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:1026)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg @12am after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T07:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg @12pm after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg @noon after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg at midnight after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T07:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

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
	
	int testPlot(User user, def tagIds, Date startDate, Date endDate, Date currentDate, String timeZoneName, Closure test) {
		def results = Entry.fetchPlotData(user, tagIds, startDate, endDate, currentDate, timeZoneName)
		
		int c = 0
		for (result in results) {
			test(result)
			++c
		}
		
		return c
	}
	
	@Test
	void testRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread repeat daily", earlyBaseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")

		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		}
		
		def activated = entry.activateGhostEntry(baseDate, lateCurrentTime, timeZone)
		def v = activated.valueString()
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
		
		def activated2 = entry.activateGhostEntry(baseDate, currentTime, timeZone2)
		v = activated2.valueString()
		assert activated2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/New_York, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
	}
	
	@Test
	void testRepeatChangeTimeZone() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", earlyBaseDate, true), null)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
		
		testEntries(user, timeZone, baseDate2, currentTime) {
			assert it.id == entry.getId()
			assert it.timeZoneName == "America/Los_Angeles"
		}
	}
	
	@Test
	void testRemindActivate() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread remind", earlyBaseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517)")
		
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		} == 1
		
		def activated = entry.activateGhostEntry(baseDate, lateCurrentTime, timeZone)
		def v = activated.valueString()
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:5)")

		assert testEntries(user, timeZone, veryLateBaseDate, lateCurrentTime) {
		} == 1

		def updated = Entry.update(activated, Entry.parse(lateCurrentTime, timeZone, "bread remind", baseDate, true, true), null, baseDate, true)
		
		assert updated == activated

		assert testEntries(user, timeZone, veryLateBaseDate, lateCurrentTime) {
		} == 1
	}
	
	@Test
	void testPlotData() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null)")
	
		assert testPlot(user, Tag.look("bread"), null, null, veryLateBaseDate, "America/Los_Angeles") {
			def date = Utils.dateToGMTString(it[0])
			assert date == "2010-07-01T22:30:00"
			assert it[1].intValue() == 1
			assert it[2] == "bread"
		} == 1
		
		assert testPlot(user, Tag.look("bread"), lateBaseDate, null, veryLateBaseDate, "America/Los_Angeles") {
		} == 0
		
		assert testPlot(user, Tag.look("bread"), earlyBaseDate, baseDate, veryLateBaseDate, "America/Los_Angeles") {
		} == 0
		
		assert testPlot(user, Tag.look("bread"), earlyBaseDate, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
		} == 1
	}
	
	@Test
	void testSumPlotData() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null)")
	
		Entry.create(userId, Entry.parse(slightDifferentCurrentTime, timeZone, "bread 3", baseDate, true), null)
		Entry.create(userId, Entry.parse(lateCurrentTime, timeZone, "bread 2", tomorrowBaseDate, true), null)
		
		def results = Entry.fetchSumPlotData(user, Tag.look("bread"), null, null, veryLateBaseDate, "America/Los_Angeles")
		
		def c = 0
		for (result in results) {
			def date = Utils.dateToGMTString(result[0])
			if (date == "2010-07-01T21:32:48") {
				assert result[1].intValue() == 4
				assert result[2] == "bread"
			} else if (date == "2010-07-02T21:32:48") {
				assert result[1].intValue() == 2
				assert result[2] == "bread"
			} else
				assert false
			++c
		}
		
		assert c == 2
		
		results = Entry.fetchSumPlotData(user, Tag.look("bread"), lateBaseDate, null, veryLateBaseDate, "America/Los_Angeles")
		
		c = 0
		for (result in results) {
			++c
		}
		
		assert c == 0
		
		results = Entry.fetchSumPlotData(user, Tag.look("bread"), earlyBaseDate, baseDate, veryLateBaseDate, "America/Los_Angeles")
		
		c = 0
		for (result in results) {
			++c
		}
		
		assert c == 0
		
		results = Entry.fetchSumPlotData(user, Tag.look("bread"), earlyBaseDate, lateBaseDate, veryLateBaseDate, "America/Los_Angeles")
		
		c = 0
		for (result in results) {
			++c
		}
		
		assert c == 2
	}
	
	@Test
	void testSumPlotDataNight() {
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1am", baseDate, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 3 2am", baseDate, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 3am", tomorrowBaseDate, true), null)
		
		def results = Entry.fetchSumPlotData(user, Tag.look("bread"), null, null, veryLateBaseDate, "America/Los_Angeles")
		
		def c = 0
		for (result in results) {
			if (c == 0) {
				assert result[1].intValue() == 4
			} else {
				assert result[1].intValue() == 2
			}
			++c
		}
	}
	
	@Test
	void testSumPlotDataAfternoon() {
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1pm", baseDate, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1pm", baseDate, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1pm", baseDate, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1pm", baseDate, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1pm", baseDate, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1pm", baseDate, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1pm", baseDate, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 3pm", baseDate, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 5am", tomorrowBaseDate, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 2pm", tomorrowBaseDate, true), null)
		
		def results = Entry.fetchSumPlotData(user, Tag.look("bread"), null, null, veryLateBaseDate, "America/Los_Angeles")
		
		def c = 0
		for (result in results) {
			if (c == 0) {
				assert result[1].intValue() == 8
			} else {
				assert result[1].intValue() == 2
			}
			++c
		}
		
		assert c == 2
	}
	
	@Test
	void testAverageTimeMidnight() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 12am", dayOne, true), null)
		
		def breadIds = [ Tag.look("bread").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert breadResult == 0
	}
	
	@Test
	void testAverageTimeOneAM() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 1am", dayOne, true), null)
		
		def breadIds = [ Tag.look("bread").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert breadResult == 3600
	}
	
	@Test
	void testAverageTimeNoon() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 12pm", dayOne, true), null)
		
		def breadIds = [ Tag.look("bread").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert breadResult == 43200
	}
	
	@Test
	void testAverageTimeNoonNoStartDateNoEndDate() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 12pm", dayOne, true), null)
		
		def breadIds = [ Tag.look("bread").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, null, null, currentDate, dateTimeZone)
		
		assert breadResult == 43200
	}
	
	@Test
	void testAverageTimeOnePM() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 1pm", dayOne, true), null)
		
		def breadIds = [ Tag.look("bread").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert breadResult == 46800
	}
	
	@Test
	void testAverageTimeAllPoints() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 6pm", dayOne, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 6am", dayOne, true), null)
		
		def breadIds = [ Tag.look("bread").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert breadResult == 43200 // when times zero out, it should use noon
	}
	
	@Test
	void testAverageTimeMorningTwoAfternoons() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 7am", dayOne, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 3pm", dayOne, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 5pm", dayOne, true), null)
		
		def breadIds = [ Tag.look("bread").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert breadResult == 50400
	}
	
	@Test
	void testAverageTime() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1am", dayOne, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 3 3am", dayOne, true), null)
		Entry repeatBreadEntry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 2am repeat", dayTwo, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 6 9am", dayThree, true), null)
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 1 1pm", dayOne, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 3 5pm", dayOne, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 2 7pm repeat", dayTwo, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 6 4pm", dayThree, true), null)
		
		def breadIds = [ Tag.look("bread").getId() ]
		def circusIds = [ Tag.look("circus").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		def circusResult = Entry.fetchAverageTime(user, circusIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert breadResult == 9519
		assert circusResult == 63215
	}
	
	@Test
	void testAverageTimeNoRepeats() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1am", dayOne, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 3 3am", dayOne, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 2am", dayTwo, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 6 9am", dayThree, true), null)
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 1 1pm", dayOne, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 3 5pm", dayOne, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 2 7pm", dayTwo, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 6 4pm", dayThree, true), null)
		
		def breadIds = [ Tag.look("bread").getId() ]
		def circusIds = [ Tag.look("circus").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		def circusResult = Entry.fetchAverageTime(user, circusIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert breadResult == 11968
		assert circusResult == 58651
	}
	
	@Test
	void testAverageTimeOnlyRepeats() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1am repeat", dayOne, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 3 3am repeat", dayOne, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 2am repeat", dayTwo, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 6 9am repeat", dayThree, true), null)
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 1 1pm repeat", dayOne, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 3 5pm repeat", dayOne, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 2 7pm repeat", dayTwo, true), null)
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 6 4pm repeat", dayThree, true), null)
		
		def breadIds = [ Tag.look("bread").getId() ]
		def circusIds = [ Tag.look("circus").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		def circusResult = Entry.fetchAverageTime(user, circusIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert breadResult == 9207
		assert circusResult == 58211
	}
	
	@Test
	void testRepeatPlotData() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 3pm repeat", earlyBaseDate, true), null)
	
		assert testPlot(user, Tag.look("bread"), null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
			def date = Utils.dateToGMTString(it[0])
			assert it[1].intValue() == 1
			assert it[2] == "bread"
		} == 8
	}
	
	@Test
	void testRemindActivatePlotData() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 3pm remind", earlyBaseDate, true), null)
	
		assert testPlot(user, Tag.look("bread"), null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
		} == 0
		
		entry.activateGhostEntry(earlyBaseDate, currentTime, "America/Los_Angeles")

		assert testPlot(user, Tag.look("bread"), null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
			def date = Utils.dateToGMTString(it[0])
			assert it[1].intValue() == 1
			assert it[2] == "bread"
		} == 8
	}
	
	@Test
	void testRepeatNullValuePlotData() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread - 3pm remind", earlyBaseDate, true), null)
	
		assert testPlot(user, Tag.look("bread"), null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
		} == 0
		
		def activated = entry.activateGhostEntry(earlyBaseDate, currentTime, "America/Los_Angeles")

		assert testPlot(user, Tag.look("bread"), null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
		} == 0

		def updated = Entry.update(activated, Entry.parse(currentTime, timeZone, "bread 6 3pm remind", earlyBaseDate, true, true), null, earlyBaseDate, true)
		
		assert testPlot(user, Tag.look("bread"), null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
		} == 8
	}
		
	void testUpdateNonRepeatToRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm ", baseDate, true), null)
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 pinned", baseDate, true, true), null, baseDate, true)
		
		assert updated == entry
		
		entry.getRepeatType().equals(Entry.RepeatType.CONTINUOUSGHOST)
	}
	
	@Test
	void testUpdateRepeatAllFuture() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat daily", earlyBaseDate, true), null)
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", baseDate, true, true), null, baseDate, true)
		
		assert updated != entry

		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id != entry.getId()
			assert it.amount.intValue() == 8
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify new event after baseDate
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
	}
	
	@Test
	void testUpdateRepeatJustOnce() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat daily", earlyBaseDate, true), null)
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", baseDate, true, true), null, baseDate, false)
		
		assert updated != entry && updated != null

		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify new event with old amount after baseDate
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
	}
	
	@Test
	void testUpdateRepeatJustTodayTwice() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat", earlyBaseDate, true), null)
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 at 2pm repeat", baseDate, true, true), null, baseDate, false)
		
		assert updated != entry && updated != null

		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify new event with old amount after baseDate
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
		} == 1

		def updated2 = Entry.update(updated, Entry.parse(currentTime, timeZone, "bread 7 at 2pm repeat", baseDate, true, true), null, baseDate, false)
		
		assert updated2 == updated

		// verify event edited on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == updated.getId()
			assert it['amount'].intValue() == 7
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify event with old amount after baseDate still there, by itself
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
	}
	
	@Test
	void testUpdateRepeatNotGhostJustOnce() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat daily", earlyBaseDate, true), null)
		
		entry.getRepeatType().unGhost()
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", baseDate, true, true), null, baseDate, false)
		
		assert updated != entry && updated != null
		
		Utils.save(entry, true)

		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify new event with old amount after baseDate
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
	}
	
	@Test
	void testUpdateRepeatNoChange() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat daily", earlyBaseDate, true), null)
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 5 at 2pm repeat daily", baseDate, true, true), null, baseDate, true)

		assert updated == entry && updated != null
		
		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify new event with old amount after baseDate
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
	}
	
	@Test
	void testUpdateRepeatOnFirstDayJustOnce() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat daily", earlyBaseDate, true), null)
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", earlyBaseDate, true, true), null, earlyBaseDate, false)

		assert updated == entry && updated != null
		
		// verify old event updated on earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
		
		// verify new event created after earlyBaseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
	}
	
	@Test
	void testUpdateRepeatOnFirstDayAllFuture() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat daily", earlyBaseDate, true), null)
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", earlyBaseDate, true, true), null, earlyBaseDate, true)

		assert updated == entry && updated != null
		
		// verify old event updated on earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
		
		// verify old event still at baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
	}
	
	@Test
	void testRepeatAgain() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), null)
		
		def entry4 = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5pm repeat daily", baseDate, true), null)
		
		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", tomorrowBaseDate, true), null)
		
		assert entry.getRepeatEnd().equals(entry2.fetchPreviousDate())
		
		Entry.delete(entry2, null)
		
		def entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", dayAfterTomorrowBaseDate, true), null)

		assert entry.getRepeatEnd().equals(entry2.fetchPreviousDate())
	}
	
	@Test
	void testLateTimestamps() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 11pm remind", baseDate, true), null)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-02T06:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517)")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 11pm", baseDate, true), null)
		v = entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T06:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")
	}
	
	@Test
	void testRepeatStart() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep start repeat", earlyBaseDate, true), null)
		assert entry.getDurationType().equals(Entry.DurationType.START)
	}
	
	@Test
	void testRepeatCreateInMiddle() {
		// test creating repeats in middle of earlier range
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread repeat daily", earlyBaseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == entry.getId()
		}
		
		def activated = entry.activateGhostEntry(lateBaseDate, lateCurrentTime, timeZone)
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-03T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
		
		// create repeat event in the middle
		def middleEntry = Entry.create(userId, Entry.parse(lateCurrentTime, timeZone, "bread repeat daily", baseDate, true), null)
		
		// make sure prior repeatEnd is reset
		def ere = entry.getRepeatEnd()
		assert entry.getRepeatEnd().equals(middleEntry.fetchPreviousDate())
		
		// delete middle event
		Entry.delete(middleEntry, null)		
	}
	
	@Test
	void testDeleteContinuousRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread pinned", baseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:pinned, repeatType:768)")
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
		}

		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id == entry.getId()
		}
		
		Entry.deleteGhost(entry, tomorrowBaseDate, true)
		
		assert entry.getRepeatEnd().equals(tomorrowBaseDate)
		
		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.identity != entry.getId()
		}
	}
	
	@Test
	void testDeleteTimedRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
		}
		
		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert (it.id == entry.getId())
		}
		
		Entry.deleteGhost(entry, tomorrowBaseDate, false)
		
		assert entry.getRepeatEnd().equals(entry.getDate())
		
		testEntries(user, timeZone, dayAfterTomorrowBaseDate, this.tomorrowCurrentTime) {
			def e2 = Entry.get(it.id)
			assert e2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-03T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
			assert e2.getId() != entry.getId()
		}
	}
	
	@Test
	void testDeleteTimedRepeatEndsToday() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")

		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		}

		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id == entry.getId()
		}
		
		Entry.deleteGhost(entry, tomorrowBaseDate, true)
		
		assert entry.getRepeatEnd().equals(tomorrowBaseDate)
		
		Entry.deleteGhost(entry, baseDate, false)
		
		assert entry.getUserId() == 0L
	}
	
	@Test
	void testDeleteTimedRepeatOnceAtBeginning() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), null)
		
		Date origDate = entry.getDate()
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		}

		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id == entry.getId()
		}
		
		Entry.deleteGhost(entry, baseDate, false)
		
		assert entry.getDate().equals(origDate + 1)
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert false // should have no entries any more at base date
		}
		
		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id == entry.getId()
		}		
	}
	
	@Test
	void testDeleteTimedRepeatTomorrow() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), null)
		
		Date origDate = entry.getDate()
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		}

		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id == entry.getId()
		}
		
		Entry.deleteGhost(entry, tomorrowBaseDate, false)
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		}
		
		testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert false // should have no entries tomorrow
		}
		
		boolean found = false
		testEntries(user, timeZone, dayAfterTomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id != entry.getId()
			found = true
		}
		
		assert found
	}
	
	@Test
	void testDeleteTimedRepeatDayAfterTomorrow() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), null)
		
		Entry.deleteGhost(entry, dayAfterTomorrowBaseDate, false)
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		}
		
		testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert it.id == entry.getId()
		}
		
		testEntries(user, timeZone, dayAfterTomorrowBaseDate, this.tomorrowCurrentTime) {
			assert false
		}
		
		boolean found = false
		testEntries(user, timeZone, dayAfterDayAfterTomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id != entry.getId()
			found = true
		}
		
		assert found
	}
	
	@Test
	void testDeleteTimedRepeatAll() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), null)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
		
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		} == 1

		assert testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id == entry.getId()
		} == 1
		
		Entry.deleteGhost(entry, tomorrowBaseDate, true)
		
		assert entry.getRepeatEnd().equals(tomorrowBaseDate)
		
		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id == entry.getId()
		}
	}
	
	@Test
	void testRepeatNonContinuous() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", earlyBaseDate, true), null)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
		}
		
		def activated = entry.activateGhostEntry(baseDate, lateCurrentTime, timeZone)
		v = activated.valueString()
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
		
		def activated2 = entry.activateGhostEntry(baseDate, currentTime, timeZone)
		v = activated2.valueString()
		assert activated2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")

		def activated3 = entry.activateGhostEntry(earlyBaseDate, currentTime, timeZone)
		v = activated3.valueString()
		assert activated3.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
		assert activated3 == entry
	}
	
	@Test
	void testOldRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread repeat daily", earlyBaseDate, true), null)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
		def entries = Entry.fetchListDataNoRepeats(user, baseDate)
		assert entries.size() == 0
		
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 3pm pinned", baseDate, true), null)
		entries = Entry.fetchListDataNoRepeats(user, baseDate)
		assert entries.size() == 0

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 3pm", baseDate, true), null)
		entries = Entry.fetchListDataNoRepeats(user, baseDate)
		assert entries.size() == 1
	}
	
	@Test
	void testRemind() {
		TagStatsRecord record = new TagStatsRecord()
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm remind", baseDate, true), record)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517)")

		// tag stats for reminders should be "none" value for the amount
		def tagStats = record.getOldTagStats()
		assert tagStats.getLastAmount() == null
		assert tagStats.getLastAmountPrecision() < 0
		
		// query for reminder should generate the ghost entry only
		testEntries(user, timeZone, lateBaseDate, lateCurrentTime) {
			assert it['id'] == entry.getId()
			assert Utils.dateToGMTString(it['date']).equals("2010-07-03T21:00:00")
		}
		
		// repeat should be here
		def entryIds = Entry.fetchReminders(user, microlateCurrentTime, 5 * 60)
		for (entryId in entryIds) {
			assert entryId['id'] == entry.getId()
		}
		
		// should be no repeats here
		entryIds = Entry.fetchReminders(user, microlateCurrentTime, 100L)
		for (entryId in entryIds) {
			assert false
		}
	}

	@Test
	void testRemindList() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 75 2pm remind", baseDate, true), null)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:75.000000000, units:, amountPrecision:3, comment:remind, repeatType:517)")
		
		testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert it.amount.intValue() == 75
			assert (it.repeatType & Entry.RepeatType.GHOST_BIT) != 0
		}

		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.amount.intValue() == 75
			assert (it.repeatType & Entry.RepeatType.GHOST_BIT) != 0
		}
	}

	@Test
	void testRemindList2() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm remind", baseDate, true), null)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517)")
		
		testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert it.amount.intValue() == 1
			assert (it.repeatType & Entry.RepeatType.GHOST_BIT) != 0
		}

		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.amount.intValue() == 1
			assert (it.repeatType & Entry.RepeatType.GHOST_BIT) != 0
		}
	}

	@Test
	void testRepeatParsing() {
		def res = Entry.parse(currentTime, timeZone, "bread repeat daily at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 1
		assert res['comment'].equals('repeat daily')
		assert res['repeatType'] == Entry.RepeatType.DAILYCONCRETEGHOST
		assert res['units'] == null

		res = Entry.parse(currentTime, timeZone, "bread repeat daily at 4pm foo", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 1
		assert res['comment'].equals('repeat daily foo')
		assert res['repeatType'] == Entry.RepeatType.DAILYCONCRETEGHOST
		assert res['units'] == null

		res = Entry.parse(currentTime, timeZone, "bread 8 slices repeat daily at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 8
		assert res['comment'].equals('repeat daily')
		assert res['repeatType'] == Entry.RepeatType.DAILYCONCRETEGHOST
		assert res['units'].equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices daily at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 8
		assert res['comment'].equals('daily')
		assert res['repeatType'] == Entry.RepeatType.DAILYCONCRETEGHOST
		assert res['units'].equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices repeat at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 8
		assert res['comment'].equals('repeat')
		assert res['repeatType'] == Entry.RepeatType.DAILYCONCRETEGHOST
		assert res['units'].equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices weekly at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 8
		assert res['comment'].equals('weekly')
		assert res['repeatType'] == Entry.RepeatType.WEEKLYCONCRETEGHOST
		assert res['units'].equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices repeat weekly at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 8
		assert res['comment'].equals('repeat weekly')
		assert res['repeatType'] == Entry.RepeatType.WEEKLYCONCRETEGHOST
		assert res['units'].equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread weekly", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 1
		assert res['comment'].equals('weekly')
		assert res['repeatType'] == Entry.RepeatType.WEEKLYCONCRETEGHOST
		assert res['units'] == null
		
		res = Entry.parse(currentTime, timeZone, "bread remind at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 1
		assert res['comment'].equals('remind')
		assert res['repeatType'] == Entry.RepeatType.REMINDDAILYGHOST
		assert res['units'] == null

		res = Entry.parse(currentTime, timeZone, "bread remind daily at 4pm foo", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 1
		assert res['comment'].equals('remind daily foo')
		assert res['repeatType'] == Entry.RepeatType.REMINDDAILYGHOST
		assert res['units'] == null

		res = Entry.parse(currentTime, timeZone, "bread 8 slices remind daily at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 8
		assert res['comment'].equals('remind daily')
		assert res['repeatType'] == Entry.RepeatType.REMINDDAILYGHOST
		assert res['units'].equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices remind at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 8
		assert res['comment'].equals('remind')
		assert res['repeatType'] == Entry.RepeatType.REMINDDAILYGHOST
		assert res['units'].equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread remind weekly", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 1
		assert res['comment'].equals('remind weekly')
		assert res['repeatType'] == Entry.RepeatType.REMINDWEEKLYGHOST
		assert res['units'] == null
		
		res = Entry.parse(currentTime, timeZone, "bread 8 slices remind weekly", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 8
		assert res['comment'].equals('remind weekly')
		assert res['repeatType'] == Entry.RepeatType.REMINDWEEKLYGHOST
		assert res['units'].equals('slices')
		
	}

	@Test
	void testUpdate() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		assert Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 1 slice", baseDate, true, true), null, baseDate, true) != null
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null)")

		assert Entry.update(entry, Entry.parse(currentTime, timeZone, "aspirin 1 tablet repeat daily", baseDate, true, true), null, baseDate, true) != null
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat daily, repeatType:1025)")
	}

	@Test
	void testUpdateDate() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		assert Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 1 slice", tomorrowBaseDate, true, true), null, baseDate, true) != null
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-02T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null)")
	}

	@Test
	void testUpdateRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread foo repeat daily", baseDate, true, false), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread foo, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
		println("Attempting to update")
		assert Entry.update(entry, Entry.parse(currentTime, timeZone, "bread repeat daily", baseDate, true, true), null, baseDate, true) != null
		
		testEntries(user, timeZone2, timeZone2BaseDate, lateCurrentTime) {
			assert it['id'] == entry.getId()
			def ds = Utils.dateToGMTString(it['date'])
			assert Utils.dateToGMTString(it['date']).equals("2010-07-03T19:00:00")
		}
	}

	@Test
	void testUpdateRepeatChangeTime() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread foo repeat daily", earlyBaseDate, true, false), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread foo, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
		println("Attempting to update")
		
		assert Entry.update(entry, Entry.parse(currentTime, timeZone, "headache 8 at 3pm repeat daily", earlyBaseDate, true, true), null, earlyBaseDate, true) != null
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == entry.getId()
			def ds = Utils.dateToGMTString(it['date'])
			assert Utils.dateToGMTString(it['date']).equals("2010-07-01T22:00:00")
		}
	}

	@Test
	void testUpdateRepeatChangeTimeZone() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread foo 2pm", earlyBaseDate, true, false), null)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread foo, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")
		println("Attempting to update")
		
		Entry.update(entry, Entry.parse(currentTime, timeZone2, "bread foo 3pm", earlyBaseDate2, true, true), null, baseDate, true)
		v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/New_York, description:bread foo, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")
	}

	@Test
	void testReplaceRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "exercise 12pm repeat daily", earlyBaseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:exercise, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1025)")
		println("Attempting to update")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "exercise 8 12pm repeat daily", baseDate, true), null)
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:exercise, amount:8.000000000, units:, amountPrecision:3, comment:repeat daily, repeatType:1025)")
		
		def repeatEnd = entry.getRepeatEnd()
		assert repeatEnd.equals(entry2.fetchPreviousDate())
	}
	
	@Test
	void testSleep() {
		Entry entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep at 3:30pm", baseDate, true), null)
		assert entry.getDurationType().equals(Entry.DurationType.START)

		Entry entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep start at 4pm", baseDate, true), null)
		assert entry2.getDurationType().equals(Entry.DurationType.START)
		
		Entry entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep 4 hours at 3:30pm", baseDate, true), null)
		assert entry3.getDurationType().equals(Entry.DurationType.NONE)

		Entry entry4 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep at 4pm start", baseDate, true), null)
		assert entry4.getDescription().equals("sleep start")
		assert entry4.getDurationType().equals(Entry.DurationType.START)

		entry4 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep at 4pm starts", baseDate, true), null)
		assert entry4.getDescription().equals("sleep starts")
		assert entry4.getDurationType().equals(Entry.DurationType.START)

		entry4 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep at 4pm starts (hola)", baseDate, true), null)
		assert entry4.getDescription().equals("sleep starts")
		assert entry4.getComment().equals("(hola)")
		assert entry4.getDurationType().equals(Entry.DurationType.START)

		entry4 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep at 4pm starts(hola)", baseDate, true), null)
		assert entry4.getDescription().equals("sleep starts")
		assert entry4.getComment().equals("(hola)")
		assert entry4.getDurationType().equals(Entry.DurationType.START)

		Entry entry5 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep at 4pm stop", baseDate, true), null)
		assert entry5.getDescription().equals("sleep stop")
		assert entry5.getDurationType().equals(Entry.DurationType.END)

		Entry entry6 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep at 4pm end", baseDate, true), null)
		assert entry6.getDescription().equals("sleep end")
		assert entry6.getDurationType().equals(Entry.DurationType.END)
	}

	@Test
	void testDuration() {
		// test creation of one side of a duration pair
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		Entry entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 3:31pm", baseDate, true), null)
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.016667000, units:hours, amountPrecision:3, comment:, repeatType:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)
		assert entry2.fetchDurationEntry().fetchIsGenerated()
		
		// test editing the start time of the duration pair
		println("== Test update start entry ==")

		Entry.update(entry, Entry.parse(currentTime, timeZone, "testxyz start at 3:01pm", baseDate, true, true), null, baseDate, true)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:01:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry.fetchEndEntry().fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test creation of new start of a duration pair
		println("== Test creation of new start entry in between start and end entry ==")

		Entry entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:16pm", baseDate, true), null)
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:16:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry3.fetchEndEntry().equals(entry2)
		assert entry.fetchEndEntry() == null
		assert entry2.fetchStartEntry().equals(entry3)
		
		// test deletion of start end of a duration pair
		println("== Test deletion of start entry from duration pair ==")
		
		Entry.delete(entry3, null)

		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test creation of new end after the duration pair
		println("== Test creation of new end after the duration pair ==")
		
		Entry entry4 = Entry.create(userId, Entry.parse(endTime, timeZone, "testxyz end at 4:31pm", baseDate, true), null)
		assert entry4.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry4.fetchStartEntry() == null
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test deletion of start of duration pair
		println("== Test deletion of start entry from duration pair ==")
		
		Entry.delete(entry2, null)
		
		assert entry.fetchEndEntry().equals(entry4)
		assert entry4.fetchStartEntry().equals(entry)
		
		assert entry4.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.500000000, units:hours, amountPrecision:3, comment:, repeatType:null)")
	}

	@Test
	void testSleepWake() {
		// test creation of one side of a duration pair
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep 3:30pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		Entry entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "wake 3:31pm", baseDate, true), null)
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:wake, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:0.016667000, units:hours, amountPrecision:3, comment:, repeatType:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)
		assert entry2.fetchDurationEntry().fetchIsGenerated()
		
		// test editing the start time of the duration pair
		println("== Test update start entry ==")

		Entry.update(entry, Entry.parse(currentTime, timeZone, "sleep at 3:01pm", baseDate, true, true), null, baseDate, true)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:01:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry.fetchEndEntry().fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test creation of new start of a duration pair
		println("== Test creation of new start entry in between start and end entry ==")

		Entry entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep start at 3:16pm", baseDate, true), null)
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:16:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry3.fetchEndEntry().equals(entry2)
		assert entry.fetchEndEntry() == null
		assert entry2.fetchStartEntry().equals(entry3)
		
		// test deletion of start end of a duration pair
		println("== Test deletion of start entry from duration pair ==")
		
		Entry.delete(entry3, null)

		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test creation of new end after the duration pair
		println("== Test creation of new end after the duration pair ==")
		
		Entry entry4 = Entry.create(userId, Entry.parse(endTime, timeZone, "sleep end at 4:31pm", baseDate, true), null)
		assert entry4.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry4.fetchStartEntry() == null
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test deletion of start of duration pair
		println("== Test deletion of start entry from duration pair ==")
		
		Entry.delete(entry2, null)
		
		assert entry.fetchEndEntry().equals(entry4)
		assert entry4.fetchStartEntry().equals(entry)
		
		assert entry4.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:1.500000000, units:hours, amountPrecision:3, comment:, repeatType:null)")
	}

	/**
	 * Test creating an interleaved duration pair: create a duration pair, then create another start before the
	 * pair and a new end in the middle of the first pair
	 */
	@Test
	void testInterleavedDuration() {
		// test creation of one side of a duration pair
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")
		assert entry.fetchEndEntry() == null
		
		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 4pm", baseDate, true), null)
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry2.fetchStartEntry() == entry
		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null)")

		// test creation of new start of a duration pair
		println("== Test creation of new start entry before start and end entry ==")

		def entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3pm", baseDate, true), null)
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry3.fetchEndEntry() == null
		assert entry2.fetchStartEntry() == entry
		assert entry.fetchEndEntry() == entry2

		// test creation of new end in between first duration pair
		println("== Test creation of new end in between first duration pair ==")
		
		def entry4 = Entry.create(userId, Entry.parse(endTime, timeZone, "testxyz end at 3:45pm", baseDate, true), null)
		assert entry4.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:45:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		
		assert entry4.fetchStartEntry() == entry
		assert entry.fetchEndEntry() == entry4
		println entry4.fetchDurationEntry().valueString()
		
		assert entry2.fetchStartEntry() == null
		assert entry2.fetchDurationEntry() == null
	}

	/**
	 * Test creating an interleaved duration pair: create a duration pair, then create another start in the
	 * middle of the interleaved pair
	 */
	@Test
	void testInterleavedDuration2() {
		// test creation of one side of a duration pair
		println("== Test creation of start entry ==")
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry.fetchEndEntry() == null
		
		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 4pm", baseDate, true), null)
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchStartEntry() == entry
		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null)")

		// test creation of new start of a duration pair
		println("== Test creation of new start entry in between first duration pair ==")

		def entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:45pm", baseDate, true), null)
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:45:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry.fetchEndEntry() == null
		
		assert entry3.fetchEndEntry() == entry2
		assert entry2.fetchStartEntry() == entry3
	}

	/**
	 * Test creating an interleaved duration pair: create a duration pair, then create another start in the
	 * middle of the interleaved pair
	 */
	@Test
	void testOrphanDurationEnd() {
		// test creation of one side of a duration pair
		println("== Test creation of start entry ==")
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry.fetchEndEntry() == null
		
		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 4pm", baseDate, true), null)
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchStartEntry() == entry
		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null)")

		// test creation of orphan end of duration pair
		println("== Test creation of new end entry after first duration pair ==")

		def entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 4:30pm", baseDate, true), null)
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry3.fetchDurationEntry() == null
		assert entry3.fetchStartEntry() == null
		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchStartEntry() == entry
		
		// test deletion of start of duration pair
		
		println("== Test deletion of middle end entry ==")
		
		Entry.delete(entry2, null)
		
		Entry newDurationEntry = entry.fetchEndEntry().fetchDurationEntry()
				
		assert newDurationEntry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:hours, amountPrecision:3, comment:, repeatType:null)")
		assert entry.fetchEndEntry() == entry3
		assert entry3.fetchStartEntry() == entry
	}
	
	static final MathContext mc = new MathContext(9)
	
	@Test
	void testTagStats() {
		def now = new Date()
		def yesterday = new Date(now.getTime() - 1000L * 60 * 60 * 24)
		def twoMonthsAgo = new Date(now.getTime() - 1000L * 60 * 60 * 24 * 60)
		def nineMonthsAgo = new Date(now.getTime() - 1000L * 60 * 60 * 24 * 9 * 30)
		def twoYearsAgo = new Date(now.getTime() - 1000L * 60 * 60 * 24 * 24 * 30)
		
		def entry = Entry.create(userId, Entry.parse(now, timeZone, "fooxyz 3 tablet", now, true), null)

		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "fooxyz 1 toblet", yesterday, true), null)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "fooxyz 1 tablet", twoMonthsAgo, true), null)

		entry = Entry.create(userId, Entry.parse(nineMonthsAgo, timeZone, "fooxyz 1 tablet", nineMonthsAgo, true), null)

		entry = Entry.create(userId, Entry.parse(twoYearsAgo, timeZone, "fooxyz 1 tablet", twoYearsAgo, true), null)

		entry = Entry.create(userId, Entry.parse(now, timeZone, "foamfoam 1 mg", now, true), null)

		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "foamfoam 1.5 mg", yesterday, true), null)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "foamfoam 1 mg", yesterday, true), null)
		
		entry = Entry.create(userId, Entry.parse(now, timeZone, "blee 1 mg", now, true), null)

		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "blee 1.5 mg", yesterday, true), null)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "blee 3 mg", yesterday, true), null)
		
		entry = Entry.create(userId, Entry.parse(now, timeZone, "whaa 24", now, true), null)

		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "whaa 14", yesterday, true), null)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "whaa 11", yesterday, true), null)
		
		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "gor", yesterday, true), null)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "gor", yesterday, true), null)
		
		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "gor 12", yesterday, true), null)

		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "whoa 1 gee repeat daily", yesterday, true), null)

		def tag = Tag.look("fooxyz")
		
		TagStats result = TagStats.createOrUpdate(userId, tag.getId())
		
		println "Usage of fooxyz: " + result.getMostRecentUsage()

		assert (result.getMostRecentUsage().getTime() / 1000L).longValue() == (now.getTime() / 1000L).longValue()
		
		assert (result.getThirdMostRecentUsage().getTime() / 1000L).longValue() == (twoMonthsAgo.getTime() / 1000L).longValue()
		
		assert result.getCountLastThreeMonths() == 3
		
		assert result.getCountLastYear() == 4
		
		assert result.getCountAllTime() == 5
		
		assert result.getLastAmount().intValue() == 3
		
		assert result.getLastAmountPrecision() == 3
		
		assert result.getLastUnits().equals("tablet")
		
		def tag2 = Tag.look("foamfoam")
		
		TagStats result2 = TagStats.createOrUpdate(userId, tag2.getId())
		
		println "Usage of foamfoam: " + result2.getMostRecentUsage()

		assert result2.getLastAmount().intValue() == 1
		
		assert result2.getLastAmountPrecision() > 0
		
		assert result2.getLastUnits().equals("mg")
		
		def tag3 = Tag.look("blee")
		
		TagStats result3 = TagStats.createOrUpdate(userId, tag3.getId())
		
		println "Usage of blee: " + result2.getMostRecentUsage()

		assert result3.getLastAmount() == null
		
		assert result3.getLastAmountPrecision() < 0
		
		assert result3.getLastUnits().equals("mg")
		
		def tag4 = Tag.look("whaa")
		
		TagStats result4 = TagStats.createOrUpdate(userId, tag4.getId())
		
		println "Usage of whaa: " + result2.getMostRecentUsage()

		assert result4.getLastAmount() == null
		
		assert result4.getLastAmountPrecision() < 0
		
		assert result4.getLastUnits().equals("")
		
		tag4 = Tag.look("gor")
		
		result4 = TagStats.createOrUpdate(userId, tag4.getId())
		
		println "Usage of gor: " + result2.getMostRecentUsage()

		assert result4.getLastAmount().intValue() == 1
		
		assert result4.getLastAmountPrecision() < 0
		
		assert result4.getLastUnits().equals("")
		
		TagStatsRecord record = new TagStatsRecord()
		
		Entry.delete(entry, record)
		
		result = record.getOldTagStats()
		
		assert result.getMostRecentUsage() == null
		
		assert result.getThirdMostRecentUsage() == null
		
		assert result.getCountLastThreeMonths() == 0
		
		assert result.getCountLastYear() == 0
		
		assert result.getCountAllTime() == 0
		
		assert result.getLastAmount() == null
		
		assert result.getLastAmountPrecision() == -1
		
		assert result.getLastUnits().equals("")
		
	}
}
