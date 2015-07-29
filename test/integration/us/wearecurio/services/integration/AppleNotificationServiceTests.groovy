package us.wearecurio.services.integration

import org.junit.*

import us.wearecurio.services.AppleNotificationService

class AppleNotificationServiceTests extends CuriousServiceTestCase {
	static transactional = true
	
	AppleNotificationService appleNotificationService
	
	List devices
	
	@Before
	void setUp() {
		super.setUp()
		devices = [
			"54f8158bbe5bd3fc0031c4fde5c6cfdc42e43b6a2fa67762c8d0bf1bd000e2fd"
		]
	}
	
	@After
	void tearDown() {
		super.tearDown()
	}
	
	// Note: this test requires that the following file exists:
	// 
	// ${userHome}/ios-cert/dev/iphone_dev.p12  
	// (see, pushNotification.apns.pathToCertificate in the Config.groovy file)
	// where ${userHome} is the home directory of your logged-in user
	// for example, on a mac, ${userHome} would be, /Users/myUserName
	//
	// The file can be found in the grails project at /ios-cert/dev/iphone_dev.p12
	@Test
	void testValidSendMessage() {
		def messageTxt = "Testing APNS"
		assert appleNotificationService.sendMessage(messageTxt, devices) == true
	}
	
	@Test
	void testForNoDeviceIDs() {
		def messageTxt = "Testing APNS"
		devices = []
		assert appleNotificationService.sendMessage(messageTxt, devices) == false
	}
}
