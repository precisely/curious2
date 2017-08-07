package us.wearecurio.model

import org.scribe.model.Token
import us.wearecurio.services.DataService
import us.wearecurio.services.EmailService
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
		accountId maxSize: 32	// New Oura API is sending 32 characters accountId
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

	OAuthAccount clearAccessToken() {
		OAuthAccount merged = this.merge()
		merged.removeAccessToken()
		removeAccessToken()
		Utils.save(merged, true)
	}
	
	Date fetchLastDataDate() {
		Date twoMonthsAgo = new Date() - 61
		if (!lastData || lastData < twoMonthsAgo)
			return twoMonthsAgo
		return lastData
	}
	
	void setAccountFailure() {
		if (!this.accessToken) return // do nothing if account already cleared
		OAuthAccount merged = clearAccessToken()
		User user = User.get(merged.userId)
		if (!user) return
		String email = user.email
		log.debug "Trying to send notification email for OAuthAccount failure to " + email
		def messageBody = "Your " + typeId.getTextName() + " account has had a synchronization failure. Please log into your precise.ly account and re-link your external account from your user profile to resume data synchronization."
		
		def messageSubject = "Your " + typeId.getTextName() + " external account needs to be re-linked"
		EmailService.get().send(email, messageSubject, messageBody)
		
		Utils.reportError("OAuthAccount failure", typeId.getTextName() + " external account needs to be re-linked for " + this + " username " + user.username)
	}

	OAuthAccount markLastPolled(Date lastData, Date lastPolled = null) {
		log.debug "Trying to set lastData for account " + this.id + " userId " + this.userId + " type " + this.typeId + ": " + lastData
		OAuthAccount merged = this.merge()
		if (merged.lastData == null)
			merged.lastData = lastData
		else if (lastData != null && lastData > merged.lastData)
			merged.lastData = lastData
		merged.lastPolled = lastPolled ?: new Date()
		this.lastData = merged.lastData
		this.lastPolled = merged.lastPolled
		log.debug "Set lastData date for " + this.id + " to: " + this.lastData
		Utils.save(merged, false)
		return merged
	}
}