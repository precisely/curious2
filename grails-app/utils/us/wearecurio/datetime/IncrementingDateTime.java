package us.wearecurio.datetime;

import java.util.Date;

import org.joda.time.*;

public class IncrementingDateTime {

	DateTime currentDateTime;
	LocalDate localDate;
	LocalTime localTime;
	DateTimeZone dateTimeZone;
	int intervalCode;

	// copied from RepeatType.groovy (for some reason cannot use the constants in RepeatType directly)
	
	static final int HOURLY_BIT = 8;
	static final int DAILY_BIT = 1;
	static final int WEEKLY_BIT = 2;
	static final int MONTHLY_BIT = 0x0010;
	static final int YEARLY_BIT = 0x0020;
	
	public IncrementingDateTime(DateTime dateTime, Date startDate, int intervalCode) {
		currentDateTime = firstRepeatAfterDate(dateTime, startDate, intervalCode);
		dateTimeZone = dateTime.getZone();
		localDate = currentDateTime.toLocalDate();
		localTime = currentDateTime.toLocalTime();
		this.intervalCode = intervalCode;
	}
	
	// utility methods for external clients for incrementing date time, code repeated in increment method, below, for speed
	public static DateTime firstRepeatAfterDate(DateTime initialDateTime, Date startDate, int intervalCode) {
		if (startDate.getTime() < initialDateTime.getMillis())
			return initialDateTime;
		
		LocalTime initialLocalTime = initialDateTime.toLocalTime();
		LocalDate initialLocalDate = initialDateTime.toLocalDate();
		DateTimeZone initialDateTimeZone = initialDateTime.getZone();
		DateTime startDateTimeInInitialDateTimeZone = new DateTime(startDate, initialDateTimeZone);
		LocalDate startLocalDate = startDateTimeInInitialDateTimeZone.toLocalDate();
		LocalTime startLocalTime = startDateTimeInInitialDateTimeZone.toLocalTime();
		
		if (intervalCode == HOURLY_BIT) {
			int initialMinute = initialLocalTime.getMinuteOfHour();
			int startMinute = startLocalTime.getMinuteOfHour();
			
			if (startMinute == initialMinute) {
				return startDateTimeInInitialDateTimeZone;
			} else {
				LocalTime newRepeatLocalTime = startLocalTime.withMinuteOfHour(initialMinute);
				if (initialMinute < startMinute) {
					newRepeatLocalTime = newRepeatLocalTime.plusHours(1);
				}
				return startLocalDate.toDateTime(newRepeatLocalTime, initialDateTimeZone);
			}
		} else {
			switch (intervalCode) {
				case DAILY_BIT:
				default:
					
				if (initialLocalTime.compareTo(startLocalTime) <= 0)
					return startLocalDate.plusDays(1).toDateTime(initialLocalTime, initialDateTimeZone);
				return startLocalDate.toDateTime(initialLocalTime, initialDateTimeZone);

				case WEEKLY_BIT:
					
				int initialDayOfWeek = initialLocalDate.getDayOfWeek();
				int limitDayOfWeek = startLocalDate.getDayOfWeek();
				
				if (initialDayOfWeek == limitDayOfWeek) {
					if (initialLocalTime.compareTo(startLocalTime) <= 0)
						return startLocalDate.plusWeeks(1).toDateTime(initialLocalTime, initialDateTimeZone);
					return startLocalDate.toDateTime(initialLocalTime, initialDateTimeZone);
				} else if (initialDayOfWeek > limitDayOfWeek) {
					return startLocalDate.withDayOfWeek(initialDayOfWeek).plusWeeks(1).toDateTime(initialLocalTime, initialDateTimeZone);
				} else
					return startLocalDate.withDayOfWeek(initialDayOfWeek).toDateTime(initialLocalTime, initialDateTimeZone);
				
				case MONTHLY_BIT: {
					int initialDayOfMonth = initialLocalDate.getDayOfMonth();
					int limitDayOfMonth = startLocalDate.getDayOfMonth();
					
					if (initialDayOfMonth == limitDayOfMonth) {
						if (initialLocalTime.compareTo(startLocalTime) <= 0)
							return startLocalDate.plusMonths(1).toDateTime(initialLocalTime, initialDateTimeZone);
						return startLocalDate.toDateTime(initialLocalTime, initialDateTimeZone);
					} else if (initialDayOfMonth < limitDayOfMonth) {
						return startLocalDate.withDayOfMonth(initialDayOfMonth).plusMonths(1).toDateTime(initialLocalTime, initialDateTimeZone);
					} else
						return startLocalDate.withDayOfMonth(initialDayOfMonth).toDateTime(initialLocalTime, initialDateTimeZone);
				}
				
				case YEARLY_BIT: {
					int initialDayOfMonth = initialLocalDate.getDayOfMonth();
					int limitDayOfMonth = startLocalDate.getDayOfMonth();
					int initialMonthOfYear = initialLocalDate.getMonthOfYear();
					int limitMonthOfYear = startLocalDate.getMonthOfYear();

					if (initialMonthOfYear == limitMonthOfYear) {
						if (initialDayOfMonth == limitDayOfMonth) {
							if (initialLocalTime.compareTo(startLocalTime) <= 0)
								return startLocalDate.plusYears(1).toDateTime(initialLocalTime, initialDateTimeZone);
							return startLocalDate.toDateTime(initialLocalTime, initialDateTimeZone);
						} else if (initialDayOfMonth < limitDayOfMonth)
							return startLocalDate.withDayOfMonth(initialDayOfMonth).plusYears(1).toDateTime(initialLocalTime, initialDateTimeZone);
						else
							return startLocalDate.withDayOfMonth(initialDayOfMonth).toDateTime(initialLocalTime, initialDateTimeZone);
					} else if (initialMonthOfYear > limitMonthOfYear)
						return startLocalDate.withMonthOfYear(initialMonthOfYear).withDayOfMonth(initialDayOfMonth).minusYears(1)
								.toDateTime(initialLocalTime, initialDateTimeZone);
					else
						return startLocalDate.withMonthOfYear(initialMonthOfYear).withDayOfMonth(initialDayOfMonth).toDateTime(initialLocalTime, initialDateTimeZone);
				}
			}
		}
	}
	
