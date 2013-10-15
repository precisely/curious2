package us.wearecurio.controller

import grails.converters.*
import us.wearecurio.model.*
import us.wearecurio.exceptions.*
import us.wearecurio.services.CorrelationService
import us.wearecurio.utility.Utils
import us.wearecurio.model.Discussion;
import us.wearecurio.model.Entry.RepeatType;
import us.wearecurio.model.Entry.TagStatsRecord

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.SimpleTimeZone
import java.util.TimeZone

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

	protected def doAddEntry(currentTimeStr, userIdStr, textStr, baseDateStr, timeZoneOffsetStr, defaultToNow) {
		debug "DataController.doAddEntry() currentTimeStr:" + currentTimeStr + ", userIdStr:" + userIdStr \
				+ ", baseDateStr:" + baseDateStr + ", timeZoneOffsetStr:" + timeZoneOffsetStr + ", defaultToNow:" + defaultToNow
		
		def user = userFromIdStr(userIdStr)

		TimeZone tz = Utils.createTimeZone(-Integer.parseInt(timeZoneOffsetStr), "GMTOFFSET" + timeZoneOffsetStr)

		def currentTime = parseDate(currentTimeStr)
		def baseDate = parseDate(baseDateStr)

		debug("Current time " + currentTime + " baseDate " + baseDate);

		def parsedEntry = Entry.parse(currentTime, tz, textStr, baseDate, defaultToNow)
		TagStatsRecord record = new TagStatsRecord()
		def entry = Entry.create(user.getId(), parsedEntry, record)

		debug("created " + entry)

		return [entry, parsedEntry['status'], record.getOldTagStats()]
	}

	protected def doUpdateEntry(entryIdStr, currentTimeStr, textStr, baseDateStr, timeZoneOffsetStr, defaultToNow) {
		debug "DataController.doUpdateEntry() entryIdStr:" + entryIdStr + ", currentTimeStr:" + currentTimeStr \
				+ ", textStr:" + textStr + ", baseDateStr:" + baseDateStr + ", timeZoneOffsetStr:" + timeZoneOffsetStr \
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
		
		TimeZone tz = Utils.createTimeZone(-Integer.parseInt(timeZoneOffsetStr),"GMTOFFSET" + timeZoneOffsetStr)

		def currentTime = parseDate(currentTimeStr)
		def baseDate = parseDate(baseDateStr)

		def m = Entry.parse(currentTime, tz, textStr, baseDate, false)

		if (entry != null) {
			TagStatsRecord record = new TagStatsRecord()
			Entry.update(entry, m, record)
			return [entry, '', record.getOldTagStats(), record.getNewTagStats()];
		} else {
			debug "Parse error"
			return [null, 'Cannot interpret entry text.', null, null];
		}
	}

	// find entries including those with null events
	protected def listEntries(User user, String dateStr) {
		debug "DataController.listEntries() userId:" + user.getId() + ", dateStr:" + dateStr
		
		return Entry.fetchListData(user, parseDate(dateStr), new Date())
	}
	
	protected def doParseCSVAcross(InputStream csvIn, Long userId) {
		debug "DataController.doParseCSVAcross() userId:" + userId
		
		Reader reader = new InputStreamReader(csvIn)

		int lineNum = 0

		def timeZoneStr = ""
		def setName = "import"
		TimeZone timeZone

		List<Date> dateList = new ArrayList<Date>()

		reader.eachCsvLine { tokens ->
			++lineNum
			if (lineNum == 1) {
				setName = tokens[0]
				timeZoneStr = tokens[1]
				def timeZoneOffset = Integer.parseInt(tokens[2])
				debug "setName: " + setName + " timeZone: " + timeZoneStr + " timeZoneOffset: " + timeZoneOffset
				timeZone = Utils.createTimeZone(timeZoneOffset, timeZoneStr)
				// delete previous import of same data set
				Entry.executeUpdate("delete Entry e where e.setName = :setName and e.userId = :userId",
						[setName: setName, userId: userId])
			} else if (lineNum == 2) {
				for (int i = 0; i < tokens.length; ++i) {
					def date = Date.parse("MM/dd/yy z", tokens[i] + " " + timeZoneStr);
					debug "Date: " + Utils.dateToGMTString(date) 
					dateList.add(date)
				}
			} else {
				for (int i = 0; i < tokens.length; ++i) {
					if (tokens[i].length() > 0) {
						def parsedEntry = Entry.parse(now, timeZone, tokens[i], dateList.get(i), false)
						parsedEntry['setName'] = setName
						def entry = Entry.create(userId, parsedEntry, null)

						debug("created " + entry)
					}
				}
			}
		}
	}

	protected def doParseCSVDown(InputStream csvIn, Long userId) {
		debug "DataController.doParseCSVDown() userId:" + userId
		
		Reader reader = new InputStreamReader(csvIn)

		int lineNum = 0

		String timeZoneStr = ""
		String setName = "import"
		TimeZone timeZone
		Integer timeZoneOffsetSecs = 0
		Date currentDate = null
		boolean analysisType = false
		DateFormat dateFormat
		Date now = new Date()

		reader.eachCsvLine { tokens ->
			++lineNum
			if (lineNum == 1) {
				setName = tokens[0]
				if (setName.startsWith("Date (GMT) for ")) {
					analysisType = true
					setName = "Imported for " + setName.substring(15)
					debug "setName: " + setName + " analysis type"
					timeZone = Utils.createTimeZone(0, "GMT")
					timeZoneOffsetSecs = 0
					dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
					dateFormat.setTimeZone(timeZone)
				} else {
					timeZoneStr = tokens[1]
					def timeZoneOffset = Integer.parseInt(tokens[2])
					debug "setName: " + setName + " timeZone: " + timeZoneStr + " timeZoneOffset: " + timeZoneOffset
					timeZone = Utils.createTimeZone(timeZoneOffset, timeZoneStr)
					dateFormat = new SimpleDateFormat("MM/dd/yy z")
					dateFormat.setTimeZone(timeZone)
				}
				// delete previous import of same data set
				Entry.executeUpdate("delete Entry e where e.setName = :setName and e.userId = :userId",
						[setName: setName, userId: userId])
			} else {
				if (analysisType) {
					if (tokens[0]) {
						currentDate = dateFormat.parse(tokens[0])
						timeZoneOffsetSecs = timeZone == null ? 0 : timeZone.getOffset(currentDate.getTime()) / 1000
						debug "Date: " + currentDate
					}
					if (currentDate) {
						Long repeatTypeId = Long.valueOf(tokens[5])
						def parsedEntry = [ \
							userId:userId, \
							tweetId:null, \
							date:currentDate, \
							timeZoneOffsetSecs:timeZoneOffsetSecs, \
							description:tokens[1], \
							amount:tokens[2], \
							units:tokens[3], \
							comment:tokens[4], \
							repeatType:repeatTypeId >= 0 ? Entry.RepeatType.get(repeatTypeId) : null, \
							setName:setName, \
							amountPrecision:Long.valueOf(tokens[6].equals("null") ? '3' : tokens[6]), \
							datePrecisionSecs:(tokens[7].equals("null") ? '180':tokens[7]), \
						]
						def entry = Entry.create(userId, parsedEntry, null)
	
						debug("created " + entry)
					}
				} else {
					if (tokens[0]) {
						currentDate = dateFormat.parse(tokens[0] + " " + timeZoneStr)
						debug "Date: " + currentDate
					}
					if (currentDate) {
						def parsedEntry = Entry.parse(now, timeZone, tokens[1], currentDate, false)
						parsedEntry['setName'] = setName
						def entry = Entry.create(userId, parsedEntry, null)
	
						debug("created " + entry)
					}
				}
			}
		}
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
	
	protected def doExportCSVHuman(OutputStream out, User user) {
		debug "DataController.doExportCSV() + userId:" + user.getId()
		
		Writer writer = new OutputStreamWriter(out)

		writeCSV(writer,"export_user" + user.getUsername())
		writer.write(",")
		writeCSV(writer,"GMT")
		writer.write(",")
		writeCSV(writer,"0")
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
		
		TimeZone timeZone = Utils.createTimeZone(0, "GMT")
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy z")
		dateFormat.setTimeZone(timeZone)
		
		for (entry in results) {
			writeCSV(writer, dateFormat.format(entry.getDate()))
			writer.write(",")
			writeCSV(writer, entry.exportString())
			writer.write("\n")
		}
		
		writer.flush()
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
		
		TimeZone timeZone = Utils.createTimeZone(0, "GMT")
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
		
		def currentTime = params.date == null ? null : parseDate(params.date)
		
		if (entry.getUserId() != sessionUser().getId()) {
			renderStringGet('You do not have permission to activate this entry.')
		}
		Entry newEntry = entry.activateGhostEntry(currentTime, new Date()) // TODO: now should probably come from client, not server; fix later
		if (newEntry != null) {
			renderJSONGet(newEntry.getJSONDesc())
		} else
			renderJSONGet("Failed to activate entry due to internal server error.")
	}

	def deleteGhostEntry(Long entryId) {
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		def entry = Entry.get(entryId)
		def userId = entry.getUserId();
		
		def currentDate = params.date == null ? null : parseDate(params.date)
		
		if (entry.getUserId() != sessionUser().getId()) {
			renderStringGet('You do not have permission to delete this entry.')
		} else
			Entry.deleteGhost(entry, currentDate)
		
		renderStringGet("success")
	}

	def getListData() {
		debug "DataController.getListData() userId:" + params.userId + " date: " + params.date
		
		def user = userFromIdStr(params.userId);
		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		def entries = Entry.fetchListData(user, parseDate(params.date), new Date())
		
		// skip continuous repeat entries with entries within the usage threshold

		renderJSONGet(entries)
	}

	def getPlotData() {
		def tags = JSON.parse(params.tags)
		def startDateStr = params.startDate
		def endDateStr = params.endDate
		
		debug "DataController.getPlotData() tags: " + tags + " startDate: " + startDateStr + " endDate: " + endDateStr

		def tagIds = []
		
		for (tagStr in tags) {
			tagIds.add(Tag.look(tagStr).getId())
		}

		def results = Entry.fetchPlotData(sessionUser(), tagIds, startDateStr ? parseDate(startDateStr) : null,
				endDateStr ? parseDate(endDateStr) : null)
		
		renderDataGet(new JSON(results))
	}

	def getSumPlotData() {
		def tags = JSON.parse(params.tags)
		def startDateStr = params.startDate
		def endDateStr = params.endDate

		debug "DataController.getSumPlotData() params: " + params
		
		def tagIds = []
		for (tagStr in tags) {
			tagIds.add(Tag.look(tagStr).getId())
		}

		def results = Entry.fetchSumPlotData((params.sumNights?.equals("true"))? true: false, sessionUser(), tagIds,
				startDateStr ? parseDate(startDateStr) : null, endDateStr ? parseDate(endDateStr) : null, -Integer.parseInt(params.timeZoneOffset))
		
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
	
	def addEntryData() { // old API
		debug("DataController.addEntryData() params:" + params)
		
		def userId = userFromIdStr(params.userId)
		
		if (userId == null) {
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		def result = doAddEntry(params.currentTime, params.userId, params.text, params.baseDate, params.timeZoneOffset,
				params.defaultToNow == '1' ? true : false)
		if (result[0] != null) {
			renderJSONGet([
				listEntries(userId, params.baseDate),
				result[1],
				result[0].getDescription(),
				result[2].getJSONShortDesc()
			])
		} else {
			renderStringGet('error')
		}
	}

	def addEntrySData() { // new API
		debug("DataController.addEntrySData() params:" + params)
		
		def userId = userFromIdStr(params.userId)
		
		if (userId == null) {
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		def result = doAddEntry(params.currentTime, params.userId, params.text, params.baseDate, params.timeZoneOffset,
				params.defaultToNow == '1' ? true : false)
		if (result[0] != null) {
			renderJSONGet([
				listEntries(userId, params.baseDate),
				result[1],
				result[2].getJSONDesc(),
				result[0].getJSONDesc()
			])
		} else {
			renderStringGet('error')
		}
	}

	def updateEntryData() { // old API
		debug("DataController.updateEntryData() displayDate: " + params.displayDate)

		def (entry, message, oldTagStats, newTagStats) = doUpdateEntry(params.entryId, params.currentTime, params.text, params.baseDate, params.timeZoneOffset,
				params.defaultToNow == '1' ? true : false)
		if (entry != null) {
			renderJSONGet(listEntries(userFromId(entry.getUserId()), params.baseDate))
		} else {
			debug "Error while updating: " + message
			renderStringGet(message)
		}
	}

	def updateEntrySData() { // new API
		debug("DataController.updateEntrySData() displayDate: " + params.displayDate)

		def (entry, message, oldTagStats, newTagStats) = doUpdateEntry(params.entryId, params.currentTime, params.text, params.baseDate, params.timeZoneOffset,
				params.defaultToNow == '1' ? true : false)
		if (entry != null) {
			renderJSONGet([listEntries(userFromId(entry.getUserId()), params.baseDate),
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
			renderJSONGet(listEntries(sessionUser(), params.displayDate))
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
		
		if (entry.getUserId() != sessionUser().getId()) {
			renderStringGet('You do not have permission to delete this entry.')
		} else if (entry.fetchIsGenerated()) {
			renderStringGet('Cannot delete generated entries.')
		} else {
			TagStatsRecord record = new TagStatsRecord()
			Entry.delete(entry, record)
			renderJSONGet([listEntries(sessionUser(), params.displayDate),
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
