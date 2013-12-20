package us.wearecurio.services



import grails.test.mixin.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(APNSService)
class APNSServiceTests {
	def devices
	void setUp() {
		devices = ["54f8158bbe5bd3fc0031c4fde5c6cfdc42e43b6a2fa67762c8d0bf1bd000e2fd"]
	}

	void testValidSendMessage() {
		def messageTxt = "Testing APNS"
		assert service.sendMessage(messageTxt, devices) == true
	}
	
	void testForNoDeviceIDs() {
		def messageTxt = "Testing APNS"
		devices = []
		assert service.sendMessage(messageTxt, devices) == false
	}
    
}
