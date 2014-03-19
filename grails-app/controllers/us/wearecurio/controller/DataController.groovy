package us.wearecurio.controller

import grails.converters.*
import us.wearecurio.model.*
import us.wearecurio.exceptions.*
import us.wearecurio.services.CorrelationService
import us.wearecurio.utility.Utils
import us.wearecurio.model.Discussion;
import us.wearecurio.model.Entry.RepeatType;
import us.wearecurio.model.Entry.TagStatsRecord

import java.math.MathContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.SimpleTimeZone
import java.util.TimeZone

import org.apache.jasper.compiler.Node.ParamsAction;
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.*

class DataController extends LoginController {
	SimpleDateFormat systemFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

	CorrelationService correlationService
	
	DateFormat dateFormat
	Date earlyBaseDate
	Date currentTime
	Date endTime
	Date repeatTime
	Date noRepeatTime
	TimeZone timeZone // simulated server time zone
	Date baseDate
	User user
	Long userId

	static debug(str) {
		log.debug(str)
	}
	
	static final String AUTH_ERROR_MESSAGE = 'You have logged into a different account in another window. Please refresh the browser window.'
	static final String SYNTAX_ERROR_MESSAGE = 'Internal error in application. Please update your application or contact Curious.'
	
	def DataController() {
		debug "constructor()"
		systemFormat.setTimeZone(TimeZone.getDefault());
	}

	protected def doAddEntry(currentTimeStr, timeZoneName, userIdStr, textStr, baseDateStr, defaultToNow) {
		debug "DataController.doAddEntry() currentTimeStr:" + currentTimeStr + ", timeZoneName:" + timeZoneName + ", userIdStr:" + userIdStr \
				+ ", baseDateStr:" + baseDateStr + ", defaultToNow:" + defaultToNow
		
		def user = userFromIdStr(userIdStr)

		def currentTime = parseDate(currentTimeStr)
		def baseDate = parseDate(baseDateStr)
		timeZoneName = timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate) : timeZoneName
		
		debug("Current time " + currentTime + " baseDate " + baseDate);

		def parsedEntry = Entry.parse(currentTime, timeZoneName, textStr, baseDate, defaultToNow)
		TagStatsRecord record = new TagStatsRecord()
		def entry = Entry.create(user.getId(), parsedEntry, record)

		debug("created " + entry)

