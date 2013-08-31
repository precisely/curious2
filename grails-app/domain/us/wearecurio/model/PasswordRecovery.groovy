package us.wearecurio.model

import org.apache.commons.logging.LogFactory

import java.sql.ResultSet
import java.util.TreeSet
import us.wearecurio.utility.Utils
import java.util.Random

class PasswordRecovery {

	private static def log = LogFactory.getLog(this)

	Date start
	Long userId
	String code
	
	static constraints = {
		code(maxSize:50)
	}
	static mapping = {
		table 'password_recovery'
		start column:'start', index:'start_index'
		userId column:'user_id', index:'user_id_index'
	}
	
	public PasswordRecovery() {}

	public PasswordRecovery(long userId) {
		this.start = new Date()
		this.userId = userId
		Random random = new Random(start.getTime() - 91242273L)
		this.code = '' + (random.nextLong() + start.getTime()) + (random.nextLong() + 1429L) + userId
	}
	
	static public PasswordRecovery create(long userId) {
		def recovery = new PasswordRecovery(userId)
		Utils.save(recovery, true)
		
		return recovery
	}
	
	static def delete(PasswordRecovery recovery) {
		recovery.delete()
	}
	
	public static final long HOURTICKS = 60L * 60000L
	
	protected static long getHourAgoTicks() {
		return new Date().getTime() - HOURTICKS;
	}
	
	// reset created to prevent expiration for another hour
	def reset() {
		start = new Date()
	}
	
	static public PasswordRecovery look(String code) {
		Date now = new Date()
		
		PasswordRecovery retVal = PasswordRecovery.findByCode(code)
		
		if (retVal == null || retVal.getStart().getTime() < getHourAgoTicks()) {
			return null
		}
		
		return retVal
	}
	
	static public deleteStaleRecoveries() {
		def c = PasswordRecovery.createCriteria()
		
		def hourAgo = new Date(getHourAgoTicks())
		
		def results = c {
			lt("start", hourAgo)
		}
		
		for (PasswordRecovery r in results) {
			r.delete()
		}
	}
}
