package us.wearecurio.services.integration

import org.apache.commons.logging.LogFactory
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.scribe.model.Response
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.Entry
import us.wearecurio.model.IntraDayQueueItem
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.UrlService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.services.DataService.DataRequestContext
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.utility.Utils

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

class WithingsDataServiceTests extends CuriousServiceTestCase {
	static transactional = true
	
	private static def log = LogFactory.getLog(this)
	
	UrlService urlService
	WithingsDataService withingsDataService
	User user2
	OAuthAccount account
	
	String mockedGeneratedURL
	String mockedGeneratedURLWithParams
	MockedHttpURLConnection mockedConnection = new MockedHttpURLConnection()
	MockedHttpURLConnection mockedConnectionWithParams = new MockedHttpURLConnection()
	
	def grailsApplication
	
	@Before
	void setUp() {
		super.setUp()
		
		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", name: "Mark Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true, hash: new DefaultHashIDGenerator().generate(12)])
		assert Utils.save(user2, true)
		
		account = new OAuthAccount([typeId: ThirdParty.WITHINGS, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id", timeZoneId: TimeZoneId.look("America/Los_Angeles").id])
		
		Utils.save(account, true)
		withingsDataService.oauthService = [
			getWithingsResource: 0,
			getWithingsResourceWithQuerystringParams: 0
		]
	}
	
	private void setWithingsResourceRepsone(MockedHttpURLConnection mockedConnection) {
		withingsDataService.oauthService['getWithingsResource'] =  { token, url, body, header ->
			mockedGeneratedURL = url
			return new Response(mockedConnection)
		}
	}
	
	private void setWithingsResourceRepsoneWithQS(MockedHttpURLConnection mockedConnection) {
		withingsDataService.oauthService['getWithingsResourceWithQuerystringParams'] =  { token, url, body, header ->
			mockedGeneratedURL = url
			return new Response(mockedConnection)
		}
	}
	
	@After
	void tearDown() {
		super.tearDown()
	}
	
	@Test
	void testRefreshSubscription() {
		Date now = new Date()
		account.lastSubscribed = new Date(now.getTime() - 10L * 24 * 60 * 60 * 1000)
		Utils.save(account, true)
		
		setWithingsResourceRepsoneWithQS(new MockedHttpURLConnection("""{"status": 0, "body": {"activities": []}}"""))
		
		withingsDataService.refreshSubscriptions()
		assert account.lastSubscribed > now.clearTime()
	}
	
	@Test
	void testSubscribeIfSuccess() {
		setWithingsResourceRepsoneWithQS(new MockedHttpURLConnection("""{"status": 0}"""))
		withingsDataService.subscribe(userId)
		assert account.lastSubscribed
	}
	
	@Test
	void testSubscribeIfNoOAuthAccount() {
		shouldFail(MissingOAuthAccountException) {
			withingsDataService.subscribe(user2.id)
		}
	}
	
	@Test
	void testSubscribeIfFail() {
		// Checking for non zero status code.
		setWithingsResourceRepsoneWithQS(new MockedHttpURLConnection("""{"status": 2554}"""))
		
		Map result = withingsDataService.subscribe(userId)
		assert result.success == false
		assert result.status == 2554
	}
	
	@Test
	void testUnSubscribeIfNoOAuthAccount() {
		shouldFail(MissingOAuthAccountException) {
			withingsDataService.unsubscribe(user2.id)	// Passing user's ID whose oauth account doesn't exist
		}
	}
	
	@Test
	void testUnsubscribeWithOAuthAccountExists() {
		setWithingsResourceRepsoneWithQS(new MockedHttpURLConnection("""{"status": 0}"""))
		
		assertNotNull OAuthAccount.findByUserIdAndTypeId(userId, ThirdParty.WITHINGS)
		withingsDataService.unsubscribe(userId)
		assertNull OAuthAccount.findByUserIdAndTypeId(userId, ThirdParty.WITHINGS)
	}
	
	@Test
	void testGetDataDefaultWithFailureStatusCode() {
		// When withings returns a non-zero status code
		setWithingsResourceRepsone(new MockedHttpURLConnection("""{"status": 2555}"""))
		
		Map result = withingsDataService.getDataDefault(account, new Date(), null, false, new DataRequestContext())
		
		assert Entry.count() == 0
		
		try {
			// When token expires with code 342
			setWithingsResourceRepsoneWithQS(new MockedHttpURLConnection("""{"status": 342}"""))
			withingsDataService.getDataDefault(account, new Date(), null, false, new DataRequestContext())
		} catch(e) {
			e.cause instanceof InvalidAccessTokenException
		}
	}
	
	@Test
	void testGetDataDefaultWithUnparsableResponse() {
		// When un-parsable string returned
		def mockedResponseData = """status = unparsable-response"""
		setWithingsResourceRepsone(new MockedHttpURLConnection(mockedResponseData))
		
		withingsDataService.getDataDefault(account, new Date(), null, false, new DataRequestContext())
		assert Entry.count() == 0
	}
	
	@Test
	void testGetDataDefault() {
		String mockedResponseData = """{"status":0,"body":{"updatetime":1385535542,"measuregrps":[{"grpid":162026288,"attrib":2,"date":1385386484,"category":1,"comment":"Hello","measures":[{"value":6500,"type":1,"unit":-2}]},{"grpid":162028086,"attrib":2,"date":1385300615,"category":1,"comment":"Nothing to comment","measures":[{"value":114,"type":9,"unit":0},{"value":113,"type":10,"unit":0},{"value":74,"type":11,"unit":0}]},{"grpid":129575271,"attrib":2,"date":1372931328,"category":1,"comment":"sa","measures":[{"value":170,"type":4,"unit":-2}]},{"grpid":129575311,"attrib":2,"date":1372844945,"category":1,"measures":[{"value":17700,"type":1,"unit":-2}]},{"grpid":129575122,"attrib":2,"date":1372844830,"category":1,"comment":"sa","measures":[{"value":6300,"type":1,"unit":-2}]},{"grpid":128995279,"attrib":0,"date":1372706279,"category":1,"measures":[{"value":84,"type":9,"unit":0},{"value":138,"type":10,"unit":0},{"value":77,"type":11,"unit":0}]},{"grpid":128995003,"attrib":0,"date":1372706125,"category":1,"measures":[{"value":65861,"type":1,"unit":-3}]}]}}"""
		setWithingsResourceRepsoneWithQS(new MockedHttpURLConnection(mockedResponseData))
		setWithingsResourceRepsone(new MockedHttpURLConnection(mockedResponseData))
		
		withingsDataService.getDataDefault(account, new Date(), null, false, new DataRequestContext())
		assert Entry.count() > 0
	}
	
	@Test
	void testGetDataActivityMetrics() {
		Map result = withingsDataService.getDataActivityMetrics(account, null, null, new DataRequestContext())
		
		assert result.success == false
		
		String mockedActivityResponse = """{"status":342}"""
		setWithingsResourceRepsoneWithQS(new MockedHttpURLConnection(mockedActivityResponse))
		
		try {
			withingsDataService.getDataActivityMetrics(account, null, null, new DataRequestContext())
		} catch(e) {
			assert e.cause instanceof InvalidAccessTokenException
			assert e.cause.provider.toLowerCase() == "withings"
		}
		
		mockedActivityResponse = """{"status":0,"body":{"activities":[]}}"""
		setWithingsResourceRepsoneWithQS(new MockedHttpURLConnection(mockedActivityResponse))
		result = withingsDataService.getDataActivityMetrics(account, new Date(), new Date(), new DataRequestContext())
		
		assertTrue result.success
		assert Entry.count() == 0	// No entries to process.
		
		mockedActivityResponse = """{"status":0,"body":{"activities":[{"date":"2014-02-05","steps":17897,"distance":16190.69,"calories":770.51,"elevation":425.03,"timezone":"America/Los_Angeles"},{"date":"2014-02-06","steps":12069,"distance":10300.22,"calories":552.4,"elevation":416.94,"timezone":"America/Los_Angeles"},{"date":"2014-02-07","steps":5182,"distance":4442.82,"calories":186.58,"elevation":99.81,"timezone":"America/Los_Angeles"},{"date":"2014-02-08","steps":13877,"distance":11466.84,"calories":573.63,"elevation":364.43,"timezone":"America/Los_Angeles"},{"date":"2014-02-09","steps":681,"distance":567.73,"calories":25.89,"elevation":14.9,"timezone":"America/Los_Angeles"},{"date":"2014-02-10","steps":13120,"distance":15563.38,"calories":999.1,"elevation":477.41,"timezone":"America/Los_Angeles"},{"date":"2014-02-11","steps":7115,"distance":7068.46,"calories":352.85,"elevation":191.14,"timezone":"America/Los_Angeles"},{"date":"2014-02-12","steps":7,"distance":7.58,"calories":0,"elevation":0,"timezone":"America/Los_Angeles"},{"date":"2014-02-16","steps":2775,"distance":2176.92,"calories":213.64,"elevation":311.58,"timezone":"America/Los_Angeles"},{"date":"2014-02-17","steps":17187,"distance":19381.9,"calories":1138.82,"elevation":427.05,"timezone":"America/Los_Angeles"},{"date":"2014-02-18","steps":5188,"distance":4691.28,"calories":212.18,"elevation":119.42,"timezone":"America/Los_Angeles"},{"date":"2014-02-19","steps":10066,"distance":9490.86,"calories":476.63,"elevation":313.55,"timezone":"America/Los_Angeles"},{"date":"2014-01-17","steps":1198,"distance":1044.62,"calories":42.48,"elevation":16.6,"timezone":"America/Los_Angeles"}]}}"""
		setWithingsResourceRepsoneWithQS(new MockedHttpURLConnection(mockedActivityResponse))
		result = withingsDataService.getDataActivityMetrics(account, new Date(), new Date() + 1, new DataRequestContext())
		assert result.success == true
		def entries = Entry.findAllByUserIdAndComment(account.getUserId(), '(Withings)')
		
		for (e in entries) {
			if (e.getDescription().equals("activity calories summary") && e.getAmount().intValue() == 770) {
				log.debug e.valueString()
				assert e.valueString().contains("date:2014-02-05T20:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:activity calories summary, amount:770.510000000, units:cal, amountPrecision:3, comment:(Withings), repeatType:null")
				e = e
			} else if (e.getDescription().equals("activity distance summary") && e.getAmount().intValue() == 6) {
				log.debug e.valueString()
				assert e.valueString().contains("date:2014-02-06T20:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:activity distance summary, amount:6.400000000, units:miles, amountPrecision:3, comment:(Withings), repeatType:null")
			} else if (e.getDescription().equals("activity elevation summary") && e.getAmount().intValue() == 425) {
				log.debug e.valueString()
				assert e.valueString().contains("date:2014-02-05T20:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:activity elevation summary, amount:425.030000000, units:meters, amountPrecision:3, comment:(Withings), repeatType:null")
				e = e
			} else if (e.getDescription().equals("activity move summary") && e.getAmount().intValue() == 17897) {
				log.debug e.valueString()
				assert e.valueString().contains("date:2014-02-05T20:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:activity move summary, amount:17897.000000000, units:steps, amountPrecision:3, comment:(Withings), repeatType:null")
				e = e
			}
		}
	}
	
	@Test
	void testIntraDayActivity() {
		String mockedActivityResponse = """{"status":0,"body":{"series":{"1368141046":{"calories":0,"duration":120},"1368141657":{"calories":0.87,"duration":60,"steps":18,"elevation":0.03,"distance":3218.69},"1368141717":{"calories":1.2,"duration":60,"steps":56,"elevation":2.4,"distance":1000}}}}"""
		setWithingsResourceRepsoneWithQS(new MockedHttpURLConnection(mockedActivityResponse))
		def result = withingsDataService.getDataIntraDayActivity(account, new Date(), new Date() + 1, new DataRequestContext())
		assert result.success == true
		def entries = Entry.findAllByUserIdAndComment(account.getUserId(), '(Withings)')
		log.debug("Result size: " + entries.size())
		for (e in entries) {
			log.debug (e.valueString())
			if (e.getDescription().equals("activity calories") && e.getAmount().intValue() == 1) {
				log.debug e.valueString()
				assert e.valueString().contains("datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:activity, amount:1.200000000, units:cal, amountPrecision:3, comment:(Withings), repeatType:null")
				e = e
			} else if (e.getDescription().equals("activity distance") && e.getAmount().intValue() == 2) {
				log.debug e.valueString()
				assert e.valueString().contains("datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:activity, amount:2.000000000, units:miles")
			} else if (e.getDescription().equals("activity elevation") && e.getAmount().intValue() == 2) {
				log.debug e.valueString()
				assert e.valueString().contains("datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:activity, amount:2.400000000, units:meters elevation, amountPrecision:3, comment:(Withings), repeatType:null")
				e = e
			} else if (e.getDescription().equals("activity move") && e.getAmount().intValue() == 18) {
				log.debug e.valueString()
				assert e.valueString().contains("datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:activity, amount:18.000000000, units:steps, amountPrecision:3, comment:(Withings), repeatType:null")
				e = e
			} else if (e.getDescription().equals("activity duration") && e.getAmount().intValue() == 2) {
				log.debug e.valueString()
				assert e.valueString().contains("datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:activity, amount:2.000000000, units:min, amountPrecision:3, comment:(Withings), repeatType:null")
				e = e
			}
		}
	}
	
	@Test
	void testIntraDayAdjacentMerge() {
		String mockedActivityResponse = "{'status':0,'body':{'series':{'1400938080':{'steps':11,'elevation':0,'calories':0.33,'distance':8.81,'duration':60},'1400938200':{'steps':6,'elevation':0,'calories':0.17,'distance':2.82,'duration':60},'1400938260':{'steps':102,'elevation':0,'calories':3.36,'distance':96.68,'duration':60},'1400938380':{'steps':94,'elevation':0,'calories':3.17,'distance':90.33,'duration':60},'1400938440':{'steps':1,'elevation':0,'calories':0.02,'distance':0.94,'duration':60},'1400938560':{'steps':12,'elevation':0,'calories':0.34,'distance':8.62,'duration':60},'1400938620':{'steps':109,'elevation':0,'calories':3.32,'distance':95.6,'duration':60},'1400938680':{'steps':97,'elevation':5.19,'calories':5.28,'distance':75.5,'duration':60},'1400938740':{'steps':70,'elevation':3.64,'calories':3.82,'distance':53.69,'duration':60},'1400938800':{'steps':27,'elevation':2.68,'calories':2.05,'distance':19.32,'duration':60},'1400938860':{'steps':98,'elevation':0,'calories':2.87,'distance':76.01,'duration':60},'1400938920':{'steps':34,'elevation':0,'calories':1,'distance':27.26,'duration':60},'1400938980':{'steps':3,'elevation':2.25,'calories':1.02,'distance':1.86,'duration':60},'1400939280':{'steps':6,'elevation':0,'calories':0.16,'distance':4.56,'duration':60},'1400939340':{'steps':7,'elevation':0,'calories':0.2,'distance':5.32,'duration':60},'1400939460':{'steps':112,'elevation':0,'calories':3.34,'distance':93.98,'duration':60},'1400939520':{'steps':4,'elevation':0,'calories':0.09,'distance':3.14,'duration':60},'1400939580':{'steps':72,'elevation':0,'calories':2.24,'distance':63.06,'duration':60},'1400939640':{'steps':122,'elevation':0,'calories':3.83,'distance':111.71,'duration':60},'1400939700':{'steps':137,'elevation':2.3,'calories':5.42,'distance':129.44,'duration':60},'1400939760':{'steps':131,'elevation':0,'calories':4.18,'distance':121.82,'duration':60},'1400939820':{'steps':125,'elevation':0,'calories':3.94,'distance':115.62,'duration':60},'1400939880':{'steps':129,'elevation':0,'calories':4.08,'distance':119.61,'duration':60},'1400939940':{'steps':8,'elevation':0,'calories':0.22,'distance':6.54,'duration':60},'1400940060':{'steps':80,'elevation':0,'calories':2.42,'distance':71.63,'duration':60},'1400940120':{'steps':132,'elevation':0,'calories':4.15,'distance':121.56,'duration':60},'1400940180':{'steps':120,'elevation':1.87,'calories':4.6,'distance':108.15,'duration':60},'1400940240':{'steps':6,'elevation':0,'calories':0.18,'distance':5.1,'duration':60},'1400940300':{'steps':40,'elevation':0,'calories':1.21,'distance':32.39,'duration':60},'1400940360':{'steps':35,'elevation':0,'calories':1,'distance':25.76,'duration':60},'1400940420':{'steps':43,'elevation':0,'calories':1.29,'distance':32.43,'duration':60},'1400940480':{'steps':70,'elevation':0,'calories':2.06,'distance':57.43,'duration':60},'1400940540':{'steps':82,'elevation':0,'calories':2.47,'distance':67.78,'duration':60},'1400940600':{'steps':92,'elevation':5.24,'calories':5.19,'distance':78.13,'duration':60},'1400940660':{'steps':101,'elevation':0.07,'calories':3.09,'distance':85.67,'duration':60},'1400940720':{'steps':61,'elevation':0,'calories':1.88,'distance':50.93,'duration':60},'1400940780':{'steps':76,'elevation':0,'calories':2.28,'distance':60.39,'duration':60},'1400940840':{'steps':115,'elevation':2.47,'calories':4.73,'distance':99.65,'duration':60},'1400940900':{'steps':114,'elevation':1.5,'calories':4.22,'distance':101.44,'duration':60},'1400940960':{'steps':61,'elevation':0,'calories':1.87,'distance':53.93,'duration':60},'1400941020':{'steps':77,'elevation':0,'calories':2.34,'distance':61.75,'duration':60},'1400941080':{'steps':116,'elevation':1.56,'calories':4.33,'distance':102.89,'duration':60},'1400941140':{'steps':99,'elevation':0,'calories':3.07,'distance':87.47,'duration':60},'1400941200':{'steps':57,'elevation':0,'calories':1.71,'distance':48.93,'duration':60},'1400941260':{'steps':33,'elevation':0,'calories':0.98,'distance':25.99,'duration':60},'1400941320':{'steps':101,'elevation':3.33,'calories':4.59,'distance':86.05,'duration':60},'1400941380':{'steps':83,'elevation':3.73,'calories':4.21,'distance':71.03,'duration':60},'1400941440':{'steps':57,'elevation':0,'calories':1.73,'distance':49.31,'duration':60},'1400941500':{'steps':112,'elevation':0,'calories':3.53,'distance':103.53,'duration':60},'1400941560':{'steps':46,'elevation':2.16,'calories':2.47,'distance':41.53,'duration':60},'1400941620':{'steps':89,'elevation':8.6,'calories':6.66,'distance':72.65,'duration':60},'1400941680':{'steps':80,'elevation':10.94,'calories':7.63,'distance':68.15,'duration':60},'1400941740':{'steps':9,'elevation':0,'calories':0.27,'distance':7.19,'duration':60},'1400944380':{'steps':9,'elevation':0,'calories':0.26,'distance':6.08,'duration':60},'1400944440':{'steps':7,'elevation':0,'calories':0.18,'distance':4.97,'duration':60},'1400944500':{'steps':1,'elevation':0,'calories':0.05,'distance':0.44,'duration':60},'1400944560':{'steps':106,'elevation':0,'calories':3.51,'distance':99.19,'duration':60},'1400944620':{'steps':6,'elevation':0,'calories':0.18,'distance':2.52,'duration':60},'1400946240':{'steps':103,'elevation':0,'calories':3.17,'distance':88.71,'duration':60},'1400946300':{'steps':21,'elevation':0,'calories':0.69,'distance':16.62,'duration':60},'1400946480':{'steps':54,'elevation':2.47,'calories':2.79,'distance':46.08,'duration':60},'1400946780':{'steps':56,'elevation':0,'calories':1.72,'distance':49.99,'duration':60},'1400946840':{'steps':7,'elevation':0,'calories':0.24,'distance':6.3,'duration':60},'1400947380':{'steps':24,'elevation':0,'calories':0.78,'distance':20.95,'duration':60},'1400947440':{'steps':108,'elevation':0,'calories':3.38,'distance':96.29,'duration':60},'1400950620':{'steps':7,'elevation':0,'calories':0.19,'distance':4.34,'duration':60},'1400952840':{'steps':6,'elevation':0,'calories':0.17,'distance':4.08,'duration':60},'1400954860':{'steps':25,'elevation':0,'calories':0.69,'distance':18.7,'duration':60},'1400954920':{'steps':22,'elevation':0,'calories':0.66,'distance':12.56,'duration':60},'1400956120':{'steps':23,'elevation':0,'calories':0.69,'distance':15.36,'duration':60},'1400956180':{'steps':21,'elevation':0,'calories':0.64,'distance':13.17,'duration':60},'1400956240':{'steps':33,'elevation':0,'calories':0.98,'distance':25.63,'duration':60},'1400956300':{'steps':14,'elevation':0,'calories':0.42,'distance':11.85,'duration':60},'1400956540':{'steps':83,'elevation':0,'calories':2.47,'distance':69.63,'duration':60},'1400956600':{'steps':5,'elevation':0,'calories':0.14,'distance':3.42,'duration':60},'1400956660':{'steps':11,'elevation':0,'calories':0.32,'distance':7.81,'duration':60},'1400956720':{'steps':11,'elevation':0,'calories':0.28,'distance':7.9,'duration':60},'1400956780':{'steps':78,'elevation':0,'calories':2.4,'distance':65.28,'duration':60},'1400956960':{'steps':29,'elevation':0,'calories':0.79,'distance':21.39,'duration':60},'1400957020':{'steps':20,'elevation':0,'calories':0.6,'distance':14.79,'duration':60},'1400958100':{'steps':49,'elevation':0,'calories':1.39,'distance':37.46,'duration':60},'1400958160':{'steps':4,'elevation':0,'calories':0.13,'distance':2.84,'duration':60},'1400958760':{'steps':62,'elevation':2.42,'calories':3,'distance':50.8,'duration':60},'1400958880':{'steps':36,'elevation':0,'calories':1.12,'distance':28.47,'duration':60},'1400959600':{'steps':15,'elevation':0,'calories':0.45,'distance':11.82,'duration':60},'1400959660':{'steps':15,'elevation':0,'calories':0.42,'distance':9.37,'duration':60},'1400960200':{'steps':50,'elevation':0,'calories':1.45,'distance':37.35,'duration':60},'1400960260':{'steps':11,'elevation':0,'calories':0.31,'distance':5.28,'duration':60},'1400960320':{'steps':8,'elevation':0,'calories':0.23,'distance':6.43,'duration':60},'1400960380':{'steps':16,'elevation':0,'calories':0.46,'distance':12.11,'duration':60},'1400960440':{'steps':12,'elevation':0,'calories':0.33,'distance':8.37,'duration':60},'1400960500':{'steps':14,'elevation':0,'calories':0.42,'distance':9.06,'duration':60},'1400960620':{'steps':9,'elevation':0,'calories':0.25,'distance':7.83,'duration':60},'1400960680':{'steps':69,'elevation':0,'calories':2.09,'distance':57.26,'duration':60},'1400960740':{'steps':3,'elevation':0,'calories':0.1,'distance':1.83,'duration':60},'1400961160':{'steps':8,'elevation':0,'calories':0.21,'distance':5.56,'duration':60},'1400961220':{'steps':100,'elevation':0,'calories':2.97,'distance':82.55,'duration':60},'1400961280':{'steps':4,'elevation':0,'calories':0.14,'distance':2.46,'duration':60},'1400961460':{'steps':11,'elevation':0,'calories':0.33,'distance':7.12,'duration':60},'1400961520':{'steps':14,'elevation':0,'calories':0.41,'distance':9.15,'duration':60},'1400961580':{'steps':21,'elevation':0,'calories':0.58,'distance':12.38,'duration':60},'1400961700':{'steps':7,'elevation':0,'calories':0.22,'distance':5.32,'duration':60},'1400961760':{'steps':26,'elevation':0,'calories':0.8,'distance':21,'duration':60},'1400962420':{'steps':31,'elevation':0,'calories':0.92,'distance':22.24,'duration':60},'1400962480':{'steps':70,'elevation':0,'calories':2.13,'distance':56.27,'duration':60},'1400962900':{'steps':13,'elevation':0,'calories':0.38,'distance':9.13,'duration':60},'1400965120':{'steps':6,'elevation':0,'calories':0.19,'distance':3.72,'duration':60},'1400965480':{'steps':6,'elevation':0,'calories':0.17,'distance':4.86,'duration':60},'1400965600':{'steps':6,'elevation':0,'calories':0.17,'distance':3.06,'duration':60},'1400966020':{'steps':12,'elevation':0,'calories':0.38,'distance':10.6,'duration':60},'1400966080':{'steps':42,'elevation':0,'calories':1.31,'distance':36.72,'duration':60},'1400966140':{'steps':8,'elevation':0,'calories':0.23,'distance':6.77,'duration':60},'1400966200':{'steps':33,'elevation':0,'calories':1.03,'distance':27.13,'duration':60},'1400966260':{'steps':6,'elevation':0,'calories':0.18,'distance':5.22,'duration':60},'1400974180':{'steps':7,'elevation':0,'calories':0.15,'distance':5.32,'duration':60},'1400974240':{'steps':65,'elevation':0.31,'calories':2.14,'distance':53.04,'duration':60},'1400974600':{'steps':36,'elevation':0,'calories':1.04,'distance':29.84,'duration':60},'1400974660':{'steps':125,'elevation':0,'calories':3.85,'distance':110.11,'duration':60},'1400974720':{'steps':82,'elevation':0,'calories':2.45,'distance':67.68,'duration':60},'1400974780':{'steps':77,'elevation':0,'calories':2.39,'distance':66.98,'duration':60},'1400974840':{'steps':71,'elevation':0,'calories':2.12,'distance':55.38,'duration':60},'1400974900':{'steps':72,'elevation':0,'calories':2.11,'distance':54.28,'duration':60},'1400974960':{'steps':32,'elevation':0,'calories':0.95,'distance':24.61,'duration':60},'1400975020':{'steps':118,'elevation':0,'calories':3.5,'distance':98.34,'duration':60},'1400975080':{'steps':67,'elevation':0,'calories':1.96,'distance':51.95,'duration':60},'1400975140':{'steps':108,'elevation':0,'calories':3.34,'distance':92.71,'duration':60},'1400975260':{'steps':34,'elevation':0,'calories':1.05,'distance':24.74,'duration':60},'1400975320':{'steps':30,'elevation':0,'calories':0.9,'distance':24.44,'duration':60},'1400975380':{'steps':53,'elevation':0,'calories':1.59,'distance':42.11,'duration':60},'1400975440':{'steps':82,'elevation':0,'calories':2.45,'distance':64.36,'duration':60},'1400975500':{'steps':42,'elevation':0,'calories':1.26,'distance':34.88,'duration':60},'1400975560':{'steps':51,'elevation':0,'calories':1.58,'distance':42.26,'duration':60},'1400980360':{'steps':35,'elevation':0,'calories':1.03,'distance':23.9,'duration':60},'1400980420':{'steps':108,'elevation':0,'calories':3.29,'distance':92.58,'duration':60},'1400980600':{'steps':79,'elevation':0,'calories':2.35,'distance':64.73,'duration':60},'1400980660':{'steps':45,'elevation':0,'calories':1.33,'distance':36.75,'duration':60},'1400980960':{'steps':17,'elevation':0,'calories':0.47,'distance':13.54,'duration':60},'1400981020':{'steps':102,'elevation':0,'calories':3.03,'distance':81.76,'duration':60},'1400981080':{'steps':71,'elevation':0,'calories':2.16,'distance':56.94,'duration':60},'1400981320':{'steps':8,'elevation':0,'calories':0.23,'distance':5.68,'duration':60},'1400982040':{'steps':10,'elevation':0,'calories':0.24,'distance':8.16,'duration':60},'1400982100':{'steps':69,'elevation':0,'calories':2.08,'distance':55.8,'duration':60},'1400982160':{'steps':75,'elevation':0,'calories':2.16,'distance':56.29,'duration':60},'1400982220':{'steps':101,'elevation':0,'calories':2.9,'distance':77.75,'duration':60},'1400982280':{'steps':120,'elevation':0,'calories':3.64,'distance':102.18,'duration':60},'1400982340':{'steps':112,'elevation':0,'calories':3.43,'distance':97.4,'duration':60},'1400982400':{'steps':65,'elevation':0,'calories':1.94,'distance':54.49,'duration':60},'1400982460':{'steps':46,'elevation':0,'calories':1.42,'distance':41.28,'duration':60},'1400982820':{'steps':90,'elevation':0,'calories':2.68,'distance':75.34,'duration':60},'1400982880':{'steps':68,'elevation':3.76,'calories':3.73,'distance':52.86,'duration':60},'1400983000':{'steps':37,'elevation':0,'calories':1.2,'distance':29.39,'duration':60},'1400983060':{'steps':17,'elevation':0,'calories':0.52,'distance':10.68,'duration':60},'1400938140':{'duration':60},'1400938320':{'duration':60},'1400938500':{'duration':60},'1400939040':{'duration':240},'1400939400':{'duration':60},'1400940000':{'duration':60},'1400941800':{'duration':2580},'1400944680':{'duration':1560},'1400946360':{'duration':120},'1400946540':{'duration':240},'1400946900':{'duration':480},'1400947500':{'duration':3120},'1400950680':{'duration':2160},'1400952900':{'duration':1960},'1400954980':{'duration':1140},'1400956360':{'duration':180},'1400956840':{'duration':120},'1400957080':{'duration':1020},'1400958220':{'duration':540},'1400958820':{'duration':60},'1400958940':{'duration':660},'1400959720':{'duration':480},'1400960560':{'duration':60},'1400960800':{'duration':360},'1400961340':{'duration':120},'1400961640':{'duration':60},'1400961820':{'duration':600},'1400962540':{'duration':360},'1400962960':{'duration':2160},'1400965180':{'duration':300},'1400965540':{'duration':60},'1400965660':{'duration':360},'1400966320':{'duration':7860},'1400974300':{'duration':300},'1400975200':{'duration':60},'1400975620':{'duration':4740},'1400980480':{'duration':120},'1400980720':{'duration':240},'1400981140':{'duration':180},'1400981380':{'duration':660},'1400982520':{'duration':300},'1400982940':{'duration':60},'1400983120':{'duration':44940}}}}"
		setWithingsResourceRepsoneWithQS(new MockedHttpURLConnection(mockedActivityResponse))
		def result = withingsDataService.getDataIntraDayActivity(account, new Date(), new Date() + 1, new DataRequestContext())
		assert result.success == true
		def entries = Entry.findAllByUserIdAndComment(account.getUserId(), '(Withings)')
		log.debug("Result size: " + entries.size())
		assert entries.size() == 85
	}
	
	def testIntraDayAdjacentMergeWithLastBlockAsStationary() {
		//When the last entry is just the duration entry
		String mockedActivityResponse = """{"status":0,"body":{"series":{"1395277577":{"steps":21,"elevation":0,"calories":0.58,"distance":16.8,"duration":60},"1395277637":{"steps":12,"elevation":0,"calories":0.33,"distance":10.12,"duration":60},"1395277697":{"steps":25,"elevation":0,"calories":0.73,"distance":21.21,"duration":60},"1395277757":{"steps":69,"elevation":0,"calories":2.15,"distance":61.13,"duration":60},"1395277817":{"steps":126,"elevation":0,"calories":4.03,"distance":118.96,"duration":60},"1395277877":{"duration":600}}}}"""
		setWithingsResourceRepsoneWithQS(new MockedHttpURLConnection(mockedActivityResponse))
		def result = withingsDataService.getDataIntraDayActivity(account, new Date(), new Date() + 1, new DataRequestContext())
		assert result.success == true
		def entries = Entry.findAllByUserIdAndComment(account.getUserId(), '(Withings)')
		log.debug("Result size: " + entries.size())
		entries.each { entry ->
			if (entry.description.contains('duration')) {
				log.debug("Asserting duration to have excluded the stationary entry ${entry.amount}")
				assert entry.amount == 5
			}
		}
		assert entries.size() == 4
	}
	
	def testIntraDayAdjacentMergeWithFirstBlockAsStationary() {
		//When the last entry is just the duration entry
		String mockedActivityResponse = """{"status":0,"body":{"series":{"1395277577":{"duration":60},"1395277637":{"steps":12,"elevation":0,"calories":0.33,"distance":10.12,"duration":60},"1395277697":{"steps":25,"elevation":0,"calories":0.73,"distance":21.21,"duration":60},"1395277757":{"steps":69,"elevation":0,"calories":2.15,"distance":61.13,"duration":60},"1395277817":{"steps":126,"elevation":0,"calories":4.03,"distance":118.96,"duration":60}}}}"""
		setWithingsResourceRepsoneWithQS(new MockedHttpURLConnection(mockedActivityResponse))
		def result = withingsDataService.getDataIntraDayActivity(account, new Date(), new Date() + 1, new DataRequestContext())
		assert result.success == true
		def entries = Entry.findAllByUserIdAndComment(account.getUserId(), '(Withings)')
		log.debug("Result size: " + entries.size())
		assert entries.size() == 4
	}
	
	void testPopulateIntraDayQueue() {
		WithingsDataService.intraDayQueue.clear()
		assert WithingsDataService.intraDayQueue.size() == 0
		
		def intraDayQueueItem = new IntraDayQueueItem(oauthAccountId: 1l, queryDate: new Date())
		Utils.save(intraDayQueueItem)
		withingsDataService.populateIntraDayQueue()
		assert WithingsDataService.intraDayQueue.size() == 1
	}
	
	void testSubscriptionParamsForHTTP() {
		Map result = withingsDataService.getSubscriptionParameters(account, false)
		assert result.callbackurl.contains("http://") == true
		
		grailsApplication.config.grails.serverURL = "https://dev.wearecurio.us/"
		result = withingsDataService.getSubscriptionParameters(account, false)
		assert result.callbackurl.contains("http://") == true
	}	
}
