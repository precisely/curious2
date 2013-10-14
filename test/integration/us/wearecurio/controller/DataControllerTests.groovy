package us.wearecurio.controller

import grails.test.*

import grails.converters.*
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

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

class DataControllerTests extends CuriousControllerTestCase {
	static transactional = true

	DateFormat dateFormat
	Date earlyBaseDate
	Date currentTime
	Date endTime
	TimeZone timeZone // simulated server time zone
	Date baseDate

	@Before
	void setUp() {
		super.setUp()

		def entryTimeZone = Utils.createTimeZone(-8 * 60 * 60, "GMTOFFSET8")
		timeZone = Utils.createTimeZone(-5 * 60 * 60, "GMTOFFSET5")
		dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		dateFormat.setTimeZone(entryTimeZone)
		earlyBaseDate = dateFormat.parse("June 25, 2010 12:00 am")
		currentTime = dateFormat.parse("July 1, 2010 3:30 pm")
		endTime = dateFormat.parse("July 1, 2010 5:00 pm")
		baseDate = dateFormat.parse("July 1, 2010 12:00 am")
	}

	@After
	void tearDown() {
		super.tearDown()
	}
	
	@Test
	void testGetPeopleData() {
		DataController controller = new DataController()
		
		controller.session.userId = userId
		controller.params['callback'] = 'callback'
		
		controller.getPeopleData()
		
		def c = controller.response.contentAsString

		assert controller.response.contentAsString.startsWith('callback([{"class":"us.wearecurio.model.User","id":' + userId + ',"birthdate"')
		assert controller.response.contentAsString.endsWith('"displayTimeAfterTag":true,"email":"y@y.com","first":"y","last":"y","location":null,"notifyOnComments":true,"password":"b0af8f04890269772b57e4702f7cfb3a","remindEmail":null,"sex":"F","twitterAccountName":null,"twitterDefaultToNow":true,"username":"y","webDefaultToNow":true}])')
    }

	@Test
	void testGetListData() {
		DataController controller = new DataController()
		
		controller.session.userId = userId
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		controller.params['callback'] = 'callback'
		controller.params['userId'] = userId.toString()
		controller.params['date'] = 'Wed, 1 Jul 2010 00:00:00 -0000'
		
		controller.getListData()

		assert controller.response.contentAsString.startsWith('callback([{"id":')
		assert controller.response.contentAsString.endsWith(',"datePrecisionSecs":180,"timeZoneOffsetSecs":-14400,"description":"bread","amount":1,"amountPrecision":3,"units":"","comment":"","repeatType":null}])')
    }

	@Test
	void testPlotData() {
		DataController controller = new DataController()
		
		controller.session.userId = userId
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

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
		DataController controller = new DataController()
		
		controller.session.userId = userId
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null)")
		entry = Entry.create(userId, Entry.parse(endTime, timeZone, "bread 1", baseDate, true), null)
		
		controller.params['callback'] = 'callback'
		controller.params['userId'] = userId.toString()
		controller.params['tags'] = new JSON(['bread']).toString()
		controller.params['startDate'] = 'Wed, 1 Jul 2009 00:00:00 -0000'
		controller.params['endDate'] = 'Wed, 1 Jul 2011 00:00:00 -0000'
		controller.params['sumNights'] = 'true'
		controller.params['timeZoneOffset'] = '36000'
		
		controller.getSumPlotData()

