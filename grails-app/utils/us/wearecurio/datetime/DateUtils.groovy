package us.wearecurio.datetime

class DateUtils {

	static Date getEndOfTheDay(Date date = new Date()) {
		Calendar cal = Calendar.getInstance()
		cal.setTime(date)
		cal.set(Calendar.HOUR_OF_DAY, 23)
		cal.set(Calendar.MINUTE, 59)
		cal.set(Calendar.SECOND, 59)
		cal.set(Calendar.MILLISECOND, 999)
		return cal.getTime()
	}

	static Date getStartOfTheDay(Date date = new Date()) {
		Calendar cal = Calendar.getInstance()
		cal.setTime(date)
		cal.set(Calendar.HOUR_OF_DAY, 0)
		cal.set(Calendar.MINUTE, 0)
		cal.set(Calendar.SECOND, 0)
		cal.set(Calendar.MILLISECOND, 000)
		return cal.getTime()
	}
}