package us.wearecurio.server

import org.apache.commons.logging.LogFactory

class DateRecord {

	private static def log = LogFactory.getLog(this)
	
	public static final int REMIND_EMAILS = 1
	public static final int ALERT_GENERATION = 2
	
	Integer code
	Date date
	
	static constraints = {
		code(unique:true)
		date(nullable:true)
	}

	public static lookup(Integer code) {
		log.debug "DateRecord.lookup() code:" + code
		DateRecord rec = DateRecord.findByCode(code)
		if (rec) return rec
		return new DateRecord(code, null)
	}
	
	public DateRecord() {
	}
	
	public DateRecord(code, date) {
		this.code = code
		this.date = date
	}
	
	String toString() {
		return "DateRecord(id:" + getId() + ", code:" + code + ", date:" + date + ")"
	}
}
