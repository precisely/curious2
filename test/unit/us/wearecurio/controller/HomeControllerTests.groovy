package us.wearecurio.controller


import grails.test.GrailsMock
import grails.test.mixin.*
import grails.test.mixin.support.*

import org.junit.*

import us.wearecurio.controller.HomeController;
import us.wearecurio.model.User
import us.wearecurio.services.FitBitDataService;
import us.wearecurio.services.SecurityService
import us.wearecurio.services.UrlService;
import us.wearecurio.services.WithingsDataService

@TestMixin(GrailsUnitTestMixin)
@Mock([User])
@TestFor(HomeController)
class HomeControllerTests {

	GrailsMock fitBitDataServiceMock
	GrailsMock securityServiceMock
	GrailsMock urlServiceMock
	GrailsMock withingsDataServiceMock

	void setUp() {
		fitBitDataServiceMock = mockFor(FitBitDataService)
		securityServiceMock = mockFor(SecurityService)
		urlServiceMock = mockFor(UrlService)
		withingsDataServiceMock = mockFor(WithingsDataService)

		User userInstance = new User([username: "dummy", email: "dummy@curious.test", sex: "M", first: "John", last: "Day",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: false])

		urlServiceMock.demand.make { map, req ->
			return map.controller + "/" + map.action + "?" + map.params?.collect { key, value -> "$key=$value" }?.join("&")
		}
		controller.urlService = urlServiceMock.createMock()

		securityServiceMock.demand.getCurrentUser(1..2) { ->
			return User.get(1)
		}
		controller.securityService = securityServiceMock.createMock()

		assert userInstance.save()
	}

	void tearDown() {
	}

	void testUnregisterwithings() {
		withingsDataServiceMock.demand.unSubscribe { userId ->
			return [success: true]
		}
		controller.withingsDataService = withingsDataServiceMock.createMock()

		controller.unregisterwithings()
		assert controller.flash.message == "withings.unsubscribe.success.message"
		assert response.redirectUrl.contains("home/userpreferences")
	}

	void testUnregisterfitbit() {
		fitBitDataServiceMock.demand.unSubscribe { userId ->
			return [success: true]
		}
		controller.fitBitDataService = fitBitDataServiceMock.createMock()

		controller.unregisterfitbit()
		assert controller.flash.message == "fitbit.unsubscribe.success.message"
		assert response.redirectUrl.contains("home/userpreferences")
	}

}