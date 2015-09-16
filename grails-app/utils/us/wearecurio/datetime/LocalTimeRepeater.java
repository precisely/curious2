package us.wearecurio.datetime;

import java.util.Date;

import org.joda.time.*;

public class LocalTimeRepeater<T> {

	IncrementingDateTime incrementingDateTime;
	long endDateTimeTicks;
	T payload;

	public LocalTimeRepeater(T payload, DateTime dateTime, Long endDateTimeTicks, int intervalCode) {
		incrementingDateTime = new IncrementingDateTime(dateTime, intervalCode);
		this.endDateTimeTicks = endDateTimeTicks;
		this.payload = payload;
	}

	public DateTime incrementDate() {
		if (incrementingDateTime != null) {			
			DateTime newDateTime = incrementingDateTime.increment();
			
			if (newDateTime.getMillis() > endDateTimeTicks) {
				incrementingDateTime = null;
				return null;
			}

			return newDateTime;
		}
		
		return null;
	}

	public boolean isActive() {
		return incrementingDateTime != null && incrementingDateTime.getCurrentDateTime().getMillis() <= endDateTimeTicks;
	}

	public Long getTimestamp() {
		if (incrementingDateTime != null)
			return incrementingDateTime.getMillis();
		
		return null;
	}
	
	public DateTime getCurrentDateTime() {
		if (incrementingDateTime != null)
			return incrementingDateTime.getCurrentDateTime();
		
		return null;
	}

	public Date getDate() {
		if (incrementingDateTime != null)
			return incrementingDateTime.toDate();
		
		return null;
	}
	
	public T getPayload() {
		return payload;
	}
}
