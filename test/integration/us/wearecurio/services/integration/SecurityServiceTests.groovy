package us.wearecurio.services.integration

import static org.junit.Assert.*

import java.math.MathContext
import java.text.DateFormat

import org.junit.After
import org.junit.Before
import org.junit.Test

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.Entry
import us.wearecurio.data.RepeatType
import us.wearecurio.model.DurationType
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.TagValueStats;
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.model.Sprint
import us.wearecurio.model.Model.Visibility
import us.wearecurio.support.EntryStats
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.services.SecurityService
import us.wearecurio.services.SecurityService.AuthenticationStatus
import us.wearecurio.services.EntryParserService
import groovy.transform.TypeChecked

import org.joda.time.DateTimeZone
import org.junit.*
import org.joda.time.*

import grails.test.mixin.*
import us.wearecurio.utility.Utils
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.Integration
import grails.test.mixin.integration.IntegrationTestMixin

@Integration
class SecurityServiceTests extends CuriousTestCase {
	static transactional = true
	
	SecurityService securityService

	@Before
	void setUp() {
		super.setUp()
		
		Locale.setDefault(Locale.US)	// For to run test case in any country.
		Utils.resetForTesting()
	}
	
	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testAuthStatus() {
		assert securityService.login("y", "y") == user
		AuthenticationStatus status = securityService.authFromUserId(user.id)
		assert status.authorized
		assert status.primary
		assert status.sprint == null
		securityService.logout()
		status = securityService.authFromUserId(user.id)
		assert !status.authorized

		Sprint sprint = Sprint.create(new Date(), user, "Caffeine + Sugar", Visibility.PUBLIC)
		assert securityService.login("y", "y") == user
		status = securityService.authFromUserId(sprint.virtualUserId)
		assert status.authorized
		assert !status.primary
		assert status.sprint == sprint
	}
/**/
}
