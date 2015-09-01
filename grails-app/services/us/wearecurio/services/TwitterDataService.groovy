package us.wearecurio.services

import org.apache.commons.logging.LogFactory

import org.springframework.transaction.annotation.Transactional

import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.ResponseList
import twitter4j.DirectMessage
import twitter4j.auth.RequestToken
import twitter4j.auth.AccessToken
import twitter4j.Status
import twitter4j.Paging

import java.net.URL
import java.util.Map
import java.util.HashMap
import java.util.Calendar

import us.wearecurio.utility.Utils
import us.wearecurio.model.*

import grails.util.GrailsUtil

class TwitterDataService {

	private static def log = LogFactory.getLog(this)

	static transactional = true

	static String curiousDataToken = "297564720-I2lqE8a3zVVbQlnmAI7suLviyaVFuel9kiAyaOcZ"
	static String curiousDataSecret = "pN2HFeBdTluBgJVDslWu7YBo2sUIxADcGFHyj9qUVo"

	TwitterData twitterData
	EntryParserService entryParserService

	def fetchTwitterData() {
		if (twitterData == null) {
			def results = TwitterData.findAll()
			for (result in results) {
				twitterData = result
				break
			}
			if (twitterData == null) {
				twitterData = new TwitterData()
				Utils.save(twitterData)
			}
		}

		return twitterData
	}

	def getCuriousTwitter() {
		Twitter twitter = new TwitterFactory().getInstance()
		twitter.setOAuthConsumer("qSMDPfxyr6oQlO7Le8Dfw", "tVIc1AOjETG5sGJmvKoycQa9m3CmUTNaZISvqUV4wgM")
		return twitter
	}

	def getCuriousDataTwitter() {
		Twitter twitter = new TwitterFactory().getInstance()
		twitter.setOAuthConsumer("vkCfJLhbefeaOo5zBNG9w", "8WK5fhtkackP7IqxE83FiFvzVpfyZVXOISYYG4KQo0")
		twitter.setOAuthAccessToken(new AccessToken(curiousDataToken, curiousDataSecret))
		return twitter
	}

	Map<String, RequestToken> requestMap = new HashMap<String, RequestToken>()

	/**
	 * First stage in OAuth verification the user has permission to access the account.
	 * We only use this to verify the user actually owns the Twitter account in question.
	 */
	def twitterAuthorizationURL(def callbackURL) {
		// The factory instance is re-useable and thread safe.
		def requestToken = getCuriousTwitter().getOAuthRequestToken(callbackURL)

		requestMap.put(requestToken.getToken(), requestToken)

		return requestToken.getAuthorizationURL()
	}

	/**
	 * Second stage in OAuth verification. Once the user grants permission to Curious,
	 * we record their Twitter username and auto-follow the user as well.
	 */
	def usernameFromAuthTokens(def authToken, def authVerifier) {
		Twitter twitter = getCuriousTwitter()

		def user = null
		def accessToken = null

		try {
			// retrieve requestToken from map, then remove from map to avoid memory leak

			def requestToken = requestMap.get(authToken)

			if (requestToken == null)
				return null

			requestMap.remove(authToken)

			accessToken = twitter.getOAuthAccessToken(requestToken, authVerifier)

			twitter.setOAuthAccessToken(accessToken)

			user = twitter.verifyCredentials()
		} catch (TwitterException e) {
			e.printStackTrace(System.out)
			return null
		}

		if (accessToken == null)
			return null

		log.debug("ID " + twitter.verifyCredentials().getScreenName())
		log.debug("token : " + accessToken.getToken())
		log.debug("tokenSecret : " + accessToken.getTokenSecret())

		Twitter dataTwitter = getCuriousDataTwitter()

		def screenName = user.getScreenName()

		dataTwitter.createFriendship(screenName, true)

		return screenName
	}

	// Only used by developers to generate one-time access token for internal Twitter account access
	def createAccessToken() {

		Twitter twitter = new TwitterFactory().getInstance()
		twitter.setOAuthConsumer("yF321ZO2dVOHz2We7Y2Edw", "1GLW8TUO8NzWsoVHqI7zRu9ZSjuzpXZEmfLTq6Wv9U")

		// The factory instance is re-useable and thread safe.
		RequestToken requestToken = twitter.getOAuthRequestToken();
		AccessToken accessToken = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (null == accessToken) {
			log.debug("Open the following URL and grant access to your account:");
			log.debug(requestToken.getAuthorizationURL());
			print("Enter the authVerifier:");
			String authVerifier = br.readLine();
			try {
				accessToken = twitter.getOAuthAccessToken(requestToken, authVerifier);
			} catch (TwitterException te) {
				if (401 == te.getStatusCode()) {
					log.debug("Unable to get the access token.");
				} else {
					te.printStackTrace();
				}
			}
		}
		log.debug("ID " + twitter.verifyCredentials().getId())
		log.debug("token : " + accessToken.getToken())
		log.debug("tokenSecret : " + accessToken.getTokenSecret())
	}

