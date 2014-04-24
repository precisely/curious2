package us.wearecurio.model

import java.util.Date
import org.apache.commons.logging.LogFactory

/**
 * Represents a group for the purpose of collecting users and 
 */

import grails.converters.*
import us.wearecurio.utility.Utils

class UserTimeZone {

	private static def log = LogFactory.getLog(this)
	
	static mapping = {
		version false
	}

	Long userId
	Integer timeZoneId
	
	public static UserTimeZone createOrUpdate(Long userId, Integer timeZoneId) {
		while (true) try {
			UserTimeZone item = UserTimeZone.lookup(userId)
			if (item != null) {
				if (item.getTimeZoneId() == timeZoneId)
					return item
				item.setTimeZoneId(timeZoneId)
			} else {
				item = new UserTimeZone(userId, timeZoneId)
			}
			
			Utils.save(item, true)
			
			return item			
		} catch (org.hibernate.exception.ConstraintViolationException e) {
			e.printStackTrace()
		} catch (org.hibernate.StaleObjectStateException e2) {
			e2.printStackTrace()
		} catch (Throwable t) {
			t.printStackTrace()			
		}
		
		return null
	}
	
	public UserTimeZone() {
	}
	
	public UserTimeZone(Long userId, Integer timeZoneId) {
		this.userId = userId
		this.timeZoneId = timeZoneId
	}
	
	static public UserTimeZone lookup(Long userId) {
		return UserTimeZone.findByUserId(userId)
	}
}
