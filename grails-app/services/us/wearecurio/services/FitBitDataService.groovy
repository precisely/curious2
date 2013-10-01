package us.wearecurio.services

import java.text.SimpleDateFormat;
import java.util.Map;

import javax.management.Notification;

import grails.converters.*

import org.scribe.builder.*
import org.scribe.builder.api.*
import org.scribe.model.*
import org.scribe.oauth.*
import us.wearecurio.model.*
import us.wearecurio.utility.Utils
import org.apache.commons.logging.LogFactory;
import us.wearecurio.thirdparty.fitbit.FitBitTagUnitMap

class FitBitDataService {
	UrlService urlService
	
	static class Api extends DefaultApi10a {
		@Override
		public String getRequestTokenEndpoint() {
			return "https://www.fitbit.com/oauth/request_token"
		}

		@Override
		public Verb getRequestTokenVerb() {
			return Verb.POST
		}

		@Override
		public String getAuthorizationUrl(Token requestToken) {
			return "https://www.fitbit.com/oauth/authorize?oauth_token=" + requestToken.getToken()
		}

		@Override
		public String getAccessTokenEndpoint() {
			return "https://www.fitbit.com/oauth/access_token"
		}
	}
	
	private static def log = LogFactory.getLog(this)
		
	static debug(str) {
		log.debug(str)
	}
	
	static transactional = true
	
	static String API_KEY = "b2610f22a2314bdc804c3463aa666876"
	static String API_SECRET = "2b7472411c834c4f9b8c8e611d8e6350"
	static String API_VERSION = "1"
	

	Map<String, Token> requestMap = new HashMap<String, Token>()
	
	protected OAuthService service
	
	protected Map lastPollTimestamps = new HashMap<Long,Long>() // prevent DOS attacks
	
	def initialize() {
		service = new ServiceBuilder().provider(Api.class).apiKey(API_KEY)
				.callback(urlService.make(controller:'home', action:'doregisterfitbit')).apiSecret(API_SECRET)
				.signatureType(SignatureType.Header).build()
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
	def authorizeAccount(Long userId, String authToken, String authVerifier) {
		// retrieve requestToken from map, then remove from map to avoid memory leak

		Token requestToken = requestMap.get(authToken)

		if (requestToken == null)
			return null

		requestMap.remove(authToken)

		Token accessTokenFromService = service.getAccessToken(requestToken, new Verifier(authVerifier))

		if (accessTokenFromService == null)
			return null
			
		/**
		 * Since FitBit doesn't return user info in response to an authentication
		 * we explicitly ask for it
		 */
		def userInfo =  getUserInfo(accessTokenFromService)
		OAuthAccount fitbitAccount = OAuthAccount.createOrUpdate(OAuthAccount.FITBIT_ID, userId, userInfo.user?.encodedId,
				accessTokenFromService.getToken(), accessTokenFromService.getSecret())
		this.subscribe(accessTokenFromService,userId)
	}
	
	/**
	 * 
	 * @param accessToken
	 * @return
	 */
	def getUserInfo(def accessToken) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.fitbit.com/${API_VERSION}/user/-/profile.json")
		service.signRequest(accessToken, request)
		debug request.getHeaders().dump()
		Response response = request.send()
			
		debug "User info return..."
		debug response.getCode()
		return JSON.parse(response.getBody())
	}
	
	def subscribe(def accessToken,def subscriptionId) {
		OAuthRequest request = new OAuthRequest(Verb.POST, "http://api.fitbit.com/${API_VERSION}/user/-/apiSubscriptions/${subscriptionId}.json")
		service.signRequest(accessToken, request)
		debug request.getHeaders().dump()
		Response response = request.send()
			
		debug "Adding a subscription returns..."
		debug response.getCode()
	}
	
	def queueNotifications(def notifications) {
		debug "Iterating through notifications"
		SimpleDateFormat formatter = new SimpleDateFormat('yyyy-MM-dd', Locale.US)
		
		notifications.each { notification ->
			debug "Saving " + notification.dump()
			notification.date = formatter.parse(notification.date)
			new FitbitNotification(notification).save(failOnError:true)
		}
	}
	
