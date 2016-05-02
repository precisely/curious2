package us.wearecurio.model

import org.scribe.model.Token
import us.wearecurio.services.DataService
import us.wearecurio.utility.Utils
import org.apache.commons.logging.LogFactory

class OAuthAccount {

	static def log = LogFactory.getLog(this)

	ThirdParty typeId
	Long userId
	String accountId
	String accessToken
	String accessSecret
	String refreshToken
	Date expiresOn
	/**
	 * Initializing lastData with current time minus two months, for retroactive data
	 */
	Date lastPolled = new Date()
	Date lastData = new Date() - 61
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
		lastData nullable: true
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
	
	Date fetchLastData() {
		if (lastData == null)
			return new Date() - 61
		return lastData
	}

	void markLastPolled(Date lastData, Date lastPolled = null) {
		log.debug "Trying to set lastData for account " + this.id + " userId " + this.userId + " type " + this.typeId + ": " + lastData
		if (this.lastData == null)
			this.lastData = lastData
		else if (lastData != null && lastData > this.lastData)
			this.lastData = lastData
		this.lastPolled = lastPolled ?: new Date()
		this.merge()
		log.debug "Set lastData date for " + this.id + " to: " + this.lastData
		Utils.save(this, false)
	}
}