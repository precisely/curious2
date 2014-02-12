package us.wearecurio.services

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.Twenty3AndMeData
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
			// Splitting bytes into chunks of a bit less than 1MB to make sure it is less than the default packet size of MYSQL Server
			List dataChunkList = dataBytes.toList().collate(1000 * 1024)

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
		log.debug "Got response from twenty3andme data with code: [$apiResponse.code]. Getting content from stream ..."

		if (apiResponse.code == 401) {
			throw new AuthenticationRequiredException(provider)
		}
		apiResponse.body
	}

	@Override
	void notificationHandler(String notificationData) {
		// Nothing to do. TwentyThreeAndMe API doesn't provide notification feature.
	}

	Map storeGenomesData(Long userId) throws AuthenticationRequiredException {
		OAuthAccount account = getOAuthAccountInstance(userId)
		checkNotNull(account)
		getDataDefault(account, null, false)
	}

}
