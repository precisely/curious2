package us.wearecurio.services

import grails.converters.JSON

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.Twenty3AndMeData
import us.wearecurio.model.User
import us.wearecurio.thirdparty.AuthenticationRequiredException

class Twenty3AndMeDataService {

	def oauthService	// From oauth plugin

	JSONObject getUserProfiles(Token tokenInstance) {
		Response apiResponse = oauthService.getTwenty3AndMeResource(tokenInstance, "https://api.23andme.com/1/names/")
		JSONObject parsedResponse = JSON.parse(apiResponse.body)
		parsedResponse
	}

	void storeGenomesData(Token tokenInstance, User userInstance) {
		if(!tokenInstance || !tokenInstance.token)
			throw new AuthenticationRequiredException("twenty3andme")

		JSONObject userProfile = getUserProfiles(tokenInstance)

		OAuthAccount account = OAuthAccount.findByTypeIdAndAccountIdAndUserId(OAuthAccount.TWENTY_3_AND_ME_ID, userProfile.id, userInstance.id)

		userProfile.profiles?.each { profile ->
			String profileId = profile.id
			Twenty3AndMeData twenty3AndMeDataInstance = Twenty3AndMeData.findOrCreateByAccountAndProfileId(account, profileId)
			twenty3AndMeDataInstance.data = getGenomesDataForProfile(tokenInstance, profileId)
			twenty3AndMeDataInstance.save()
			if (twenty3AndMeDataInstance.hasErrors()) {
				log.warn "Error saving $twenty3AndMeDataInstance: $twenty3AndMeDataInstance.errors"
			}
		}
	}

	String getGenomesDataForProfile(Token tokenInstance, String profileId) {
		Response apiResponse = oauthService.getTwenty3AndMeResource(tokenInstance, "https://api.23andme.com/1/genomes/" + profileId)
		apiResponse.body
	}

}
