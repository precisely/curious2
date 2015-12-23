package us.wearecurio.datetime

class DateUtils {

	static Long getEndOfTheDay(Date startDate) {
		Calendar cal = Calendar.getInstance()
		cal.setTime(startDate)
		cal.set(Calendar.HOUR_OF_DAY, 23)
		cal.set(Calendar.MINUTE, 59)
		cal.set(Calendar.SECOND, 59)
		cal.set(Calendar.MILLISECOND, 0)
		Long endTime = cal.getTimeInMillis()
		return endTime
	}

}