		assert controller.response.contentAsString.startsWith('callback([[')
		assert controller.response.contentAsString.endsWith(',2]])')
    }

	@Test
	void testGetTagsData() {
		DataController controller = new DataController()
		
		controller.session.userId = userId
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null)")
		entry = Entry.create(userId, Entry.parse(endTime, timeZone, "bread 1", baseDate, true), null)
		
		controller.params['callback'] = 'callback'
		controller.params['sort'] = 'alpha'
		
		controller.getTagsData()
		
		def c = controller.response.contentAsString
		
		assert controller.response.contentAsString.equals('callback([{"id":' + entry.getTag().getId() + ',"iscontinuous":null,"c":2,"description":"bread","showpoints":null}])')
    }

	@Test
	void testSetTagPropertiesData() {
		DataController controller = new DataController()
		
		controller.session.userId = userId
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null)")
		entry = Entry.create(userId, Entry.parse(endTime, timeZone, "bread 1", baseDate, true), null)
		
		controller.params['callback'] = 'callback'
		controller.params['tags'] = "['bread']"
		controller.params['isContinuous'] = 'true'
		
		controller.setTagPropertiesData()
		
		def c = controller.response.contentAsString
		
		assert Tag.look('bread').getPropertiesForUser(userId).getIsContinuous()
		
		assert controller.response.contentAsString.equals("callback('success')")
    }

	@Test
	void testAddEntryData() {
		DataController controller = new DataController()
		
		controller.session.userId = userId

		controller.params['currentTime'] = 'Fri, 21 Jan 2011 21:46:20 GMT'
		controller.params['text'] = '3:30pm testing 25 units comment'
		controller.params['timeZoneOffset'] = '18000'
		controller.params['userId'] = userId.toString()
		controller.params['callback'] = 'jsonp1295646296016'
		controller.params['_'] = '1295646380043'
		controller.params['defaultToNow'] = '1'
		controller.params['baseDate'] = 'Fri, 21 Jan 2011 05:00:00 GMT'
		controller.params['action'] = 'addEntryData'
		controller.params['controller'] = 'home'

		controller.addEntryData()

		assert controller.response.contentAsString.contains(',"date":new Date(1295641800000),"datePrecisionSecs":180,"timeZoneOffsetSecs":-18000,"description":"testing","amount":25,"amountPrecision":3,"units":"units","comment":"comment","repeatType":null}') \
			|| controller.response.contentAsString.contains(',"date":"2011-01-21T20:30:00Z","datePrecisionSecs":180,"timeZoneOffsetSecs":-18000,"description":"testing","amount":25,"amountPrecision":3,"units":"units","comment":"comment","repeatType":null}') \
    }

	@Test
	void testUpdateEntryData() {
		DataController controller = new DataController()
		
		controller.session.userId = userId

		println "User ID: " + userId

		def entry = Entry.create(userId, [
					userId:userId.toString(),
					tweetId:"",
					date:'Fri, 21 Jan 2011 21:46:20 GMT',
					datePrecisionSecs:180,
					timeZoneOffsetSecs:-18000,
					description:'updatetest',
					amount:new BigDecimal("2.0"),
					units:'units',
					comment:'comment',
					repeatType:null,
					setName:'',
					amountPrecision:3
				], null)

		Utils.save(entry, true)

		controller.params['entryId'] = entry.getId().toString()
		controller.params['currentTime'] = 'Fri, 22 Jan 2011 21:46:20 GMT'
		controller.params['text'] = 'updatetest voracious 2 units'
		controller.params['baseDate'] = 'Fri, 22 Jan 2011 5:00:00 GMT'
		controller.params['timeZoneOffset'] = "18000"
		controller.params['defaultToNow'] = '1'

		controller.updateEntryData()

		assert controller.response.contentAsString.contains(',"date":new Date(1295715600000),"datePrecisionSecs":86400,"timeZoneOffsetSecs":-18000,"description":"updatetest voracious","amount":2,"amountPrecision":3,"units":"units","comment":"","repeatType":null}') \
			|| controller.response.contentAsString.contains(',"date":"2011-01-22T17:00:00Z","datePrecisionSecs":86400,"timeZoneOffsetSecs":-18000,"description":"updatetest voracious","amount":2,"amountPrecision":3,"units":"units","comment":"","repeatType":null}') \
	}

	@Test
	void testDeleteEntryData() {
		DataController controller = new DataController()
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), null)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		controller.session.userId = userId
		controller.params.entryId = entry.getId()
		controller.params.displayDate = "Sat, 11 Aug 2012 07:00:00 GMT"
		controller.params.currentTime = "Sat, 11 Aug 2012 09:00:00 GMT"
		controller.params.baseDate = "Sat, 11 Aug 2012 07:00:00 GMT"
		
		controller.params['callback'] = 'callback'
		
		controller.deleteEntryData()
		
		assert controller.response.contentAsString.equals('callback([])')
    }

	@Test
	void testSetPreferencesData() {
		DataController controller = new DataController()
		
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
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), null)
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneOffsetSecs:-14400, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null)")

		controller.session.userId = userId
		
		controller.params.clear()
		controller.params.callback = 'callback'
		controller.params.all = 'true'
		
		def retVal = controller.autocompleteData()
		
		assert controller.response.contentAsString.equals('callback({"freq":["bread"],"alg":["bread"]})')
	}

	@Test
	void testSavePlotData() {
		DataController controller = new DataController()
		
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
		DataController controller = new DataController()
		
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
		
		assert controller.response.contentAsString.contains('"name":"Discussion name","userId":' + userId + ',"isPublic":false,"created":')
	}
	
	@Test
	void testLoadPlotDataId() {
		DataController controller = new DataController()
		
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
		DataController controller = new DataController()
		
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
		DataController controller = new DataController()
		
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
		DataController controller = new DataController()
		
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
		DataController controller = new DataController()
		
		def plotDataObj = PlotData.createOrReplace(user, 'name', '{foo:foo,isSnapshot:true}', false)
		
		Utils.save(plotDataObj, true)

		controller.session.userId = userId
		
		controller.params.callback = 'callback'
		
		def retVal = controller.listPlotData()
		
		assert controller.response.contentAsString.startsWith('callback([{"id":' + plotDataObj.getId() + ',"name":"name","created"')
	}
	
	@Test
	void testLoadSnapshotDataId() {
		DataController controller = new DataController()
		
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
		DataController controller = new DataController()
		
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
		DataController controller = new DataController()
		
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
		
		Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), null)
		Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 slice", baseDate, true), null)
		Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet repeat daily", baseDate, true), null)
		
		def out = new ByteArrayOutputStream()
		
		controller.doExportCSVAnalysis(out, user)
		
		def outString = out.toString()
		
		assert outString.equals('"Date (GMT) for y","Tag","Amount","Units","Comment","RepeatType","Amount Precision","Date Precision"' \
			+ "\n" + '"2010-07-01 20:00:00 GMT","aspirin",1.000000000,"tablet","repeat daily",256,3,86400' \
			+ "\n" + '"2010-07-01 23:30:00 GMT","bread",1.000000000,"","",-1,3,180' \
			+ "\n" + '"2010-07-01 23:30:00 GMT","bread",1.000000000,"slice","",-1,3,180' \
			+ "\n")
	}

	@Test
	void testImportAnalysisFormat() {
		DataController controller = new DataController()
		
		def outString = '"Date (GMT) for y","Tag","Amount","Units","Comment","RepeatType","Amount Precision","Date Precision"' \
			+ "\n" + '"2010-07-01 23:30:00 GMT","bread",1.000000000,"","",-1,3,180' \
			+ "\n" + '"2010-07-01 23:30:00 GMT","bread",1.000000000,"slice","",-1,3,180' \
			+ "\n" + '"2010-07-01 23:30:00 GMT","aspirin",1.000000000,"tablet","repeat daily",1,3,180' \
			+ "\n" + '"2010-07-01 23:30:00 GMT","aspirin",1.000000000,"tablet","repeat daily",1,null,180' \
			+ "\n" + '"2010-07-01 23:30:00 GMT","aspirin",1.000000000,"tablet","repeat daily",1,3,null' \
			+ "\n"

		def input = new ByteArrayInputStream(outString.getBytes())
		
		controller.doParseCSVDown(input, userId)
		
		def results = Entry.findAllByUserId(userId)
		
		def testStr = "Entry('bread 1.000000000 24:30 ', date:2010-07-01T23:30:00, datePrecisionSecs:180, repeatType:null)" \
				+ "Entry('bread 1.000000000 slice 24:30 ', date:2010-07-01T23:30:00, datePrecisionSecs:180, repeatType:null)" \
				+ "Entry('aspirin 1.000000000 tablet 24:30 repeat daily', date:2010-07-01T23:30:00, datePrecisionSecs:180, repeatType:1)" \
				+ "Entry('aspirin 1.000000000 tablet 24:30 repeat daily', date:2010-07-01T23:30:00, datePrecisionSecs:180, repeatType:1)" \
				+ "Entry('aspirin 1.000000000 tablet 24:30 repeat daily', date:2010-07-01T23:30:00, datePrecisionSecs:180, repeatType:1)"

		def c = 0;
				
		for (e in results) {
			assert testStr.indexOf(e.toString()) >= 0
			++c
		}
		
		assert c == 5
	}

	@Test
	void testImportHumanFormat() {
		DataController controller = new DataController()
		
		def outString = '"export_userx","GMT","0"' \
			+ "\n" + '"07/02/2010 GMT","rererg 12 1:39 comment"' \
			+ "\n" + '"07/04/2010 GMT","raspberry ginger smoothie 1:20 "' \
			+ "\n" + '"07/04/2010 GMT","hello kitty 6:00 "' \
			+ "\n"
		
		def input = new ByteArrayInputStream(outString.getBytes())
		
		controller.doParseCSVDown(input, userId)
		
		def results = Entry.findAllByUserId(userId)
		
		def testStr = "Entry('rererg 12.000000000 2:39 comment', date:2010-07-02T01:39:00, datePrecisionSecs:180, repeatType:null)" \
				+ "Entry('raspberry ginger smoothie 2:20 ', date:2010-07-04T01:20:00, datePrecisionSecs:180, repeatType:null)" \
				+ "Entry('hello kitty 7:00 ', date:2010-07-04T06:00:00, datePrecisionSecs:180, repeatType:null)"
		
		def c = 0;
				
		for (e in results) {
			assert testStr.indexOf(e.toString()) >= 0
			++c
		}
		
		assert c == 3
	}
}
