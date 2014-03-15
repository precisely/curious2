import static org.junit.Assert.*

import java.math.MathContext
import java.text.DateFormat
import java.text.SimpleDateFormat

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

class DateTests extends GroovyTestCase {
	static transactional = true

	DateFormat dateFormat
	DateFormat dateFormatLA
	DateFormat dateFormatNY
	String timeZoneName
	String timeZoneName2
	TimeZone timeZone
	TimeZone timeZone2
	
	@Before
	void setUp() {
		Locale.setDefault(Locale.US)	// For to run test case in any country.
		TimeZoneId.clearCacheForTesting()
		
		timeZoneName = "America/Los_Angeles"
		timeZoneName2 = "America/New_York"
		
		timeZone = TimeZoneId.look(timeZoneName).toTimeZone()
		timeZone2 = TimeZoneId.look(timeZoneName2).toTimeZone()
		
		dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
		dateFormatLA = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
		dateFormatNY = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
		dateFormatLA.setTimeZone(timeZone)
		dateFormatNY.setTimeZone(timeZone2)
		
	}

	@After
	void tearDown() {
	}

	@Test
	void testDate() {
		Date testDatePlain = dateFormat.parse("2100-01-01")
		Date testDateLAFormatter = dateFormatLA.parse("2100-01-01")
		Date testDateNYFormatter = dateFormatNY.parse("2100-01-01")
		
		println testDatePlain
		println testDateLAFormatter
		println testDateNYFormatter
		
		println dateFormat.format(testDatePlain)
		println dateFormatLA.format(testDatePlain)
		println dateFormatNY.format(testDatePlain)
		println dateFormat.format(testDateLAFormatter)
		println dateFormatLA.format(testDateLAFormatter)
		println dateFormatNY.format(testDateLAFormatter)
		println dateFormat.format(testDateNYFormatter)
		println dateFormatLA.format(testDateNYFormatter)
		println dateFormatNY.format(testDateNYFormatter)
	}
}
