package us.wearecurio.services

import us.wearecurio.model.InitialLoginConfiguration
import us.wearecurio.model.InterestArea
import us.wearecurio.model.InterestSubject
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
	
    Map register(User user, String promoCode="") {
		String promo = promoCode?.trim()?.toLowerCase()
		PromoCodeLookupResult lookupResult
		InitialLoginConfiguration loginConfig
		if (promo != null && promo != "") {
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
		
		return [
			success: true, 
			initialLoginConfig: loginConfig, 
			promoCodeLookupResult: lookupResult
		]
    }
	
	def setUserInterestsAndBookmarks(User user, List interestTags, List bookmarks) {
		if (user == null) return
		interestTags?.each{user.addInterestTag(Tag.create(it))}
		bookmarks?.each{Entry.createBookmark(user.id,it)}		
	}
	
	def selectPromoCodeDefaults(User user, String promoCode=""){
		InitialLoginConfiguration ilc = null
		if (promoCode != null && promoCode != "") {
			ilc = InitialLoginConfiguration.findByPromoCode(promoCode)
		} 
		if (ilc == null) {
			ilc = InitialLoginConfiguration.defaultConfiguration()
		}
		
		selectInitialLoginConfigurationDefaults(user, ilc)
	}
	
	def selectInitialLoginConfigurationDefaults(User user, InitialLoginConfiguration loginConfig){
		if (loginConfig != null && loginConfig.interestAreas == null) return
		
		selectInterestAreasDefaultsByObjects(
			user,		
			(loginConfig ?
			 	loginConfig :
			 	InitialLoginConfiguration.defaultConfiguration())
				.interestAreas
				.findAll {it.interest != null && 
					it.interest.name != null && 
					it.interest.name != "" && 
					it.preSelect}
				.unique {i1,i2-> i1.interest.name.toLowerCase() <=> i2.interest.name.toLowerCase()}
				.collect {it.interest}
		)
	}

	def selectInterestAreaDefaults(User user, String interest){
		if (interest == null || interest == "") return
		
		selectInterestAreasDefaultsByStrings(user, [interest])
	}
	
	def selectInterestAreasDefaultsByStrings(User user, List interests){
		if (interests == null) return
		
		//TODO: refactor to reduce to single db call
		selectInterestAreasDefaultsByObjects(
			user,
			interests
			.findAll {it != null && it instanceof String && it != ""}
				.unique {i1,i2->i1.toLowerCase() <=> i2.toLowerCase()}
				.collect {InterestArea.findByName(it)}
				.findAll {it != null} //TODO: necessary?
		)
	}	
		
	def selectInterestAreasDefaultsByObjects(User user, List interests){
		if (interests == null) return
		
		List subjectObs = []		
		
		interests
			.findAll {it != null && it instanceof InterestArea && it.interestSubjects != null}
			.unique {i1,i2->i1.name.toLowerCase() <=> i2.name.toLowerCase()}
			.each {interest->
				interest.interestSubjects
					.findAll {it.subject != null && it.preSelect}
					.unique {s1,s2->s1.subject.name.toLowerCase() <=> s2.subject.name.toLowerCase()}
					.collect {it.subject}
					.each {subjectObs << it}
			}		
		
		selectInterestSubjectsByObjects(
			user, 
			subjectObs.unique {s1,s2->s1.name.toLowerCase() <=> s2.name.toLowerCase()}
		)
	}
	
	def selectInterestSubject(User user, String subject){
		if (subject == null || subject == "") return
		
		selectInterestSubjectsByStrings(user, [subject])
	}
	
	def selectInterestSubjectsByStrings(User user, List subjects){
		if (subjects == null) return
		
		//TODO: refactor to reduce to single db call
		selectInterestSubjectsByObjects(
			user, 
			subjects
				.findAll {it != null && it instanceof String}
				.unique {s1, s2->s1.toLowerCase() <=> s2.toLowerCase()}
				.collect {InterestSubject.findByName(it)}
				.findAll {it != null} //TODO: necessary?
		)
	}
	
	def selectInterestSubject(User user, InterestSubject subject){
		if (subject == null) return
		
		selectInterestSubjectsByObjects(user, [subject])
	}
	
	def selectInterestSubjectsByObjects(User user, List subjects){
		if (subjects == null) return
		
		List interestTags = []
		List bookmarks = []

		subjects
			.findAll{it != null && it instanceof InterestSubject }
			.each { subject->
				subject.interestTags?.each {
					interestTags << it.trim()
				}
				subject.bookmarks?.each {
					bookmarks << it.trim()
				}				
			}
		setUserInterestsAndBookmarks(
			user, 
			interestTags.unique {t1, t2 -> t1.toLowerCase() <=> t2.toLowerCase()}, 
			bookmarks.unique {b1, b2 -> b1.toLowerCase() <=> b2.toLowerCase()}
		)
	}
}
