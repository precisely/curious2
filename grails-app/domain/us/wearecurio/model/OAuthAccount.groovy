package us.wearecurio.model

import static us.wearecurio.model.ThirdParty.*

import org.scribe.model.Token

import us.wearecurio.utility.Utils

class OAuthAccount {

	ThirdParty typeId
	Long userId
	String accountId
	String accessToken
	String accessSecret
	String refreshToken
	Date expiresOn
	Date lastPolled
	Date lastSubscribed

	static constraints = {
		accessToken maxSize: 1024
		expiresOn nullable: true
		userId(unique:['typeId'])
		lastPolled(nullable:true)
		lastSubscribed(nullable:true)
		refreshToken nullable: true
		//typeId inList: []
	}

	static def createOrUpdate(ThirdParty type, Long userId, String accountId, String accessToken, String accessSecret) {
		int c = 0

		while (++c < 4) {
			OAuthAccount account = OAuthAccount.findOrCreateByUserIdAndTypeId(userId, type)

			account.accountId = accountId
			account.accessToken = accessToken
			account.accessSecret = accessSecret

			if (Utils.save(account, true)) {
				return account
			}
		}

		return null
	}

	static def delete(OAuthAccount account) {
		account.delete()
	}

	@Override
	String toString() {
		// TODO Fix this. Log statement skips after first line.
		return "OAuthAccount(id:" + getId() + ", userId:" + userId + ", typeId:" + typeId.id
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