	// utility methods for external clients for incrementing date time, code repeated in increment method, below, for speed
	public static DateTime lastRepeatBeforeDateTime(DateTime initialDateTime, DateTime limitDateTime, int intervalCode) {
		if (initialDateTime.compareTo(limitDateTime) > 0)
			return null; // if initial date is after limit, return null --- no last repeat
		
		LocalTime initialLocalTime = initialDateTime.toLocalTime();
		LocalDate initialLocalDate = initialDateTime.toLocalDate();
		DateTimeZone initialDateTimeZone = initialDateTime.getZone();
		DateTime limitDateTimeInInitialDateTimeZone = new DateTime(limitDateTime, initialDateTimeZone);
		LocalDate limitLocalDate = limitDateTimeInInitialDateTimeZone.toLocalDate();
		LocalTime limitLocalTime = limitDateTimeInInitialDateTimeZone.toLocalTime();
		
		if (intervalCode == HOURLY_BIT) {
			int initialMinute = initialLocalTime.getMinuteOfHour();
			int limitMinute = limitLocalTime.getMinuteOfHour();
			
			if (limitMinute == initialMinute) {
				return limitDateTimeInInitialDateTimeZone.minusHours(1);
			} else {
				LocalTime newRepeatLocalTime = limitLocalTime.withMinuteOfHour(initialMinute);
				if (initialMinute > limitMinute) {
					newRepeatLocalTime = newRepeatLocalTime.minusHours(1);
				}
				return limitLocalDate.toDateTime(newRepeatLocalTime, initialDateTimeZone);
			}
		} else {
			switch (intervalCode) {
				case DAILY_BIT:
				default:
					
				if (initialLocalTime.compareTo(limitLocalTime) >= 0)
					return limitLocalDate.minusDays(1).toDateTime(initialLocalTime, initialDateTimeZone);
				return limitLocalDate.toDateTime(initialLocalTime, initialDateTimeZone);

				case WEEKLY_BIT:
					
				int initialDayOfWeek = initialLocalDate.getDayOfWeek();
				int limitDayOfWeek = limitLocalDate.getDayOfWeek();
				
				if (initialDayOfWeek == limitDayOfWeek) {
					if (initialLocalTime.compareTo(limitLocalTime) >= 0)
						return limitLocalDate.minusWeeks(1).toDateTime(initialLocalTime, initialDateTimeZone);
					return limitLocalDate.toDateTime(initialLocalTime, initialDateTimeZone);
				} else if (initialDayOfWeek > limitDayOfWeek) {
					return limitLocalDate.withDayOfWeek(initialDayOfWeek).minusWeeks(1).toDateTime(initialLocalTime, initialDateTimeZone);
				} else
					return limitLocalDate.withDayOfWeek(initialDayOfWeek).toDateTime(initialLocalTime, initialDateTimeZone);
				
				case MONTHLY_BIT: {
					int initialDayOfMonth = initialLocalDate.getDayOfMonth();
					int limitDayOfMonth = limitLocalDate.getDayOfMonth();
					
					if (initialDayOfMonth == limitDayOfMonth) {
						if (initialLocalTime.compareTo(limitLocalTime) >= 0)
							return limitLocalDate.minusMonths(1).toDateTime(initialLocalTime, initialDateTimeZone);
						return limitLocalDate.toDateTime(initialLocalTime, initialDateTimeZone);
					} else if (initialDayOfMonth > limitDayOfMonth) {
						return limitLocalDate.withDayOfMonth(initialDayOfMonth).minusMonths(1).toDateTime(initialLocalTime, initialDateTimeZone);
					} else
						return limitLocalDate.withDayOfMonth(initialDayOfMonth).toDateTime(initialLocalTime, initialDateTimeZone);
				}
				
				case YEARLY_BIT: {
					int initialDayOfMonth = initialLocalDate.getDayOfMonth();
					int limitDayOfMonth = limitLocalDate.getDayOfMonth();
					int initialMonthOfYear = initialLocalDate.getMonthOfYear();
					int limitMonthOfYear = limitLocalDate.getMonthOfYear();

					if (initialMonthOfYear == limitMonthOfYear) {
						if (initialDayOfMonth == limitDayOfMonth) {
							if (initialLocalTime.compareTo(limitLocalTime) >= 0)
								return limitLocalDate.minusYears(1).toDateTime(initialLocalTime, initialDateTimeZone);
							return limitLocalDate.toDateTime(initialLocalTime, initialDateTimeZone);
						} else if (initialDayOfMonth > limitDayOfMonth)
							return limitLocalDate.withDayOfMonth(initialDayOfMonth).minusYears(1).toDateTime(initialLocalTime, initialDateTimeZone);
						else
							return limitLocalDate.withDayOfMonth(initialDayOfMonth).toDateTime(initialLocalTime, initialDateTimeZone);
					} else if (initialMonthOfYear > limitMonthOfYear)
						return limitLocalDate.withMonthOfYear(initialMonthOfYear).withDayOfMonth(initialDayOfMonth).minusYears(1)
								.toDateTime(initialLocalTime, initialDateTimeZone);
					else
						return limitLocalDate.withMonthOfYear(initialMonthOfYear).withDayOfMonth(initialDayOfMonth).toDateTime(initialLocalTime, initialDateTimeZone);
				}
			}
		}
	}
	
