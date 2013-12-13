package us.wearecurio.model

import org.apache.commons.logging.LogFactory

class PushNotificationDevice {
	private static def log = LogFactory.getLog(this)
	
	public static int IOS_DEVICE = 1
	public static int ANDROID_DEVICE = 2
	
	Integer deviceType
	String token
	Long userId
	
	Date dateCreated
	Date lastUpdated
	
    static constraints = {
		deviceType inList:[IOS_DEVICE,ANDROID_DEVICE]
    }
	
	static mapping = {
		table 'push_notification_device'
		token type: 'text'
		userId column:'user_id', index:'user_id_index'
	}
}
