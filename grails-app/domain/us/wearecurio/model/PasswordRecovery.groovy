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
		version false
		table 'password_recovery'
		start column:'start', index:'start_index'
		userId column:'user_id', index:'user_id_index'
	}
	
	PasswordRecovery() {}

	PasswordRecovery(long userId) {
		this.start = new Date()
		this.userId = userId
		Random random = new Random(start.getTime() - 91242273L)
		this.code = '' + (random.nextLong() + start.getTime()) + (random.nextLong() + 1429L) + userId
	}
	
	static PasswordRecovery create(long userId) {
		def recovery = new PasswordRecovery(userId)
		Utils.save(recovery, true)
		
		return recovery
	}
	
	static def delete(PasswordRecovery recovery) {
		recovery.delete(flush:true)
	}
	
	public static final long DAYTICKS = 24L * 60L * 60000L
	
	protected static long getStaleAgoTicks() {
		return new Date().getTime() - DAYTICKS;
	}
	
	// reset created to prevent expiration for another hour
	def reset() {
		start = new Date()
	}
	
	static PasswordRecovery look(String code) {
		Date now = new Date()
		
		PasswordRecovery retVal = PasswordRecovery.findByCode(code)
		
		if (retVal == null || retVal.getStart().getTime() < getStaleAgoTicks()) {
			return null
		}
		
		return retVal
	}
	
	static deleteStale() {
		def c = PasswordRecovery.createCriteria()
		
		def hourAgo = new Date(getStaleAgoTicks())
		
		PasswordRecovery.executeUpdate("delete PasswordRecovery p where p.start < :hourAgo",
				[hourAgo:hourAgo])
	}
}
