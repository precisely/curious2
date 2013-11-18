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
	
	DateTimeZone toDateTimeZone() {
		if (dateTimeZone != null)
			return dateTimeZone
		
		return dateTimeZone = DateTimeZone.forID(name)
	}
	
	static TimeZoneId fromId(Integer id) {
		TimeZoneId timeZoneId = idToTimeZoneId.get(id)
		
		if (timeZoneId == null) {
			return TimeZoneId.get((Long)id)
		}
		
		return timeZoneId
	}
	
	static TimeZoneId look(String name) {
		if (name == null) name = "Etc/UTC"
		
		TimeZoneId timeZoneId = nameToTimeZoneId.get(name)
		
		if (timeZoneId != null) return timeZoneId
		
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

	// try to guess time zone name from offset
	static String getTimeZoneName(Date date, int offsetSecs) {		
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
}
