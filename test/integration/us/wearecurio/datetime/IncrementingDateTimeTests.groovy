package us.wearecurio.datetime

import static org.junit.Assert.*

import java.text.DateFormat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormat
import org.junit.*

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.datetime.IncrementingDateTime
import us.wearecurio.utility.Utils

import us.wearecurio.data.RepeatType

class IncrementingDateTimeTests extends CuriousTestCase {
	static transactional = true
	
	DateTimeZone entryTimeZone
	DateTimeZone entryTimeZone2
	DateTimeFormatter dateTimeFormatter
	DateTimeFormatter dateTimeFormatter2
	
	DateTime parseDateTime(String dateStr) {
		return dateTimeFormatter.parseDateTime(dateStr)		
	}
	
	DateTime parseDateTime2(String dateStr) {
		return dateTimeFormatter2.parseDateTime(dateStr)		
	}
	
	@Before
	void setUp() {
		entryTimeZone = DateTimeZone.forID("America/Los_Angeles")
		entryTimeZone2 = DateTimeZone.forID("America/New_York")
		dateTimeFormatter = DateTimeFormat.forPattern("MMM d yyyy HH:mm").withZone(entryTimeZone)
		dateTimeFormatter2 = DateTimeFormat.forPattern("MMM d yyyy HH:mm").withZone(entryTimeZone2)
		
		super.setUp()
	}
	
	@After
	void tearDown() {
		super.tearDown()
	}
	
