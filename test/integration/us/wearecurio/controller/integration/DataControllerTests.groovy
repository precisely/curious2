package us.wearecurio.controller.integration

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONElement
import org.junit.After
import org.junit.Before
import org.junit.Test
import us.wearecurio.controller.DataController
import us.wearecurio.data.RepeatType
import us.wearecurio.model.*
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.registration.UserRegistration
import us.wearecurio.model.survey.QuestionStatus
import us.wearecurio.model.survey.Survey
import us.wearecurio.model.survey.SurveyStatus
import us.wearecurio.model.survey.UserAnswer
import us.wearecurio.model.profiletags.ProfileTag
import us.wearecurio.services.EntryParserService
import us.wearecurio.support.EntryStats
import us.wearecurio.utility.Utils

import java.text.DateFormat

class DataControllerTests extends CuriousControllerTestCase {
	static transactional = true

	DataController controller
	DateFormat dateFormat
	Date earlyBaseDate
	Date currentTime
	Date endTime
	String timeZone // simulated server time zone
	Date baseDate
	Date tomorrowBaseDate
	Date tomorrowCurrentTime
	Date winterCurrentTime
	Date winterLaterTime
	Date winterBaseDate

	EntryParserService entryParserService

	private static def LOG = new File("debug.out")
	public static def log(text) {
		LOG.withWriterAppend("UTF-8", { writer ->
			writer.write( "TagPropertiesTests: ${text}\n")
		})
	}

	@Before
	void setUp() {
		Entry.executeUpdate("delete Entry e")
		Discussion.executeUpdate("delete Discussion d")
		DiscussionPost.executeUpdate("delete DiscussionPost p")
		TagStats.executeUpdate("delete TagStats t")
		TagUnitStats.executeUpdate("delete TagUnitStats t")
		TagProperties.executeUpdate("delete TagProperties tp")

		super.setUp()

		controller = new DataController()
		Locale.setDefault(Locale.US)	// For to run test case in any country.

		def entryTimeZone = Utils.createTimeZone(-8 * 60 * 60, "GMTOFFSET8", true)
		timeZone = "America/Los_Angeles"
		dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		dateFormat.setTimeZone(entryTimeZone)
		earlyBaseDate = dateFormat.parse("June 25, 2010 12:00 am")
		currentTime = dateFormat.parse("July 1, 2010 3:30 pm")
		endTime = dateFormat.parse("July 1, 2010 5:00 pm")
		baseDate = dateFormat.parse("July 1, 2010 12:00 am")
		tomorrowBaseDate = dateFormat.parse("July 2, 2010 12:00 am")
		tomorrowCurrentTime = dateFormat.parse("July 2, 2010 3:30 pm")
		winterCurrentTime = dateFormat.parse("December 1, 2010 3:30 pm")
		winterLaterTime = dateFormat.parse("December 1, 2010 4:30 pm")
		winterBaseDate = dateFormat.parse("December 1, 2010 12:00 am")
	}

	@After
	void tearDown() {
		super.tearDown()
		Entry.executeUpdate("delete Entry e")
		Discussion.executeUpdate("delete Discussion d")
		DiscussionPost.executeUpdate("delete DiscussionPost p")
		TagStats.executeUpdate("delete TagStats t")
		TagUnitStats.executeUpdate("delete TagUnitStats t")
		TagProperties.executeUpdate("delete TagProperties tp")
	}

	@Test
	void testAddTutorialEntry() {
		controller.session.userId = userId

		controller.params.putAll([currentTime:'Fri, 30 Oct 2015 19:11:17 GMT',
			text:'sleep 8 hours',
			dateToken:'1446232277272',
			userId:userId.toString(),
			callback:'jQuery21105134934808593243_1446228256265',
			_:'1446228256271',
			SYNCHRONIZER_TOKEN:'12b3b03c-6062-47d2-9bf8-baee385e3b6e',
			defaultToNow:'1',
			timeZoneName:'America/Los_Angeles',
			baseDate:'Fri, 30 Oct 2015 07:00:00 GMT',
			SYNCHRONIZER_URI:'createSingleHelpEntrysDataCSRF',
			action:'createSingleHelpEntrysData',
			format:'null',
			controller:'home'
		])

		controller.createSingleHelpEntrysData()

		def x = controller.response.contentAsString

		assert !x.contains("success: false")
	}

