package us.wearecurio.model

import static uk.co.desirableobjects.oauth.scribe.SupportedOauthVersion.*
import uk.co.desirableobjects.oauth.scribe.SupportedOauthVersion

class IntraDayQueueItem implements Comparable {

	Date queryDate
	Long oauthAccountId
	Date dateCreated

	static mapping = {
		version false
		table 'withings_intra_day_queue'
		date column:'query_date'
		ownerId column:'oauth_account_id', index:'oauth_account_id_index'
	}

	//Note: this class has a natural ordering that is inconsistent with equals.
	int compareTo(Object object) {
		if (object.oauthAccountId == this.oauthAccountId)
			return 0

		return this.oauthAccountId - object.oauthAccountId
	}

}
