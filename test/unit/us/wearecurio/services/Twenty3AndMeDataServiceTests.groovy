package us.wearecurio.services

import grails.test.mixin.*
import uk.co.desirableobjects.oauth.scribe.OauthService;
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.Twenty3AndMeData
import us.wearecurio.model.User

@TestFor(Twenty3AndMeDataService)
@Mock([OAuthAccount, Twenty3AndMeData, /*User*/])
class Twenty3AndMeDataServiceTests {
	
	def oauthServiceMock

	void setUp() {
		User userInstance = new User(username: "dummy", email: "dummy@curious.test", sex: "M")
		assert userInstance.save()
		
		/*oauthServiceMock = mockFor(OauthService)
		oauthServiceMock.demand.getTwenty3AndMeResource { token, url -> return "" }
		service.oauthService = oauthServiceMock.createMock()*/
	}

	void tearDown() {
	}

	void testGetUserProfiles() {
		//service.getUserProfiles(null)
	}

}