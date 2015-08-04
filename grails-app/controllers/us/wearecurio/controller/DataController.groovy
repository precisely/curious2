package us.wearecurio.controller

import grails.converters.JSON

import java.math.MathContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

import org.grails.databinding.SimpleMapDataBindingSource
import org.grails.plugins.elasticsearch.ElasticSearchService
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.springframework.http.HttpStatus

import static org.springframework.http.HttpStatus.*
import us.wearecurio.model.*
import us.wearecurio.services.EntryParserService
import us.wearecurio.services.EntryParserService.ParseAmount
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats
import us.wearecurio.utility.Utils

class DataController extends LoginController {

	def tokenService
	EntryParserService entryParserService
	
	static debug(str) {
		log.debug(str)
	}

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

		def parsedEntry = entryParserService.parse(currentTime, timeZoneName, textStr, baseDate, defaultToNow)
		EntryStats stats = new EntryStats()
		def entry = Entry.create(user.getId(), parsedEntry, stats)
		ArrayList<TagStats> tagStats = stats.finish()

		debug("created " + entry)

		return [entry, parsedEntry['status'], tagStats.get(0)]
	}

	protected def doUpdateEntry(entryIdStr, currentTimeStr, textStr, baseDateStr, timeZoneName, defaultToNow, allFuture) {
		debug "DataController.doUpdateEntry() entryIdStr:" + entryIdStr + ", currentTimeStr:" + currentTimeStr \
				+ ", textStr:" + textStr + ", baseDateStr:" + baseDateStr + ", timeZoneName:" + timeZoneName \
				+ ", defaultToNow:" + defaultToNow

		def entry = Entry.get(Long.parseLong(entryIdStr))

		def oldEntry = entry

		if (entry.getUserId() == 0L) {
			debug "Attempting to edit a deleted entry."
			return [null, 'Attempting to edit a deleted entry.', null, null]
		}

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

		EntryStats stats = new EntryStats(entry.getUserId())
		
		// activate repeat entry if it has a repeat type
		if (entry.getRepeatType() != null && entry.getRepeatType().isContinuous()) {
			debug "Activating continuous entry " + entry
			Entry newEntry = entry.activateContinuousEntry(baseDate, currentTime, timeZoneName, stats)
			if (newEntry != null) {
				entry = newEntry
				debug "New activated entry " + newEntry
			} else
				debug "No entry activation"
		}

		def m = entryParserService.parse(currentTime, timeZoneName, textStr, baseDate, false, true)

		if (entry != null) {
			entry = entry.update(m, stats, baseDate, allFuture)
			def tagStats = stats.finish()
			return [entry, '', tagStats[0], tagStats.size() > 0 ? tagStats[1] : null];
		} else {
			debug "Parse error"
			stats.finish()
			return [null, 'Cannot interpret entry text.', null, null];
		}
	}

	// find entries including those with null events
	protected def listEntries(User user, String timeZoneName, Date baseDate, Date currentTime) {
		debug "DataController.listEntries() userId:" + user.getId() + ", timeZoneName:" + timeZoneName + ", baseDate:" + baseDate

		timeZoneName = timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate) : timeZoneName

		return Entry.fetchListData(user, timeZoneName, baseDate, currentTime)
	}

	MathContext mc = new MathContext(9)

	protected def doParseCSVDown(InputStream csvIn, Long userId) {
		debug "DataController.doParseCSVDown() userId:" + userId

		Reader reader = new InputStreamReader(csvIn)

		int lineNum = 0

		String setName
		String timeZoneName
		Date currentDate = null
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss z").withZone(DateTimeZone.UTC)
		Date now = new Date()
		
		EntryStats stats = new EntryStats(userId)

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
				Entry.executeUpdate("update Entry e set e.userId = null where e.setIdentifier = :setIdentifier and e.userId = :userId",
						[setIdentifier: Identifier.look(setName), userId: userId])
			} else {
				if (tokens[0]) {
					currentDate = dateTimeFormatter.parseDateTime(tokens[0]).toDate()
					debug "Date: " + currentDate
				}
				if (currentDate) {
					Long repeatTypeId = Long.valueOf(tokens[5])
					def parsedEntry = [ \
						userId:userId, \
						date:currentDate, \
						timeZoneName:tokens[8], \
						tag:Tag.look(tokens[1]), \
						baseTag:Tag.look(tokens.length > 9 ? tokens[9] : tokens[1]), \
						amount:new BigDecimal(tokens[2], mc), \
						units:tokens[3], \
						comment:tokens[4], \
						repeatType:repeatTypeId >= 0 ? RepeatType.look((long)repeatTypeId) : null, \
						setName:setName, \
						amountPrecision:Integer.valueOf(tokens[6].equals("null") ? '3' : tokens[6]), \
						datePrecisionSecs:Integer.valueOf(tokens[7].equals("null") ? '180':tokens[7]), \
					]
					def entry = Entry.createSingle(userId, parsedEntry, null, stats)

					debug("created " + entry)
				}
			}
		}
		
		stats.finish()

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
		writer.write(",")
		writeCSV(writer,"Base Tag")
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
			writer.write(",")
			if (entry.getBaseTag())
				writeCSV(writer, entry.getBaseTag().getDescription())
			else
				writeCSV(writer, entry.getTag().getDescription())
			
			writer.write("\n")
		}

		writer.flush()
	}

	def getPeopleData() {
		debug "DataController.getPeopleData"

		def user = sessionUser()

		if (user == null) {
			debug "auth failure - return empty array"
			renderStringGet([])
			return
		}

		debug "user:" + user

		renderJSONGet([user.getJSONDesc()])
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
	
	// legacy support for obsolete call
	def deleteGhostEntry() {
		deleteGhostEntryData()
	}
	
	def deleteGhostEntryData() {
		debug "DataController.deleteGhostEntryData() params:" + params

		def user = sessionUser()
		def entry = Entry.get(Long.parseLong(params.entryId))

		Map result = Entry.canDelete(entry, user)
		
		if (result.canDelete) {
			renderJSONGet(deleteGhostEntryHelper(params))
		} else {
			renderStringGet(g.message(code: result.messageCode))
			return
		}
	}
	
	private def deleteGhostEntryHelper(Map params) {
		def entry = Entry.get(Long.parseLong(params.entryId))
		def allFuture = params.all?.equals("true") ? true : false

		Date baseDate = parseDate(params.baseDate)
		Date currentTime = parseDate(params.currentTime ?: params.date) ?: new Date()
		def timeZoneName = params.timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate) : params.timeZoneName

		EntryStats stats = new EntryStats()
		Entry.deleteGhost(entry, stats, currentTime, allFuture)
		def tagStats = stats.finish()
		return [listEntries(sessionUser(), timeZoneName, baseDate, currentTime),
			tagStats[0]?.getJSONDesc(),
			tagStats.size() > 1 ? tagStats[1].getJSONDesc() : null]
	}


	def getListData() {
		debug "DataController.getListData() userId:" + params.userId + " date: " + params.date + " timeZoneName:" + params.timeZoneName

		def user = userFromIdStr(params.userId)
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
	
	def getInterestTagsData() {
		debug "DataController.getInterestTagsData() userId:" + params.userId

		User user = sessionUser()
		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		def tags = user.fetchInterestTagsJSON()

		debug "Interest tags:" + tags

		renderJSONGet([interestTags:tags])
	}
	
	def addInterestTagData() {
		debug "DataController.addInterestTagData() userId:" + params.userId + ", tagName: " + params.tagName

		User user = sessionUser()
		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}
		
		if (!params.tagName) {
			debug "no tag name specified"
			renderStringGet("No tag name specified")
			return
		}

		user.addInterestTag(Tag.look(params.tagName.toLowerCase()))
		
		if (Utils.save(user, true))  {
			debug "Successfully added tag"
			renderJSONGet([interestTags:user.fetchInterestTagsJSON()])
		} else {
			debug "Failure adding tag"
			renderStringGet("Error adding interest tag")
		}
	}
	
	def updateInterestTagData() {
		debug "DataController.updateInterestTagData() userId:" + params.userId + ", oldTagName: " + params.oldTagName + ", newTagName: " + params.newTagName

		User user = sessionUser()
		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}
		
		if ((!params.oldTagName) || (!params.newTagName)) {
			debug "no tag name specified"
			renderStringGet("No tag name specified")
			return
		}
		
		Tag oldTag = Tag.findByDescription(params.oldTagName.toLowerCase())
		Tag newTag = Tag.look(params.newTagName.toLowerCase())
		
		if ((!oldTag) || (!newTag)) {
			debug "tag not found"
			renderStringGet("Internal error: tag not found")
			return

		}

		user.deleteInterestTag(oldTag)
		user.addInterestTag(newTag)
		
		if (Utils.save(user, true))  {
			debug "Successfully updated tag"
			renderJSONGet([interestTags:user.fetchInterestTagsJSON()])
		} else {
			debug "Failure removing tag"
			renderStringGet("Error adding interest tag")
		}
	}
	
	def deleteInterestTagData() {
		debug "DataController.deleteInterestTagData() userId:" + params.userId + ", tagName: " + params.tagName
 
		User user = sessionUser()
		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}
		
		if (!params.tagName) {
			debug "no tag name specified"
			renderStringGet("No tag name specified")
			return
		}
		
		Tag tag = Tag.findByDescription(params.tagName.toLowerCase())
		
		if (!tag) {
			debug "tag not found"
			renderStringGet("Internal error: tag not found")
			return

		}

		user.deleteInterestTag(tag)
		
		if (Utils.save(user, true))  {
			debug "Successfully removed tag"
			renderJSONGet([interestTags:user.fetchInterestTagsJSON()])
		} else {
			debug "Failure removing tag"
			renderStringGet("Error adding interest tag")
		}
	}
	
	protected def fetchPlotEntries() {
		def tags = JSON.parse(params.tags)
		def startDateStr = params.startDate
		def endDateStr = params.endDate

		def tagIds = []
		for (tagStr in tags) {
			tagIds.add(Tag.look(tagStr).getId())
		}

		def plotInfo = [:]
		
		def plotEntries = Entry.fetchPlotData(sessionUser(), tagIds, startDateStr ? parseDate(startDateStr) : null,
			endDateStr ? parseDate(endDateStr) : null, new Date(), params.timeZoneName, plotInfo)
		
		return [entries:plotEntries, tagIds:tagIds, unitGroupId:plotInfo.unitGroupId,
				valueScale:plotInfo.valueScale]
	}

	def getPlotData() {
		debug "DataController.getPlotData() params:" + params

		renderDataGet(new JSON(fetchPlotEntries().entries))
	}

	def getPlotDescData() {
		debug "DataController.getPlotDescData() params:" + params

		User user = sessionUser()
		
		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}
		
		def results = fetchPlotEntries()
		
		def minMax = Entry.fetchTagIdsMinMax(user.getId(), results.tagIds)
		
		renderDataGet(new JSON([entries:results.entries, min:minMax[0], max:minMax[1],
				unitGroupId:results.unitGroupId, valueScale:results.valueScale]))
	}
	
	protected def fetchSumPlotEntries() {
		def tags = JSON.parse(params.tags)
		def startDateStr = params.startDate
		def endDateStr = params.endDate

		def tagIds = []
		for (tagStr in tags) {
			tagIds.add(Tag.look(tagStr).getId())
		}

		def plotInfo = [:]
		
		def entries = Entry.fetchSumPlotData(sessionUser(), tagIds,
			startDateStr ? parseDate(startDateStr) : null, endDateStr ? parseDate(endDateStr) : null, new Date(), params.timeZoneName, plotInfo)
		
		return [entries:entries, tagIds:tagIds, unitGroupId:plotInfo.unitGroupId,
				valueScale:plotInfo.valueScale]
	}

	def getSumPlotData() {
		debug "DataController.getSumPlotData() params:" + params
		
		renderDataGet(new JSON(fetchSumPlotEntries().entries))
	}

	def getSumPlotDescData() {
		debug "DataController.getSumPlotDescData() params:" + params
		
		User user = sessionUser()
		
		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}
		
		def results = fetchSumPlotEntries()
		
		def minMax = Entry.fetchTagIdsMinMax(user.getId(), results.tagIds)
		
		renderDataGet(new JSON([entries:results.entries, min:minMax[0], max:minMax[1], unitGroupId:results.unitGroupId,
				valueScale:results.valueScale]))
	}

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

		Date baseDate = parseDate(params.baseDate)
		Date currentTime = parseDate(params.currentTime ?: params.date) ?: new Date()
		
		def result = doAddEntry(params.currentTime, params.timeZoneName, params.userId, params.text, params.baseDate,
				params.defaultToNow == '1' ? true : false)
		if (result[0] != null) {
			renderJSONGet([
				listEntries(userId, params.timeZoneName, baseDate, currentTime),
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

		Date baseDate = parseDate(params.baseDate)
		Date currentTime = parseDate(params.currentTime ?: params.date) ?: new Date()
		
		def (entry, message, oldTagStats, newTagStats) = doUpdateEntry(params.entryId, params.currentTime, params.text, params.baseDate, params.timeZoneName,
				params.defaultToNow == '1' ? true : false, (params.allFuture ?: '0') == '1' ? true : false)
		if (entry != null) {
			renderJSONGet([listEntries(userFromId(entry.getUserId()), params.timeZoneName, baseDate, currentTime),
					oldTagStats?.getJSONDesc(), newTagStats?.getJSONDesc()])
		} else {
			debug "Error while updating: " + message
			renderStringGet(message)
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

		def currentTime = params.currentTime == null ? new Date() : parseDate(params.currentTime)
		def baseDate = params.baseDate == null? null : parseDate(params.baseDate)
		def timeZoneName = params.timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate) : params.timeZoneName

		if (entry.getUserId() != sessionUser().getId()) {
			renderStringGet('You do not have permission to delete this entry.')
		} else if (entry.fetchIsGenerated()) {
			renderStringGet('Cannot delete generated entries.')
		} else {
			EntryStats stats = new EntryStats()
			Entry.delete(entry, stats)
			def tagStats = stats.finish()
			renderJSONGet([listEntries(sessionUser(), timeZoneName, parseDate(params.displayDate), currentTime),
				tagStats[0]?.getJSONDesc(),
				tagStats.size() > 1 ? tagStats[1].getJSONDesc() : null])
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
				tagProperties.setIsContinuous(params.isContinuous.equals('true'))
				if (params.showPoints != null)
					tagProperties.setShowPoints(params.showPoints.equals('true'))
				Utils.save(tagProperties, true)
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
	
	ElasticSearchService elasticSearchService

	def listDiscussionData() {
		debug "DataController.listDiscussionData() params:" + params

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		params.max = params.max ?: 10
		params.offset = params.offset ?: 0

		List groupNameList = params.userGroupNames ? params.list("userGroupNames") : []
		debug "Trying to load list of discussions for  $user.id and list:" + params.userGroupNames

		Map discussionData = groupNameList ? UserGroup.getDiscussionsInfoForGroupNameList(user, groupNameList, params) :
				UserGroup.getDiscussionsInfoForUser(user, true, false, params)

		debug "Found $discussionData"

		renderJSONGet(discussionData)
	}

	def listCommentData(String discussionHash) {
		debug "DataController.listCommentData() params: $params"

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		if (!discussionHash) {
			renderStringGet("Blank discussion call")
			return
		}
		Discussion discussion = discussionHash ? Discussion.findByHash(discussionHash) : null

		if (!discussion) {
			debug "Discussion not found for hash [$discussionHash]."
			renderStringGet "That discussion topic no longer exists."
			return
		}

		params.max = params.max ?: 5
		params.offset = params.offset ?: 0

		Map model = discussion.getJSONModel(params)
		model.posts = model.posts*.getJSONDesc()
		model.putAll([isAdmin: UserGroup.canAdminDiscussion(user, discussion)])

		debug "Found Comment data: $model"

		renderJSONGet(model)
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

		def discussion = Discussion.findByHash(params.discussionHash)

		if (discussion == null) {
			renderStringGet('No such discussion hash ' + params.discussionHash)
			return;
		}

		if (Discussion.update(discussion, params, user)) {
			renderStringGet('success')
		} else {
			renderStringGet('Failed to update discussion name')
		}
	}

	def deleteDiscussionHash() {
		debug "DataController.deleteDiscussionHash() params:" + params

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to delete discussion data " + params.hash

		def discussion = Discussion.findByHash(params.hash)

		if (discussion == null) {
			renderStringGet('No such discussion hash ' + params.hash)
			return;
		}

		Discussion.delete(discussion)

		renderStringGet('success')
	}
	
	def createDiscussionData(Long plotDataId, String name, Long id, String discussionPost) {
		def user = sessionUser()
		UserGroup group = Discussion.loadGroup(params.group, user)

		debug "DiscussionController.create to group: " + group?.dump()
		if (group) {
			Discussion discussion = Discussion.loadDiscussion(id, plotDataId, user)
			discussion = discussion ?: Discussion.create(user, name, group, null)

			if (discussion != null) {
				Utils.save(discussion, true)
				discussion.createPost(user, discussionPost)
				renderStringGet('success')
			} else {
				renderStringGet('fail')
			}
		}
	}
	
	def deleteCommentData(String discussionHash, Long clearPostId) {
		def user = sessionUser()
		Discussion discussion
		if (discussionHash && clearPostId) {
			discussion = Discussion.findByHash(discussionHash)
			DiscussionPost.deleteComment(clearPostId, user, discussion)
			renderStringGet('success')
		} else {
			renderStringGet('fail')
		}
	}

	def createCommentData(String discussionHash, String message, Long plotIdMessage) {
		debug "Attemping to add comment '" + message + "', plotIdMessage: " + plotIdMessage
		def user = sessionUser()
		Discussion discussion = Discussion.findByHash(discussionHash)
		if (discussion) {
			def result = DiscussionPost.createComment(message, user, discussion, plotIdMessage, params)
			if (result && !(result instanceof String)) {
				renderStringGet('success')
			} else {
				renderStringGet('fail')
			}
		} else {
			renderStringGet('fail')
		}
	}

	def createHelpEntriesData() {
		log.debug "Entries recieved to save: $params and entries: ${params['entries[]']}"

		if (session.showHelp) {
			session.showHelp = null
		}

		List entries = []
		if (params['entries[]'] instanceof String) {
			entries.push(params['entries[]'])
		} else {
			entries = params['entries[]']
		}
		
		boolean operationSuccess = true;
		String messageCode = "default.create.label"
		List createdEntries = []

		if (params.entryId) {
			List result = doUpdateEntry(params.entryId, params.currentTime, params['entries[]'], params.baseDate, params.timeZoneName,
				params.defaultToNow == '1' ? true : false, (params.allFuture ?: '0') == '1' ? true : false)

			if (result[0]) {
				createdEntries.push(result[0])
			} else {
				operationSuccess = false
				messageCode = "not.saved.message"
			}
		} else {
			Entry.withTransaction { status ->
				// Iterating over all the entries received and creating entries for them
				entries.any({
					def result = doAddEntry(params.currentTime, params.timeZoneName, sessionUser().id.toString(), 
						it, params.baseDate, true)
					if (!result[0]) {
						operationSuccess = false
						messageCode = "not.saved.message"
						status.setRollbackOnly()
						return true
					} else {
						createdEntries.push(result[0])
						return
					}
				})
			}
		}
		JSON.use("jsonDate") {
			renderJSONPost([success: operationSuccess, createdEntries: createdEntries, 
				message: g.message(code: messageCode, args: ['Entries'])])
		}
	}

	def hideHelpData() {
		if (session.showHelp) {
			session.showHelp = null
		}
		renderJSONGet(success: true)
	}

	def getAutocompleteParticipantsData() {
		if (params.searchString) {
			List searchResults = User.withCriteria {
				projections{
					property("username")
					property("id")
				}
				and {
					or {
						ilike("username", "%${params.searchString}%")
						ilike("email", "%${params.searchString}%")
					}
					or {
						eq("virtual", false)
						isNull("virtual")
					}
				}
				maxResults(10)
			}
			renderJSONGet([success: true, usernameList: searchResults.collect{it.getAt(0)}, userIdList: searchResults.collect{it.getAt(1)}])
		} else {
			renderJSONGet([success: false])
		}
	}

	def deleteDiscussionData() {
		Discussion discussion = Discussion.get(params.discussionHash)
		if (discussion == null) {
			debug "DiscussionId not found: " + params.discussionHash
			renderJSONGet([success: false, message: "That discussion topic no longer exists."])
			return
		}
		Map result = Discussion.delete(discussion, sessionUser())
		renderJSONGet(result)
		return
	}

	def saveSurveyData() {
		log.debug "Data.saveSurveyData() $params"
		User currentUserInstance = sessionUser()

		if (!params.answer) {
			renderJSONPost([success: false, message: g.message(code: "default.blank.message", args: ["Answers"])])
			return
		}

		UserSurveyAnswer.withTransaction { status ->
			try {
				params.answer.each({ questionAnswerMap ->
					UserSurveyAnswer userSurveyAnswer = UserSurveyAnswer.create(currentUserInstance, questionAnswerMap.key, questionAnswerMap.value)
					if (!userSurveyAnswer) {
						throw new IllegalArgumentException()
					}
				})
				session.survey = null
				renderJSONPost([success: true])
			} catch (IllegalArgumentException e) {
				status.setRollbackOnly()
				renderJSONPost([success: false, message: g.message(code: "not.saved.message", args: ["Answers"])])
			}
		}
	}

	def getSurveyData() {
		log.debug "Data.getSurveyData()"
		List questions = SurveyQuestion.findAllByStatus(SurveyQuestion.QuestionStatus.ACTIVE,
			[max: 50, sort: "priority", order: "desc"])
		Map model = [questions: questions]
		render template: "/survey/questions", model: model
	}

	def getSprintParticipantsData(int offset, int max) {
		Sprint sprint = Sprint.findByHash(params.id)
		if (!sprint) {
			renderJSONGet([success: false, message: g.message(code: "default.not.found.message",                                             
					args: ["sprint", params.id])])
			return
		}

		renderJSONGet([success: true, participants: sprint.getParticipants(max, offset)])
	}
}
