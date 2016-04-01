package us.wearecurio.services

//import grails.transaction.Transactional

import us.wearecurio.model.InitialLoginConfiguration
import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.User

class UserRegistrationService {

	static enum PromoCodeLookupResult {
		FOUND(0), INVALID(1), UNSPECIFIED(2)
	
		final Integer id
		
		static PromoCodeLookupResult get(int id) {
			if (id == 0) return FOUND
			else if (id == 1) return INVALID
			else return UNSPECIFIED
		}
		
		PromoCodeLookupResult(int id) {
			this.id = id
		}
		
		int getId() {
			return id
		}
	}
		
	static transactional = false
	
    Map register(User user, String promoCode=null) {
		String promo = promoCode?.trim()?.toLowerCase()
		PromoCodeLookupResult lookupResult
		InitialLoginConfiguration loginConfig
		if( promo != null && promo != "") {
			loginConfig = InitialLoginConfiguration.findByPromoCode(promo)
			if (loginConfig == null) {
				lookupResult = PromoCodeLookupResult.INVALID
			} else {
				lookupResult = PromoCodeLookupResult.FOUND
			}
		} else {
			lookupResult = PromoCodeLookupResult.UNSPECIFIED
		}
		
		if (loginConfig == null) {
			loginConfig = InitialLoginConfiguration.defaultConfiguration()
		}
		loginConfig.interestTags?.each{user.addInterestTag(Tag.create(it))}
		loginConfig.bookmarks?.each{Entry.createBookmark(user.id,it)}
		
		return [
			success: true, 
			initialLoginConfig: loginConfig, 
			promoCodeLookupResult: lookupResult
		]
    }
}
