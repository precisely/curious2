package us.wearecurio.services



import grails.test.mixin.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(GCMService)
class GCMServiceTests {
	def devices
	void setUp() {
		devices = ["APA91bEDW_lcmjr1K-sH9yLOPF-dg-IKtb06WzVEp2rmyE3O8vxCw8DYt01kOLHGTlqqmJsLlnAju9fWbxm3HyZ40fONzRK9D-lO3N_ckA3DjQY7vRem4Z_pFUeN4ZJYN9cgcPcyaMH0e6BIHiaW1ro-NYLhu3LD4A"]
	}

    void testValidSendMessage() {
		def messageTxt = "Testing GCM"
        assert service.sendMessage(messageTxt, devices) == true
    }
	
	void testForNoDeviceIDs() {
		def messageTxt = "Testing GCM"
		devices = []
		assert service.sendMessage(messageTxt, devices) == false
	}
}
