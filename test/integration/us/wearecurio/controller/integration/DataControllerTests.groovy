package us.wearecurio.controller.integration

import grails.test.*
import grails.converters.*
import us.wearecurio.controller.DataController;
import us.wearecurio.model.*
import grails.util.GrailsUtil
import us.wearecurio.utility.Utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

import us.wearecurio.model.User
import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.services.EntryParserService
import us.wearecurio.support.EntryStats
import us.wearecurio.support.EntryCreateMap
import static org.junit.Assert.*

import org.joda.time.DateTime
import org.junit.*

import grails.test.mixin.*

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
		TagStats.executeUpdate("delete TagUnitStats t")
		
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
		winterBaseDate = dateFormat.parse("December 1, 2010 12:00 am")
	}

	@After
	void tearDown() {
		super.tearDown()
		Entry.executeUpdate("delete Entry e")
		Discussion.executeUpdate("delete Discussion d")
		DiscussionPost.executeUpdate("delete DiscussionPost p")
	}

	@Test
	void testGetPeopleData() {
		controller.session.userId = userId
		controller.params['callback'] = 'callback'

		controller.getPeopleData()

		def c = controller.response.contentAsString

		assert controller.response.contentAsString.startsWith('callback([{"id":' + userId)
		assert controller.response.contentAsString.contains(',"virtual":false,"username":"y","email":"y@y.com","remindEmail":null,"name":"y y","sex":"F","birthdate":')
		assert controller.response.contentAsString.contains('"website":null,"notifyOnComments":true,"created"')
	}

	@Test
	void testGetListDataSingleDate() {
		controller.session.userId = userId

		EntryStats stats = new EntryStats(userId)
		
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
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
		assert controller.response.contentAsString.contains(',"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","description":"bread","amount":1.000000000,"amountPrecision":3,"units":"","comment":"","repeatType":null')
		}

	@Test
	void testGetListDataMultipleDate() {
		controller.session.userId = userId

		EntryStats stats = new EntryStats(userId)
		
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(tomorrowCurrentTime, timeZone, "bread 7", tomorrowBaseDate, true), stats)
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
		def x = controller.response.contentAsString
		assert controller.response.contentAsString.startsWith('callback({"07/01/2010":[{"')
		assert controller.response.contentAsString.contains(',"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","description":"bread","amount":1.000000000,"amountPrecision":3,"units":"","comment":"","repeatType":null')
		assert controller.response.contentAsString.contains('"07/01/2010":[{"')
		assert controller.response.contentAsString.contains(',"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","description":"bread","amount":7.000000000,"amountPrecision":3,"units":"","comment":"","repeatType":null')
	}

	@Test
	void testPlotData() {
		controller.session.userId = userId

		EntryStats stats = new EntryStats(userId)
		
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
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
		assert controller.response.contentAsString.endsWith(',1.000000000,"bread"]])')
		}

	@Test
	void testGetSumPlotData() {
		controller.session.userId = userId

		EntryStats stats = new EntryStats(userId)
		
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		entry = Entry.create(userId, entryParserService.parse(endTime, timeZone, "bread 1", baseDate, true), stats)
		
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
		assert controller.response.contentAsString.endsWith(',2.000000000,"bread"]])')
		}

	@Test
	void testGetTagsData() {
		controller.session.userId = userId

		EntryStats stats = new EntryStats(userId)
		
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		entry = Entry.create(userId, entryParserService.parse(endTime, timeZone, "bread 1", baseDate, true), stats)
		
		stats.finish()

		controller.params['isContinuous'] = null
		controller.params['callback'] = 'callback'
		controller.params['sort'] = 'alpha'

		log("controller.params: ${controller.params}")

		controller.getTagsData()

		def c = controller.response.contentAsString

		assert controller.response.contentAsString.equals('callback([{"id":' + entry.getTag().getId() + ',"iscontinuous":0,"c":2,"description":"bread","showpoints":null}])')
		}

	@Test
	void testSetTagPropertiesData() {
		controller.session.userId = userId

		EntryStats stats = new EntryStats(userId)
		
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		entry = Entry.create(userId, entryParserService.parse(endTime, timeZone, "bread 1", baseDate, true), stats)
		
		stats.finish()

		controller.params['callback'] = 'callback'
		controller.params['tags'] = "['bread']"
		controller.params['isContinuous'] = 'true'

		controller.setTagPropertiesData()

		def c = controller.response.contentAsString

		assert Tag.look('bread').getPropertiesForUser(userId).isContinuous

		assert controller.response.contentAsString.equals("callback('success')")
	}
	
	@Test
	void testAddEntrySData() {
		controller.session.userId = userId

		controller.params['currentTime'] = 'Fri, 21 Jan 2011 21:46:20 GMT'
		controller.params['text'] = '3:30pm testing 25 units (comment)'
		controller.params['timeZoneName'] = 'America/Los_Angeles'
		controller.params['userId'] = userId.toString()
		controller.params['callback'] = 'jsonp1295646296016'
		controller.params['_'] = '1295646380043'
		controller.params['defaultToNow'] = '1'
		controller.params['baseDate'] = 'Fri, 21 Jan 2011 05:00:00 GMT'
		controller.params['action'] = 'addEntrySData'
		controller.params['controller'] = 'home'

		controller.addEntrySData()

		assert controller.response.contentAsString.contains(',"date":new Date(1295641800000),"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","description":"testing","amount":25.000000000,"amountPrecision":3,"units":"units","comment":"(comment)","repeatType":null') \
			|| controller.response.contentAsString.contains(',"date":"2011-01-21T20:30:00Z","datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","description":"testing","amount":25.000000000,"amountPrecision":3,"units":"units","comment":"(comment)","repeatType":null') \
	}

	@Test
	void testUpdateEntrySData() {
		controller.session.userId = userId

		println "User ID: " + userId

		EntryStats stats = new EntryStats(userId)
		
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "updatetest 2 units (comment)", baseDate, true), stats)
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

		assert controller.response.contentAsString.contains(',"date":new Date(1295719200000),"datePrecisionSecs":86400,"timeZoneName":"America/Chicago","description":"updatetest voracious","amount":2.000000000,"amountPrecision":3,"units":"units","comment":"","repeatType":null') \
			|| controller.response.contentAsString.contains(',"date":"2011-01-22T18:00:00Z","datePrecisionSecs":86400,"timeZoneName":"America/Chicago","description":"updatetest voracious","amount":2.000000000,"amountPrecision":3,"units":"units","comment":"","repeatType":null') \
	}

	@Test
	void testUpdateRepeatEntrySData() {
		controller.session.userId = userId

		println "User ID: " + userId

		EntryStats stats = new EntryStats(userId)
		
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread repeat daily", earlyBaseDate, true), stats)
		
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
		
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
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
	void testDeleteRepeatEntrySData() {
		DataController controller = new DataController()

		EntryStats stats = new EntryStats(userId)
		
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 repeat", baseDate, true), stats)

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
	void testAutocompleteData() {
		DataController controller = new DataController()

		EntryStats stats = new EntryStats(userId)
		
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		stats.finish()
		
		controller.session.userId = userId

		controller.params.clear()
		controller.params.callback = 'callback'
		controller.params.all = 'true'

		def retVal = controller.autocompleteData()

		assert controller.response.contentAsString.equals('callback({"freq":["bread"],"alg":["bread"]})')
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
	void "test list comment data for no id & for for first page"() {
		controller.session.userId = userId

		Discussion discussion = setUpForCommentPage()

		controller.params.callback = 'callback'

		// Test no discussion id
		controller.listCommentData()

		String contentAsString = controller.response.contentAsString
		assert contentAsString.contains('Blank discussion call')

		controller.params.discussionHash = discussion.hash

		// Test pagination with first page
		controller.listCommentData()
		contentAsString = controller.response.contentAsString

		assert contentAsString.contains('"message":"comment1"')
		assert contentAsString.contains('"message":"comment2"')
		assert contentAsString.contains('"message":"comment3"')
		assert !contentAsString.contains('"message":"comment6"')
	}

	@Test
	void "test list discussion data for second page in pagination"() {
		Discussion discussion = setUpForCommentPage()

		controller.session.userId = userId
		controller.params.callback = 'callback'
		controller.params.discussionHash = discussion.hash
		controller.params.offset = 5

		// Test pagination with second page
		controller.listCommentData()
		String contentAsString = controller.response.contentAsString

		assert contentAsString.contains('"message":"comment1"')		// Will be available as first post
		assert !contentAsString.contains('"message":"comment5"')
		assert contentAsString.contains('"message":"comment6"')
	}

	@Test
	void "test set discussion name"() {
		Discussion discussion = setUpForCommentPage()

		controller.session.userId = userId
		controller.params.callback = 'callback'
		controller.params.discussionHash = discussion.hash
		controller.params.name = "foo"
		
		// Test pagination with second page
		controller.setDiscussionNameData()
		String contentAsString = controller.response.contentAsString

		assert contentAsString == "callback('success')"
		assert discussion.name == "foo"
	}

	@Test
	void testLoadPlotDataId() {
		def plotDataObj = PlotData.createOrReplace(user, 'name', '{foo:foo}', false)

		Utils.save(plotDataObj, true)

		controller.session.userId = userId

		controller.params.callback = 'callback'
		controller.params.id = plotDataObj.getId().toString()

		def retVal = controller.loadPlotDataId()

		String content = controller.response.contentAsString
		assert content.contains('"username":"y"') && content.contains('"foo":"foo"})')
	}

	@Test
	void testDeletePlotDataId() {
		def plotDataObj = PlotData.createOrReplace(user, 'name', '{foo:foo}', false)

		Utils.save(plotDataObj, true)

		controller.session.userId = userId

		controller.params.callback = 'callback'
		controller.params.id = plotDataObj.getId().toString()

		def retVal = controller.deletePlotDataId()

		assert controller.response.contentAsString.equals("callback('success')")
	}

	@Test
	void testDeleteNonPlotDataId() {
		def plotDataObj = PlotData.createOrReplace(user, 'name', '{foo:foo}', true)

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
		assert content.startsWith('{"plotDataId":')

		def responseData = JSON.parse(content)

		def plotData = PlotData.get(responseData.plotDataId)

		assert plotData != null
		assert plotData.getIsSnapshot()
	}

	@Test
	void testListSnapshotData() {
		def plotDataObj = PlotData.createOrReplace(user, 'name', '{foo:foo,isSnapshot:true}', false)

		Utils.save(plotDataObj, true)

		controller.session.userId = userId

		controller.params.callback = 'callback'

		def retVal = controller.listPlotData()

		assert controller.response.contentAsString.startsWith('callback([{"id":' + plotDataObj.getId() + ',"name":"name","created"')
	}

	@Test
	void testLoadSnapshotDataId() {
		def plotDataObj = PlotData.createOrReplace(user, 'name', '{foo:foo}', true)

		Utils.save(plotDataObj, true)

		controller.session.userId = userId

		controller.params.callback = 'callback'
		controller.params.id = plotDataObj.getId().toString()

		def retVal = controller.loadSnapshotDataId()

		assert controller.response.contentAsString.equals('callback({"username":"y","foo":"foo"})')
	}

	@Test
	void testDeleteSnapshotDataId() {
		def plotDataObj = PlotData.createOrReplace(user, 'name', '{foo:foo}', true)

		Utils.save(plotDataObj, true)

		controller.session.userId = userId

		controller.params.callback = 'callback'
		controller.params.id = plotDataObj.getId().toString()

		def retVal = controller.deleteSnapshotDataId()

		assert controller.response.contentAsString.equals("callback('success')")
	}

	@Test
	void testDeleteNonSnapshotDataId() {
		def plotDataObj = PlotData.createOrReplace(user, 'name', '{foo:foo}', false)

		Utils.save(plotDataObj, true)

		controller.session.userId = userId

		controller.params.callback = 'callback'
		controller.params.id = plotDataObj.getId().toString()

		def retVal = controller.deleteSnapshotDataId()

		assert !controller.response.contentAsString.equals("callback('success')")
	}

	@Test
	void testExport() {
		DataController controller = new DataController()

		EntryStats stats = new EntryStats(userId)
		
		Entry.create(userId, entryParserService.parse(winterCurrentTime, timeZone, "bread 1", winterBaseDate, true), stats)
		Entry.create(userId, entryParserService.parse(winterCurrentTime, timeZone, "bread 1 slice", winterBaseDate, true), stats)
		Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet repeat daily", winterBaseDate, true), stats)
		
		stats.finish()

		def out = new ByteArrayOutputStream()

		controller.doExportCSVAnalysis(out, user)

		def outString = out.toString()

		def compareStr = '"Date (GMT) for y","Tag","Amount","Units","Comment","RepeatType","Amount Precision","Date Precision","Time Zone","Base Tag"\n' \
				+ '"2010-12-01 20:00:00 GMT","aspirin [tablets]",1.000000000,"tablet","repeat",1025,3,86400,"America/Los_Angeles","aspirin"\n' \
				+ '"2010-12-01 23:30:00 GMT","bread",1.000000000,"","",-1,3,180,"America/Los_Angeles","bread"\n' \
				+ '"2010-12-01 23:30:00 GMT","bread [slices]",1.000000000,"slice","",-1,3,180,"America/Los_Angeles","bread"\n'
		
		assert outString.equals(compareStr)
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
	void testCreateDiscussion() {
		controller.session.userId = user.getId()

		// Test for invalid params
		controller.params['name'] = 'dummyDiscussion'
		controller.createDiscussionData()
		controller.response.text == "fail"

		// Test for valid params
		controller.params['name'] = 'dummyDiscussion'
		controller.params['discussionPost'] = 'dummyPost'
		controller.createDiscussionData()
		controller.response.text == "success"

	}
	
	@Test
	void testDeleteComment() {
		controller.session.userId = user.getId()
		Discussion discussionInstance = Discussion.create(user, "dummyDiscussion")
		DiscussionPost discussionPostInstance = new DiscussionPost([discussionId: discussionInstance.id])

		// Test for invalid params
		controller.params['discussionHash'] = discussionInstance.hash
		controller.params['clearPostId'] = 23
		controller.deleteCommentData()
		controller.response.text == "fail"
		
		// Test for valid params
		discussionPostInstance.save(flush: true)
		controller.params['discussionHash'] = discussionInstance.hash
		controller.params['clearPostId'] = discussionPostInstance.id
		controller.deleteCommentData()
		controller.response.text == "success"
	}
	
	@Test
	void testCreateComment() {
		controller.session.userId = user.getId()
		Discussion discussionInstance = Discussion.create(user, "dummyDiscussion")
		
		// Test for invalid params
		controller.params['discussionHash'] = 23
		controller.params['message'] = 'dummyMessage'
		controller.createCommentData()
		controller.response.text == "fail"

		// Test for valid params
		controller.params['discussionHash'] = discussionInstance.hash
		controller.params['message'] = 'dummyMessage'
		controller.createCommentData()
		controller.response.text == "success"
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
	void "Test SaveSurveyData"() {
		Map questionParams = [code: "qcode1", priority: 4, question: "Question 1?", status: "ACTIVE"]
		SurveyQuestion surveyQuestionInstance1 = SurveyQuestion.create(questionParams)
		questionParams = [code: "qcode2", priority: 5, question: "Question 2?", status: "ACTIVE"]
		SurveyQuestion surveyQuestionInstance2 = SurveyQuestion.create(questionParams)
		questionParams = [code: "qcode3", priority: 3, question: "Question 3?", status: "ACTIVE"]
		SurveyQuestion surveyQuestionInstance3 = SurveyQuestion.create(questionParams)

		controller.session.userId = user.getId()

		// When all questions are answered properly
		controller.params['answer'] = [qcode1: 'Answer no. 1.', qcode2: 'Answer no. 2.',
			qcode3: 'Answer no. 3.']
		controller.saveSurveyData()
		assert controller.response.json.success == true
		assert UserSurveyAnswer.count() == 3
		controller.response.reset()

		// When an answer is missing for a question
		controller.params['answer'] = ['qcode1': null, 'qcode2': 'Answer no. 2.',
			'qcode3': 'Answer no. 3.']
		controller.saveSurveyData()
		assert UserSurveyAnswer.count() == 3		// 3 answers already created in previous call
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("not.saved.message", ["Answers"] as Object[], null)

		controller.response.reset()

		// When blank data is sent
		controller.params['answer'] = [:]
		controller.saveSurveyData()
		assert UserSurveyAnswer.count() == 3
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("default.blank.message", ["Answers"] as Object[], null)
	}

	@Test
	void "Test testGetSurveyData"() {
		Map questionParams = [code: "qcode1", priority: 4, question: "Question 1?", status: "ACTIVE"]
		SurveyQuestion surveyQuestionInstance1 = SurveyQuestion.create(questionParams)
		questionParams = [code: "qcode2", priority: 5, question: "Question 2?", status: "ACTIVE"]
		SurveyQuestion surveyQuestionInstance2 = SurveyQuestion.create(questionParams)
		questionParams = [code: "qcode3", priority: 3, question: "Question 3?", status: "INACTIVE"]
		SurveyQuestion surveyQuestionInstance3 = SurveyQuestion.create(questionParams)


		def responseData = controller.getSurveyData()
		assert controller.response.text.contains("<div class=\"item active\">")
		assert !controller.response.text.contains("qcode3")
	}
}
