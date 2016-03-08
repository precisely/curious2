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
	Integer type
	
	static final Integer PASSWORD_TYPE = 1
	static final Integer VERIFICATION_TYPE = 2
	
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

	PasswordRecovery(long userId, Integer type = PASSWORD_TYPE) {
		this.start = new Date()
		this.userId = userId
		Random random = new Random(start.getTime() - 91242273L)
		this.code = '' + (random.nextLong() + start.getTime()) + (random.nextLong() + 1429L) + userId
		this.type = type
	}
	
	static PasswordRecovery create(long userId, Integer type = PASSWORD_TYPE) {
		def recovery = new PasswordRecovery(userId, type)
		Utils.save(recovery, true)
		
		return recovery
	}
	
	static PasswordRecovery createVerification(long userId, Integer type = VERIFICATION_TYPE) {
		def recovery = new PasswordRecovery(userId, type)
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
	
	static PasswordRecovery look(String code, Integer type = PASSWORD_TYPE) {
		Date now = new Date()
		
		PasswordRecovery retVal = PasswordRecovery.findByCodeAndType(code, type)
		
		if (retVal == null || retVal.getStart().getTime() < getStaleAgoTicks()) {
			return null
		}
		
		return retVal
	}
	
	static PasswordRecovery lookVerification(String code) {
		return look(code, VERIFICATION_TYPE)
	}
	
	static deleteStale() {
		def c = PasswordRecovery.createCriteria()
		
		def hourAgo = new Date(getStaleAgoTicks())
		
		PasswordRecovery.executeUpdate("delete PasswordRecovery p where p.start < :hourAgo",
				[hourAgo:hourAgo])
	}
}
