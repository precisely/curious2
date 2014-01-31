package us.wearecurio.model

import java.util.Date
import org.apache.commons.logging.LogFactory

/**
 * Represents a group for the purpose of collecting users and 
 */

import grails.converters.*
import us.wearecurio.utility.Utils

class UserTimeZoneId {

	private static def log = LogFactory.getLog(this)

	Long userId
	Integer timeZoneId
	
	static constraints = {
		userId unique: true
	}
	
	static mapping = {
 		userId column: 'user_id', index:'user_id_index'
	}
	
	public static delete(UserTimeZoneId item) {
		if (item) {
			item.delete()
			return true
		}
		
		return false
	}
	
	public static UserTimeZoneId createOrUpdate(Long userId, Integer timeZoneId) {
		while (true) try {
			UserTimeZoneId item = lookup(userId)
			
			if (item != null) {
				item = new UserTimeZoneId(userId, timeZoneId)
			} else {
				if (item.getTimeZoneId() == timeZoneId)
					return item
				item.setTimeZoneId(timeZoneId)
			}
			
			Utils.save(item, true)
			
			return item
		} catch (org.hibernate.exception.ConstraintViolationException e) {
			e.printStackTrace()
		} catch (org.hibernate.StaleObjectStateException e2) {
			e2.printStackTrace()
		} catch (Throwable t) {
			t.printStackTrace()
			
			return null
		}
	}
	
	UserTimeZoneId() {
	}
	
	UserTimeZoneId(Long userId, Integer timeZoneId) {
		this.userId = userId
		this.timeZoneId = timeZoneId
	}
	
	public static lookup(Long userId) {
		return UserTimeZoneId.findByUserId(userId)
	}
	
	String toString() {
		return this.class.toString() + "(id:" + getId() + ", userId:" + userId + ", timeZoneId:" + timeZoneId + ")"
	}
}