		return [entry, parsedEntry['status'], record.getOldTagStats()]
	}

	protected def doUpdateEntry(entryIdStr, currentTimeStr, textStr, baseDateStr, timeZoneName, defaultToNow, allFuture) {
		debug "DataController.doUpdateEntry() entryIdStr:" + entryIdStr + ", currentTimeStr:" + currentTimeStr \
				+ ", textStr:" + textStr + ", baseDateStr:" + baseDateStr + ", timeZoneName:" + timeZoneName \
				+ ", defaultToNow:" + defaultToNow
		
		def entry = Entry.get(Long.parseLong(entryIdStr))

		if (entry.getUserId() != sessionUser().getId()) {
			debug "No permission to edit this entry"
			return [null, 'You do not have permission to edit this entry.', null, null]
		}
			
		if (entry.fetchIsGenerated()) {
			debug "Can't edit a generated entry"
			return [null, 'You cannot edit a generated entry.', null, null]
		}
		
		def currentTime = parseDate(currentTimeStr)
		def baseDate = parseDate(baseDateStr)
		timeZoneName = timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate) : timeZoneName
		
		def m = Entry.parse(currentTime, timeZoneName, textStr, baseDate, false, true)

		if (entry != null) {
			TagStatsRecord record = new TagStatsRecord()
			entry = Entry.update(entry, m, record, baseDate, allFuture)
			return [entry, '', record.getOldTagStats(), record.getNewTagStats()];
		} else {
			debug "Parse error"
			return [null, 'Cannot interpret entry text.', null, null];
		}
	}

	// find entries including those with null events
	protected def listEntries(User user, String timeZoneName, String dateStr) {
		debug "DataController.listEntries() userId:" + user.getId() + ", timeZoneName:" + timeZoneName + ", dateStr:" + dateStr
		
		Date baseDate = parseDate(dateStr)
		
		timeZoneName = timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate) : timeZoneName
		
		return Entry.fetchListData(user, timeZoneName, baseDate, new Date())
	}
	
	MathContext mc = new MathContext(9)
	
	protected def doParseCSVDown(InputStream csvIn, Long userId) {
		debug "DataController.doParseCSVDown() userId:" + userId
		
		Reader reader = new InputStreamReader(csvIn)

		int lineNum = 0

		String setName = "import"
		String timeZoneName
		Date currentDate = null
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss z").withZone(DateTimeZone.UTC)
		Date now = new Date()

		reader.eachCsvLine { tokens ->
			++lineNum
			if (lineNum == 1) {
				setName = tokens[0]
				if (setName.startsWith("Date (UTC) for ")) {
					setName = "Imported for " + setName.substring(15)
					debug "setName: " + setName + " analysis type"
				} else {
					return false // format error
				}
				// delete previous import of same data set
				Entry.executeUpdate("delete Entry e where e.setName = :setName and e.userId = :userId",
						[setName: setName, userId: userId])
			} else {
				if (tokens[0]) {
					currentDate = dateTimeFormatter.parseDateTime(tokens[0]).toDate()
					debug "Date: " + currentDate
				}
				if (currentDate) {
					Long repeatTypeId = Long.valueOf(tokens[5])
					def parsedEntry = [ \
						userId:userId, \
						tweetId:null, \
						date:currentDate, \
						timeZoneName:tokens[8], \
						description:tokens[1], \
						amount:new BigDecimal(tokens[2], mc), \
						units:tokens[3], \
						comment:tokens[4], \
						repeatType:repeatTypeId >= 0 ? Entry.RepeatType.get(repeatTypeId) : null, \
						setName:setName, \
						amountPrecision:Integer.valueOf(tokens[6].equals("null") ? '3' : tokens[6]), \
						datePrecisionSecs:Integer.valueOf(tokens[7].equals("null") ? '180':tokens[7]), \
					]
					def entry = Entry.create(userId, parsedEntry, null)

					debug("created " + entry)
				}
			}
		}
		
		return true
	}

	/**
	 * Returns a field value escaped for special characters
	 * @param input A String to be evaluated
	 * @return A properly formatted String
	 */
	static String csvEscape(String input) {
		if (input.contains(",") || input.contains("\n") || input.contains('"') || (!input.trim().equals(input))) {
			return '"' + input.replaceAll('"', '""') + '"'
		} else {
			return '"' + input + '"'
		}
	}
	
	static writeCSV(Writer writer, str) {
		writer.write(csvEscape(str))
	}
	
	static writeNumber(Writer writer, str) {
		writer.write(str)
	}
	
	protected def doExportCSVAnalysis(OutputStream out, User user) {
		debug "DataController.doExportCSV() + userId:" + user.getId()
		
		Writer writer = new OutputStreamWriter(out)

		writeCSV(writer,"Date (GMT) for " + user.getUsername())
		writer.write(",")
		writeCSV(writer,"Tag")
		writer.write(",")
		writeCSV(writer,"Amount")
		writer.write(",")
		writeCSV(writer,"Units")
		writer.write(",")
		writeCSV(writer,"Comment")
		writer.write(",")
		writeCSV(writer,"RepeatType")
		writer.write(",")
		writeCSV(writer,"Amount Precision")
		writer.write(",")
		writeCSV(writer,"Date Precision")
		writer.write(",")
		writeCSV(writer,"Time Zone")
		writer.write("\n")

		def c = Entry.createCriteria()
		def results = c {
			and {
				eq("userId", user.getId())
				not {
					isNull("date")
				}
			}
			order("date","asc")
		}
		
		TimeZone timeZone = Utils.createTimeZone(0, "GMT", false)
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
		dateFormat.setTimeZone(timeZone)
		
		for (Entry entry in results) {
			writeCSV(writer, dateFormat.format(entry.getDate()))
			writer.write(",")
			writeCSV(writer, entry.getTag().getDescription())
			writer.write(",")
			writeNumber(writer, entry.getAmount().toString())
			writer.write(",")
			writeCSV(writer, entry.getUnits())
			writer.write(",")
			writeCSV(writer, entry.getComment())
			writer.write(",")
			writeNumber(writer, entry.getRepeatType() ? entry.getRepeatType().getId().toString() : "-1")
			writer.write(",")
			writeNumber(writer, entry.getAmountPrecision().toString())
			writer.write(",")
			writeNumber(writer, entry.getDatePrecisionSecs().toString())
			writer.write(",")
			writeCSV(writer, entry.fetchTimeZoneName())
			writer.write("\n")
		}
		
		writer.flush()
	}

	def getPeopleData() {
		debug "DataController.getPeopleData"
		
		def user = sessionUser()
		
		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}
		
		debug "user:" + user

		renderJSONGet([user])
	}

	// for backwards compatibility with older mobile apps - no longer used
	def getEntriesData() {
		debug "DataController.getEntriesData() userId:" + params.userId + " date: " + params.date
		
		def user = userFromIdStr(params.userId);
		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		def entries = Entry.fetchListDataNoRepeats(user, parseDate(params.date), new Date())
		
		// skip continuous repeat entries with entries within the usage threshold

		renderJSONGet(entries)
	}

	def activateGhostEntry() {
		debug "DataController.activateGhostEntry()"

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		Entry entry = Entry.get(params.entryId.toLong());
		def userId = entry.getUserId();
		
		if (!UniqueTimedToken.acquire("activateGhost:" + userId + ":" + params.entryId + ":" + params.date, new Date(), 1500)) {
			renderStringGet("error") // silently fail
		} else {
			Date baseDate = params.date == null ? null : parseDate(params.date)
			Date currentTime = params.currentTime == null ? new Date() : parseDate(params.currentTime)
			String timeZoneName = params.timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate) : params.timeZoneName
	
			if (entry.getUserId() != sessionUser().getId()) {
				renderStringGet('You do not have permission to activate this entry.')
			}
			Entry newEntry = entry.activateGhostEntry(baseDate, currentTime, timeZoneName)
			if (newEntry != null) {
				renderJSONGet(newEntry.getJSONDesc())
			} else
				renderStringGet("Failed to activate entry due to internal server error.")
		}
	}

	def deleteGhostEntryData() {
		debug "DataController.deleteGhostEntryData() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		def entry = Entry.get(Long.parseLong(params.entryId))
		def userId = entry.getUserId();
		def allFuture = params.all?.equals("true") ? true : false
		
		def currentDate = params.date == null ? null : parseDate(params.date)
		
		if (entry.getUserId() != sessionUser().getId()) {
			renderStringGet('You do not have permission to delete this entry.')
		} else
			Entry.deleteGhost(entry, currentDate, allFuture)
		
		renderStringGet("success")
	}

	def getListData() {
		debug "DataController.getListData() userId:" + params.userId + " date: " + params.date + " timeZoneName:" + params.timeZoneName
		
		def user = userFromIdStr(params.userId);
		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}
		
		def timeZoneName = params.timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(params.date) : params.timeZoneName
		DateTimeZone userTimeZone = TimeZoneId.look(timeZoneName).toDateTimeZone()
		
		def currentTime = params.currentTime == null ? new Date() : parseDate(params.currentTime)
		
		def entries
		
		if (params.date != null && params.date instanceof String) {
			debug "DataController.getListData() '" + params.date + "'"
			entries = Entry.fetchListData(user, timeZoneName, parseDate(params.date), currentTime)
		} else {
			def dateList = params.list("date[]")
			debug "DataController.getListData() " + dateList.dump()
			entries = [:]
			dateList.each { date ->
				date = parseDate(date)
				LocalDate localDate = new DateTime(date.time, DateTimeZone.forID('UTC')).withZone(userTimeZone).toLocalDate()
				def data = Entry.fetchListData(user, timeZoneName, date, currentTime)
				debug "DataController.getListData(): " + date + " data: " + data
				entries = entries << ["${localDate.toString('MM/dd/yyyy')}":data]
			}
		}

		debug "DataController.getListData() Returning entries "+entries.dump()
		// skip continuous repeat entries with entries within the usage threshold

		renderJSONGet(entries)
	}

	def getPlotData() {
		def tags = JSON.parse(params.tags)
		def startDateStr = params.startDate
		def endDateStr = params.endDate
		def timeZoneName = params.timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate) : params.timeZoneName
		
		debug "DataController.getPlotData() tags:" + tags + " startDate:" + startDateStr + " endDate:" + endDateStr + " timeZoneName:" + timeZoneName

		def tagIds = []
		
		for (tagStr in tags) {
			tagIds.add(Tag.look(tagStr).getId())
		}

		def results = Entry.fetchPlotData(sessionUser(), tagIds, startDateStr ? parseDate(startDateStr) : null,
				endDateStr ? parseDate(endDateStr) : null, new Date(), timeZoneName)
		
		renderDataGet(new JSON(results))
	}

	def getSumPlotData() {
		def tags = JSON.parse(params.tags)
		def startDateStr = params.startDate
		def endDateStr = params.endDate
		def timeZoneName = params.timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate) : params.timeZoneName
		
		debug "DataController.getSumPlotData() params: " + params
		
		def tagIds = []
		for (tagStr in tags) {
			tagIds.add(Tag.look(tagStr).getId())
		}

		def results = Entry.fetchSumPlotData(sessionUser(), tagIds,
				startDateStr ? parseDate(startDateStr) : null, endDateStr ? parseDate(endDateStr) : null, new Date(), timeZoneName)
		
		renderDataGet(new JSON(results))
	}

	/*
	def correlateData() {
		debug("DataController.correlateData()")

		if (params.tags1 == null) {
			renderDataGet('Need to specify at least one set of tags to correlate with')
			return
		}
		
		def tags1 = params.tags1 == null ? [] : JSON.parse(params.tags1)

		def tags2 = params.tags2 == null ? [] : JSON.parseDate(params.tags2)
		
		if (tags1.length() == 0) {
			renderDataGet('Need to specify at least one set of tags to correlate with')
			return
		}
		
		def startDateStr = params.startDate
		def endDateStr = params.endDate

		debug "DataController.getSumData() params: " + params

		def data1 = findEntriesStartEnd(sessionUser(), tags1, startDateStr, endDateStr)
		
		def data2 = findEntriesStartEnd(sessionUser(), tags2, startDateStr, endDateStr)
	}*/

	def getTagsData() {
		debug("DataController.getTagsData() order:" + params.sort)
		
		renderJSONGet(Entry.getTags(sessionUser(), params.sort == 'freq' ? Entry.BYCOUNT : Entry.BYALPHA))
	}
	
	def addEntrySData() { // new API
		debug("DataController.addEntrySData() params:" + params)
		
		def userId = userFromIdStr(params.userId)
		
		if (userId == null) {
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		def result = doAddEntry(params.currentTime, params.timeZoneName, params.userId, params.text, params.baseDate,
				params.defaultToNow == '1' ? true : false)
		if (result[0] != null) {
			renderJSONGet([
				listEntries(userId, params.timeZoneName, params.baseDate),
				result[1],
				result[2].getJSONDesc(),
				result[0].getJSONDesc()
			])
		} else {
			renderStringGet('error')
		}
	}

	def updateEntrySData() { // new API
		debug("DataController.updateEntrySData() params:" + params)

		def (entry, message, oldTagStats, newTagStats) = doUpdateEntry(params.entryId, params.currentTime, params.text, params.baseDate, params.timeZoneName,
				params.defaultToNow == '1' ? true : false, (params.allFuture ?: '0') == '1' ? true : false)
		if (entry != null) {
			renderJSONGet([listEntries(userFromId(entry.getUserId()), params.timeZoneName, params.baseDate),
					oldTagStats?.getJSONDesc(), newTagStats?.getJSONDesc()])
		} else {
			debug "Error while updating: " + message
			renderStringGet(message)
		}
	}

	def deleteEntryData() { // old API
		debug "DataController.deleteEntryData() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		def entry = Entry.get(params.entryId.toLong());
		def userId = entry.getUserId();
		
		def currentTime = params.currentTime == null ? null : parseDate(params.currentTime)
		def baseDate = params.baseDate == null? null : parseDate(params.baseDate)
		
		if (entry.getUserId() != sessionUser().getId()) {
			renderStringGet('You do not have permission to delete this entry.')
		} else if (entry.fetchIsGenerated()) {
			renderStringGet('Cannot delete generated entries.')
		} else {
			TagStatsRecord record = new TagStatsRecord()
			Entry.delete(entry, record)
			renderJSONGet(listEntries(sessionUser(), TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate), params.displayDate))
		}
	}
	
	def deleteEntrySData() { // new API
		debug "DataController.deleteEntrySData() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		def entry = Entry.get(params.entryId.toLong());
		def userId = entry.getUserId();
		
		def currentTime = params.currentTime == null ? null : parseDate(params.currentTime)
		def baseDate = params.baseDate == null? null : parseDate(params.baseDate)
		def timeZoneName = params.timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate) : params.timeZoneName
		
		if (entry.getUserId() != sessionUser().getId()) {
			renderStringGet('You do not have permission to delete this entry.')
		} else if (entry.fetchIsGenerated()) {
			renderStringGet('Cannot delete generated entries.')
		} else {
			TagStatsRecord record = new TagStatsRecord()
			Entry.delete(entry, record)
			renderJSONGet([listEntries(sessionUser(), timeZoneName, params.displayDate),
				record.getOldTagStats()?.getJSONDesc(),
				record.getNewTagStats()?.getJSONDesc()])
		}
	}
	
	def setTagPropertiesData() {
		debug "DataController.setTagPropertiesData() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}
		
		if (!params.tags) {
			renderStringGet(SYNTAX_ERROR_MESSAGE)
			return
		}

		def tags = JSON.parse(params.tags)
		for (tagStr in tags) {
			Tag tag = Tag.look(tagStr)
			if (tag) {
				TagProperties tagProperties = TagProperties.createOrLookup(user.getId(), tag.getId())
				if (params.isContinuous != null)
					tagProperties.setIsContinuous(params.isContinuous.equals('true'))
				if (params.showPoints != null)
					tagProperties.setShowPoints(params.showPoints.equals('true'))
			}
		}
		
		renderStringGet('success')
	}

	def setPreferencesData() {
		debug "DataController.setPreferencesData() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		user.updatePreferences(params)

		Utils.save(user)

		renderDataGet(1)
	}

	protected def clearTags() {
		debug "DataController.clearTags()"
		
		session.tags = null
		session.tagsCache = null
	}
	
	protected def getTags(boolean byDate) {
		debug "DataController.getTags() byDate:" + byDate
		
		def user = sessionUser()
		if (user == null)
			return null
		if (session.tags != null) {
			if (++session.tagsStale < 20) {
				return session.tags
			}
		}
		session.tagsStale = 0
		def tags = Entry.getTags(sessionUser(), byDate ? Entry.BYRECENT : Entry.BYCOUNT)
		def descriptions = []
		for (tag in tags) {
			descriptions.add(tag[0])
		}
		session.tags = descriptions
		
		return session.tags
	}
	
	static String ALLAUTOTAGS = "**ALLTAGS**"
	static String ALLAUTOTAGSWITHINFO = "**ALLTAGSINFO**"
	
	protected def getAutocompleteTags(term) {
		debug "DataController.getAutocompleteTags() term " + term
		
		def user = sessionUser()
		if (user == null)
			return null
		
		if (term.equals(ALLAUTOTAGS)) {
			debug "ALLAUTOTAGS"
			return getSessionCache("AUTOTAGS*" + term, {
				return Entry.getAutocompleteTags(user)
			})
		}
		
		if (term.equals(ALLAUTOTAGSWITHINFO)) {
			debug "ALLAUTOTAGSWITHINFO"
			return Entry.getAutocompleteTagsWithInfo(user)
		}
		
		return getSessionCache("AUTOTAGS*" + term, {
			def tags = Entry.getAutocompleteTags(user)
			def algTags = tags['alg']
			def freqTags = tags['freq']
		
			def data = []
			
			def num = 0
			
			for (tag in algTags) {
				if (tag.indexOf(term) >= 0) {
					if (++num > 3) break
					debug "Adding " + tag
					data.add(tag)
				}
			}
			
			num = 0

			for (tag in freqTags) {
				if (tag.indexOf(term) >= 0 && (!data.contains(tag))) {
					if (++num > 3) break
					debug "Adding " + tag
					data.add(tag)
				}
			}

			return data
		})
	}
	
	def autocompleteData() {
		debug "DataController.autocompleteData() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		if (params.all != null) {
			if (params.all.equals('info')) {
				debug "Get all autocomplete tags by algorithm with units and last amount info"
				renderJSONGet(getAutocompleteTags(ALLAUTOTAGSWITHINFO))
			} else {
				debug "Get all autocomplete tags by algorithm"
				renderJSONGet(getAutocompleteTags(ALLAUTOTAGS))
			}
		} else {
			// this is using an outdated algorithm; currently this is computed in the browser
			debug "Autocomplete suggestions for " + params.partial
			renderJSONGet(getAutocompleteTags(params.partial))
		}
	}
	
	def listPlotData() {
		debug "DataController.listPlotData() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to load list of dynamic plots " + user.getId()

		def c = PlotData.createCriteria()

		def entries = c {
			eq("userId", user.getId())
			not {
				eq("isSnapshot", true)
			}
			order("created", "asc")
		}
		
		for (entry in entries) {
			debug "Found " + entry
		}

		renderJSONGet(Utils.listJSONDesc(entries))
	}

	def savePlotData() {
		debug "DataController.savePlotData() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		try {
			def plotDataObj = PlotData.createOrReplace(user, params.name, params.plotData, false)
			
			Utils.save(plotDataObj, true)
			
			renderJSONPost(['success', plotDataObj.getId()])
		} catch (Exception e) {
			renderJSONPost(['error'])
		}
	}
	
	def loadPlotDataId() {
		debug "DataController.loadPlotDataId() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to load plot data " + params.id

		def plotData = PlotData.get(Long.valueOf(params.id))
		
		if (plotData == null) {
			renderStringGet('No such graph id ' + params.id)
			return;
		}
		
		debug "PlotData: " + plotData.getJsonPlotData()
		
		if (!plotData.getIsDynamic()) {
			renderStringGet('Not a live graph')
			return;
		}
		
		renderDataGet(plotData.getJsonPlotData())
	}
	
	def deletePlotDataId() {
		debug "DataController.deletePlotDataId() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to delete plot data " + params.id

		def plotData = PlotData.get(Long.valueOf(params.id))
		
		if (plotData == null) {
			renderStringGet('No such graph id ' + params.id)
			return;
		}
		
		if (!plotData.getIsDynamic()) {
			renderStringGet('Not a live graph')
			return;
		}
		
		try {
			PlotData.delete(plotData)
			
			renderStringGet('success')
		} catch (Exception e) {
			renderStringGet('error')
		}
	}
	
	def listSnapshotData() {
		debug "DataController.listSnapshotData() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to load list of snapshots for " + user.getId()

		def c = PlotData.createCriteria()

		def entries = c {
			eq("userId", user.getId())
			eq("isSnapshot", true)
			order("created", "asc")
		}
		
		for (entry in entries) {
			debug "Found " + entry
		}

		renderJSONGet(Utils.listJSONDesc(entries))
	}

	def listDiscussionData() {
		debug "DataController.listDiscussionData() params:" + params
		
		def user = sessionUser()
		
		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		def groupNameList = params.userGroupNames ? JSON.parse(params.userGroupNames) : null
		
		debug "Trying to load list of discussions for " + user.getId() + " and list:" + params.userGroupNames

		def entries = groupNameList ? UserGroup.getDiscussionsInfoForGroupNameList(user, groupNameList) : \
				UserGroup.getDiscussionsInfoForUser(user, true)
		
		for (entry in entries) {
			debug "Found " + entry
		}

		renderJSONGet(entries)
	}

	def saveSnapshotData() {
		debug "DataController.saveSnapshotData() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}
		
		debug "Saving " + params.snapshotData
		
		def plotDataObj = PlotData.createOrReplace(user, params.name, params.snapshotData, true)
		
		Utils.save(plotDataObj, true)
		
		renderJSONPost([plotDataId:plotDataObj.getId()])
	}
	
	def loadSnapshotDataId() {
		debug "DataController.loadSnapshotDataId() params:" + params
		
		Long plotDataId = Long.valueOf(params.id)
		
		def user = sessionUser()

		if (user == null) {
			Discussion discussion = Discussion.getDiscussionForPlotDataId(plotDataId)
			if (!discussion.getIsPublic()) {
				debug "auth failure"
				renderStringGet(AUTH_ERROR_MESSAGE)
				return
			}
		}

		debug "Trying to load plot data " + params.id

		def plotData = PlotData.get(plotDataId)
		
		if (plotData == null) {
			renderStringGet('No such graph id ' + params.id)
			return;
		}
		
		debug "PlotData: " + plotData.getJsonPlotData()
		
		if (!plotData.getIsSnapshot()) {
			renderStringGet('Graph is not a snapshot')
			return;
		}
		
		renderDataGet(plotData.getJsonPlotData())
	}

	def deleteSnapshotDataId() {
		debug "DataController.deleteSnapshotDataId() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to delete snapshot data " + params.id

		def plotData = PlotData.get(Long.valueOf(params.id))
		
		if (plotData == null) {
			renderStringGet('No such graph id ' + params.id)
			return;
		}
		
		if (!plotData.getIsSnapshot()) {
			renderStringGet('Graph is not a snapshot')
			return;
		}
		
		PlotData.delete(plotData)
		
		renderStringGet('success')
	}
	
	def setDiscussionNameData() {
		debug "DataController.setDiscussionNameData() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to set discussion name " + params.name

		def discussion = Discussion.get(Long.valueOf(params.discussionId))
		
		if (discussion == null) {
			renderStringGet('No such discussion id ' + params.discussionId)
			return;
		}
		
		if (Discussion.update(discussion, params, user)) {
			renderStringGet('success')
		} else {
			renderStringGet('Failed to update discussion name')
		}
	}

	def deleteDiscussionId() {
		debug "DataController.deleteDiscussionId() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to delete discussion data " + params.id

		def discussion = Discussion.get(Long.valueOf(params.id))
		
		if (discussion == null) {
			renderStringGet('No such discussion id ' + params.id)
			return;
		}
		
		Discussion.delete(discussion)
		
		renderStringGet('success')
	}
}
