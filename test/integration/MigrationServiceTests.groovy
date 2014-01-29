import static org.junit.Assert.*
import grails.test.*
import grails.test.mixin.*

import org.junit.*

import us.wearecurio.server.Migration
import us.wearecurio.services.MigrationService

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

		// Confirm no existance of test migration id.
		Migration.findByCode(MigrationService.TEST_MIGRATION_ID)?.delete()

		migrationService.tryMigration(MigrationService.TEST_MIGRATION_ID) {
			didMigration = true
		}

		assertTrue(didMigration)

		assert migrationService.shouldDoMigration(MigrationService.TEST_MIGRATION_ID) == null
	}
}
