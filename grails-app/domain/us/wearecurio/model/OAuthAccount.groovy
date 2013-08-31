package us.wearecurio.model

import java.util.Map;
import java.util.Date;

import org.apache.commons.logging.LogFactory
import us.wearecurio.utility.Utils

class OAuthAccount {
	private static def log = LogFactory.getLog(this)

	public static int WITHINGS_ID = 1
	public static int FITBIT_ID = 2
	
	Integer typeId
	Long userId
	String accountId
	String accessToken
	String accessSecret
	Date lastPolled
	Date lastSubscribed

	static constraints = {
		userId(unique:true)
		lastPolled(nullable:true)
		lastSubscribed(nullable:true)
	}

	public static def createOrUpdate(Integer typeId, Long userId, String accountId, String accessToken, String accessSecret) {
		int c = 0
		
		while (++c < 3) {
			OAuthAccount account = OAuthAccount.findByUserId(userId)
			
			if (account) {
				account.setAccountId(accountId)
				account.setTypeId(typeId)
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
	
	String toString() {
		return "OAuthAccount(id:" + getId() + ", userId:" + userId + ", typeId:" + typeId
				+ ", accessToken:" + (accessToken != null ? 'has token' : 'null')
				+ ", accessSecret:" + (accessSecret != null ? 'has secret' : 'null' + ",")
				+ ", lastPolled:" + lastPolled ?: 'null'
				+ ", lastSubscribed:" + lastSubscribed ?: 'null'
				+ ")"
	}
}
