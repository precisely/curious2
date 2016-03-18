package us.wearecurio.filters

import grails.test.spock.IntegrationSpec

class EmailVerificationCheckFiltersSpec extends IntegrationSpec{

	void "Test populateEmailVerificationEndpoints"() {

		when:
		EmailVerificationCheckFilters.populateEmailVerificationEndpoints()

		then:
		List<Map> verificationEndpoints = EmailVerificationCheckFilters.emailVerificationEndpoints
		verificationEndpoints.find {it.controller == "data"}?.actions?.contains("saveSnapshotData") == true
		verificationEndpoints.find {it.controller == "discussionPost"}?.actions?.contains("save") == true
		verificationEndpoints.find {it.controller == "discussionPost"}?.actions?.contains("delete") == true
		verificationEndpoints.find {it.controller == "home"}?.actions?.contains("saveSnapshotData") == true
		verificationEndpoints.find {it.controller == "mobiledata"}?.actions?.contains("saveSnapshotData") == true
		verificationEndpoints.find {it.controller == "discussion"}?.actions?.contains("save") == true
	}
}
