package us.wearecurio.services

import java.util.Date;
import java.util.Map;

import grails.converters.JSON
import grails.util.Environment

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty;
import us.wearecurio.model.Twenty3AndMeData
import us.wearecurio.model.User
import us.wearecurio.thirdparty.AuthenticationRequiredException

class Twenty3AndMeDataService extends DataService {

	Twenty3AndMeDataService() {
		profileURL = "https://api.23andme.com/1/names/"
		provider = "twenty3andme"
		typeId = ThirdParty.TWENTY_THREE_AND_ME
	}

	/**
	 * Used to return completed geonomes data which are stored as chuncked format.
	 * @param oauthAccount Instance of OAuthAccount
	 * @param profileId
	 * @return
	 */
	String constructGenomesData(OAuthAccount oauthAccount, String profileId) {
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

	@Override
	Map getDataDefault(OAuthAccount account, Date notificationDate, boolean refreshAll) {
		Token tokenInstance = account.tokenInstance
		JSONObject userInfo = getUserProfile(tokenInstance)

		userInfo["profiles"]?.each { profile ->
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

	String getGenomesDataForProfile(Token tokenInstance, String profileId) throws AuthenticationRequiredException {
		Response apiResponse = oauthService.getTwenty3AndMeResource(tokenInstance, "https://api.23andme.com/1/genomes/" + profileId)
		log.debug "Got response from twent3andme data with code: [$apiResponse.code]. Getting content from stream ..."

		if (apiResponse.code == 401) {
			throw new AuthenticationRequiredException(provider)
		}
		apiResponse.body
	}

	@Override
	void notificationHandler(String notificationData) {
		// Nothing to do. TwentyThreeAndMe API doesn't provide notification feature.
	}

	Map storeGenomesData() throws AuthenticationRequiredException {
		OAuthAccount account = getOAuthAccountInstance()
		checkNotNull(account)
		getDataDefault(account, null, false)
	}

}
