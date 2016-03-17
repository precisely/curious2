package us.wearecurio.services

import grails.test.mixin.TestFor
import spock.lang.Specification

import us.wearecurio.data.BitSet
import us.wearecurio.data.UserSettings

import us.wearecurio.model.InitialLoginConfiguration
import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.User

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(UserRegistrationService)
@Mock([User,InitialLoginConfiguration,Entry,Tag])
class UserRegistrationServiceSpec extends Specification {

	User user
	
    def setup() {
		user = new User()
		user.username = 'shane'
		user.sex = 'F'
		user.name = 'shane macgowen'
		user.email = 'shane@pogues.com'
		user.birthdate = '01/01/1960'
		user.password = 'shanexyz'
		user.action = 'doregister'
		user.controller ='home'
		user.save(true)
    }

    def cleanup() {
    }

    void "test setUserPromoCode with no promo code"() {
		given: "the default InitialLoginConfiguration"
		InitialLoginConfiguration login = InitialLoginConfiguration.default()
		
		when: "setUserPromoCode is called"
		Map results = service.setUserPromoCode(user)
		
		then: "user"
		results
		results.success
		results.customLogin.promoCode == InitialLoginConfiguration.DEFAULT_PROMO_CODE
    }
	
//    void "test setUserPromoCode with promo code"() {
//		given: "a promo code"
//		
//		
//		when: ""
//		then: ""
//    }
//
//    void "test setUserPromoCode with blank promo code"() {
//		given: "a promo code"
//		
//		
//		when: ""
//		then: ""
//    }

}