	def poll() {
		if (!(GrailsUtil.environment.equals('production') || GrailsUtil.environment.equals('test')))
			return; // only run in production or test environment, not development sandbox

		def parseAll = false

		if (GrailsUtil.environment.equals('development'))
			parseAll = true

		log.debug "Starting poll service"

		Twitter twitter = getCuriousDataTwitter()

		log.debug "Attempting to download twitter DMs"

		fetchTwitterData()

		ResponseList<DirectMessage> responses;

		Long lastEntryId = twitterData.getLastEntryId()
		if (lastEntryId == null) log.debug "Last entry ID is null"
		else log.debug "Last entry Id is " + lastEntryId

		if (twitterData.getLastEntryId() == null)
			responses = twitter.getDirectMessages(new Paging(3417852672L))
		else
			responses = twitter.getDirectMessages(new Paging(twitterData.getLastEntryId() - 1000000L))

		Long maxEntryId = null;

		def Map<TimeZone, Calendar> baseCalendars = new HashMap<TimeZone, Calendar>()

		for (DirectMessage message: responses) {
			try {
				TODO: FIX
				if (Entry.findByTweetid(message.getId()) != null) {
					log.debug "Tweet " + message.getId() + " already downloaded, skipping"
					continue
				}

				def sender = message.getSender()
				Long messageId = message.getId()
				if (maxEntryId == null)
					maxEntryId = messageId
				else if (maxEntryId < message.getId())
					maxEntryId = messageId
				def twitterAccountName = sender.getScreenName().toString()
				def entryStr = message.getText()
				log.debug "DM: " + message
				TimeZone tz = null
				if (sender.getUtcOffset() != -1) {
					log.debug "Time zone of DM: " + sender.getTimeZone() + ":" + sender.getUtcOffset()
					tz = Utils.createTimeZone(sender.getUtcOffset(), sender.getTimeZone(), false)
				} else {
					tz = Utils.createTimeZone(0, "UTC", false)
				}

				log.debug "TimeZone: " + tz + " tweet created at " + message.getCreatedAt()

				// retrieve or calculate base date for twitter send date

				def calendar;

				if ((calendar = baseCalendars.get(tz)) == null)
					calendar = Calendar.getInstance(tz);

				calendar.setTime(message.getCreatedAt())

				int year = calendar.get(Calendar.YEAR)
				int month = calendar.get(Calendar.MONTH)
				int day = calendar.get(Calendar.DAY_OF_MONTH)

				calendar.clear()

				calendar.set(year, month, day, 0, 0, 0)

				def baseDate = calendar.getTime()

				log.debug "Base date: " + baseDate

				if (parseAll)
					twitterAccountName = 'syntheticzero'

				def users = User.findAllByTwitterAccountName(twitterAccountName)
				if (users == null)
					log.debug "No user found for account " + twitterAccountName + ", skipping message"
				else for (User user in users) {
						def parsedEntry = entryParserService.parse(message.getCreatedAt(), tz, entryStr, baseDate, user.getTwitterDefaultToNow())

						if (parsedEntry == null) {
							log.debug "Failed to parse tweet, skipping"

							continue;
						} else {
							log.debug "Parsed tweet " + parsedEntry
						}

						parsedEntry['tweetId'] = message.getId()
						Entry entry = Entry.create(user.getId(), parsedEntry, null)
						if (entry != null) {
							log.debug "Created entry " + entry
						} else
							log.debug "Failed to parse entry '" + entryStr + "'"
					}
			} catch (Throwable t) {
				log.debug "Failed to parse tweet due to exception"
				t.printStackTrace()
				continue;
			}
		}

		if (maxEntryId != null) {
			log.debug "Setting last entry ID to " + maxEntryId
			twitterData.setLastEntryId(maxEntryId)
		}

		Utils.save(twitterData)

		log.debug "Done downloading twitter DMs"
	}
}