	@Test
	void "test last repeat before date time"() {
		DateTime initial = parseDateTime("January 1 2020 15:00")
		DateTime last = parseDateTime("January 6 2020 15:00")
		
		DateTime lastRepeat = IncrementingDateTime.lastRepeatBeforeDateTime(initial, last, RepeatType.DAILY_BIT)
		
		assert lastRepeat.equals(parseDateTime("January 5 2020 15:00"))
		
		assert parseDateTime2("January 6 2020 18:00").getMillis() == last.getMillis()
		
		initial = parseDateTime("January 1 2020 15:00")
		last = parseDateTime2("January 6 2020 18:00")
		
		lastRepeat = IncrementingDateTime.lastRepeatBeforeDateTime(initial, last, RepeatType.DAILY_BIT)
		
		assert lastRepeat.getMillis() == parseDateTime("January 5 2020 15:00").getMillis()
		assert lastRepeat.getZone().equals(entryTimeZone)
		
		initial = parseDateTime("January 1 2020 15:00")
		last = parseDateTime("January 6 2020 16:00")
		
		lastRepeat = IncrementingDateTime.lastRepeatBeforeDateTime(initial, last, RepeatType.DAILY_BIT)
		
		assert lastRepeat.getMillis() == parseDateTime("January 6 2020 15:00").getMillis()
		
		initial = parseDateTime("January 1 2020 15:00")
		last = parseDateTime("January 6 2020 14:00")
		
		lastRepeat = IncrementingDateTime.lastRepeatBeforeDateTime(initial, last, RepeatType.DAILY_BIT)
		
		assert lastRepeat.getMillis() == parseDateTime("January 5 2020 15:00").getMillis()
		
		initial = parseDateTime("January 1 2020 15:00")
		last = parseDateTime("January 6 2020 15:00")
		
		lastRepeat = IncrementingDateTime.lastRepeatBeforeDateTime(initial, last, RepeatType.WEEKLY_BIT)
		
		assert lastRepeat.getMillis() == parseDateTime("January 1 2020 15:00").getMillis()

		initial = parseDateTime("January 1 2020 15:00")
		last = parseDateTime("January 8 2020 15:00")
		
		lastRepeat = IncrementingDateTime.lastRepeatBeforeDateTime(initial, last, RepeatType.WEEKLY_BIT)
		
		assert lastRepeat.getMillis() == parseDateTime("January 1 2020 15:00").getMillis()
		
		initial = parseDateTime("January 1 2020 15:00")
		last = parseDateTime("January 8 2020 16:00")
		
		lastRepeat = IncrementingDateTime.lastRepeatBeforeDateTime(initial, last, RepeatType.WEEKLY_BIT)
		
		assert lastRepeat.getMillis() == parseDateTime("January 8 2020 15:00").getMillis()
		
		initial = parseDateTime("January 30 2020 15:00")
		last = parseDateTime("March 15 2020 16:00")
		
		lastRepeat = IncrementingDateTime.lastRepeatBeforeDateTime(initial, last, RepeatType.MONTHLY_BIT)
		
		assert lastRepeat.getMillis() == parseDateTime("February 29 2020 15:00").getMillis()
		
		initial = parseDateTime("January 30 2021 15:00")
		last = parseDateTime("March 15 2021 16:00")
		
		lastRepeat = IncrementingDateTime.lastRepeatBeforeDateTime(initial, last, RepeatType.MONTHLY_BIT)
		
		assert lastRepeat.getMillis() == parseDateTime("February 28 2021 15:00").getMillis()
		
		initial = parseDateTime("January 15 2021 15:00")
		last = parseDateTime("March 16 2021 16:00")
		
		lastRepeat = IncrementingDateTime.lastRepeatBeforeDateTime(initial, last, RepeatType.MONTHLY_BIT)
		
		assert lastRepeat.getMillis() == parseDateTime("March 15 2021 15:00").getMillis()
		
		initial = parseDateTime("January 15 2021 15:00")
		last = parseDateTime("March 15 2021 16:00")
		
		lastRepeat = IncrementingDateTime.lastRepeatBeforeDateTime(initial, last, RepeatType.MONTHLY_BIT)
		
		assert lastRepeat.getMillis() == parseDateTime("March 15 2021 15:00").getMillis()

		initial = parseDateTime("January 15 2021 15:00")
		last = parseDateTime("March 15 2021 16:00")
		
		lastRepeat = IncrementingDateTime.lastRepeatBeforeDateTime(initial, last, RepeatType.YEARLY_BIT)
		
		assert lastRepeat.getMillis() == parseDateTime("January 15 2021 15:00").getMillis()

		initial = parseDateTime("January 15 2021 15:00")
		last = parseDateTime("March 15 2023 16:00")
		
		lastRepeat = IncrementingDateTime.lastRepeatBeforeDateTime(initial, last, RepeatType.YEARLY_BIT)
		
		assert lastRepeat.getMillis() == parseDateTime("January 15 2023 15:00").getMillis()

		initial = parseDateTime("January 15 2021 15:00")
		last = parseDateTime("January 15 2023 15:00")
		
		lastRepeat = IncrementingDateTime.lastRepeatBeforeDateTime(initial, last, RepeatType.YEARLY_BIT)
		
		assert lastRepeat.getMillis() == parseDateTime("January 15 2022 15:00").getMillis()
	}
	
	@Test
	void "Test utility methods"() {
		LocalDate localDate = new LocalDate(2001, 1, 30)
		assert IncrementingDateTime.addMonth(localDate).getDayOfMonth() == 28
		
		localDate = new LocalDate(2001, 2, 27)
		assert IncrementingDateTime.monthDay(localDate, 31).getDayOfMonth() == 28
		
		localDate = new LocalDate(2004, 2, 29)
		assert IncrementingDateTime.addYear(localDate).getDayOfMonth() == 28
		
		localDate = new LocalDate(2004, 2, 29)
		assert IncrementingDateTime.subtractYear(localDate).getDayOfMonth() == 28
		
		localDate = new LocalDate(2016, 3, 13)
		LocalTime localTime = new LocalTime(2, 30);
		assert IncrementingDateTime.makeDateTime(localDate, localTime, DateTimeZone.forID("America/Los_Angeles")).getHourOfDay() == 3
	}
}