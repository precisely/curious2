package us.wearecurio.model

import groovy.transform.AutoClone

import us.wearecurio.model.TutorialInfo
import us.wearecurio.utility.Utils

class InitialLoginConfiguration {
	static final String DEFAULT_PROMO_CODE = "default"
	
	String promoCode

	static hasMany = [interestAreas: InterestAreaBoolean]
	
	TutorialInfo getTutorialInfo() {
		TutorialInfo ret = TutorialInfo.findByName(promoCode)
		if (ret == null) {
			ret = TutorialInfo.defaultTutorialInfo()
		}
		
		return ret
	}
	
	List getInterestSubjects() {
		List subjects = []
		for (InterestAreaBoolean interest in interestAreas) {
			for (InterestSubjectBoolean subject in interest.interest.interestSubjects) {
				InterestSubjectBoolean matched = subjects.find{it.subject.name == subject.subject.name}
				if (matched == null) {
					subjects << new InterestSubjectBoolean(subject.subject, (interest.preSelect && subject.preSelect))
				} else if (!matched.preSelect && interest.preSelect && subject.preSelect) {
					matched.preSelect = true
				}
			}
		}
		
		return subjects
	}
	
	private setDefaults() {
		InterestSubject subject = InterestSubject.createInterestSubject(
			"sleep",
			["sleep"],
			["sleep 8 hours"],
			"tags and bookmarks related to sleep",
			true )
		InterestArea interest = InterestArea.create()
		interest.name = "sleep"
		interest.addToInterestSubjects(new InterestSubjectBoolean(subject, true))
		Utils.save(interest,true)
		addToInterestAreas(new InterestAreaBoolean(interest,true))
	}
	
	static copy(InitialLoginConfiguration from, InitialLoginConfiguration to) {
		if (from.tutorialInfo.name == TutorialInfo.DEFAULT_TUTORIAL) {
			if (to.tutorialInfo.name != TutorialInfo.DEFAULT_TUTORIAL) {
				to.tutorialInfo.delete()
			}
		} else {
			if (to.tutorialInfo.name == TutorialInfo.DEFAULT_TUTORIAL) {
				TutorialInfo newTI = new TutorialInfo()
				newTI.name = to.promoCode
				TutorialInfo.copy(from.tutorialInfo, newTI)
				Utils.save(newTI,true)
			} else {
				TutorialInfo curTI = to.tutorialInfo
				TutorialInfo.copy(from.tutorialInfo, curTI)
				Utils.save(curTI,true)
			}
		}
		from.interestAreas.each{
			to.addToInterestAreas(new InterestAreaBoolean(it.interest, it.preSelect))
		}
	}
	
	static InitialLoginConfiguration defaultConfiguration() {
		InitialLoginConfiguration ret = InitialLoginConfiguration.findByPromoCode(DEFAULT_PROMO_CODE)
		
		if (ret == null) {
			ret = InitialLoginConfiguration.create()
			ret.promoCode = DEFAULT_PROMO_CODE
			ret.setDefaults()
			Utils.save(ret, true)
		}
		
		return ret
	}
	
	static InitialLoginConfiguration createFromDefault() {
		InitialLoginConfiguration ret = InitialLoginConfiguration.create()
		ret.promoCode = ""
		
		InitialLoginConfiguration d = defaultConfiguration()
		if (d != null) {
			copy(d, ret)
		}
		
		return ret
	}
	
	static InitialLoginConfiguration createFromDefault(String promoCode, Boolean save=false) {
		InitialLoginConfiguration ret = createFromDefault()
		if (ret != null) {
			ret.promoCode = promoCode
			
			if (save) {
				Utils.save(ret, true)
			}		
		}
		
		return ret
	}
	
    static constraints = {
		promoCode(maxSize:32, unique: true, blank:false, nullable:false)
    }
}
