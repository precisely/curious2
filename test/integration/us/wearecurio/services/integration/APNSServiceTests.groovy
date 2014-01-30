package us.wearecurio.services.integration

import us.wearecurio.services.APNSService

class APNSServiceTests {

	APNSService APNSService

	List devices

	void setUp() {
		devices = ["54f8158bbe5bd3fc0031c4fde5c6cfdc42e43b6a2fa67762c8d0bf1bd000e2fd"]
	}

	void testValidSendMessage() {
		def messageTxt = "Testing APNS"
		assert APNSService.sendMessage(messageTxt, devices) == true
	}

	void testForNoDeviceIDs() {
		def messageTxt = "Testing APNS"
		devices = []
		assert APNSService.sendMessage(messageTxt, devices) == false
	}
}