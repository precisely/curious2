package us.wearecurio.model

import java.util.Map;

import us.wearecurio.model.Entry.RepeatType;

class ThirdPartyNotification {

	String collectionType
	Date date
	String ownerId
	String ownerType
	String subscriptionId
	Status status = Status.UNPROCESSED
	int typeId

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
	WITHINGS(1, "withings"),
	FITBIT(2, "fitbit"),
	TWENTY_THREE_AND_ME(3, "twenty3andme"),
	MOVES(4, "moves"),
	IHEALTH(5, "ihealth"),
	HUMAN(6, "human"),
	TWITTER(7, "twitter"),
	//JAWBONE(8, )

	final int id
	final String providerName

	ThirdParty(int id, String providerName) {
		this.id = id
		this.providerName = providerName
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
}