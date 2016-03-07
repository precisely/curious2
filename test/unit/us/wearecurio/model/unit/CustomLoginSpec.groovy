package us.wearecurio.model.unit

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

import us.wearecurio.model.CustomLogin

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@Mock(CustomLogin)
class CustomLoginSpec extends Specification {
    void "test default CustomLogin"() {
		when: "given a default CustomLogin"
		CustomLogin login = CustomLogin.defaultCustomLogin()
		
		then: "defaults are correctly set"
		login.promoCode == CustomLogin.defaultPromoCode
		login.customQuestion1 == "Does caffeine affect my sleep?"
    }
}
