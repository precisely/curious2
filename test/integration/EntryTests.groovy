import java.math.MathContext;
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone
import us.wearecurio.model.User
import us.wearecurio.utility.Utils
import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.Entry.TagStatsRecord

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*


class EntryTests extends GroovyTestCase {
	static transactional = true

	DateFormat dateFormat
	Date earlyBaseDate
	Date currentTime
	Date endTime
	TimeZone timeZone // simulated server time zone
	Date baseDate
	Date tomorrowBaseDate
	Date tomorrowCurrentTime
	Date dayAfterTomorrowBaseDate
	Date lateBaseDate
	Date lateCurrentTime
	Date microlateCurrentTime
	User user
	Long userId
	
	@Before
	void setUp() {
		def entryTimeZone = Utils.createTimeZone(-8 * 60 * 60, "GMTOFFSET8")
		timeZone = Utils.createTimeZone(-5 * 60 * 60, "GMTOFFSET5")
		dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		dateFormat.setTimeZone(entryTimeZone)
		earlyBaseDate = dateFormat.parse("June 25, 2010 12:00 am")
		currentTime = dateFormat.parse("July 1, 2010 3:30 pm")
		lateCurrentTime = dateFormat.parse("July 3, 2010 3:30 pm")
		endTime = dateFormat.parse("July 1, 2010 5:00 pm")
		baseDate = dateFormat.parse("July 1, 2010 12:00 am")
		tomorrowBaseDate = dateFormat.parse("July 2, 2010 12:00 am")
		tomorrowCurrentTime = dateFormat.parse("July 2, 2010 2:15 pm")
		dayAfterTomorrowBaseDate = dateFormat.parse("July 3, 2010 12:00 am")
		lateBaseDate = dateFormat.parse("July 3, 2010 12:00 am")
		microlateCurrentTime = dateFormat.parse("July 3, 2010 1:58 pm")
		
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
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 slice", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet repeat daily", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneOffsetSecs:-14400, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat daily, repeatType:768)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet @4pm repeat weekly", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:2)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet 4pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null)")

		// test assume recent time
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet 3:00", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet at 4:00 pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet at 4 pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet at 16h00", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet at 16:00", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "9am aspirin 1 tablet repeat weekly", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:2)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg @9h00 after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg at 9:00 after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg at 09h00 after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg at 9pm after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "craving sweets/starches/carbohydrates 3 at 9pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:craving sweets/starches/carbohydrates, amount:3.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		// decimal value
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "9am aspirin 1.2 mg repeat weekly", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:1.200000000, units:mg, amountPrecision:3, comment:repeat weekly, repeatType:2)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "9am aspirin .7 mg repeat weekly", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:0.700000000, units:mg, amountPrecision:3, comment:repeat weekly, repeatType:2)")

		// test metadata entry parsing

		entry = Entry.create(userId, Entry.parseMeta("fast caffeine metabolizer"), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:null, datePrecisionSecs:0, timeZoneOffsetSecs:0, description:fast caffeine metabolizer, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		// test odd tweets

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "what's going on???", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:what's going on???, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "snack#3: 2 bags of chips", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:snack#3, amount:2.000000000, units:bags, amountPrecision:3, comment:of chips, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "snack#3", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:snack#3, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "weight: 310#", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:weight, amount:310.000000000, units:#, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "weight: 310lbs", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:weight, amount:310.000000000, units:lbs, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "headache 8 at 4pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:headache, amount:8.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		// test current time for late base date

		entry = Entry.create(userId, Entry.parse(dateFormat.parse("July 1, 2010 6:00 pm"), timeZone, "just a tag", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneOffsetSecs:-14400, description:just a tag, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		// test time before amount

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "heart rate at 4pm 72bpm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "heart rate at 4pm = 72bpm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null)")

		// number in tag

		entry = Entry.create(userId, Entry.parse(dateFormat.parse("July 1, 2010 6:00 pm"), timeZone, "methyl b-12", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneOffsetSecs:-14400, description:methyl b-12, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		// yes/no values

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 yes at 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12: yes at 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 no at 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:methyl b-12, amount:0.000000000, units:, amountPrecision:0, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 4pm yes", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 4pm: yes", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 yes we can at 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:methyl b-12 yes we can, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 yes we can 4 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:methyl b-12 yes we can, amount:4.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 yes we can 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:methyl b-12 yes we can, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		// none value
		
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 4pm: none", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:methyl b-12, amount:null, units:, amountPrecision:-1, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 none 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:methyl b-12, amount:null, units:, amountPrecision:-1, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 none we can at 4pm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:methyl b-12 none we can, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		// test time parsing with unambiguous formats

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg 9pm after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg 9:00 after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "heart rate 4pm 72bpm", baseDate, false), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet repeat weekly at 9", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:2)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg @12am after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T07:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg @12pm after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg @noon after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg at midnight after coffee", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T07:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null)")

	}

	@Test
	void testRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread repeat daily", earlyBaseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:768)")
		def entries = Entry.fetchListData(user, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
		}
		
		def activated = entry.activateGhostEntry(baseDate, lateCurrentTime)
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")
		
		def activated2 = entry.activateGhostEntry(baseDate, currentTime)
		assert activated2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")
	}
	
	@Test
	void testRemindActivate() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread remind", earlyBaseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517)")
		def entries = Entry.fetchListData(user, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
		}
		
		def activated = entry.activateGhostEntry(baseDate, lateCurrentTime)
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:null, units:, amountPrecision:-1, comment:remind, repeatType:517)")

		def activated2 = entry.activateGhostEntry(earlyBaseDate, currentTime)
		def v = activated.valueString()
		assert activated2.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneOffsetSecs:-14400, description:bread, amount:null, units:, amountPrecision:-1, comment:remind, repeatType:517)")
	}
	
	@Test
	void testRepeatAgain() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), null)
		
		def entry4 = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5pm repeat daily", baseDate, true), null)
		
		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", tomorrowBaseDate, true), null)
		
		assert entry.getRepeatEnd().equals(entry.getDate())
		
		Entry.delete(entry2, new TagStatsRecord())
		
		def entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", dayAfterTomorrowBaseDate, true), null)

		assert entry.getRepeatEnd().equals(entry.getDate())
	}
	
	@Test
	void testLateTimestamps() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 11pm remind", baseDate, true), null)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-02T06:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517)")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 11pm", baseDate, true), null)
		v = entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T06:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")
	}
	
	@Test
	void testRepeatStart() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep start repeat", earlyBaseDate, true), null)
		assert entry.getDurationType().equals(Entry.DurationType.NONE)
	}
	
	@Test
	void testRepeatCreateInMiddle() {
		// test creating repeats in middle of earlier range
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread repeat daily", earlyBaseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:768)")
		def entries = Entry.fetchListData(user, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
		}
		
		def activated = entry.activateGhostEntry(lateBaseDate, lateCurrentTime)
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-03T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")
		
		// create repeat event in the middle
		def middleEntry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread repeat daily", baseDate, true), null)
		
		// make sure prior repeatEnd is reset
		assert entry.getRepeatEnd().toString().equals((new Date(baseDate.getTime() - 12*60*60000L)).toString())
	}
	
	@Test
	void testDeleteContinuousRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread repeat daily", baseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:768)")
		def entries = Entry.fetchListData(user, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
		}

		def list = Entry.fetchListData(user, tomorrowBaseDate, this.tomorrowCurrentTime)
		
		boolean foundRepeat = false
		
		for (record in list) {
			if (record.id == entry.getId())
				foundRepeat = true
		}
		
		assert foundRepeat
		
		Entry.deleteGhost(entry, tomorrowBaseDate, true)
		
		assert entry.getRepeatEnd().equals(tomorrowBaseDate)
		
		list = Entry.fetchListData(user, tomorrowBaseDate, this.tomorrowCurrentTime)
		
		for (record in list) {
			if (record.id == entry.getId())
				assert false
		}
	}
	
	@Test
	void testDeleteTimedRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:513)")
		def entries = Entry.fetchListData(user, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
		}

		def list = Entry.fetchListData(user, tomorrowBaseDate, this.tomorrowCurrentTime)
		
		boolean foundRepeat = false
		
		for (record in list) {
			if (record.id == entry.getId())
				foundRepeat = true
		}
		
		assert foundRepeat
		
		Entry.deleteGhost(entry, tomorrowBaseDate, false)
		
		assert entry.getRepeatEnd().equals(tomorrowBaseDate)
		
		list = Entry.fetchListData(user, dayAfterTomorrowBaseDate, this.tomorrowCurrentTime)
		
		for (record in list) {
			def e2 = Entry.get(record.id)
			assert e2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-03T21:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:513)")
			assert e2.getId() != entry.getId()
		}
	}
	
	@Test
	void testDeleteTimedRepeatAll() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), null)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:513)")
		def entries = Entry.fetchListData(user, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
		}

		def list = Entry.fetchListData(user, tomorrowBaseDate, this.tomorrowCurrentTime)
		
		boolean foundRepeat = false
		
		for (record in list) {
			if (record.id == entry.getId())
				foundRepeat = true
		}
		
		assert foundRepeat
		
		Entry.deleteGhost(entry, tomorrowBaseDate, true)
		
		assert entry.getRepeatEnd().equals(tomorrowBaseDate)
		
		list = Entry.fetchListData(user, tomorrowBaseDate, this.tomorrowCurrentTime)
		
		for (record in list) {
			if (record.id == entry.getId())
				assert false
		}
	}
	
	@Test
	void testRepeatNonContinuous() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", earlyBaseDate, true), null)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:513)")
		def entries = Entry.fetchListData(user, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
		}
		
		def activated = entry.activateGhostEntry(baseDate, lateCurrentTime)
		v = activated.valueString()
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1)")
		
		def activated2 = entry.activateGhostEntry(baseDate, currentTime)
		v = activated2.valueString()
		assert activated2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1)")

		def activated3 = entry.activateGhostEntry(earlyBaseDate, currentTime)
		v = activated3.valueString()
		assert activated3.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:1)")
		assert activated3 == entry
	}
	
	@Test
	void testOldRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread repeat daily", earlyBaseDate, true), null)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:768)")
		def entries = Entry.fetchListDataNoRepeats(user, baseDate)
		assert entries.size() == 0
		
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 3pm repeat daily", baseDate, true), null)
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
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517)")

		// tag stats for reminders should be "none" value for the amount
		def tagStats = record.getOldTagStats()
		assert tagStats.getLastAmount() == null
		assert tagStats.getLastAmountPrecision() < 0
		
		// query for reminder should generate the ghost entry only
		def entries = Entry.fetchListData(user, lateBaseDate, lateCurrentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
			assert entryDesc['date'].toString().equals("2010-07-03 17:00:00.0")
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
		TagStatsRecord record = new TagStatsRecord()
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 75 2pm remind", baseDate, true), record)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:75.000000000, units:, amountPrecision:3, comment:remind, repeatType:5)")
		
		def list = Entry.fetchListData(user, tomorrowBaseDate, currentTime)
		
		for (e in list) {
			assert e.amount == null
			assert (e.repeatType & Entry.RepeatType.GHOST_BIT) != 0
		}

		list = Entry.fetchListData(user, baseDate, currentTime)
		
		for (e in list) {
			assert e.amount.intValue() == 75
			assert (e.repeatType & Entry.RepeatType.GHOST_BIT) == 0
		}
	}

	@Test
	void testRemindList2() {
		TagStatsRecord record = new TagStatsRecord()
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm remind", baseDate, true), record)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517)")
		
		def list = Entry.fetchListData(user, tomorrowBaseDate, currentTime)
		
		for (e in list) {
			assert e.amount == null
			assert (e.repeatType & Entry.RepeatType.GHOST_BIT) != 0
		}

		list = Entry.fetchListData(user, baseDate, currentTime)
		
		for (e in list) {
			assert e.amount == null
			assert (e.repeatType & Entry.RepeatType.GHOST_BIT) != 0
		}
	}

	@Test
	void testRepeatParsing() {
		def res = Entry.parse(currentTime, timeZone, "bread repeat daily at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 1
		assert res['comment'].equals('repeat daily')
		assert res['repeatType'] == Entry.RepeatType.DAILYGHOST
		assert res['units'] == null

		res = Entry.parse(currentTime, timeZone, "bread repeat daily at 4pm foo", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 1
		assert res['comment'].equals('repeat daily foo')
		assert res['repeatType'] == Entry.RepeatType.DAILYGHOST
		assert res['units'] == null

		res = Entry.parse(currentTime, timeZone, "bread 8 slices repeat daily at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 8
		assert res['comment'].equals('repeat daily')
		assert res['repeatType'] == Entry.RepeatType.DAILY
		assert res['units'].equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices daily at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 8
		assert res['comment'].equals('daily')
		assert res['repeatType'] == Entry.RepeatType.DAILY
		assert res['units'].equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices repeat at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 8
		assert res['comment'].equals('repeat')
		assert res['repeatType'] == Entry.RepeatType.DAILY
		assert res['units'].equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices weekly at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 8
		assert res['comment'].equals('weekly')
		assert res['repeatType'] == Entry.RepeatType.WEEKLY
		assert res['units'].equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices repeat weekly at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 8
		assert res['comment'].equals('repeat weekly')
		assert res['repeatType'] == Entry.RepeatType.WEEKLY
		assert res['units'].equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread weekly", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 1
		assert res['comment'].equals('weekly')
		assert res['repeatType'] == Entry.RepeatType.WEEKLYGHOST
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
		assert res['repeatType'] == Entry.RepeatType.REMINDDAILY
		assert res['units'].equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices remind at 4pm", baseDate, true)

		assert res['description'].equals('bread')
		assert res['amount'].intValue() == 8
		assert res['comment'].equals('remind')
		assert res['repeatType'] == Entry.RepeatType.REMINDDAILY
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
		assert res['repeatType'] == Entry.RepeatType.REMINDWEEKLY
		assert res['units'].equals('slices')
		
	}

	@Test
	void testUpdate() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 1 slice", baseDate, true, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null)")

		Entry.update(entry, Entry.parse(currentTime, timeZone, "aspirin 1 tablet repeat daily", baseDate, true, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneOffsetSecs:-14400, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat daily, repeatType:1)")
	}

	@Test
	void testUpdateRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread foo repeat daily", baseDate, true, false), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneOffsetSecs:-14400, description:bread foo, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:768)")
		println("Attempting to update")
		Entry.update(entry, Entry.parse(currentTime, timeZone, "bread repeat daily", baseDate, true, true), null)
		def entries = Entry.fetchListData(user, lateBaseDate, lateCurrentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
			assert entryDesc['date'].toString().equals("2010-07-03 15:00:00.0")
		}
	}

	@Test
	void testUpdateRepeatChangeTime() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread foo repeat daily", earlyBaseDate, true, false), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneOffsetSecs:-14400, description:bread foo, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:768)")
		println("Attempting to update")
		
		Entry.update(entry, Entry.parse(currentTime, timeZone, "headache 8 at 3pm repeat daily", earlyBaseDate, true, true), null)
		def entries = Entry.fetchListData(user, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
			assert entryDesc['date'].toString().equals("2010-07-01 18:00:00.0")
		}
	}

	@Test
	void testReplaceRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "exercise 12pm repeat daily", earlyBaseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:exercise, amount:1.000000000, units:, amountPrecision:-1, comment:repeat daily, repeatType:513)")
		println("Attempting to update")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "exercise 8 12pm repeat daily", baseDate, true), null)
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:exercise, amount:8.000000000, units:, amountPrecision:3, comment:repeat daily, repeatType:1)")
		
		def repeatEnd = entry.getRepeatEnd()
		assert repeatEnd.equals(new Date(baseDate.getTime() - 12 * 60 * 60000L))
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
		println("=== Test creation of start entry ===")
		
		Entry entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		// test creation of the other side of the duration pair
		println("=== Test creation of end entry ===")

		Entry entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 3:31pm", baseDate, true), null)
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz, amount:0.016667000, units:hours, amountPrecision:3, comment:, repeatType:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)
		assert entry2.fetchDurationEntry().fetchIsGenerated()
		
		// test editing the start time of the duration pair
		println("=== Test update start entry ===")

		Entry.update(entry, Entry.parse(currentTime, timeZone, "testxyz start at 3:01pm", baseDate, true, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:01:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry.fetchEndEntry().fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test creation of new start of a duration pair
		println("=== Test creation of new start entry in between start and end entry ===")

		Entry entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:16pm", baseDate, true), null)
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:16:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry3.fetchEndEntry().equals(entry2)
		assert entry.fetchEndEntry() == null
		assert entry2.fetchStartEntry().equals(entry3)
		
		// test deletion of start end of a duration pair
		println("=== Test deletion of start entry from duration pair ===")
		
		Entry.delete(entry3, null)

		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test creation of new end after the duration pair
		println("=== Test creation of new end after the duration pair ===")
		
		Entry entry4 = Entry.create(userId, Entry.parse(endTime, timeZone, "testxyz end at 4:31pm", baseDate, true), null)
		assert entry4.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:31:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry4.fetchStartEntry() == null
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test deletion of start of duration pair
		println("=== Test deletion of start entry from duration pair ===")
		
		Entry.delete(entry2, null)
		
		assert entry.fetchEndEntry().equals(entry4)
		assert entry4.fetchStartEntry().equals(entry)
		
		assert entry4.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:31:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz, amount:1.500000000, units:hours, amountPrecision:3, comment:, repeatType:null)")
	}

	@Test
	void testSleepWake() {
		// test creation of one side of a duration pair
		println("=== Test creation of start entry ===")
		
		Entry entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep 3:30pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:sleep, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		// test creation of the other side of the duration pair
		println("=== Test creation of end entry ===")

		Entry entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "wake 3:31pm", baseDate, true), null)
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:wake, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:sleep, amount:0.016667000, units:hours, amountPrecision:3, comment:, repeatType:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)
		assert entry2.fetchDurationEntry().fetchIsGenerated()
		
		// test editing the start time of the duration pair
		println("=== Test update start entry ===")

		Entry.update(entry, Entry.parse(currentTime, timeZone, "sleep at 3:01pm", baseDate, true, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:01:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:sleep, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry.fetchEndEntry().fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:sleep, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test creation of new start of a duration pair
		println("=== Test creation of new start entry in between start and end entry ===")

		Entry entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep start at 3:16pm", baseDate, true), null)
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:16:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:sleep start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry3.fetchEndEntry().equals(entry2)
		assert entry.fetchEndEntry() == null
		assert entry2.fetchStartEntry().equals(entry3)
		
		// test deletion of start end of a duration pair
		println("=== Test deletion of start entry from duration pair ===")
		
		Entry.delete(entry3, null)

		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test creation of new end after the duration pair
		println("=== Test creation of new end after the duration pair ===")
		
		Entry entry4 = Entry.create(userId, Entry.parse(endTime, timeZone, "sleep end at 4:31pm", baseDate, true), null)
		assert entry4.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:31:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:sleep end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry4.fetchStartEntry() == null
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test deletion of start of duration pair
		println("=== Test deletion of start entry from duration pair ===")
		
		Entry.delete(entry2, null)
		
		assert entry.fetchEndEntry().equals(entry4)
		assert entry4.fetchStartEntry().equals(entry)
		
		assert entry4.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:31:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:sleep, amount:1.500000000, units:hours, amountPrecision:3, comment:, repeatType:null)")
	}

	/**
	 * Test creating an interleaved duration pair: create a duration pair, then create another start before the
	 * pair and a new end in the middle of the first pair
	 */
	@Test
	void testInterleavedDuration() {
		// test creation of one side of a duration pair
		println("=== Test creation of start entry ===")
		
		Entry entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")
		assert entry.fetchEndEntry() == null
		
		// test creation of the other side of the duration pair
		println("=== Test creation of end entry ===")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 4pm", baseDate, true), null)
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry2.fetchStartEntry() == entry
		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null)")

		// test creation of new start of a duration pair
		println("=== Test creation of new start entry before start and end entry ===")

		def entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3pm", baseDate, true), null)
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry3.fetchEndEntry() == null
		assert entry2.fetchStartEntry() == entry
		assert entry.fetchEndEntry() == entry2

		// test creation of new end in between first duration pair
		println("=== Test creation of new end in between first duration pair ===")
		
		def entry4 = Entry.create(userId, Entry.parse(endTime, timeZone, "testxyz end at 3:45pm", baseDate, true), null)
		assert entry4.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:45:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		
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
		println("=== Test creation of start entry ===")
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry.fetchEndEntry() == null
		
		// test creation of the other side of the duration pair
		println("=== Test creation of end entry ===")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 4pm", baseDate, true), null)
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchStartEntry() == entry
		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null)")

		// test creation of new start of a duration pair
		println("=== Test creation of new start entry in between first duration pair ===")

		def entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:45pm", baseDate, true), null)
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:45:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

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
		println("=== Test creation of start entry ===")
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz start, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry.fetchEndEntry() == null
		
		// test creation of the other side of the duration pair
		println("=== Test creation of end entry ===")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 4pm", baseDate, true), null)
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchStartEntry() == entry
		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null)")

		// test creation of orphan end of duration pair
		println("=== Test creation of new end entry after first duration pair ===")

		def entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 4:30pm", baseDate, true), null)
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz end, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null)")

		assert entry3.fetchDurationEntry() == null
		assert entry3.fetchStartEntry() == null
		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchStartEntry() == entry
		
		// test deletion of start of duration pair
		
		println("=== Test deletion of middle end entry ===")
		
		Entry.delete(entry2, null)
		
		Entry newDurationEntry = entry.fetchEndEntry().fetchDurationEntry()
				
		assert newDurationEntry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:testxyz, amount:1.000000000, units:hours, amountPrecision:3, comment:, repeatType:null)")
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
