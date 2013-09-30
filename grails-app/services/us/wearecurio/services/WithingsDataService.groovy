package us.wearecurio.services

import org.apache.commons.logging.LogFactory

import grails.converters.*

import org.scribe.builder.*
import org.scribe.builder.api.*
import org.scribe.model.*
import org.scribe.oauth.*

import java.net.URL
import java.util.Map
import java.util.HashMap
import java.util.Calendar

import us.wearecurio.utility.Utils
import us.wearecurio.model.*

import grails.util.GrailsUtil

class WithingsDataService {
	UrlService urlService
	
	static class Api extends DefaultApi10a {
		@Override
		public String getRequestTokenEndpoint() {
			return "https://oauth.withings.com/account/request_token"
		}

		@Override
		public Verb getRequestTokenVerb() {
			return Verb.GET
		}

		@Override
		public String getAuthorizationUrl(Token requestToken) {
			return "https://oauth.withings.com/account/authorize?oauth_token=" + requestToken.getToken()
		}

		@Override
		public String getAccessTokenEndpoint() {
			return "https://oauth.withings.com/account/access_token"
		}
	}

	private static def log = LogFactory.getLog(this)

	static debug(str) {
		log.debug(str)
	}
	
	static transactional = true

	static String API_KEY = "74b17c41e567dc3451092829e04c342f5c68c04806980936e1ec9cfeb8f3"
	static String API_SECRET = "78d839937ef5c44407b4996ed7c204ed6c55b3e76318d1371c608924b994db"

	Map<String, Token> requestMap = new HashMap<String, Token>()
	
	protected OAuthService service
			
	protected Map lastPollTimestamps = new HashMap<Long,Long>() // prevent DOS attacks
	
	// called from BootStrap
	
	def initialize() {
		service = new ServiceBuilder().provider(Api.class).apiKey(API_KEY)
				.callback(urlService.make(controller:'home', action:'doregisterwithings')).apiSecret(API_SECRET)
				.signatureType(SignatureType.QueryString).build()
	}
	
	/**
	 * First stage in OAuth verification
	 */
	def getAuthorizationURL() {
		Token requestToken = service.getRequestToken()

		requestMap.put(requestToken.getToken(), requestToken)
		
		debug "Request token: " + requestToken.getToken()

		return service.getAuthorizationUrl(requestToken)
	}

	/**
	 * Second stage in OAuth verification.
	 */
	def authorizeAccount(Long userId, Long withingsUserId, String authToken, String authVerifier) {
		debug "Authorizing WithingsDataService for user " + userId + " and userId " + withingsUserId
		// retrieve requestToken from map, then remove from map to avoid memory leak

		Token requestToken = requestMap.get(authToken)

		if (requestToken == null) {
			debug "requestToken is null, failure"
			return null
		}

		requestMap.remove(authToken)

		Token accessTokenFromService = service.getAccessToken(requestToken, new Verifier(authVerifier))

		if (accessTokenFromService == null) {
			debug "accessTokenFromService is null, failure"
			return null
		}
			
		OAuthAccount withingsAccount = OAuthAccount.createOrUpdate(OAuthAccount.WITHINGS_ID, userId, withingsUserId.toString(),
				accessTokenFromService.getToken(), accessTokenFromService.getSecret())
		
		Utils.save(withingsAccount, true)
		
		debug "Created withingsAccount " + withingsAccount
		
		this.subscribe(withingsAccount)
		
		this.poll(withingsUserId.toString())
	}
	
	def poll() {
		def accounts = OAuthAccount.findAllByTypeId(OAuthAccount.WITHINGS_ID)
		
		for (OAuthAccount account in accounts) {
			this.getData(account, false)
		}
	}
	
	def poll(String accountId) {
		debug "poll() accountId:" + accountId
		
		if (accountId == null)
			return false
		
		long now = new Date().getTime()
		
		Long lastPoll = lastPollTimestamps.get(accountId)
		
		if (lastPoll != null && now - lastPoll < 500) { // don't allow polling faster than once every 500ms
			return false
		}
		
		lastPollTimestamps.put(accountId, now)
		
		def accounts = OAuthAccount.findAllByAccountIdAndTypeId(accountId, OAuthAccount.WITHINGS_ID)
		
		for (OAuthAccount account in accounts) {
			this.getData(account, false)
		}
		
		return true
	}
	
	def subscribe(OAuthAccount account) {
		debug "subscribe() account:" + account
		
		Integer offset = 0
		boolean more = true
		long serverTimestamp = 0
		Long userId = account.getUserId()
			
	    OAuthRequest request = new OAuthRequest(Verb.GET, "http://wbsapi.withings.net/notify")
	    request.addQuerystringParameter("userid", account.getAccountId().toString())
	    request.addQuerystringParameter("action", "subscribe")
	    request.addQuerystringParameter("callbackurl", "http://dev.wearecurio.us/home/notifywithings")
	    request.addQuerystringParameter("comment", "Notify Curious app of new data")
		Token accessToken = new Token(account.getAccessToken(), account.getAccessSecret())			
	    service.signRequest(accessToken, request)
			
	    Response response = request.send()
			
	    System.out.println("Subscribe return...")
	    System.out.println()
	    System.out.println(response.getCode())
	    System.out.println(response.getBody())
		
		if (response.getCode() == 0) {
			account.setLastSubscribed(new Date())
			debug "set last subscribed: " + account.getLastSubscribed()
			Utils.save(account)
		}
	}
	