	@Test
	void testAutocompleteData() {
		DataController controller = new DataController()

		EntryStats stats = new EntryStats(userId)

		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), stats)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		stats.finish()

		controller.session.userId = userId

		controller.params.clear()
		controller.params.callback = 'callback'
		controller.params.all = 'true'
		controller.params.currentTime = "Sat, 11 Jul 2010 08:00:00 GMT"

		def retVal = controller.autocompleteData()

		String str = controller.response.contentAsString
		assert str.startsWith('callback({"freq":')
		assert str.contains('"sleep"')
		assert str.contains('"mood"')
		assert str.contains('"bread"]})')
	}

	@Test
	void testAutocompleteData2() {
		DataController controller = new DataController()

		EntryStats stats = new EntryStats(userId)

		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), stats)
		Entry.create(userId, entryParserService.parse(currentTime - 1, timeZone, "bread 1", null, null, baseDate, true), stats)
		Entry.create(userId, entryParserService.parse(currentTime - 2, timeZone, "bread 1", null, null, baseDate, true), stats)

		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		stats.finish()

		controller.session.userId = userId

		controller.params.clear()
		controller.params.callback = 'callback'
		controller.params.all = 'true'
		controller.params.currentTime = "Sat, 11 Jul 2010 08:00:00 GMT"

		def retVal = controller.autocompleteData()

		String str = controller.response.contentAsString
		assert str.startsWith('callback({"freq":')
		assert str.contains('"sleep"')
		assert str.contains('"mood"')
		assert str.contains('"alg":["bread"')
	}

	@Test
	void testLoadSnapshotDataId() {
		PlotData plotData = PlotData.create(user, 'name', '{foo:foo}', true)
		Utils.save(plotData, true)
		Discussion discussion = Discussion.create(user, "testCreateDiscussion", null, null)
		DiscussionPost post = discussion.createPost(user, plotData.id, "comment")

		DiscussionPost findPost = DiscussionPost.findByPlotDataId(plotData.id)

		assert findPost == post

		controller.session.userId = userId

		controller.params.callback = 'callback'
		controller.params.id = plotData.id.toString()
		controller.params.discussionHash = discussion.hash

		controller.loadSnapshotDataId()

		String c = controller.response.contentAsString

		assert c.startsWith('callback({')
		assert c.contains('"foo":"foo"')
		assert c.contains('"username":"y"')
	}

	@Test
	void testAddEntrySprint() {
		controller.session.userId = userId

		Sprint sprint = Sprint.create(currentTime, user, "Caffeine + Sugar", Visibility.PUBLIC)

		controller.params.putAll([currentTime:'Fri, 30 Oct 2015 19:11:17 GMT',
			text:'headache button',
			dateToken:'1446232277272',
			userId:sprint.virtualUserId.toString(),
			callback:'jQuery21105134934808593243_1446228256265',
			_:'1446228256271',
			SYNCHRONIZER_TOKEN:'12b3b03c-6062-47d2-9bf8-baee385e3b6e',
			defaultToNow:'1',
			timeZoneName:'America/Los_Angeles',
			baseDate:'Fri, 30 Oct 2015 07:00:00 GMT',
			SYNCHRONIZER_URI:'addEntryCSRF',
			action:'addEntrySData',
			format:'null',
			controller:'home'
		])

		controller.addEntrySData()

		def x = controller.response.contentAsString

		assert x.contains('"description":"headache"')
	}

	@Test
	void testUpdateEntrySDataSprint() {
		controller.session.userId = userId

		println "User ID: " + userId

		EntryStats stats = new EntryStats(userId)

		Sprint sprint = Sprint.create(currentTime, user, "Caffeine + Sugar", Visibility.PUBLIC)

		def entry = Entry.create(sprint.virtualUserId, entryParserService.parse(currentTime, timeZone, "headache pinned", null, null, baseDate, true), stats)

		stats.finish()

		controller.params['entryId'] = entry.getId().toString()
		controller.params['currentTime'] = 'Fri, 22 Jan 2011 21:46:20 GMT'
		controller.params['text'] = 'head pain pinned'
		controller.params['baseDate'] = 'Fri, 22 Jan 2011 06:00:00 GMT'
		controller.params['timeZoneName'] = "America/Chicago"
		controller.params['defaultToNow'] = '1'
		controller.params['action'] = 'updateEntrySData'
		controller.params['controller'] = 'home'
		controller.params['allFuture'] = '1'

		controller.updateEntrySData()

		def x = controller.response.contentAsString

		assert x.contains('"description":"head pain"')
	}

	@Test
	void testAddEntrySData2() {
		controller.session.userId = userId

		controller.params.putAll([currentTime:'Fri, 30 Oct 2015 19:11:17 GMT',
			text:'request swing 11:30 (Megan)',
			dateToken:'1446232277272',
			userId:userId.toString(),
			callback:'jQuery21105134934808593243_1446228256265',
			_:'1446228256271',
			SYNCHRONIZER_TOKEN:'12b3b03c-6062-47d2-9bf8-baee385e3b6e',
			defaultToNow:'1',
			timeZoneName:'America/Los_Angeles',
			baseDate:'Fri, 30 Oct 2015 07:00:00 GMT',
			SYNCHRONIZER_URI:'addEntryCSRF',
			action:'addEntrySData',
			format:'null',
			controller:'home'
		])

		controller.addEntrySData()

		def x = controller.response.contentAsString

		assert x.contains(',"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","tagId":')
		assert x.contains(',"description":"request swing","amount":1.000000000,"amountPrecision":-1,"units":"","comment":"(Megan)","repeatType":null,"repeatEnd":null,"amounts":{"0":{"amount":1.000000000,"amountPrecision":-1,"units":""}},"normalizedAmounts":{"0":{"amount":1.000000000,"amountPrecision":-1,"units":"","sum":false')
	}

	@Test
	void testExport() {
		DataController controller = new DataController()

		EntryStats stats = new EntryStats(userId)

		Entry.create(userId, entryParserService.parse(winterCurrentTime, timeZone, "bread 1", null, null, winterBaseDate, true), stats)
		Entry.create(userId, entryParserService.parse(winterLaterTime, timeZone, "bread 1 slice", null, null, winterBaseDate, true), stats)
		Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet repeat daily", null, null, winterBaseDate, true), stats)

		stats.finish()

		def out = new ByteArrayOutputStream()

		controller.doExportCSVAnalysis(out, user)

		def outString = out.toString()

		def compareStr = '"Date (GMT) for y","Tag","Amount","Units","Comment","RepeatType","Amount Precision","Date Precision","Time Zone","Base Tag"\n' \
				+ '"2010-12-01 20:00:00 GMT","aspirin [tablets]",1.000000000,"tablet","repeat",1025,3,86400,"America/Los_Angeles","aspirin"\n' \
				+ '"2010-12-01 23:30:00 GMT","bread",1.000000000,"","",-1,3,180,"America/Los_Angeles","bread"\n' \
				+ '"2010-12-02 00:30:00 GMT","bread [slices]",1.000000000,"slice","",-1,3,180,"America/Los_Angeles","bread"\n'

		assert outString.equals(compareStr)
	}

	@Test
	void "Test createHelpEntriesData when entryId passed in parameters"() {
		EntryStats stats = new EntryStats(userId)

		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep 2 hours", null, null, baseDate, true), stats)
		stats.finish()

		assert Entry.get(entry.id).amount == 2.000000000g

		controller.session.userId = userId
		controller.params['entries[]'] = 'sleep 3 hours'
		controller.params['entryId'] = entry.id.toString()
		controller.params["currentTime"] = "Wed, 25 Feb 2015 10:44:07 GMT"
		controller.params["baseDate"] = "Wed, 25 Feb 2015 00:00:00 GMT"
		controller.params["timeZoneName"] = "Asia/Kolkata"

		controller.createHelpEntriesData()
		assert controller.response.json.success == true
		assert Entry.get(entry.id).amount == 3.000000000g
	}

	@Test
	void testGetPeopleData() {
		controller.session.userId = userId
		controller.params['callback'] = 'callback'

		controller.getPeopleData()

		def c = controller.response.contentAsString

		assert controller.response.contentAsString.startsWith('callback([{"id":' + userId)
		assert controller.response.contentAsString.contains(',"virtual":false,"')
		assert controller.response.contentAsString.contains('"avatarURL":null,')
		assert controller.response.contentAsString.contains('"username":"y"')
		assert controller.response.contentAsString.contains('"sex":"F",')
		assert controller.response.contentAsString.contains('"website":null')
	}

	@Test
	void testGetListDataSingleDate() {
		controller.session.userId = userId

		EntryStats stats = new EntryStats(userId)

		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), stats)
		stats.finish()
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		controller.params['callback'] = 'callback'
		controller.params['userId'] = userId.toString()
		controller.params['timeZoneName'] = 'America/Los_Angeles'
		controller.params['date'] = 'Wed, 1 Jul 2010 00:00:00 -0000'
		controller.params['currentTime'] = 'Wed, 1 Jul 2010 10:00:00 -0000'

		controller.getListData()

		assert controller.response.contentAsString.startsWith('callback([{"id":')
		assert controller.response.contentAsString.contains(',"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","tagId":')
		assert controller.response.contentAsString.contains(',"description":"bread","amount":1.000000000,"amountPrecision":3,"units":"","comment":"","repeatType":null')
	}

	@Test
	void testGetListDataMultipleDate() {
		controller.session.userId = userId

		EntryStats stats = new EntryStats(userId)

		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), stats)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(tomorrowCurrentTime, timeZone, "bread 7", null, null, tomorrowBaseDate, true), stats)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-02T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		stats.finish()

		controller.params['callback'] = 'callback'
		controller.params['userId'] = userId.toString()
		controller.params['timeZoneName'] = 'America/Los_Angeles'
		controller.params['date[]'] = []
		controller.params['date[]'][0] = 'Wed, 1 Jul 2010 00:00:00 -0800'
		controller.params['date[]'][1] = 'Thu, 2 Jul 2010 00:00:00 -0800'
		controller.params['currentTime'] = 'Wed, 1 Jul 2010 10:00:00 -0800'

		controller.getListData()
		println controller.response.contentAsString
		assert controller.response.contentAsString.contains(',"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","tagId":')
		assert controller.response.contentAsString.contains(',"description":"bread","amount":1.000000000,"amountPrecision":3,"units":"","comment":"","repeatType":null')
		assert controller.response.contentAsString.contains('"07/01/2010":[{"')
		assert controller.response.contentAsString.contains(',"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","tagId":')
		assert controller.response.contentAsString.contains(',"description":"bread","amount":1.000000000,"amountPrecision":3,"units":"","comment":"","repeatType":null')
	}

	@Test
	void testPlotData() {
		controller.session.userId = userId

		EntryStats stats = new EntryStats(userId)

		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), stats)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		stats.finish()

		controller.params['callback'] = 'callback'
		controller.params['userId'] = userId.toString()
		controller.params['tags'] = new JSON(['bread']).toString()
		controller.params['startDate'] = 'Wed, 1 Jul 2009 00:00:00 -0000'
		controller.params['endDate'] = 'Wed, 1 Jul 2011 00:00:00 -0000'

		controller.getPlotData()

		assert controller.response.contentAsString.startsWith('callback([[')
		assert controller.response.contentAsString.endsWith(',1.000000000,"bread",' + Tag.look('bread').id + ']])')
		}

	@Test
	void testGetSumPlotData() {
		controller.session.userId = userId

		EntryStats stats = new EntryStats(userId)

		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), stats)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		entry = Entry.create(userId, entryParserService.parse(endTime, timeZone, "bread 1", null, null, baseDate, true), stats)

		stats.finish()

		controller.params['callback'] = 'callback'
		controller.params['userId'] = userId.toString()
		controller.params['tags'] = new JSON(['bread']).toString()
		controller.params['startDate'] = 'Wed, 1 Jul 2009 00:00:00 -0000'
		controller.params['endDate'] = 'Wed, 1 Jul 2011 00:00:00 -0000'
		controller.params['sumNights'] = 'true'
		controller.params['timeZoneOffset'] = '36000'

		controller.getSumPlotData()

		assert controller.response.contentAsString.startsWith('callback([[')
		assert controller.response.contentAsString.endsWith(',2.000000000,"bread",' + Tag.look('bread').id + ']])')
		}

	@Test
	void testGetTagsData() {
		controller.session.userId = userId

		EntryStats stats = new EntryStats(userId)

		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), stats)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		entry = Entry.create(userId, entryParserService.parse(endTime, timeZone, "bread 1", null, null, baseDate, true), stats)

		stats.finish()

		controller.params['isContinuous'] = null
		controller.params['callback'] = 'callback'
		controller.params['sort'] = 'alpha'

		log("controller.params: ${controller.params}")

		controller.getTagsData()

		def c = controller.response.contentAsString

		assert c.startsWith('callback([{"')
		assert c.contains('"iscontinuous":0')
		assert c.contains('"description":"bread"')
		assert c.contains('"id":' + entry.tag.id)
		assert c.contains('"c":2')
		assert c.contains('"showpoints":null')
		}

	@Test
	void testSetTagPropertiesData() {
		controller.session.userId = userId

		EntryStats stats = new EntryStats(userId)

		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), stats)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		entry = Entry.create(userId, entryParserService.parse(endTime, timeZone, "bread 1", null, null, baseDate, true), stats)

		stats.finish()

		controller.params['callback'] = 'callback'
		controller.params['tags'] = "['bread']"
		controller.params['isContinuous'] = 'true'

		controller.setTagPropertiesData()

		def c = controller.response.contentAsString

		assert Tag.look('bread').getPropertiesForUser(userId).isContinuous

		assert controller.response.contentAsString.equals("callback('success')")
	}

	Map getEntryParams() {
		Map params = [:]

		params['currentTime'] = 'Fri, 21 Jan 2011 21:46:20 GMT'
		params['text'] = '3:30pm testing 25 units (comment)'
		params['timeZoneName'] = 'America/Los_Angeles'
		params['userId'] = userId.toString()
		params['callback'] = 'jsonp1295646296016'
		params['_'] = '1295646380043'
		params['defaultToNow'] = '1'
		params['baseDate'] = 'Fri, 21 Jan 2011 05:00:00 GMT'
		params['action'] = 'addEntrySData'
		params['controller'] = 'home'

		return params 
	}

	@Test
	void testAddEntrySData() {
		controller.session.userId = userId

		controller.params.putAll(getEntryParams())

		controller.addEntrySData()

		assert controller.response.contentAsString.contains(',"date":new Date(1295641800000),"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","tagId":')
		assert controller.response.contentAsString.contains(',"description":"testing","amount":25.000000000,"amountPrecision":3,"units":"units","comment":"(comment)","repeatType":null,"repeatEnd":null,"amounts":{"0":{"amount":25.0000,"amountPrecision":3,"units":"units"}},"normalizedAmounts"')
	}

	@Test
	void "test AddEntrySData for updating the FIRST_ENTRY and SECOND_ENTRY bits when user creates entries from mobile app"() {
		controller.session.userId = userId

		User user = User.get(userId)
		int FIRST_ENTRY = 11
		int SECOND_ENTRY = 12

		// First clearing the previously set bits. (Just to ensure that this call sets the bits correctly)
		user.settings.clear(FIRST_ENTRY)
		user.settings.clear(SECOND_ENTRY)

		assert !user.settings.get(FIRST_ENTRY)
		assert !user.settings.get(SECOND_ENTRY)
		assert !user.settings.hasEntryCreationCountCompleted()

		// Sending the mobileSessionId parameter
		controller.params.mobileSessionId = "abcdefghij1245231"

		controller.params.putAll(getEntryParams())

		// First entry count
		controller.addEntrySData()

		assert controller.response.contentAsString.contains(',"date":new Date(1295641800000),"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","tagId":')
		assert controller.response.contentAsString.contains(',"description":"testing","amount":25.000000000,"amountPrecision":3,"units":"units","comment":"(comment)","repeatType":null,"repeatEnd":null,"amounts":{"0":{"amount":25.0000,"amountPrecision":3,"units":"units"}},"normalizedAmounts"')
		
		assert user.settings.get(FIRST_ENTRY)
		assert !user.settings.get(SECOND_ENTRY)
		assert !user.settings.hasEntryCreationCountCompleted()

		// Second entry count
		controller.params['currentTime'] = 'Fri, 22 Jan 2011 21:46:20 GMT'
		controller.params['text'] = '4:30pm testing 25 units (comment)'
		controller.addEntrySData()

		assert user.settings.get(FIRST_ENTRY)
		assert user.settings.get(SECOND_ENTRY)
		assert user.settings.hasEntryCreationCountCompleted()
	}

	@Test
	void "test AddEntrySData for not updating the FIRST_ENTRY and SECOND_ENTRY bits when user creates entries from web"() {
		controller.session.userId = userId

		User user = User.get(userId)
		int FIRST_ENTRY = 11
		int SECOND_ENTRY = 12

		// First clearing the previously set bits. (Just to ensure that this call sets the bits correctly)
		user.settings.clear(FIRST_ENTRY)
		user.settings.clear(SECOND_ENTRY)

		assert !user.settings.get(FIRST_ENTRY)
		assert !user.settings.get(SECOND_ENTRY)
		assert !user.settings.hasEntryCreationCountCompleted()

		controller.params.putAll(getEntryParams())

		controller.addEntrySData()

		assert controller.response.contentAsString.contains(',"date":new Date(1295641800000),"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","tagId":')
		assert controller.response.contentAsString.contains(',"description":"testing","amount":25.000000000,"amountPrecision":3,"units":"units","comment":"(comment)","repeatType":null,"repeatEnd":null,"amounts":{"0":{"amount":25.0000,"amountPrecision":3,"units":"units"}},"normalizedAmounts"')

		// There will be no change in the user settings bits for entry creation count
		assert !user.settings.get(FIRST_ENTRY)
		assert !user.settings.get(SECOND_ENTRY)
		assert !user.settings.hasEntryCreationCountCompleted()
	}

	@Test
	void "test AddEntrySData for updating the BOOKMARK_CREATED bits when user creates bookmarks from mobile app"() {
		controller.session.userId = userId

		User user = User.get(userId)
		int BOOKMARK_CREATED_1 = 19
		int BOOKMARK_CREATED_2 = 20
		int BOOKMARK_CREATED_3 = 21

		// First clearing the previously set bits. (Just to ensure that this call sets the bits correctly)
		user.settings.clear(BOOKMARK_CREATED_1)
		user.settings.clear(BOOKMARK_CREATED_2)
		user.settings.clear(BOOKMARK_CREATED_3)

		assert !user.settings.get(BOOKMARK_CREATED_1)
		assert !user.settings.get(BOOKMARK_CREATED_2)
		assert !user.settings.get(BOOKMARK_CREATED_3)

		// Sending the mobileSessionId parameter
		controller.params.mobileSessionId = "abcdefghij1245231"

		controller.params.putAll(getEntryParams())
		controller.params['text'] = 'Bookmark 1'
		controller.params['repeatTypeId'] = RepeatType.CONTINUOUS.id.toString()

		// For first bookmark
		controller.addEntrySData()

		assert user.settings.get(BOOKMARK_CREATED_1)
		assert !user.settings.get(BOOKMARK_CREATED_2)
		assert !user.settings.get(BOOKMARK_CREATED_3)
		assert !user.settings.hasBookmarkCreationCountCompleted()

		// For second bookmark
		controller.params['currentTime'] = 'Fri, 22 Jan 2011 21:46:20 GMT'
		controller.params['text'] = 'Bookmark 2'
		controller.addEntrySData()

		assert user.settings.get(BOOKMARK_CREATED_1)
		assert user.settings.get(BOOKMARK_CREATED_2)
		assert !user.settings.get(BOOKMARK_CREATED_3)
		assert !user.settings.hasBookmarkCreationCountCompleted()

		// For third bookmark
		controller.params['currentTime'] = 'Fri, 23 Jan 2011 21:46:20 GMT'
		controller.params['text'] = 'Bookmark 3'
		controller.addEntrySData()

		assert user.settings.get(BOOKMARK_CREATED_1)
		assert user.settings.get(BOOKMARK_CREATED_2)
		assert user.settings.get(BOOKMARK_CREATED_3)
		assert user.settings.hasBookmarkCreationCountCompleted()
	}

	@Test
	void "test AddEntrySData for updating the FIRST_ALERT_ENTRY bit when user creates an alert entry from mobile app"() {
		controller.session.userId = userId

		User user = User.get(userId)
		int FIRST_ALERT_ENTRY = 22

		// First clearing the previously set bits. (Just to ensure that this call sets the bits correctly)
		user.settings.clear(FIRST_ALERT_ENTRY)

		assert !user.settings.get(FIRST_ALERT_ENTRY)
		assert !user.settings.hasFirstAlertEntryCountCompleted()

		// Sending the mobileSessionId parameter
		controller.params.mobileSessionId = "abcdefghij1245231"

		controller.params.putAll(getEntryParams())
		controller.params['repeatTypeId'] = RepeatType.ALERT.id.toString()

		controller.addEntrySData()

		assert user.settings.get(FIRST_ALERT_ENTRY)
		assert user.settings.hasFirstAlertEntryCountCompleted()
	}

	@Test
	void testUpdateEntrySData() {
		controller.session.userId = userId

		println "User ID: " + userId

		EntryStats stats = new EntryStats(userId)

		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "updatetest 2 units (comment)", null, null, baseDate, true), stats)
		stats.finish()

		controller.params['entryId'] = entry.getId().toString()
		controller.params['currentTime'] = 'Fri, 22 Jan 2011 21:46:20 GMT'
		controller.params['text'] = 'updatetest voracious 2 units'
		controller.params['baseDate'] = 'Fri, 22 Jan 2011 06:00:00 GMT'
		controller.params['timeZoneName'] = "America/Chicago"
		controller.params['defaultToNow'] = '1'
		controller.params['action'] = 'updateEntrySData'
		controller.params['controller'] = 'home'
		controller.params['allFuture'] = '1'

		controller.updateEntrySData()

		assert controller.response.contentAsString.contains(',"date":new Date(1295719200000),"datePrecisionSecs":86400,"timeZoneName":"America/Chicago","tagId":')
		assert controller.response.contentAsString.contains(',"description":"updatetest voracious","amount":2.000000000,"amountPrecision":3,"units":"units","comment":"","repeatType":null')
	}

	@Test
	void testUpdateRepeatEntrySData() {
		controller.session.userId = userId

		println "User ID: " + userId

		EntryStats stats = new EntryStats(userId)

		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread repeat daily", null, null, earlyBaseDate, true), stats)

		stats.finish()

		controller.params['entryId'] = entry.getId().toString()
		controller.params['currentTime'] = 'Thu, 1 Jul 2010 21:46:20 GMT'
		controller.params['text'] = 'updatetest voracious 2 units'
		controller.params['baseDate'] = 'Thu, 1 Jul 2010 07:00:00 GMT'
		controller.params['timeZoneName'] = "America/Los_Angeles"
		controller.params['defaultToNow'] = '1'
		controller.params['action'] = 'updateEntrySData'
		controller.params['controller'] = 'home'
		controller.params['allFuture'] = '1'

		controller.updateEntrySData()

		controller.response.contentAsString.contains('"date":new Date(1278010800000),"datePrecisionSecs":86400,"timeZoneName":"America/Los_Angeles","description":"updatetest voracious","amount":2.000000000,"amountPrecision":3,"units":"units","comment":"","repeatType":null')
	}

	@Test
	void testDeleteEntrySData() {
		DataController controller = new DataController()

		EntryStats stats = new EntryStats(userId)

		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), stats)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		stats.finish()

		controller.session.userId = userId
		controller.params.entryId = entry.getId().toString()
		controller.params.displayDate = "Sat, 11 Aug 2012 07:00:00 GMT"
		controller.params.date = "Sat, 11 Aug 2012 09:00:00 GMT"
		controller.params.baseDate = "Sat, 11 Aug 2012 07:00:00 GMT"
		controller.params.timeZoneName = "America/Los_Angeles"

		controller.params['callback'] = 'callback'

		controller.deleteEntrySData()

		def content = controller.response.contentAsString

		assert content.startsWith('callback([[]')
	}

	@Test
	void "Test delete entry when entry belongs to trackathon and user does not have write permission on trackathon"() {
		DataController controller = new DataController()

		Map params = new HashMap()
		params.put("username", "testuser2")
		params.put("email", "test2@test.com")
		params.put("password", "eiajrvaer")
		params.put("first", "first2")
		params.put("last", "last2")
		params.put("sex", "M")
		params.put("location", "New York, NY")
		params.put("birthdate", "12/1/1991")

		User user2 = User.create(params)
		Utils.save(user2, true)

		Sprint sprint = Sprint.create(currentTime, user2, "Caffeine + Sugar", Visibility.PUBLIC)
		assert sprint.userId == user2.id
		assert sprint.name == "Caffeine + Sugar"
		assert sprint.visibility == Visibility.PUBLIC
		assert sprint.fetchTagName() == "caffeine sugar trackathon"

		Utils.save(sprint, true)

		EntryStats stats = new EntryStats(sprint.virtualUserId)

		def entry = Entry.create(sprint.virtualUserId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), stats)

		stats.finish()

		controller.session.userId = userId
		controller.params.entryId = entry.getId().toString()
		controller.params.displayDate = "Sat, 11 Aug 2012 07:00:00 GMT"
		controller.params.date = "Sat, 11 Aug 2012 09:00:00 GMT"
		controller.params.baseDate = "Sat, 11 Aug 2012 07:00:00 GMT"
		controller.params.timeZoneName = "America/Los_Angeles"

		controller.params['callback'] = 'callback'

		controller.deleteEntrySData()

		def content = controller.response.contentAsString

		assert content.contains('You do not have permission to delete this entry.')
	}

	@Test
	void "Test delete entry when entry belongs to trackathon and user has write permission on trackathon"() {
		DataController controller = new DataController()

		Sprint sprint = Sprint.create(currentTime, user, "Caffeine + Sugar", Visibility.PUBLIC)
		assert sprint.userId == userId
		assert sprint.name == "Caffeine + Sugar"
		assert sprint.visibility == Visibility.PUBLIC
		assert sprint.fetchTagName() == "caffeine sugar trackathon"

		Utils.save(sprint, true)

		EntryStats stats = new EntryStats(sprint.virtualUserId)

		def entry = Entry.create(sprint.virtualUserId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), stats)

		stats.finish()

		controller.session.userId = userId
		controller.params.entryId = entry.getId().toString()
		controller.params.displayDate = "Sat, 11 Aug 2012 07:00:00 GMT"
		controller.params.date = "Sat, 11 Aug 2012 09:00:00 GMT"
		controller.params.baseDate = "Sat, 11 Aug 2012 07:00:00 GMT"
		controller.params.timeZoneName = "America/Los_Angeles"

		controller.params['callback'] = 'callback'

		controller.deleteEntrySData()

		def content = controller.response.contentAsString

		assert !content.contains('You do not have permission to delete this entry.')
		assert content.startsWith('callback([[]')
	}

	@Test
	void testDeleteRepeatEntrySData() {
		DataController controller = new DataController()

		EntryStats stats = new EntryStats(userId)

		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 repeat", null, null, baseDate, true), stats)

		stats.finish()

		controller.session.userId = userId
		controller.params.entryId = entry.getId().toString()
		controller.params.displayDate = "Sat, 11 Jul 2012 08:00:00 GMT"
		controller.params.date = "Sat, 11 Jul 2012 08:00:00 GMT"
		controller.params.baseDate = "Sat, 11 Jul 2012 08:00:00 GMT"
		controller.params.timeZoneName = "America/Los_Angeles"

		controller.params['callback'] = 'callback'

		controller.deleteGhostEntryData()

		def content = controller.response.contentAsString

		assert content.startsWith('callback([[]')
	}

	@Test
	void testSetPreferencesData() {
		controller.session.userId = userId

		controller.params.clear()
		controller.params.callback = 'callback'
		controller.params.putAll([
			displayTimeAfterTag:'on',
			username:'y', sex:'F',
			preaction:'index',
			twitterDefaultToNow:'on',
			precontroller:'home',
			name:'x y',
			email:'x@x.x',
			userId:userId.toString(),
			birthdate:'01/01/2001',
			password:'',
			action:'doupdateuserpreferences',
			controller:'home'
		])

		def retVal = controller.setPreferencesData()

		assert controller.response.contentAsString.equals('callback(1)')
	}

	@Test
	void testSavePlotData() {
		controller.session.userId = userId

		controller.params.clear()
		controller.params.callback = 'callback'
		controller.params.name = 'name'
		controller.params.plotData = '{}'

		def retVal = controller.savePlotData()

		assert controller.response.contentAsString.startsWith('["success"')
	}

	private Discussion setUpForCommentPage() {
		UserGroup curious = UserGroup.create("curious", "Curious Discussions", "Discussion topics for Curious users",
			[isReadOnly:false, defaultNotify:false])

		curious.addMember(user)

		Discussion discussion = Discussion.create(user, "Discussion name")
		discussion.createPost(user, "comment1", currentTime)
		discussion.createPost(user, "comment2", currentTime + 1)
		discussion.createPost(user, "comment3", currentTime + 2)
		discussion.createPost(user, "comment4", currentTime + 3)
		discussion.createPost(user, "comment5", currentTime + 4)
		discussion.createPost(user, "comment6", currentTime + 5)
		Utils.save(discussion, true)

		curious.addDiscussion(discussion)
		return discussion
	}

	@Test
	void testLoadPlotDataId() {
		def plotDataObj = PlotData.create(user, 'name', '{foo:foo}', false)

		Utils.save(plotDataObj, true)

		controller.session.userId = userId

		controller.params.callback = 'callback'
		controller.params.id = plotDataObj.getId().toString()

		def retVal = controller.loadPlotDataId()

		String content = controller.response.contentAsString
		assert content.contains('"username":"y"') && content.contains('"foo":"foo"')
	}

	@Test
	void testDeletePlotDataId() {
		def plotDataObj = PlotData.create(user, 'name', '{foo:foo}', false)

		Utils.save(plotDataObj, true)

		controller.session.userId = userId

		controller.params.callback = 'callback'
		controller.params.id = plotDataObj.getId().toString()

		def retVal = controller.deletePlotDataId()

		assert controller.response.contentAsString.equals("callback('success')")
	}

	@Test
	void testDeleteNonPlotDataId() {
		def plotDataObj = PlotData.create(user, 'name', '{foo:foo}', true)

		Utils.save(plotDataObj, true)

		controller.session.userId = userId

		controller.params.callback = 'callback'
		controller.params.id = plotDataObj.getId().toString()

		def retVal = controller.deletePlotDataId()

		assert !controller.response.contentAsString.equals("callback('success')")
	}

	@Test
	void testSaveSnapshotData() {
		controller.session.userId = userId

		controller.params.clear()
		controller.params.callback = 'callback'
		controller.params.name = 'snap name'
		controller.params.snapshotData = '{foo:foo,isSnapshot:true}'

		def retVal = controller.saveSnapshotData()

		def content = controller.response.contentAsString
		def responseData = JSON.parse(content)

		assert responseData.success
		def discussion = Discussion.findByHash(responseData.discussionHash)

		assert discussion != null
		Long plotDataId = DiscussionPost.get(discussion.firstPostId).plotDataId
		assert PlotData.get(plotDataId).getIsSnapshot()
	}

	@Test
	void testListSnapshotData() {
		def plotDataObj = PlotData.create(user, 'name', '{foo:foo,isSnapshot:true}', false)

		Utils.save(plotDataObj, true)

		controller.session.userId = userId

		controller.params.callback = 'callback'

		def retVal = controller.listPlotData()

		assert controller.response.contentAsString.startsWith('callback([{"id":' + plotDataObj.getId() + ',"name":"name","created"')
	}

	@Test
	void testLoadSnapshotDataIdNoHash() {
		def plotDataObj = PlotData.create(user, 'name', '{foo:foo}', true)

		Utils.save(plotDataObj, true)

		controller.session.userId = userId

		controller.params.callback = 'callback'
		controller.params.id = plotDataObj.getId().toString()

		def retVal = controller.loadSnapshotDataId()

		assert !controller.response.contentAsString.equals('callback({"username":"y","foo":"foo"})')
	}

	@Test
	void testDeleteSnapshotDataId() {
		def plotDataObj = PlotData.create(user, 'name', '{foo:foo}', true)

		Utils.save(plotDataObj, true)

		controller.session.userId = userId

		controller.params.callback = 'callback'
		controller.params.id = plotDataObj.getId().toString()

		def retVal = controller.deleteSnapshotDataId()

		assert controller.response.contentAsString.equals("callback('success')")
	}

	@Test
	void testDeleteNonSnapshotDataId() {
		def plotDataObj = PlotData.create(user, 'name', '{foo:foo}', false)

		Utils.save(plotDataObj, true)

		controller.session.userId = userId

		controller.params.callback = 'callback'
		controller.params.id = plotDataObj.getId().toString()

		def retVal = controller.deleteSnapshotDataId()

		assert !controller.response.contentAsString.equals("callback('success')")
	}

	@Test
	void testImportAnalysisFormat() {
		def outString = '"Date (GMT) for y","Tag","Amount","Units","Comment","RepeatType","Amount Precision","Date Precision","Time Zone"' \
			+ "\n" + '"2010-07-01 23:30:00 GMT","bread",1.000000000,"","",-1,3,180,"America/Los_Angeles"' \
			+ "\n" + '"2010-07-01 23:30:00 GMT","bread",1.000000000,"slice","",-1,3,180,"America/Los_Angeles"' \
			+ "\n" + '"2010-07-01 23:30:00 GMT","aspirin",1.000000000,"tablet","repeat daily",1,3,180,"America/Los_Angeles"' \
			+ "\n" + '"2010-07-01 23:31:00 GMT","aspirin",1.000000000,"tablet","repeat daily",1,-1,180,"America/Los_Angeles"' \
			+ "\n" + '"2010-07-01 23:32:00 GMT","aspirin",1.000000000,"tablet","repeat daily",1,3,null,"America/Los_Angeles"' \
			+ "\n"

		def input = new ByteArrayInputStream(outString.getBytes())

		controller.doParseCSVDown(input, userId)

		def results = Entry.findAllByUserId(userId)

		def testStr = "Entry(date:2010-07-01T23:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)" \
				+ "Entry(date:2010-07-01T23:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)" \
				+ "Entry(date:2010-07-01T23:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat daily, repeatType:1, repeatEnd:null)" \
				+ "Entry(date:2010-07-01T23:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:-1, comment:repeat daily, repeatType:1, repeatEnd:null)" \
				+ "Entry(date:2010-07-01T23:32:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat daily, repeatType:1, repeatEnd:null)"

		def c = 0

		for (e in results) {
			def v = e.contentString()
			assert testStr.indexOf(v) >= 0
			++c
		}

		assert c == 5
	}

	@Test
	void "Test createHelpEntriesData when no entries are passed in params"() {
		controller.session.userId = user.getId()
		controller.session.showHelp = true
		controller.params['entries[]'] = []
		controller.params["currentTime"] = "Wed, 25 Feb 2015 10:44:07 GMT"
		controller.params["baseDate"] = "Wed, 25 Feb 2015 00:00:00 GMT"
		controller.params["timeZoneName"] = "Asia/Kolkata"

		controller.createHelpEntriesData()
		assert controller.response.json.success
		assert !controller.session.showHelp
	}

	@Test
	void "Test createHelpEntriesData when no blank entries are passed in params"() {
		controller.session.userId = user.getId()
		controller.params['entries[]'] = ['mood 3', 'sleep 9 hrs']
		controller.params["currentTime"] = "Wed, 25 Feb 2015 10:44:07 GMT"
		controller.params["baseDate"] = "Wed, 25 Feb 2015 00:00:00 GMT"
		controller.params["timeZoneName"] = "Asia/Kolkata"

		controller.createHelpEntriesData()
		assert controller.response.json.success == true
		assert Entry.count() == 2
	}

	@Test
	void "Test saveSurveyData to save answers for all active questions"() {
		given: 'A Survey instance with active questions for all 3 answer types'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)

		surveyInstance.addToQuestions(question: 'This is the first question.', priority: '1', status: 'ACTIVE',
				answerType: 'DESCRIPTIVE')

		surveyInstance.addToQuestions(question: 'This is the second question.', priority: '2', status: 'ACTIVE',
				answerType: 'MCQ_RADIO', isRequired: true)
		surveyInstance.questions[1].addToAnswers([associatedProfileTags: [Tag.look('mood')], priority: '1',
				answer: 'This is the second answer first radio', associatedTrackingTags: [Tag.look('sleep')]])
		surveyInstance.questions[1].addToAnswers([associatedTrackingTags: [Tag.look('run')], priority: '2',
				answer: 'This is the second answer second radio'])

		surveyInstance.addToQuestions(question: 'This is the third question.', priority: '3', status: 'ACTIVE',
				answerType: 'MCQ_CHECKBOX')
		surveyInstance.questions[2].addToAnswers([associatedProfileTags: [Tag.look('sleep')], priority: '1',
				answer: 'This is the third answer first checkbox', associatedTrackingTags: [Tag.look('mood')]])
		surveyInstance.questions[2].addToAnswers([associatedProfileTags: [Tag.look('coffee')], priority: '2',
				answer: 'This is the third answer second checkbox'])
		surveyInstance.save(flush: true)

		when: 'A required question is not answered'
		controller.session.userId = user.id
		controller.params.surveyCode = 's001'

		controller.params.questionId0 = surveyInstance.questions[0].id
		controller.params.questionId1 = surveyInstance.questions[1].id

		controller.params.questionId2 = surveyInstance.questions[2].id
		controller.params.answerText20 = 'This is the third answer first checkbox'
		controller.params.answerId20 = surveyInstance.questions[2].answers[0].id
		controller.params.answerText21 = 'This is the third answer second checkbox'
		controller.params.answerId21 = surveyInstance.questions[2].answers[1].id

		controller.saveSurveyData()

		then: 'The survey should rollback and no answers will be saved'
		JSONElement json = JSON.parse(controller.response.text)
		assert json.success == false
		assert json.message == 'Survey Data could not be saved'
		assert UserAnswer.count() == 0
		assert ProfileTag.count() == 0
		assert TagStats.count() == 0

		when: 'The saveSurveyData action is hit with survey code for active survey and all answers'
		controller.response.reset()
		controller.session.userId = user.id
		controller.params.surveyCode = 's001'

		controller.params.questionId0 = surveyInstance.questions[0].id
		controller.params.answerText0 = 'This is the first answer.'

		controller.params.questionId1 = surveyInstance.questions[1].id
		controller.params.answerText10 = 'This is the second answer first radio'
		controller.params.answerId10 = surveyInstance.questions[1].answers[0].id

		controller.params.questionId2 = surveyInstance.questions[2].id
		controller.params.answerText20 = 'This is the third answer first checkbox'
		controller.params.answerId20 = surveyInstance.questions[2].answers[0].id
		controller.params.answerText21 = 'This is the third answer second checkbox'
		controller.params.answerId21 = surveyInstance.questions[2].answers[1].id

		controller.saveSurveyData()

		then: 'The request should complete successfully and UserAnswer and profiletags, tagstats should be created'
		JSONElement json1 = JSON.parse(controller.response.text)
		assert json1.success == true
		assert UserAnswer.count() == 4 // one descriptive, one radio and two checkboxes
		assert ProfileTag.count() == 3 // mood, coffee, sleep
		assert TagStats.count() == 2 // sleep, mood (Only one out of run or mood will be created depending on user's answer)
	}

	@Test
	void "Test testGetSurveyData to get only active survey"() {
		given: 'A Survey instance'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.INACTIVE)
		surveyInstance.addToQuestions(question: 'This is the first question.', priority: '1', status: 'INACTIVE',
				answerType: 'DESCRIPTIVE')
		surveyInstance.save(flush: true)
		UserRegistration userRegistration = UserRegistration.create(user.id, surveyInstance.code)
		assert UserRegistration.count() == 1
		assert UserRegistration.findByUserId(user.id).id == userRegistration.id

		when: 'The getSurveyTemplateData action is hit with survey code for Inactive survey'
		controller.session.userId = user.id
		controller.getSurveyTemplateData()

		then: 'The request should fail'
		assert controller.response.json.success == false

		when: 'There are no active questions for this survey'
		surveyInstance.status = SurveyStatus.ACTIVE
		surveyInstance.questions[0].status = QuestionStatus.INACTIVE
		surveyInstance.save(flush: true)

		controller.response.reset()
		controller.session.userId = user.id
		controller.params.surveyCode = 's001'
		controller.getSurveyTemplateData()

		then: 'The request should fail'
		assert controller.response.json.success == false
	}

	@Test
	void "Test requestTrackingProjectData"() {
		when: "email is blank in parameters"
		controller.params["requesterEmail"] = ""
		controller.params["topic"] = "Deep Sleep"
		assert !TrackingProjectRequest.count()
		controller.requestTrackingProjectData()

		then: "error should be thrown with proper error message"
		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.blank.message", ["Email"] as Object[],
				null)
		assert !TrackingProjectRequest.count()

		controller.response.reset()

		when: "topic is blank"
		controller.params["requesterEmail"] = "selena@gomz.com"
		controller.params["topic"] = ""
		!TrackingProjectRequest.count()
		controller.requestTrackingProjectData()

		then: "error should be thrown with proper error message"
		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.blank.message", ["Health topic"] as Object[],
				null)
		assert !TrackingProjectRequest.count()

		controller.response.reset()

		when: "email is not of valid email type"
		controller.params["requesterEmail"] = "selena@"
		controller.params["topic"] = "Blood pressure"
		assert !TrackingProjectRequest.count()
		controller.requestTrackingProjectData()

		then: "error should be thrown with proper error message"
		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.invalid.email.message", ["selena@"] as
				Object[], null)
		assert !TrackingProjectRequest.count()

		controller.response.reset()

		when: "request parameters are fine"
		controller.params["requesterEmail"] = "selena@gomz.com"
		controller.params["topic"] = "Blood pressure"
		assert !TrackingProjectRequest.count()
		controller.requestTrackingProjectData()

		then: "an instance of TrackingProjectRequest is created"
		assert controller.response.json.success
		assert TrackingProjectRequest.count() == 1
		assert TrackingProjectRequest.first().email == "selena@gomz.com"
	}

	@Test
	void 'test getAllTagsWithInputType action'() {
		given: 'A few instances of Tag, TagStats and TagInputType'
		User userInstance = User.create([username:'shaneMac1', sex:'F', name:'shane macgowen', email:'shane@pogues.com',
				birthdate:'01/01/1960', password:'shanexyz', action:'doregister',controller:'home'])

		Tag tagInstance1 = Tag.create('sleep')
		Tag tagInstance2 = Tag.create('mood')
		Tag tagInstance3 = Tag.create('energy')

		// Clearing old instances if any.
		TagStats.findAllByUserId(userInstance.id)*.delete(flush: true)

		TagStats.createOrUpdate(userInstance.id, tagInstance1.id)
		TagStats.createOrUpdate(userInstance.id, tagInstance2.id)
		TagStats.createOrUpdate(userInstance.id, tagInstance3.id)

		// Clearing old instances if any.
		TagInputType.list()*.delete(flush: true)
		TagInputType.clearCache()

		new TagInputType(tagId: tagInstance1.id, max: 10, min: 0, noOfLevels: 5,
				inputType: InputType.THUMBS, defaultUnit: 'miles').save(flush: true)
		new TagInputType(tagId: tagInstance2.id, max: 10, min: 0, noOfLevels: 5,
				inputType: InputType.SMILEY, defaultUnit: 'hours').save(flush: true)
		new TagInputType(tagId: tagInstance3.id, max: 10, min: 0, noOfLevels: 5,
				inputType: InputType.LEVEL, defaultUnit: 'calories').save(flush: true)

		when: 'getAllTagsWithInputType action is hit'
		controller.request.method = 'GET'
		controller.getAllTagsWithInputType()

		then: 'Server responds with all available TagInputType and the resulting TagInputTypes are added to ' +
				'BoundedCache'
		List jsonResponse = controller.response.json.tagsWithInputTypeList

		assert jsonResponse.size() == 3
		assert jsonResponse[0].description == 'energy'
		assert jsonResponse[1].description == 'mood'
		assert jsonResponse[2].description == 'sleep'

		assert controller.response.json.cacheDate == TagInputType.cacheDate.time
		assert TagInputType.cachedTagInputTypes.size() == 3

		when: 'The clientCacheDate is present in request and the cache is not updated after this date'
		controller.response.reset()
		Long oldCacheDate = TagInputType.cacheDate.time
		controller.params.lastInputTypeCacheDate = oldCacheDate
		controller.getAllTagsWithInputType()

		then: 'The result should be empty'
		assert controller.response.json == [:]

		when: 'The cache is updated and the client still has old cache date'
		controller.response.reset()
		controller.params.lastInputTypeCacheDate = oldCacheDate

		new TagInputType(tagId: Tag.look('tea').id, max: 10, min: 0, noOfLevels: 5,
				inputType: InputType.LEVEL, defaultUnit: 'calories').save(flush: true)
		TagInputType.initializeCachedTagWithInputTypes()

		controller.getAllTagsWithInputType()
		jsonResponse = controller.response.json.tagsWithInputTypeList

		then: 'The response should have updated cache date and TagInputTypeList'
		assert jsonResponse.size() == 4
		assert jsonResponse[0].description == 'energy'
		assert jsonResponse[1].description == 'mood'
		assert jsonResponse[2].description == 'sleep'
		assert jsonResponse[3].description == 'tea'

		Long updatedCacheDate = TagInputType.cacheDate.time

		assert oldCacheDate < updatedCacheDate
		assert controller.response.json.cacheDate == updatedCacheDate
		assert TagInputType.cachedTagInputTypes.size() == 4
	}

	@Test
	void 'test getTagsForAutoComplete for various searchString'() {
		given: 'A few Tag instances'
		List tagDescriptions = ['sleep', 'run', 'coffee', 'exercise', 'mood']
		tagDescriptions.each {String description ->
			Tag.look(description)
		}

		when: 'getTagsForAutoComplete action is hit with empty searchString'
		controller.request.method = 'GET'
		controller.params.q = ''
		controller.getTagsForAutoComplete()

		then: 'The server returns false'
		assert controller.response.status == 200
		assert controller.response.json.success == false

		when: 'getTagsForAutoComplete action is hit and no results are found for the given searchString'
		controller.response.reset()
		controller.params.clear()
		controller.params.q = 'zxcvbnm'
		controller.getTagsForAutoComplete()

		then: 'Server returns empty list in response'
		assert controller.response.status == 200
		assert controller.response.json.size() == 0

		when: 'getTagsForAutoComplete action is hit and results are found for the given searchString'
		controller.response.reset()
		controller.params.clear()
		controller.params.q = 'e'
		controller.getTagsForAutoComplete()

		then: 'Server returns list of matching results'
		assert controller.response.status == 200
		assert controller.response.json.size() != null
		controller.response.json.each {
			assert it.name.contains('e')
		}
	}
}
