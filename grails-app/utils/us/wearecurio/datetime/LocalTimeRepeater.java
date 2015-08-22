package us.wearecurio.datetime;

import java.util.Date;

import org.joda.time.*;

public class LocalTimeRepeater<T> {

	DateTime currentDateTime;
	long endDateTimeTicks;
	DateTimeZone dateTimeZone;
	LocalDate localDate;
	LocalTime localTime;
	T payload;

	public LocalTimeRepeater(T payload, DateTime dateTime, Long endDateTimeTicks) {
		currentDateTime = dateTime;
		this.endDateTimeTicks = endDateTimeTicks;
		dateTimeZone = dateTime.getZone();
		localDate = dateTime.toLocalDate();
		localTime = dateTime.toLocalTime();
		this.payload = payload;
	}

	public DateTime incrementDate() {
		localDate = localDate.plusDays(1);
		// http://stackoverflow.com/questions/5451152/how-to-handle-jodatime-illegal-instant-due-to-time-zone-offset-transition
		try {
			currentDateTime = localDate.toDateTime(localTime, dateTimeZone);
		} catch(Exception e) {
			localTime = localTime.plusHours(1);
			currentDateTime = localDate.toDateTime(localTime, dateTimeZone);
		}

		if (currentDateTime.getMillis() > endDateTimeTicks) {
			return currentDateTime = null;
		}

		return currentDateTime;
	}

	public boolean isActive() {
		return currentDateTime != null && currentDateTime.getMillis() <= endDateTimeTicks;
	}

	public Long getTimestamp() {
		if (currentDateTime != null)
			return currentDateTime.getMillis();
		
		return null;
	}
	
	public DateTime getCurrentDateTime() {
		return currentDateTime;
	}

	public Date getDate() {
		if (currentDateTime != null)
			return currentDateTime.toDate();
		return null;
	}
	
	public T getPayload() {
		return payload;
	}
}
