import static org.junit.Assert.*
import grails.test.*
import grails.test.mixin.*

import org.junit.*

import us.wearecurio.integration.CuriousTestCase;
import us.wearecurio.server.Migration
import us.wearecurio.services.MigrationService
import us.wearecurio.model.User

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

		// Confirm no existence of test migration id.
		Migration.findByTag("" + MigrationService.TEST_MIGRATION_ID)?.delete()

		migrationService.tryMigration(MigrationService.TEST_MIGRATION_ID) {
			didMigration = true
		}

		assertTrue(didMigration)

		assert migrationService.shouldDoMigration(MigrationService.TEST_MIGRATION_ID) == null
	}
	
	@Test
	void "Test migrateUserVirtualUserGroupId"() {
		Map params = new HashMap()
		params.put("username", "testuser")
		params.put("email", "test@test.com")
		params.put("password", "eiajrvaer")
		params.put("first", "first")
		params.put("last", "last")
		params.put("sex", "F")
		params.put("birthdate", "12/1/1990")
		
		def user = User.create(params)
		user.virtualUserGroupId = null
		
		def users = User.withCriteria{ isNull "virtualUserGroupId" }
		
		assert users
		assert users.size() > 0
		assert users.find{ it -> it.id == user.id }

		MigrationService.migrateUserVirtualUserGroupId()
		
		users = User.withCriteria{ isNull "virtualUserGroupId" }
		
		assert users.find{ it -> it.id == user.id } == null
	}
}
