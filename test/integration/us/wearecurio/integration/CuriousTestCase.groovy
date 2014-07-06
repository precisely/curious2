package us.wearecurio.integration
import grails.test.*
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

abstract class CuriousTestCase extends GroovyTestCase {

	void setUp() {
		super.setUp()
		
		Utils.resetForTesting()
	}

	void tearDown() {
		super.tearDown()
	}
}
