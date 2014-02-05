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
		table 'fitbit_notification'
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
		ThirdParty.each {
			map.put(it.id, it)
		}
	}

	static ThirdParty get(Integer id) {
		map.get(id)
	}

	boolean supportsOAuth2() {
		oauthVersion == SupportedOauthVersion.TWO
	}
}