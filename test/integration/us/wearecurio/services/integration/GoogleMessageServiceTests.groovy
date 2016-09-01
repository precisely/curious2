package us.wearecurio.services.integration
/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class GoogleMessageServiceTests extends CuriousServiceTestCase {
	static transactional = true
	
	def devices
	def googleMessageService
	
	void setup() {
		devices = ["APA91bEDW_lcmjr1K-sH9yLOPF-dg-IKtb06WzVEp2rmyE3O8vxCw8DYt01kOLHGTlqqmJsLlnAju9fWbxm3HyZ40fONzRK9D-lO3N_ckA3DjQY7vRem4Z_pFUeN4ZJYN9cgcPcyaMH0e6BIHiaW1ro-NYLhu3LD4A"]
	}

	void cleanup() {
	}

    void testValidSendMessage() {
		given:
		def messageTxt = "Testing GCM"

		expect:
		assert googleMessageService.sendMessage(messageTxt, devices) == true
	}

	void testForNoDeviceIDs() {
		given:
		def messageTxt = "Testing GCM"
		devices = []

		expect:
		assert googleMessageService.sendMessage(messageTxt, devices) == false
	}
}
