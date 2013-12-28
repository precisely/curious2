package us.wearecurio.services

import grails.converters.JSON
import grails.util.Environment;

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
		if (Environment.current == Environment.DEVELOPMENT) {
			log.info "Profile response from 23andme API: $apiResponse.body"
		}
		JSONObject parsedResponse = JSON.parse(apiResponse.body)
		if (parsedResponse.error) {
			log.error "Error getting profile information from 23andme API: $apiResponse.body"
		}
		parsedResponse
	}

	Map storeGenomesData(Token tokenInstance, User userInstance) {
		if (!tokenInstance || !tokenInstance.token)
			throw new AuthenticationRequiredException("twenty3andme")

		JSONObject userProfile = getUserProfiles(tokenInstance)

		OAuthAccount account = OAuthAccount.findByTypeIdAndAccountIdAndUserId(OAuthAccount.TWENTY_3_AND_ME_ID, userProfile.id, userInstance.id)

		userProfile.profiles?.each { profile ->
			String profileId = profile.id
			byte[] dataBytes = getGenomesDataForProfile(tokenInstance, profileId).bytes
			// Splitting bytes into chunks of 1MB to match default packet size of MYSQL Server
			List dataChunkList = dataBytes.toList().collate(1024 * 1024)

			dataChunkList.eachWithIndex { dataChunk, index ->
				Twenty3AndMeData twenty3AndMeDataInstance = Twenty3AndMeData.findOrCreateByAccountAndProfileIdAndSequence(account, profileId, index)
				twenty3AndMeDataInstance.data = dataChunk as byte[]
				twenty3AndMeDataInstance.save(flush: true)
				if (twenty3AndMeDataInstance.hasErrors()) {
					log.warn "Error saving $twenty3AndMeDataInstance: $twenty3AndMeDataInstance.errors"
				}
			}
		}
		[success: true]
	}

	String getGenomesDataForProfile(Token tokenInstance, String profileId) {
		Response apiResponse = oauthService.getTwenty3AndMeResource(tokenInstance, "https://api.23andme.com/1/genomes/" + profileId)
		log.debug "Got response from twent3andme data. Getting content from stream ..."
		apiResponse.body
	}

	String constructGenomesData(OAuthAccount oauthAccount, String profileId = "") {
		List twenty3AndMeDataByteList = Twenty3AndMeData.withCriteria {
			projections {
				property("data")
			}
			account {
				eq("id", oauthAccount.id)
			}
			if(profileId) {
				eq("profileId", profileId)
			}
			order("sequence", "asc")
		}

		if(!twenty3AndMeDataByteList)	return "";

		ByteArrayOutputStream twenty3AndMeDataByte = new ByteArrayOutputStream()
		twenty3AndMeDataByteList.each {
			twenty3AndMeDataByte.write(it as byte[])
		}

		twenty3AndMeDataByte.toString()
	}

}
