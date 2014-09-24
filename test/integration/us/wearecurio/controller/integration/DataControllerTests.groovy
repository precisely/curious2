package us.wearecurio.controller.integration

import grails.test.*

import grails.converters.*
import us.wearecurio.controller.DataController;
import us.wearecurio.model.*
import grails.util.GrailsUtil
import us.wearecurio.utility.Utils
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone
import us.wearecurio.model.User
import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.support.EntryStats
import us.wearecurio.support.EntryCreateMap

import static org.junit.Assert.*
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
		assert controller.response.contentAsString.contains('"displayTimeAfterTag":true,"email":"y@y.com","first":"y","last":"y","location":null,"notifyOnComments":true,"password":"b0af8f04890269772b57e4702f7cfb3a","remindEmail":null,"sex":"F","twitterAccountName":null,"twitterDefaultToNow":true,"username":"y","webDefaultToNow":true')
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
		assert controller.response.contentAsString.contains(',"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","description":"bread","amount":1,"amountPrecision":3,"units":"","comment":"","repeatType":null')
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
		assert controller.response.contentAsString.contains(',"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","description":"bread","amount":1,"amountPrecision":3,"units":"","comment":"","repeatType":null')
		assert controller.response.contentAsString.contains('"07/01/2010":[{"')
		assert controller.response.contentAsString.contains(',"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","description":"bread","amount":7,"amountPrecision":3,"units":"","comment":"","repeatType":null')
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
		assert controller.response.contentAsString.endsWith(',1,"bread"]])')
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
		assert controller.response.contentAsString.endsWith(',2,"bread"]])')
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

		assert controller.response.contentAsString.contains(',"date":new Date(1295641800000),"datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","description":"testing","amount":25,"amountPrecision":3,"units":"units","comment":"(comment)","repeatType":null') \
			|| controller.response.contentAsString.contains(',"date":"2011-01-21T20:30:00Z","datePrecisionSecs":180,"timeZoneName":"America/Los_Angeles","description":"testing","amount":25,"amountPrecision":3,"units":"units","comment":"(comment)","repeatType":null') \
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

		assert controller.response.contentAsString.contains(',"date":new Date(1295719200000),"datePrecisionSecs":86400,"timeZoneName":"America/Chicago","description":"updatetest voracious","amount":2,"amountPrecision":3,"units":"units","comment":"","repeatType":null') \
			|| controller.response.contentAsString.contains(',"date":"2011-01-22T18:00:00Z","datePrecisionSecs":86400,"timeZoneName":"America/Chicago","description":"updatetest voracious","amount":2,"amountPrecision":3,"units":"units","comment":"","repeatType":null') \
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

		controller.response.contentAsString.contains('"date":new Date(1278010800000),"datePrecisionSecs":86400,"timeZoneName":"America/Los_Angeles","description":"updatetest voracious","amount":2,"amountPrecision":3,"units":"units","comment":"","repeatType":null')
	}

	@Test
	void testDeleteEntrySData() {
		DataController controller = new DataController()

		EntryStats stats = new EntryStats(userId)
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), stats)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		stats.finish()
		
		controller.session.userId = userId
		controller.params.entryId = entry.getId()
		controller.params.displayDate = "Sat, 11 Aug 2012 07:00:00 GMT"
		controller.params.currentTime = "Sat, 11 Aug 2012 09:00:00 GMT"
		controller.params.baseDate = "Sat, 11 Aug 2012 07:00:00 GMT"
		controller.params.timeZoneName = "America/Los_Angeles"

		controller.params['callback'] = 'callback'

		controller.deleteEntrySData()

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
		controller.params['name'] = 'dummyDiscussion'
		controller.params['discussionPost'] = 'dummyPost'

		controller.createDiscussion()
//		assert controller.response.contentAsString ('success')
		assert Discussion.count() == 1
	}
}
