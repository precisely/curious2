package us.wearecurio.datetime;

import grails.converters.*

import org.apache.commons.logging.LogFactory

import us.wearecurio.model.Entry
import us.wearecurio.services.DatabaseService
import us.wearecurio.utility.Utils

import org.joda.time.*

class LocalTimeRepeater {

	private static def log = LogFactory.getLog(this)

	DateTime currentDateTime
	long endDateTimeTicks
	DateTimeZone dateTimeZone
	LocalDate localDate
	LocalTime localTime
	def payload

	public LocalTimeRepeater(payload, DateTime dateTime, Long endDateTimeTicks) {
		currentDateTime = dateTime
		this.endDateTimeTicks = endDateTimeTicks
		dateTimeZone = dateTime.getZone()
		localDate = dateTime.toLocalDate()
		localTime = dateTime.toLocalTime()
		this.payload = payload
	}

	public DateTime incrementDate() {
		localDate = localDate.plusDays(1)
		// http://stackoverflow.com/questions/5451152/how-to-handle-jodatime-illegal-instant-due-to-time-zone-offset-transition
		try {
			currentDateTime = localDate.toDateTime(localTime, dateTimeZone)
		} catch(e) {
			localTime = localTime.plusHours(1)
			currentDateTime = localDate.toDateTime(localTime, dateTimeZone)
		}

		if (currentDateTime.getMillis() >= endDateTimeTicks) {
			return currentDateTime = null
		}

		return currentDateTime
	}

	def isActive() {
		return currentDateTime != null && currentDateTime.getMillis() <= endDateTimeTicks
	}

	Long getTimestamp() {
		return currentDateTime?.getMillis()
	}

	Date getDate() {
		return currentDateTime?.toDate()
	}
}