	public static DateTime incrementDateTime(DateTime currentDateTime, int amount, int intervalCode) {
		if (amount == 0) return currentDateTime;
		
		if (intervalCode == HOURLY_BIT) {
			return currentDateTime.plusHours(amount);
		} else {
			DateTimeZone dateTimeZone = currentDateTime.getZone();
			LocalDate localDate = currentDateTime.toLocalDate();
			LocalTime localTime = currentDateTime.toLocalTime();
			
			switch (intervalCode) {
				case DAILY_BIT:
				localDate = localDate.plusDays(amount);
				break;
					
				case WEEKLY_BIT:
				localDate = localDate.plusWeeks(amount);
				break;
					
				case MONTHLY_BIT:
				localDate = localDate.plusMonths(amount);
				break;
					
				case YEARLY_BIT:
				localDate = localDate.plusYears(amount);
				break;					
			}
			try {
				currentDateTime = localDate.toDateTime(localTime, dateTimeZone);
			} catch (Exception e) {
				// http://stackoverflow.com/questions/5451152/how-to-handle-jodatime-illegal-instant-due-to-time-zone-offset-transition
				localTime = localTime.plusHours(1);
				currentDateTime = localDate.toDateTime(localTime, dateTimeZone);
			}
		}

		return currentDateTime;
	}
	
	// increment date by interval specified in passed-in repeatType
	
	public DateTime increment() {
		return increment(1);
	}
	
	public DateTime increment(int amount) {
		if (amount == 0) return currentDateTime;
		
		if (intervalCode == HOURLY_BIT) {
			currentDateTime = currentDateTime.plusHours(amount);
			localDate = currentDateTime.toLocalDate();
			localTime = currentDateTime.toLocalTime();
		} else {
			switch (intervalCode) {
				case DAILY_BIT:
				localDate = localDate.plusDays(amount);
				break;
					
				case WEEKLY_BIT:
				localDate = localDate.plusWeeks(amount);
				break;
					
				case MONTHLY_BIT:
				localDate = localDate.plusMonths(amount);
				break;
					
				case YEARLY_BIT:
				localDate = localDate.plusYears(amount);
				break;					
			}
			try {
				currentDateTime = localDate.toDateTime(localTime, dateTimeZone);
			} catch (Exception e) {
				// http://stackoverflow.com/questions/5451152/how-to-handle-jodatime-illegal-instant-due-to-time-zone-offset-transition
				localTime = localTime.plusHours(1);
				currentDateTime = localDate.toDateTime(localTime, dateTimeZone);
			}
		}

		return currentDateTime;
	}

	public Long getTimestamp() {
		if (currentDateTime != null)
			return currentDateTime.getMillis();
		
		return null;
	}
	
	public DateTime getCurrentDateTime() {
		return currentDateTime;
	}
	
	public Long getMillis() {
		return currentDateTime.getMillis();
	}

	public Date toDate() {
		if (currentDateTime != null)
			return currentDateTime.toDate();
		
		return null;
	}
}