	public static final BigDecimal KG_TO_POUNDS = new BigDecimal(220462, 5)
	public static final BigDecimal M_TO_FEET = new BigDecimal(328084, 5)
	public static final String WITHINGS_SET_NAME = "withings import"
	
	def getData(OAuthAccount account) {
		return getData(account, false)
	}
	
	def refreshSubscriptions() {
		debug "refreshSubscriptions()"
		def c = OAuthAccount.createCriteria()
		
		def now = new Date()
		def weekAgo = new Date(now.getTime() - 7L * 24 * 60 * 60 * 1000)
		
		def results = c {
			eq("typeId", OAuthAccount.WITHINGS_ID)
			lt("lastSubscribed", weekAgo)
		}
		
		for (OAuthAccount account in results) {
			this.subscribe(account)
		}
	}
	
	def getRefreshSubscriptionsTask() {
		return { refreshSubscriptions() }
	}
	
	def getData(OAuthAccount account, boolean refreshAll) {
		debug "WithingsDataService.getData() account:" + account + " refreshAll: " + refreshAll
		
		Integer offset = 0
		boolean more = true
		long serverTimestamp = 0
		Long userId = account.getUserId()
		
		if (refreshAll)
				Entry.executeUpdate("delete Entry e where e.setName = :setName and e.userId = :userId",
						[setName: WITHINGS_SET_NAME, userId: userId])
		
		while (more) {
		    OAuthRequest request = new OAuthRequest(Verb.GET, "http://wbsapi.withings.net/measure")
		    request.addQuerystringParameter("userid", account.getAccountId().toString())
		    request.addQuerystringParameter("action", "getmeas")
			if (offset > 0)
				request.addQuerystringParameter("offset", offset.toString())
			
			Long lastPolled = account.getLastPolled() != null ? account.getLastPolled().getTime() / 1000L : null
			if (lastPolled != null && (!refreshAll))
			request.addQuerystringParameter("startdate", lastPolled .toString())
			
			Token accessToken = new Token(account.getAccessToken(), account.getAccessSecret())
			
		    service.signRequest(accessToken, request)
			
		    Response response = request.send()
			
		    System.out.println("Got it! Lets see what we found...")
		    System.out.println()
		    System.out.println(response.getCode())
		    System.out.println(response.getBody())
			def data
			try {
				data = JSON.parse(response.getBody())
			} catch (Exception e) {
				debug "WithingsDataService.getData(): Exception while parsing response " + e
				return false
			}
			if (data.status != 0) {
				return false
			}
			def groups = data.body.measuregrps
			offset = groups.size()
			more = data.body.more ? true : false
			serverTimestamp = data.body.updatetime * 1000L
			for (group in groups) {
				Date date = new Date(group.date * 1000L)
				def measures = group.measures
				for (measure in measures) {
					BigDecimal value = new BigDecimal(measure.value, -measure.unit)
					System.out.println("type: " + measure.type + " value: " + value)
					int amountPrecision = 2
					String description
					String units
					
					switch (measure.type) {
						case 1: // weight (kg)
						description = "weight"
						amountPrecision = 2
						value = value.multiply(KG_TO_POUNDS).setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)
						units = "lbs"
						break
						
						case 4: // height (m)
						description = "height"
						amountPrecision = 5
						value = value.multiply(M_TO_FEET).setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)
						units = "feet"
						break
						
						case 5: // fat free mass (kg)
						description = "fat free mass"
						amountPrecision = 2
						value = value.multiply(KG_TO_POUNDS).setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)
						units = "lbs"
						break
						
						case 6: // fat ratio (%)
						description = "fat ratio"
						units = "%"
						break
						
						case 8: // fat mass weight (kg)
						description = "fat mass weight"
						amountPrecision = 2
						value = value.multiply(KG_TO_POUNDS).setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)
						units = "lbs"
						break
						
						case 9: // blood pressure diastolic (mmHg)
						description = "blood pressure diastolic"
						value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)
						units = "mmHg"
						break
						
						case 10: // blood pressure systolic (mmHg)
						description = "blood pressure systolic"
						value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)
						units = "mmHg"
						break
						
						case 11: // pulse (bpm)
						description = "heart rate"
						value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)
						units = "bpm"
						break
					}
					def entry = Entry.create(userId, date, 0, description, value, units, "", "withings import", amountPrecision)
				}
			}
		}
		
		if (serverTimestamp > 0) {
			account.setLastPolled(new Date(serverTimestamp))
			Utils.save(account, true)
		}
	}
}
