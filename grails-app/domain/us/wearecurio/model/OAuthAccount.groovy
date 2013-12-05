package us.wearecurio.model

import org.apache.commons.logging.LogFactory
import org.scribe.model.Token

import us.wearecurio.utility.Utils

class OAuthAccount {

	private static def log = LogFactory.getLog(this)

	public static int WITHINGS_ID = 1
	public static int FITBIT_ID = 2
	public static int TWENTY_3_AND_ME_ID = 3
	public static int MOVES_ID = 4

	Integer typeId
	Long userId
	String accountId
	String accessToken
	String accessSecret
	Date lastPolled
	Date lastSubscribed

	static constraints = {
		userId(unique:['typeId'])
		lastPolled(nullable:true)
		lastSubscribed(nullable:true)
		typeId inList: [FITBIT_ID, TWENTY_3_AND_ME_ID, WITHINGS_ID, MOVES_ID]
	}

	public static def createOrUpdate(Integer typeId, Long userId, String accountId, String accessToken, String accessSecret) {
		int c = 0

		while (++c < 4) {
			OAuthAccount account = OAuthAccount.findByUserIdAndTypeId(userId, typeId)

			if (account) {
				account.setAccountId(accountId)
				account.setAccessToken(accessToken)
				account.setAccessSecret(accessSecret)

				return account;
			}

			account = new OAuthAccount(
					typeId:typeId,
					userId:userId,
					accountId:accountId,
					accessToken:accessToken,
					accessSecret:accessSecret
					)

			if (Utils.save(account, true)) {
				return account
			}
		}

		return null
	}

	public OAuthAccount() {
	}

	public static def delete(OAuthAccount account) {
		account.delete()
	}

	@Override
	String toString() {
		return "OAuthAccount(id:" + getId() + ", userId:" + userId + ", typeId:" + typeId
				+ ", accessToken:" + (accessToken != null ? 'has token' : 'null')
				+ ", accessSecret:" + (accessSecret != null ? 'has secret' : 'null' + ",")
				+ ", lastPolled:" + lastPolled ?: 'null'
				+ ", lastSubscribed:" + lastSubscribed ?: 'null'
				+ ")"
	}

	Token getTokenInstance() {
		new Token(accessToken, accessSecret)
	}

}