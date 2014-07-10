import static org.junit.Assert.*
import grails.test.*
import grails.test.mixin.*

import org.junit.*

import us.wearecurio.integration.CuriousTestCase;
import us.wearecurio.server.Migration
import us.wearecurio.services.MigrationService

class MigrationServiceTests extends CuriousTestCase {
	static transactional = true

	def migrationService

	@Before
	void setUp() {
		super.setUp()
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testDoMigration() {
		def didMigration = false

		// Confirm no existance of test migration id.
		Migration.findByCode(MigrationService.TEST_MIGRATION_ID)?.delete()

		migrationService.tryMigration(MigrationService.TEST_MIGRATION_ID) {
			didMigration = true
		}

		assertTrue(didMigration)

		assert migrationService.shouldDoMigration(MigrationService.TEST_MIGRATION_ID) == null
	}
}
