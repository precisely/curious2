package us.wearecurio.model

import java.util.Date;

import us.wearecurio.utility.Utils

import org.joda.time.*

class TimeZoneId {

	static transients = {
		"dateTimeZone"
	}
    static constraints = {
		name(maxSize:100, unique:true)
		dateTimeZone(nullable:true)
    }
	static mapping = {
		name column:'name', index:'name_idx'
	}
	
	TimeZoneId(String name) {
		this.name = name
	}

	String name
	transient DateTimeZone dateTimeZone
	
	static Map<String, TimeZoneId> nameToTimeZoneId = new HashMap<String, TimeZoneId>()
	static Map<Integer, TimeZoneId> idToTimeZoneId = new HashMap<Integer, TimeZoneId>()
	
	static void clearCacheForTesting() {
		nameToTimeZoneId = new HashMap<String, TimeZoneId>()
		idToTimeZoneId = new HashMap<Integer, TimeZoneId>()
	}
	
	DateTimeZone toDateTimeZone() {
		if (dateTimeZone != null)
			return dateTimeZone
		
		return dateTimeZone = DateTimeZone.forID(name)
	}
	
	static TimeZoneId fromId(Integer id) {
		if (id == null) {
			return TimeZoneId.look("America/New_York")
		}
		TimeZoneId timeZoneId = idToTimeZoneId.get(id)
		
		if (timeZoneId == null) {
			return TimeZoneId.get((Long)id)
		}
		
		return timeZoneId
	}
	
	static TimeZoneId look(String name) {
		if (name == null) name = "Etc/UTC"
		
		TimeZoneId timeZoneId = nameToTimeZoneId.get(name)
		
		if (timeZoneId != null && timeZoneId.name == name) {
			return timeZoneId
		}
		
		timeZoneId = TimeZoneId.findByName(name)
		
		if (timeZoneId != null) {
			nameToTimeZoneId.put(name, timeZoneId)
			idToTimeZoneId.put((Integer)timeZoneId.getId(), timeZoneId)
			return timeZoneId
		}
		
		timeZoneId = new TimeZoneId(name)
		
		Utils.save(timeZoneId, true)
		
		nameToTimeZoneId.put(name, timeZoneId)
		idToTimeZoneId.put((Integer)timeZoneId.getId(), timeZoneId)
		
		return timeZoneId
	}
	
	public static DateTime nextDayFromDateSameTime(Date baseDate, DateTime dateTime) {
		DateTimeZone dateTimeZone = dateTime.getZone()
		DateTime baseDateTime = new DateTime(baseDate, dateTimeZone)
		LocalDate localDate = baseDateTime.toLocalDate()
		LocalTime localTime = dateTime.toLocalTime()
		
		DateTime result = localDate.plusDays(1).toDateTime(localTime, dateTimeZone)
		if (result.toDate() < baseDate + 1) {
			return localDate.plusDays(2).toDateTime(localTime, dateTimeZone)
		} else
			return result
	}
	
	public static DateTime nextDaySameTime(DateTime dateTime) {
		LocalDate localDate = dateTime.toLocalDate()
		LocalTime localTime = dateTime.toLocalTime()
		
		DateTimeZone dateTimeZone = dateTime.getZone()
		
		return localDate.plusDays(1).toDateTime(localTime, dateTimeZone)
	}
	
	public static DateTime previousDayFromDateSameTime(Date baseDate, DateTime dateTime) {
		DateTimeZone dateTimeZone = dateTime.getZone()
		DateTime baseDateTime = new DateTime(baseDate, dateTimeZone)
		LocalDate localDate = baseDateTime.toLocalDate()
		LocalTime localTime = dateTime.toLocalTime()
		
		DateTime result = localDate.minusDays(1).toDateTime(localTime, dateTimeZone)
		if (result.toDate() < baseDate - 1) {
			return localDate.toDateTime(localTime, dateTimeZone)
		} else
			return result
	}
	
	public static LocalDate getNthSundayOfMonth(final int n, final int month, final int year) {
		final LocalDate firstSunday = new LocalDate(year, month, 1).withDayOfWeek(DateTimeConstants.SUNDAY);
		if (n > 1) {
			final LocalDate nThSunday = firstSunday.plusWeeks(n - 1);
			final LocalDate lastDayInMonth = firstSunday.dayOfMonth().withMaximumValue();
			if (nThSunday.isAfter(lastDayInMonth)) {
				throw new IllegalArgumentException("There is no " + n + "th Sunday in this month!");
			}
			return nThSunday;
		}
		return firstSunday;
	}

	// try to guess time zone name from offset, for converting deprecated entry time values
	static String guessTimeZoneName(Date date, int offsetSecs) {		
		DateTime dateTime = new DateTime(date, DateTimeZone.forOffsetMillis((int)offsetSecs * 1000))
		
		boolean dst = true
		
		int month = dateTime.getMonthOfYear()
		int day = dateTime.getDayOfMonth()
		int hour = dateTime.getHourOfDay()
		
		if (month < 3 || month > 11) {
			dst = false
		} else if (month > 3 && month < 11) {
			dst = true
		} else if (month == 3) {
			// figure out if date is equal to or after second sunday of the month
			LocalDate secondSunday = getNthSundayOfMonth(2, 3, dateTime.getYear())
			int secondSundayDay = secondSunday.getDayOfMonth()
			
			if (day < secondSundayDay)
				dst = false
			else if (day > secondSundayDay)
				dst = true
			else
				dst = hour >= 2
			
		} else {
			// figure out if date is equal to or after first sunday of the month
			LocalDate firstSunday = getNthSundayOfMonth(1, 11, dateTime.getYear())
			int firstSundayDay = firstSunday.getDayOfMonth()
			
			if (day < firstSundayDay)
				dst = true
			else if (day > firstSundayDay)
				dst = false
			else
				dst = hour < 2
			
		}
		
		// -28800 = GMT-8 = PST = America/Los_Angeles
		// -25200 = GMT-7 = PDT
		// -25200 = America/Denver
		// -21600 = America/Chicago
		// -18000 = America/New_York
		
		if (dst) offsetSecs -= 60 * 60
		
		return guessTimeZoneNameFromOffset(offsetSecs)
	}
	
	// try to guess time zone name from offset, for converting deprecated entry time values
	static String guessTimeZoneNameFromOffset(int offsetSecs) {		
		if (offsetSecs == -28800)
			return "America/Los_Angeles"
		else if (offsetSecs == -25200)
			return "America/Los_Angeles"
		else if (offsetSecs == -21600)
			return "America/New_York"
		else if (offsetSecs == -18000)
			return "America/New_York"
		else if (offsetSecs > 0)
			return "Asia/Kolkata"
			
		return "America/New_York" // default everything else to NYC for now
	}
	
	// try to guess time zone name from baseDate alone, for converting deprecated time zone values
	static String guessTimeZoneNameFromBaseDate(Date baseDate) {
		LocalTime utcTime = new DateTime(baseDate, DateTimeZone.UTC).toLocalTime()
		
		int hours = utcTime.getHourOfDay()
		if (hours == 16 || hours == 17)
			return "America/Los_Angeles"
		else if (hours == 19 || hours == 20)
			return "America/New_York"
		else if (hours < 13)
			return "Asia/Kolkata"
		
		return "America/New_York" // default everything else to NYC for now
	}
}