	def poll(def notification) {
		debug "poll() accountId:" + notification.ownerId
		debug "poll() notification: " + notification.dump()
		def fitBitTagUnitMap = new FitBitTagUnitMap()
		SimpleDateFormat formatter = new SimpleDateFormat('yyyy-MM-dd', Locale.US)
		SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		def accountId = notification.ownerId
		if (accountId == null)
			return false
			
		long now = new Date().getTime()
		
		Long lastPoll = lastPollTimestamps.get(accountId)
		
		if (lastPoll != null && now - lastPoll < 500) { // don't allow polling faster than once every 500ms
			return false
		}
		
		lastPollTimestamps.put(accountId, now)
		
		def accounts = OAuthAccount.findAllByAccountIdAndTypeId(accountId, OAuthAccount.FITBIT_ID)
		try {
			for (OAuthAccount account in accounts) {
				def collectionType = notification.collectionType
				def requestUrl
				if (collectionType.equals("foods")) {
					requestUrl = "http://api.fitbit.com/${API_VERSION}/user"+
						"/${notification.ownerId}/${collectionType}/log/date/${formatter.format(notification.date)}.json"
				} else if (collectionType.equals('body')) {
					//Getting body measurements
					requestUrl = "http://api.fitbit.com/${API_VERSION}/user"+
						"/${notification.ownerId}/${collectionType}/date/${formatter.format(notification.date)}.json"
					return false
	
					//Getting body weight
					/*requestUrl = "http://api.fitbit.com/${API_VERSION}/user"+
						"/${notification.ownerId}/${collectionType}/log/weight/date/${formatter.format(notification.date)}.json"
					this.getData(account, requestUrl, false)
	
					//Getting body fat
					requestUrl = "http://api.fitbit.com/${API_VERSION}/user"+
						"/${notification.ownerId}/${collectionType}/log/fat/date/${formatter.format(notification.date)}.json"
					this.getData(account, requestUrl, false)*/
				} else if (collectionType.equals('activities')) {
					requestUrl = "http://api.fitbit.com/${API_VERSION}/user" +
						"/${notification.ownerId}/${collectionType}/date/${formatter.format(notification.date)}.json"
					def activityData = this.getData(account, requestUrl, false)
					def setName = formatter.format(notification.date)+"activity"
					Entry.executeUpdate("delete Entry e where e.setName = :setName and e.userId = :userId",
						[setName: setName, userId: account.userId])
					
					activityData.summary.distances.each { distance ->
						def entryDate = notification.date
						try {
							activityData.activities.each { activity ->
								if (activity.name.equals(distance.activity) && activity.hasStartTime) {
									entryDate = inputFormat.parse(formatter.format(notification.date)+"T"+activity.startTime+":00.000")
									throw new Exception("return from closure") 
								}
							}
						} catch(Exception e) {
							//do nothing
						}
						if (!distance.activity.equals('loggedActivities')) { //Skipping loggedActivities
							debug "Importing activity: " + distance.activity
							fitBitTagUnitMap.buildEntry("miles",distance.distance.toBigDecimal(),account.userId,
								entryDate,[setName:setName,tagName:"activity $distance.activity"])
						}
					}
					
					fitBitTagUnitMap.buildEntry("steps",activityData.summary.steps.toBigDecimal(),account.userId,
						notification.date,[setName:setName])
					fitBitTagUnitMap.buildEntry("fairlyActiveMinutes",activityData.summary.fairlyActiveMinutes.toBigDecimal(),account.userId,
						notification.date,[setName:setName])
					fitBitTagUnitMap.buildEntry("lightlyActiveMinutes",activityData.summary.lightlyActiveMinutes.toBigDecimal(),account.userId,
						notification.date,[setName:setName])
					fitBitTagUnitMap.buildEntry("sedentaryMinutes",activityData.summary.sedentaryMinutes.toBigDecimal(),account.userId,
						notification.date,[setName:setName])
					fitBitTagUnitMap.buildEntry("veryActiveMinutes",activityData.summary.veryActiveMinutes.toBigDecimal(),account.userId,
						notification.date,[setName:setName])
				} else if (collectionType.equals('sleep')) {
					debug "Fetch sleep data next."
					requestUrl = "http://api.fitbit.com/${API_VERSION}/user"+
					"/${notification.ownerId}/${collectionType}/date/${formatter.format(notification.date)}.json"
					def sleepData = this.getData(account, requestUrl, false)
					sleepData.sleep.each { logEntry ->
						Date entryDate = inputFormat.parse(logEntry.startTime)
						Entry.executeUpdate("delete Entry e where e.setName = :setName and e.userId = :userId",
							[setName: logEntry.logId.toString(), userId: account.userId])
						fitBitTagUnitMap.buildEntry("duration",logEntry.duration.toBigDecimal(),account.userId,
							entryDate,[setName:logEntry.logId])
						fitBitTagUnitMap.buildEntry("awakeningsCount",logEntry.awakeningsCount.toBigDecimal(),account.userId,
							entryDate,[setName:logEntry.logId])
						fitBitTagUnitMap.buildEntry("efficiency",logEntry.efficiency.toBigDecimal(),account.userId,
							entryDate,[setName:logEntry.logId])
					}
				}
				
			}
		} catch(Exception e) {
			e.printStackTrace()
			return false
		}
		
		return true
	}
	
	def getData(def account, def requestUrl, def refreshAll) {
		debug "Fetching data from fitbit"
		debug "Request Url: " + requestUrl
		OAuthRequest request = new OAuthRequest(Verb.GET, requestUrl)
	    request.addHeader("Accept-Language","en_US");
		Token accessToken = new Token(account.getAccessToken(), account.getAccessSecret())			
	    service.signRequest(accessToken, request)
		Response response = request.send()
		debug response.dump()
		def jsonResponse = JSON.parse(response.getBody())
		debug "Fetch collectionType data:" + jsonResponse
		debug "Response Code " + response.getCode()
		return jsonResponse
	}

}