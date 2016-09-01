package us.wearecurio.services.integration

import us.wearecurio.services.AppleNotificationService

class AppleNotificationServiceTests extends CuriousServiceTestCase {
	static transactional = true
	
	AppleNotificationService appleNotificationService
	
	List devices
	
	void setup() {
		devices = [
			"54f8158bbe5bd3fc0031c4fde5c6cfdc42e43b6a2fa67762c8d0bf1bd000e2fd"
		]
	}
	
	void cleanup() {
	}
	
	// Note: this test requires that the following file exists:
	// 
	// ${userHome}/ios-cert/dev/iphone_dev.p12  
	// (see, pushNotification.apns.pathToCertificate in the Config.groovy file)
	// where ${userHome} is the home directory of your logged-in user
	// for example, on a mac, ${userHome} would be, /Users/myUserName
	//
	// The file can be found in the grails project at /ios-cert/dev/iphone_dev.p12
	void testValidSendMessage() {
		given:
		def messageTxt = "Testing APNS"
		
		expect:
		assert appleNotificationService.sendMessage(messageTxt, devices) == true
	}
	
	void testForNoDeviceIDs() {
		given:
		def messageTxt = "Testing APNS"
		devices = []
		
		expect:
		assert appleNotificationService.sendMessage(messageTxt, devices) == false
	}
}
