package us.wearecurio.services

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.Twenty3AndMeData
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.utility.Utils

class Twenty3AndMeDataService extends DataService {

	Twenty3AndMeDataService() {
		profileURL = "https://api.23andme.com/1/names/"
		provider = "Twenty3AndMe"
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
	Map getDataDefault(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll) throws
			InvalidAccessTokenException {
		Token tokenInstance = account.tokenInstance
		JSONObject userInfo = getUserProfile(tokenInstance)
		startDate = startDate ?: account.getLastPolled() ?: earlyStartDate

		userInfo["profiles"]?.each { profile ->
			String profileId = profile.id
			byte[] dataBytes = getGenomesDataForProfile(tokenInstance, profileId).bytes
			// Splitting bytes into chunks of a bit less than 1MB to make sure it is less than the default packet size of MYSQL Server
			List dataChunkList = dataBytes.toList().collate(1000 * 1024)

			dataChunkList.eachWithIndex { dataChunk, index ->
				Twenty3AndMeData twenty3AndMeDataInstance = Twenty3AndMeData.findOrCreateByAccountAndProfileIdAndSequence(account, profileId, index)
				twenty3AndMeDataInstance.data = dataChunk as byte[]
				Utils.save(twenty3AndMeDataInstance, true)
				if (twenty3AndMeDataInstance.hasErrors()) {
					log.warn "Error saving $twenty3AndMeDataInstance: $twenty3AndMeDataInstance.errors"
				}
			}
		}
		[success: true]
	}

	String getTimeZoneName(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException {}
	
	public static final String GENOME_URL = "https://api.23andme.com/1/genomes/"

	String getGenomesDataForProfile(Token tokenInstance, String profileId) throws InvalidAccessTokenException {
		Response apiResponse = oauthService.getTwenty3AndMeResource(tokenInstance, GENOME_URL + profileId)
		log.debug "Got response from twenty3andme data with code: [$apiResponse.code]. Getting content from stream ..."

		if (apiResponse.code == 401) {
			throw new InvalidAccessTokenException(provider)
		}
		apiResponse.body
	}

	@Override
	List<ThirdPartyNotification> notificationHandler(String notificationData) {
		// Nothing to do. TwentyThreeAndMe API doesn't provide notification feature.
	}

	Map storeGenomesData(Long userId) throws MissingOAuthAccountException, InvalidAccessTokenException {
		OAuthAccount account = getOAuthAccountInstance(userId)
		checkNotNull(account)
		getDataDefault(account, null, null, false)
	}

}
