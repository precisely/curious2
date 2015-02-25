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

	private static def LOG = new File("debug.out")
	public static def log(text) {
		LOG.withWriterAppend("UTF-8", { writer ->
			writer.write( "TagPropertiesTests: ${text}\n")
		})
	}

	@Before
	void setUp() {
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
	}

	@Test
	void testGetPeopleData() {
		controller.session.userId = userId
		controller.params['callback'] = 'callback'

		controller.getPeopleData()

		def c = controller.response.contentAsString

		assert controller.response.contentAsString.startsWith('callback([{"class":"us.wearecurio.model.User","id":' + userId + ',"birthdate"')
		assert controller.response.contentAsString.contains('"displayTimeAfterTag":true,"email":"y@y.com","first":"y","interestTags":null,"last":"y","notifyOnComments":true,"password":"b0af8f04890269772b57e4702f7cfb3a","remindEmail":null,"sex":"F","twitterAccountName":null,"twitterDefaultToNow":true,"username":"y","virtual":null,"webDefaultToNow":true')
	}

	@Test
	void testGetListDataSingleDate() {
		controller.session.userId = userId

		EntryStats stats = new EntryStats(userId)
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
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
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(tomorrowCurrentTime, timeZone, "bread 7", tomorrowBaseDate, true), stats)
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
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
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
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		entry = Entry.create(userId, Entry.parse(endTime, timeZone, "bread 1", baseDate, true), stats)
		
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
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		entry = Entry.create(userId, Entry.parse(endTime, timeZone, "bread 1", baseDate, true), stats)
		
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
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		entry = Entry.create(userId, Entry.parse(endTime, timeZone, "bread 1", baseDate, true), stats)
		
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
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "updatetest 2 units (comment)", baseDate, true), stats)
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
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread repeat daily", earlyBaseDate, true), stats)
		
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
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
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
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 repeat", baseDate, true), stats)

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
			first:'x',
			last:'y',
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
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
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

	@Test
	void testListDiscussionData() {
		controller.session.userId = userId

		UserGroup curious = UserGroup.create("curious", "Curious Discussions", "Discussion topics for Curious users",
				[isReadOnly:false, defaultNotify:false])

		curious.addMember(user)

		Discussion discussion = Discussion.create(user, "Discussion name")
		discussion.createPost(DiscussionAuthor.create(user), null, "comment")
		Utils.save(discussion, true)

		curious.addDiscussion(discussion)

		controller.params.callback = 'callback'

		def retVal = controller.listDiscussionData()

		def content = controller.response.contentAsString

		assert content.contains('"name":"Discussion name","userId":' + userId + ',"isPublic":false,"created":')
	}

	private Discussion setUpForCommentPage() {
		UserGroup curious = UserGroup.create("curious", "Curious Discussions", "Discussion topics for Curious users",
			[isReadOnly:false, defaultNotify:false])

		curious.addMember(user)
	
		Discussion discussion = Discussion.create(user, "Discussion name")
		discussion.createPost(DiscussionAuthor.create(user), "comment1")
		discussion.createPost(DiscussionAuthor.create(user), "comment2")
		discussion.createPost(DiscussionAuthor.create(user), "comment3")
		discussion.createPost(DiscussionAuthor.create(user), "comment4")
		discussion.createPost(DiscussionAuthor.create(user), "comment5")
		discussion.createPost(DiscussionAuthor.create(user), "comment6")
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

		controller.params.discussionId = discussion.id

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
		controller.params.discussionId = discussion.id
		controller.params.offset = 5

		// Test pagination with second page
		controller.listCommentData()
		String contentAsString = controller.response.contentAsString

		assert contentAsString.contains('"message":"comment1"')		// Will be available as first post
		assert !contentAsString.contains('"message":"comment5"')
		assert contentAsString.contains('"message":"comment6"')
	}

	@Test
	void testLoadPlotDataId() {
		def plotDataObj = PlotData.createOrReplace(user, 'name', '{foo:foo}', false)

		Utils.save(plotDataObj, true)

		controller.session.userId = userId

		controller.params.callback = 'callback'
		controller.params.id = plotDataObj.getId().toString()

		def retVal = controller.loadPlotDataId()

		assert controller.response.contentAsString.equals('callback({"username":"y","foo":"foo"})')
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
		
		Entry.create(userId, Entry.parse(winterCurrentTime, timeZone, "bread 1", winterBaseDate, true), stats)
		Entry.create(userId, Entry.parse(winterCurrentTime, timeZone, "bread 1 slice", winterBaseDate, true), stats)
		Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet repeat daily", winterBaseDate, true), stats)
		
		stats.finish()

		def out = new ByteArrayOutputStream()

		controller.doExportCSVAnalysis(out, user)

		def outString = out.toString()

		def compareStr = '"Date (GMT) for y","Tag","Amount","Units","Comment","RepeatType","Amount Precision","Date Precision","Time Zone","Base Tag"\n' \
				+ '"2010-12-01 20:00:00 GMT","aspirin tablet",1.000000000,"tablet","repeat",1025,3,86400,"America/Los_Angeles","aspirin"\n' \
				+ '"2010-12-01 23:30:00 GMT","bread",1.000000000,"","",-1,3,180,"America/Los_Angeles","bread"\n' \
				+ '"2010-12-01 23:30:00 GMT","bread slice",1.000000000,"slice","",-1,3,180,"America/Los_Angeles","bread"\n'
		
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

		def c = 0;

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
		controller.params['discussionId'] = discussionInstance.id
		controller.params['clearPostId'] = 23
		controller.deleteCommentData()
		controller.response.text == "fail"
		
		// Test for valid params
		discussionPostInstance.save(flush: true)
		controller.params['discussionId'] = discussionInstance.id
		controller.params['clearPostId'] = discussionPostInstance.id
		controller.deleteCommentData()
		controller.response.text == "success"
	}
	
	@Test
	void testCreateComment() {
		controller.session.userId = user.getId()
		Discussion discussionInstance = Discussion.create(user, "dummyDiscussion")
		
		// Test for invalid params
		controller.params['discussionId'] = 23
		controller.params['message'] = 'dummyMessage'
		controller.createCommentData()
		controller.response.text == "fail"

		// Test for valid params
		controller.params['discussionId'] = discussionInstance.id
		controller.params['message'] = 'dummyMessage'
		controller.createCommentData()
		controller.response.text == "success"
	}

	Sprint dummySprint
	User dummyUser2
	void mockSprintData() {
		dummySprint = Sprint.create(user, "demo", Model.Visibility.PRIVATE)
		
		Map params = [username: "a", sex: "F", last: "y", email: "a@a.com", birthdate: "01/01/2001", first: "a", password: "y"]
		dummyUser2 = User.create(params)

		Utils.save(dummyUser2, true)
	}

	@Test
	void "Test createSprintData when wrong sprintId is passed"() {
		mockSprintData()
		controller.params["sprintId"] = 0
		controller.params["name"] = "Sprint1"
		controller.params["description"] = "Description1"
		controller.params["durationDays"] = 6
		controller.createSprintData()
		assert controller.response.json.error == true
	}

	@Test
	void "Test createSprintData when null sprintId is passed"() {
		mockSprintData()
		controller.params["sprintId"] = null
		controller.params["name"] = "Sprint1"
		controller.params["description"] = "Description1"
		controller.params["durationDays"] = 6
		controller.createSprintData()
		assert controller.response.json.error == true
	}

	@Test
	void "Test createSprintData when correct sprintId is passed"() {
		mockSprintData()
		controller.params["sprintId"] = dummySprint.id
		controller.params["name"] = "Sprint1"
		controller.params["description"] = "Description1"
		controller.params["durationDays"] = 6
		
		controller.createSprintData()
		assert controller.response.json.success == true
		assert controller.response.json.id == dummySprint.id
		assert dummySprint.name == "Sprint1"
	}

	@Test
	void "test deleteSprintData when wrong sprintId is passed"() {
		mockSprintData()
		controller.params["sprintId"] = 0
		
		controller.deleteSprintData()
		assert controller.response.json.success == false
	}

	@Test
	void "test deleteSprintData when null sprintId is passed"() {
		mockSprintData()
		controller.params["sprintId"] = null
		
		controller.deleteSprintData()
		assert controller.response.json.success == false
	}

	@Test
	void "test deleteSprintData when Non admin user tries to delete the sprint"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["sprintId"] = dummySprint.id
		controller.deleteSprintData()
		assert controller.response.json.success == false
	}

	@Test
	void "test deleteSprintData when admin tries to delete the sprint"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = dummySprint.id
		controller.deleteSprintData()
		assert controller.response.json.success == true
	}

	@Test
	void "Test fetchSprintData when wrong sprintId is passed"() {
		mockSprintData()
		controller.params["sprintId"] = 0
		
		controller.fetchSprintData()
		assert controller.response.json.error == true
	}

	@Test
	void "Test fetchSprintData when null sprintId is passed"() {
		mockSprintData()
		controller.params["sprintId"] = null
		
		controller.fetchSprintData()
		assert controller.response.json.error == true
	}

	@Test
	void "Test fetchSprintData when non admin user tries to fetch data"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["sprintId"] = dummySprint.id
		controller.fetchSprintData()
		assert controller.response.json.error == true
	}

	@Test
	void "Test fetchSprintData when admin tries to fetch data"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = dummySprint.id
		dummySprint.addMember(dummyUser2.getId())
		controller.fetchSprintData()
		assert controller.response.json.sprint.id == dummySprint.id
		assert controller.response.json.participants.size() == 3	//Including virtual user
	}

	@Test
	void "Test startSprintData when wrong sprintId is passed"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = 0
		
		controller.startSprintData()
		assert controller.response.json.success == false
		assert dummySprint.hasStarted(user.getId(), new Date()) == false
	}

	@Test
	void "Test startSprintData when null sprintId is passed"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = null
		
		controller.startSprintData()
		assert controller.response.json.success == false
		assert dummySprint.hasStarted(user.getId(), new Date()) == false
	}

	@Test
	void "Test startSprintData when non member user tries to start the sprint"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["sprintId"] = dummySprint.id
		
		controller.startSprintData()
		assert controller.response.json.success == false
		assert dummySprint.hasStarted(dummyUser2.getId(), new Date()) == false
	}

	@Test
	void startSprint() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = dummySprint.id
		controller.params["now"] = "Wed, 25 Feb 2015 10:44:07 GMT"
		
		controller.startSprintData()
		assert controller.response.json.success == true
		assert dummySprint.hasStarted(user.getId(), new Date()) == true
	}

	@Test
	void "Test stopSprintData when wrong sprintId is passed"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = 0
		
		controller.stopSprintData()
		assert controller.response.json.success == false
		assert dummySprint.hasStarted(user.getId(), new Date()) == false
	}

	@Test
	void "Test stopSprintData when null sprintId is passed"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = null
		
		controller.stopSprintData()
		assert controller.response.json.success == false
		assert dummySprint.hasStarted(user.getId(), new Date()) == false
	}

	@Test
	void "Test stopSprintData when non member user tries to stop the sprint"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["sprintId"] = dummySprint.id
		
		controller.stopSprintData()
		assert controller.response.json.success == false
		assert dummySprint.hasStarted(dummyUser2.getId(), new Date()) == false
	}

	@Test
	void "Test stopSprintData when a member has not started the sprint"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = dummySprint.id
		
		controller.params["now"] = "Wed, 25 Feb 2015 10:44:07 GMT"
		
		controller.stopSprintData()
		assert controller.response.json.success == false
	}

	@Test
	void stopSprintData() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = dummySprint.id
		controller.params["now"] = "Wed, 25 Feb 2015 10:44:07 GMT"
		
		controller.startSprintData()
		controller.response.reset()
		controller.params["now"] = "Wed, 25 Feb 2015 11:44:07 GMT"
		
		controller.stopSprintData()
		assert controller.response.json.success == true
		assert dummySprint.hasEnded(user.getId(), new Date()) == true
	}

	@Test
	void "Test leaveSprintData when wrong sprintId is passed"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = 0
		
		controller.leaveSprintData()
		assert controller.response.json.success == false
	}

	@Test
	void "Test leaveSprintData when null sprintId is passed"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = null
		
		controller.leaveSprintData()
		assert controller.response.json.success == false
	}

	@Test
	void "Test leaveSprintData when non member user tries to leave the sprint"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["sprintId"] = dummySprint.id
		
		controller.leaveSprintData()
		assert controller.response.json.success == false
	}

	@Test
	void "Test leaveSprintData when a member leaves the sprint"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = dummySprint.id
		
		controller.leaveSprintData()
		assert controller.response.json.success == true
		assert dummySprint.hasMember(user.getId()) == false
	}

	@Test
	void "Test joinSprintData when wrong sprintId is passed"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["sprintId"] = 0
		
		controller.joinSprintData()
		assert controller.response.json.success == false
	}

	@Test
	void "Test joinSprintData when null sprintId is passed"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["sprintId"] = null
		
		controller.joinSprintData()
		assert controller.response.json.success == false
	}

	@Test
	void "Test joinSprintData when member user tries to join the sprint"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = dummySprint.id
		
		controller.joinSprintData()
		assert controller.response.json.success == false
	}

	@Test
	void "Test joinSprintData when a non member joins the sprint"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["sprintId"] = dummySprint.id
		
		controller.joinSprintData()
		assert controller.response.json.success == true
		assert dummySprint.hasMember(dummyUser2.getId()) == true
	}

	@Test
	void "Test addMemberToSprintData when wrong sprintId is passed"() {
		mockSprintData()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintId"] = 0
		
		controller.addMemberToSprintData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "Can not add participant!"
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "Test addMemberToSprintData when null sprintId is passed"() {
		mockSprintData()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintId"] = null
		
		controller.addMemberToSprintData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "Can not add participant!"
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "Test addMemberToSprintData when user name is null"() {
		mockSprintData()
		controller.params["sprintId"] = dummySprint.id
		
		controller.addMemberToSprintData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "No user with such username found!"
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "Test addMemberToSprintData when user name is wrong"() {
		mockSprintData()
		controller.params["username"] = "z"
		controller.params["sprintId"] = dummySprint.id
		
		controller.addMemberToSprintData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "No user with such username found!"
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "Test addMemberToSprintData when a non admin performs the action"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintId"] = dummySprint.id
		
		controller.addMemberToSprintData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "you don'thave permission to add members to this sprint!"
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "Test addMemberToSprintData"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintId"] = dummySprint.getId()
		
		controller.addMemberToSprintData()
		assert controller.response.json.success == true
		assert dummySprint.hasMember(dummyUser2.getId()) == true
	}

	@Test
	void "Test addAdminToSprintData when wrong sprintId is passed"() {
		mockSprintData()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintId"] = 0
		
		controller.addAdminToSprintData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "Can not add admin!"
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "Test addAdminToSprintData when null sprintId is passed"() {
		mockSprintData()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintId"] = null
		
		controller.addAdminToSprintData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "Can not add admin!"
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "Test addAdminToSprintData when user name is null"() {
		mockSprintData()
		controller.params["sprintId"] = dummySprint.id
		
		controller.addAdminToSprintData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "No user with such username found!"
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "Test addAdminToSprintData when user name is wrong"() {
		mockSprintData()
		controller.params["username"] = "z"
		controller.params["sprintId"] = dummySprint.id
		
		controller.addAdminToSprintData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "No user with such username found!"
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "Test addAdminToSprintData when a non admin performs the action"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintId"] = dummySprint.id
		
		controller.addAdminToSprintData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "you don'thave permission to add admins to this sprint!"
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "Test addAdminToSprintData"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintId"] = dummySprint.getId()
		
		controller.addAdminToSprintData()
		assert controller.response.json.success == true
		assert dummySprint.hasAdmin(dummyUser2.getId()) == true
	}

	@Test
	void "Test deleteSprintMemberData when wrong sprintId is passed"() {
		mockSprintData()
		controller.params["username"] = user.username
		controller.params["sprintId"] = 0
		
		controller.deleteSprintMemberData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "Can not delete participant!"
		assert dummySprint.hasMember(user.getId()) == true
	}

	@Test
	void "Test deleteSprintMemberData when null sprintId is passed"() {
		mockSprintData()
		controller.params["username"] = user.username
		controller.params["sprintId"] = null
		
		controller.deleteSprintMemberData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "Can not delete participant!"
		assert dummySprint.hasMember(user.getId()) == true
	}

	@Test
	void "Test deleteSprintMemberData when user name is null"() {
		mockSprintData()
		controller.params["sprintId"] = dummySprint.id
		
		controller.deleteSprintMemberData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "Can not delete participant!"
		assert dummySprint.hasMember(user.getId()) == true
	}

	@Test
	void "Test deleteSprintMemberData when user name is wrong"() {
		mockSprintData()
		controller.params["username"] = "z"
		controller.params["sprintId"] = dummySprint.id
		
		controller.deleteSprintMemberData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "Can not delete participant!"
	}

	@Test
	void "Test deleteSprintMemberData when a non admin performs the action"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["username"] = user.username
		controller.params["sprintId"] = dummySprint.id
		
		controller.deleteSprintMemberData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "you don'thave permission to delete members of this sprint!"
		assert dummySprint.hasMember(user.getId()) == true
	}

	@Test
	void "Test deleteSprintMemberData when a non member is being deleted"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintId"] = dummySprint.id
		
		controller.deleteSprintMemberData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "No such Member to delete from this sprint!"
	}

	@Test
	void "Test deleteSprintMemberData"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["username"] = user.username
		controller.params["sprintId"] = dummySprint.getId()
		
		controller.deleteSprintMemberData()
		assert controller.response.json.success == true
		assert dummySprint.hasMember(user.getId()) == false
	}

	@Test
	void "Test deleteSprintAdminData when wrong sprintId is passed"() {
		mockSprintData()
		controller.params["username"] = user.username
		controller.params["sprintId"] = 0
		
		controller.deleteSprintAdminData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "Can not delete Admin!"
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "Test deleteSprintAdminData when null sprintId is passed"() {
		mockSprintData()
		controller.params["username"] = user.username
		controller.params["sprintId"] = null
		
		controller.deleteSprintAdminData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "Can not delete Admin!"
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "Test deleteSprintAdminData when user name is null"() {
		mockSprintData()
		controller.params["sprintId"] = dummySprint.id
		
		controller.deleteSprintAdminData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "Can not delete Admin!"
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "Test deleteSprintAdminData when user name is wrong"() {
		mockSprintData()
		controller.params["username"] = "z"
		controller.params["sprintId"] = dummySprint.id
		
		controller.deleteSprintAdminData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "Can not delete Admin!"
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "Test deleteSprintAdminData when a non admin performs the action"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["username"] = user.username
		controller.params["sprintId"] = dummySprint.id
		
		controller.deleteSprintAdminData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "you don'thave permission to delete admins of this sprint!"
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "Test deleteSprintAdminData when a non admin is being deleted"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintId"] = dummySprint.id
		
		controller.deleteSprintAdminData()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "No such Admin to delete from this sprint!"
	}

	def sessionFactory

	@Test
	void "Test deleteSprintAdminData"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["username"] = user.username
		controller.params["sprintId"] = dummySprint.getId()
		
		controller.deleteSprintAdminData()
		assert controller.response.json.success == true
		assert dummySprint.hasAdmin(user.getId()) == false
	}
}
