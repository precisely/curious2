import grails.test.*

import us.wearecurio.services.MigrationService
import us.wearecurio.server.Session
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

class MigrationServiceTests extends GroovyTestCase {
	static transactional = true

	def migrationService
	
	@Before
	void setUp() {
	}

	@After
	void tearDown() {
	}

	@Test
	void testDoMigration() {
		def didMigration = false
		
		migrationService.tryMigration(MigrationService.TEST_MIGRATION_ID) {
			didMigration = true
		}
		
		assertTrue(didMigration)
		
		assert migrationService.shouldDoMigration(MigrationService.TEST_MIGRATION_ID) == null
	}
}
