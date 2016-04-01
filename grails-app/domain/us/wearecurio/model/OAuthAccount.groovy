package us.wearecurio.model

import org.scribe.model.Token
import us.wearecurio.services.DataService
import us.wearecurio.utility.Utils

class OAuthAccount {

	ThirdParty typeId
	Long userId
	String accountId
	String accessToken
	String accessSecret
	String refreshToken
	Date expiresOn
	/**
	 * Initializing the value with current time since we only need data after the user links a device/account.
	 */
	Date lastPolled = new Date()
	Date lastSubscribed
	Integer timeZoneId

	static constraints = {
		accessToken maxSize: 1024
		accountId maxSize: 24	// Jawbone sends around 22 characters accountId
		expiresOn nullable: true
		userId(unique:['typeId'])
		lastSubscribed(nullable:true)
		refreshToken nullable: true
		timeZoneId nullable: true
	}
	
	static mapping = {
		version false
	}
	
	static def createOrUpdate(ThirdParty type, Long userId, String accountId, String accessToken, String accessSecret) {
		int c = 0

		while (++c < 4) {
			OAuthAccount.withTransaction {
				OAuthAccount account = OAuthAccount.findOrCreateByUserIdAndTypeId(userId, type)
	
				account.accountId = accountId
				account.accessToken = accessToken
				account.accessSecret = accessSecret
	
				if (Utils.save(account, true)) {
					return account
				}
			}
		}

		return null
	}

	static def delete(OAuthAccount account) {
		account.delete(flush:true)
	}
	
	DataService getDataService() {
		return DataService.getDataServiceForTypeId(typeId)
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

	// Used to set accessToken with blank value to represent expired accessToken & not-linked account case.
	void removeAccessToken() {
		this.accessToken = ""
	}

	void clearAccessToken() {
		removeAccessToken()
		Utils.save(this, true)
	}

	void markLastPolled() {
		this.lastPolled = new Date()
		Utils.save(this, true)
	}
}