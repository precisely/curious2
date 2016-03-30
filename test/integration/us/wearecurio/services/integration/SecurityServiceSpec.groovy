package us.wearecurio.services.integration

import grails.test.spock.IntegrationSpec
import us.wearecurio.services.SecurityService

class SecurityServiceSpec extends IntegrationSpec {

	SecurityService securityService
    void "test populateNoAuthMethods"() {
		given: "noauthActions map"
		securityService.noauthActions = [:]

		when: "populateNoAuthMethods method is called"
		securityService.populateNoAuthMethods()

		then: "noauthActions map is populated with controllers mapped to noAuth actions in that controller"
		securityService.noauthActions.login.containsAll(["register", "recover", "login", "dologin", 
				"dologinData", "forgot", "doforgot", "doforgotData", "dorecover", "doregister", "doregisterData"])
		securityService.noauthActions.admin.containsAll(["register", "recover", "login", "dologin", 
				"dologinData", "forgot", "doforgot", "doforgotData", "dorecover", "doregister", "doregisterData"])
		securityService.noauthActions.data.containsAll(["register", "recover", "login", "dologin", 
				"dologinData", "forgot", "doforgot", "doforgotData", "dorecover", "doregister", "doregisterData", "loadSnapshotDataId", "getPeopleData"])
		securityService.noauthActions.home.containsAll(["register", "recover", "login", "dologin", 
				"dologinData", "forgot", "doforgot", "doforgotData", "dorecover", "doregister", "doregisterData", "notifywithings", "notifyfitbit", 
			"termsofservice_home", "homepage", "social", "loadSnapshotDataId", "getPeopleData"])
		securityService.noauthActions.discussion.containsAll(["register", "recover", "login", "dologin", 
				"dologinData", "forgot", "doforgot", "doforgotData", "dorecover", "doregister", "doregisterData", "show"])
		securityService.noauthActions.discussionPost.containsAll(["register", "recover", "login", "dologin", 
				"dologinData", "forgot", "doforgot", "doforgotData", "dorecover", "doregister", "doregisterData", "save"])

    }
}
