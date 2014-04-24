package us.wearecurio.model

import static uk.co.desirableobjects.oauth.scribe.SupportedOauthVersion.*
import uk.co.desirableobjects.oauth.scribe.SupportedOauthVersion

class ThirdPartyNotification {

	String collectionType
	Date date
	String ownerId
	String ownerType
	String subscriptionId
	Status status = Status.UNPROCESSED
	ThirdParty typeId

	static mapping = {
		version false
		table 'third_party_notification'
		date column:'log_date'
		ownerId column:'owner_id', index:'owner_id_index'
		ownerType column:'owner_type'
		subscriptionId column:'subscription_id', index:'subscription_id_index'
		status column:'status', index:'status_index'
	}

	static enum Status {
		UNPROCESSED(0), PROCESSED(1)

		final Integer id

		Status(Integer id) {
			this.id = id
		}
	}
}

/**
 * Represents various third party type for various usage.
 * Each type in this enum represent a third party, holding various data as follows:
 * 
 * @param id Unique Integer id for each type
 * @param providerName String representation of each provider to work with oauth plugin.
 * @param oauthVersion OauthVersion of each third party they support.
 * @see https://github.com/aiten/grails-oauth-scribe/blob/master/src/groovy/uk/co/desirableobjects/oauth/scribe/SupportedOauthVersion.groovy?source=c
 * @author causecode
 *
 */
enum ThirdParty {
	WITHINGS(1, "withings", ONE),
	FITBIT(2, "fitbit", ONE),
	TWENTY_THREE_AND_ME(3, "twenty3andme", TWO),
	MOVES(4, "moves", TWO),
	IHEALTH(5, "ihealth", TWO),
	HUMAN(6, "human", TWO),
	TWITTER(7, "twitter", ONE),
	//JAWBONE(8, )

	final int id
	final String providerName
	final SupportedOauthVersion oauthVersion

	ThirdParty(int id, String providerName, SupportedOauthVersion oauthVersion) {
		this.id = id
		this.providerName = providerName
		this.oauthVersion = oauthVersion
	}

	private static final Map<Integer, ThirdParty> map = new HashMap<Integer, ThirdParty>()

	static {
		ThirdParty.each { thirdParty ->
			map.put(thirdParty.id, thirdParty)
		}
	}

	static ThirdParty get(Integer id) {
		map.get(id)
	}

	boolean supportsOAuth2() {
		oauthVersion == SupportedOauthVersion.TWO
	}
}