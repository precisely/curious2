package us.wearecurio.utility

import org.apache.commons.logging.LogFactory

import grails.plugin.mail.MailService
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.SimpleTimeZone
import java.util.Calendar

import org.joda.time.*

/**
 *
 * @author mitsu
 */
class Utils {
	
	public static final long THIRTY_SECONDS = 30000L;
	public static final long FIVE_MINUTES = 5 * 60000L;
	public static final long HOUR = 60 * 60000L;
	public static final long DAY = 24 * 60 * 60000L;
	
	private static def log = LogFactory.getLog(this)

	static def listJSONDesc(list) {
		def retVal = []
		for (obj in list) {
			if (obj instanceof Map)
				retVal.add(obj)
			else
				retVal.add(obj.getJSONDesc())
		}

		return retVal;
	}

	static def listJSONShortDesc(list) {
		def retVal = []
		for (obj in list) {
			retVal.add(obj.getJSONShortDesc())
		}

		return retVal;
	}
	
	static def save(obj) {
		return save(obj, false)
	}

	static def save(obj, boolean flush) {
		if (!obj.save(flush:flush)) {
			log.debug "Error saving " + obj.toString()
			obj.errors.each {
				log.debug it
			}
			return false;
		} else {
			log.debug "Object saved successfully " + obj.toString()
		}

		return true;
	}

	/**
	 * Simple utility method to do equals in a null-safe manner
	 */
	static boolean equals(a, b) {
		if (a == null) return b == null
		if (b == null) return false
		if (a == b) return true
		return a.equals(b)
	}
	
	/**
	 *
	 * Save object but ignore stale object exception
	 *
	 */
	static def trySave(obj) {
		try {
			return save(obj, true)
		} catch (org.hibernate.StaleObjectStateException e) {
			log.debug "Object stale, not saved: " + obj
			return false
		}
	}
	
	/**
	 * Offset is in minutes
	 */
	private static def Map<String, TimeZone> timeZones = new HashMap<Integer, TimeZone>()

	// offset is in seconds, not milliseconds
	static TimeZone createTimeZone(int offset, String idStr, boolean dst) {
		Integer intOffset = offset;

		TimeZone tz = timeZones.get(idStr)

		if (tz != null) return tz;

		if (dst)
			tz = new SimpleTimeZone(offset * 1000, idStr,
					Calendar.APRIL, 1, -Calendar.SUNDAY,
					7200000,
					Calendar.OCTOBER, -1, Calendar.SUNDAY,
					7200000,
					3600000)
		else
			tz = new SimpleTimeZone(offset * 1000, idStr)

		timeZones.put(idStr, tz)

		return tz
	}
	
	static DateFormat gmtDateFormat

	static {
		Calendar gmtCalendar = Calendar.getInstance(new SimpleTimeZone(0, "GMT"))
		gmtDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
		gmtDateFormat.setCalendar(gmtCalendar)
	}

	static String dateToGMTString(Date date) {
		return date == null ? "null" : gmtDateFormat.format(date)
	}
	
	static MailService mailService
	
	public static setMailService(MailService service) { mailService = service }
	
	public static MailService getMailService() { return mailService }

}
