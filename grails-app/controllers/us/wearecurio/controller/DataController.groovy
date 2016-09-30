package us.wearecurio.controller

import grails.converters.JSON
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import us.wearecurio.annotations.EmailVerificationRequired
import us.wearecurio.data.DecoratedUnitRatio
import us.wearecurio.data.RepeatType
import us.wearecurio.data.UnitGroupMap
import us.wearecurio.data.UserSettings
import us.wearecurio.model.*
import us.wearecurio.model.Model.Visibility
import us.wearecurio.security.NoAuth
import us.wearecurio.services.EntryParserService
import us.wearecurio.services.EntryParserService.ParseAmount
import us.wearecurio.services.SearchService
import us.wearecurio.services.SecurityService.AuthenticationStatus
import us.wearecurio.support.EntryStats
import us.wearecurio.utility.Utils

import java.math.MathContext
import java.text.SimpleDateFormat

class DataController extends LoginController {

	EntryParserService entryParserService
	SearchService searchService

	static debug(str) {
		log.debug(str)
	}

	def DataController() {
		debug "constructor()"
		systemFormat.setTimeZone(TimeZone.getDefault());
	}

	private def getParsedEntry(Map params, User user) {
		def p = [defaultToNow:'1']
		p.putAll(params)

		boolean defaultToNow = (p.defaultToNow == '1')

		debug "DataController.getParsedEntry() params:" + p

		Date currentTime = parseDate(p.currentTime)
		Date baseDate = parseDate(p.baseDate)
		Long repeatTypeId = parseLong(p.repeatTypeId)
		Date repeatEnd = parseDate(p.repeatEnd)
		String timeZoneName = p.timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(p.baseDate) : p.timeZoneName

		debug("Current time " + currentTime + " baseDate " + baseDate)
		
		String text = p.text
		
		return entryParserService.parse(currentTime, timeZoneName, p.text, repeatTypeId, repeatEnd, baseDate, defaultToNow, p.tutorial ? EntryParserService.UPDATEMODE_TUTORIAL : 0)
	}

	private def createEntries(Map params, User user, EntryStats stats) {
		def p = [defaultToNow:'1']
		p.putAll(params)

		boolean defaultToNow = (p.defaultToNow == '1')

		debug "DataController.createEntries() params:" + p

		Date currentTime = parseDate(p.currentTime)
		Date baseDate = parseDate(p.baseDate)
		Long repeatTypeId = parseLong(p.repeatTypeId)
		Date repeatEnd = parseDate(p.repeatEnd)
		String timeZoneName = p.timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(p.baseDate) : p.timeZoneName

		debug("Current time " + currentTime + " baseDate " + baseDate)
		
		return Entry.parseAndCreate(user.id, stats, currentTime, timeZoneName, p.text, repeatTypeId, repeatEnd, baseDate, defaultToNow, p.tutorial ? EntryParserService.UPDATEMODE_TUTORIAL : 0)
	}

	protected def doAddEntry(Map params) {
		AuthenticationStatus authStatus = authFromUserIdStr(params.userId)

		User user = authStatus.user
		// preprocess text and split by commas
		
		String text
		
		EntryStats stats = new EntryStats(user.id)
		
		debug("Original entry text: " + params.text)
		
		def (status, entries) = createEntries(params, user, stats)
			
		ArrayList<TagStats> tagStats = stats.finish()

		Entry entry = entries ? entries.last() : null
		
		if (entry == null)
			return 'No entry text provided'
		
		boolean showEntryBalloon
		boolean showBookmarkBalloon
		if (params.containsKey('mobileSessionId') && !params.isHelpEntry) {
			// Currently showing the balloons only for the mobile app. TODO add support for balloons on web.
			UserSettings userSettings = user.settings
			if (entry.getRepeatType()?.isContinuous()) {
				showBookmarkBalloon = !userSettings.hasBookmarkCreationCountCompleted()
				if (showBookmarkBalloon) {
					userSettings.countBookmarkCreation()
				}
			} else {
				showEntryBalloon = !userSettings.hasEntryCreationCountCompleted()
				if (showEntryBalloon) {
					userSettings.countEntryCreation()
				}

				if (entry.getRepeatType()?.isReminder()) {
					userSettings.countFirstAlertEntry()
				}
			}

			Utils.save(user, true)
		}

		authStatus.sprint?.reindex()

		return [entry, status, tagStats?.get(0), showEntryBalloon, showBookmarkBalloon, entries]
	}

	protected def doUpdateEntry(Map parms) {
		debug "DataController.doUpdateEntry() params:" + parms

		Map p = [defaultToNow:'1', allFuture:'1']
		p.putAll(parms)

		boolean allFuture = (p.allFuture == '1')

		Entry entry = Entry.get(Long.parseLong(p.entryId))

		if (entry.getUserId() == 0L) {
			debug "Attempting to edit a deleted entry."
			return [null, 'Attempting to edit a deleted entry.', null, null]
		}

		Long userId = entry.getUserId()

		AuthenticationStatus authStatus = authFromUserId(entry.getUserId())

		if (!authStatus.authorized) {
			debug "Illegal access to entry " + entry
			return [null, 'You do not have permission to edit this entry.', null, null]
		}

		if (entry.fetchIsGenerated()) {
			debug "Can't edit a generated entry"
			return [null, 'You cannot edit a generated entry.', null, null]
		}

		Date currentTime = parseDate(p.currentTime)
		Date baseDate = parseDate(p.baseDate)
		String timeZoneName = p.timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate) : p.timeZoneName

		Long repeatTypeId = parseLong(p.repeatTypeId)
		Date repeatEnd = parseDate(p.repeatEnd)

		EntryStats stats = new EntryStats(userId)

		// activate repeat entry if it has a repeat type
		if (entry.getRepeatType() != null && entry.getRepeatType().isContinuous() && !p.bookmarkEdit) {
			debug "Activating continuous entry " + entry
			Entry newEntry = entry.activateContinuousEntry(baseDate, currentTime, timeZoneName, stats)
			if (newEntry != null) {
				entry = newEntry
				debug "New activated entry " + newEntry
			} else
				debug "No entry activation"
		}

		boolean wasContinuous = entry.getRepeatType()?.isContinuous()
		
		if (repeatEnd == null && entry.repeatEnd != null)
			repeatEnd = entry.repeatEnd

		def m = entryParserService.parse(currentTime, timeZoneName, p.text, repeatTypeId, repeatEnd, baseDate, false, 1)

		if (!m) {
			debug "Parse error"
			stats.finish()
			return [null, 'Cannot interpret entry text.', null, null];
		} else if (m['repeatType']?.isContinuous() && (!wasContinuous) && !p.bookmarkEdit) {
			// if creating a new continuous (button) entry and the previous entry was not continuous, create a new entry instead
			entry = Entry.create(userId, m, stats)
		} else if (entry != null) {
			entry = entry.update(m, stats, baseDate, allFuture)
		}

		authStatus.sprint?.reindex()

		ArrayList<TagStats> tagStats = stats.finish()
		if (tagStats == null) {
			return [entry, '', null, null]
		} else
			return [entry, '', tagStats[0], tagStats.size() > 0 ? tagStats[1] : null];
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
					String units = tokens[3]
					DecoratedUnitRatio unitRatio = UnitGroupMap.theMap.lookupDecoratedUnitRatio(units)
					Tag baseTag = Tag.look(tokens.length > 9 ? tokens[9] : tokens[1])
					Integer amountPrecision = Integer.valueOf(tokens[6].equals("null") ? '3' : tokens[6])
					ParseAmount amount = new ParseAmount(Tag.look(tokens[1]), baseTag, new BigDecimal(tokens[2], mc), amountPrecision, units, unitRatio, DurationType.NONE)
					def parsedEntry = [ \
						userId:userId, \
						date:currentDate, \
						timeZoneName:tokens[8], \
						amount:amount, \
						comment:tokens[4], \
						repeatType:repeatTypeId >= 0 ? RepeatType.look((long)repeatTypeId) : null, \
						setName:setName, \
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

	@NoAuth
	def getPeopleData() {
		debug "DataController.getPeopleData"

		def user = sessionUser()

		if (user == null) {
			debug "auth failure - return empty array"
			renderJSONGet([])
			return
		}

		debug "user:" + user

		Map userData = user.getJSONDesc()
		userData["notificationCount"] = searchService.getNewNotificationCount(user)
		renderJSONGet([userData])
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

		EntryStats stats = new EntryStats(entry.userId)
		Entry.deleteGhost(entry, stats, currentTime, allFuture)
		def tagStats = stats.finish()
		return [listEntries(sessionUser(), timeZoneName, baseDate, currentTime),
			tagStats == null ? null : (tagStats[0]?.getJSONDesc()),
			tagStats == null ? null : (tagStats.size() > 1 ? tagStats[1]?.getJSONDesc() : null)]
	}


	def getListData(Boolean respondAsMap) {
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
				entries = entries << ["${localDate.toString('MM/dd/yyyy')}":data]
			}
		}

		debug "DataController.getListData() Returning entries "+entries.dump()
		// skip continuous repeat entries with entries within the usage threshold

		/*
		 * It should always be rendered as a map after mobile app is updated. This is currently needed to backward
		 * support the mobile code.
		 */
		if (respondAsMap) {
			renderJSONGet([entries: entries, deviceEntryStates: user.settings.getDeviceEntryStates()])
			return
		}

		/* Currently showing the balloons only for the mobile app.
		 * TODO add support for balloons on web.
		 * TODO The params['balloonData'] check will be removed once the updated app is in the app store.
		 */
		if (params.containsKey('mobileSessionId') && params.balloonData) {
			boolean showRemindAlertBalloon = !user.settings.hasFirstAlertEntryCountCompleted()
			renderJSONGet([entries, showRemindAlertBalloon])
			
			return
		}

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
			Tag tag = Tag.look(tagStr)
			if (tag)
				tagIds.add(tag.getId())
		}

		def plotInfo = [:]

		def plotEntries = Entry.fetchPlotData(sessionUser(), tagIds, startDateStr ? parseDate(startDateStr) : null,
			endDateStr ? parseDate(endDateStr) : null, new Date(), plotInfo)

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

		List result = doAddEntry(params)

		if (result[0] != null) {
			AuthenticationStatus authStatus = authFromUserIdStr(params.userId)

			User user = authStatus.user

			renderJSONGet([
				listEntries(userId, params.timeZoneName, baseDate, currentTime),
				result[1],
				result[2]?.getJSONDesc(),
				result[0].getJSONDesc(),
				// TODO respond this as map instead of passing data based on index
				user.settings.getDeviceEntryStates(),
				result[3], // ShowEntryBalloon flag
				result[4]  // ShowBookmarkBalloon flag
			])
		} else {
			renderStringGet('error')
		}
	}

	def updateEntrySData() { // new API
		debug("DataController.updateEntrySData() params:" + params)

		Date baseDate = parseDate(params.baseDate)
		Date currentTime = parseDate(params.currentTime ?: params.date) ?: new Date()

		def (entry, message, oldTagStats, newTagStats) = doUpdateEntry(params)
		if (entry != null) {
			User user = sessionUser()

			renderJSONGet([listEntries(userFromId(entry.getUserId()), params.timeZoneName, baseDate, currentTime),
					oldTagStats?.getJSONDesc(), newTagStats?.getJSONDesc(),
					// TODO respond this as map instead of passing data based on index
					user.settings.getDeviceEntryStates()])
		} else {
			debug "Error while updating: " + message
			renderStringGet(message)
		}
	}

	def deleteEntrySData(Long entryId) { // new API
		debug "DataController.deleteEntrySData() params:" + params

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		Entry entry = Entry.get(entryId)
		def userId = entry.getUserId();

		if (!userId) {
			debug "Entry already deleted"
			renderStringGet(g.message(code: "entry.already.deleted"))
			return
		}

		def currentTime = params.currentTime == null ? new Date() : parseDate(params.currentTime)
		def baseDate = params.baseDate == null? null : parseDate(params.baseDate)
		def timeZoneName = params.timeZoneName == null ? TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate) : params.timeZoneName
		User entryOwner = User.get(userId);
		User currentUser = sessionUser()

		if (!entryOwner.virtual && entryOwner.id != currentUser.id) {
			renderStringGet('You do not have permission to delete this entry.')
		} else if (entry.fetchIsGenerated()) {
			renderStringGet('Cannot delete generated entries.')
		} else {
			// Allow deleting entries belonging to a trackathon only if user has write permission for that trackathon
			if (entryOwner.virtual) {
				Sprint sprint = Sprint.findByVirtualUserId(entryOwner.id)
				if (!sprint.hasWriter(currentUser.id)) {
					renderStringGet('You do not have permission to delete this entry.')
					return
				}
			}
			EntryStats stats = new EntryStats(userId)
			Entry.delete(entry, stats)
			def tagStats = stats.finish()
			renderJSONGet([listEntries(currentUser, timeZoneName, parseDate(params.displayDate), currentTime),
				tagStats[0]?.getJSONDesc(),
				tagStats.size() > 1 ? tagStats[1].getJSONDesc() : null,
				// TODO respond this as map instead of passing data based on index
				user.settings.getDeviceEntryStates()])
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

	protected def getAutocompleteTags(term, Date now) {
		debug "DataController.getAutocompleteTags() term " + term

		def user = sessionUser()
		if (user == null)
			return null

		if (term.equals(ALLAUTOTAGS)) {
			debug "ALLAUTOTAGS"
			return getSessionCache("AUTOTAGS*" + term, {
				return Entry.getAutocompleteTags(user, now)
			})
		}

		if (term.equals(ALLAUTOTAGSWITHINFO)) {
			debug "ALLAUTOTAGSWITHINFO"
			return Entry.getAutocompleteTagsWithInfo(user, now)
		}

		return getSessionCache("AUTOTAGS*" + term, {
			def tags = Entry.getAutocompleteTags(user, now)
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

		Date now

		if (params.currentTime) {
			now = parseDate(params.currentTime)
		} else
			now = new Date()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		if (params.all != null) {
			if (params.all.equals('info')) {
				debug "Get all autocomplete tags by algorithm with units and last amount info"
				renderJSONGet(getAutocompleteTags(ALLAUTOTAGSWITHINFO, now))
			} else {
				debug "Get all autocomplete tags by algorithm"
				renderJSONGet(getAutocompleteTags(ALLAUTOTAGS, now))
			}
		} else {
			// this is using an outdated algorithm; currently this is computed in the browser
			debug "Autocomplete suggestions for " + params.partial
			renderJSONGet(getAutocompleteTags(params.partial, now))
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
			def plotDataObj = PlotData.create(user, params.name, params.plotData, false)

			Utils.save(plotDataObj, true)

			renderJSONPost(['success', plotDataObj.getId()])
		} catch (Exception e) {
			Utils.reportError("Error while saving plot data", e)
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

		PlotData plotData = PlotData.get(Long.valueOf(params.id))

		if (plotData.userId != user.id) {
			renderStringGet('Not authorized to load this graph');
			return;
		}

		if (plotData == null) {
			renderStringGet('No such graph id ' + params.id)
			return;
		}

		if (!plotData.getIsDynamic()) {
			renderStringGet('Not a live graph')
			return;
		}

		renderDataGet(plotData.fetchJsonPlotData())
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
			Utils.reportError("Error while deleting plot data", e)
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

	@EmailVerificationRequired
	def saveSnapshotData() {
		debug "DataController.saveSnapshotData() params:" + params

		def user = sessionUser()

		if (!user) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Saving " + params.snapshotData

		UserGroup group = null
		Visibility visibility = Visibility.PUBLIC

		if (params.group) {
			if (params.group == Visibility.PRIVATE.name()) {
				visibility = Visibility.PRIVATE
			} else {
				String groupName = params.group
				if (groupName == Visibility.PUBLIC.name())
					groupName = null
				group = Discussion.loadGroup(groupName, user)

				if (!group) {
					renderJSONPost([success: false, message: g.message(code: "default.permission.denied")])
					return
				}
			}
		}

		PlotData plotDataObj = PlotData.create(user, params.name, params.snapshotData, true)

		Utils.save(plotDataObj, true)

		String name = plotDataObj.getName()
		Discussion discussion = Discussion.create(user, name, visibility)

		if (discussion) {
			if (group) {
				group.addDiscussion(discussion)
			}
			DiscussionPost.createFirstPost(null, user, discussion, plotDataObj.id)
		}
		renderJSONPost([success: true, discussionHash: discussion.hash])
	}

	@NoAuth
	def loadSnapshotDataId() {
		debug "DataController.loadSnapshotDataId() params:" + params

		Long plotDataId = Long.valueOf(params.id)
		String discussionHash = params.discussionHash
		if (!discussionHash) {
			debug "old version"
			renderStringGet("You're using an old version of the client; please update to the latest version (reload the browser or update your mobile app).")
			return
		}

		def user = sessionUser()

		if (user == null) {
			Discussion discussion = Discussion.getDiscussionForPlotDataId(plotDataId)
			if (!discussion.getIsPublic()) {
				debug "auth failure"
				renderStringGet("You do not have authorization to load this graph.")
				return
			}
		}

		debug "Trying to load plot data " + params.id

		PlotData plotData = PlotData.get(plotDataId)

		if (plotData == null) {
			renderStringGet('No such graph id ' + params.id)
			return;
		}

		def c = DiscussionPost.createCriteria()
		def results = c {
			eq("plotDataId", plotDataId)
			maxResults(1)
		}

		if (!results) {
			debug "auth failure - no post associated with this plotDataId"
			renderStringGet("You do not have authorization to load this graph.")
			return
		}

		DiscussionPost post = results[0]

		Discussion discussion = Discussion.get(post.discussionId)

		if (discussionHash != discussion.hash) {
			debug "auth failure - discussion hash doesn't match"
			renderStringGet("You do not have authorization to load this graph.")
			return
		}

		if (!plotData.getIsSnapshot()) {
			renderStringGet('Graph is not a snapshot')
			return;
		}

		renderDataGet(plotData.fetchJsonPlotData())
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

	def createSingleHelpEntrysData() {
		debug("DataController.createSingleHelpEntrysData() params:" + params)

		def userId = userFromIdStr(params.userId)

		if (userId == null) {
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		AuthenticationStatus authStatus = authFromUserIdStr(params.userId)
		User user = authStatus.user
		Date baseDate = parseDate(params.baseDate)
		Date currentTime = parseDate(params.currentTime ?: params.date) ?: new Date()
		def parsedEntry = getParsedEntry(params, user)
		def amount = parsedEntry.amounts[0]
		if (parsedEntry.tag?.description?.contains("cannot understand what you typed") ||
				(parsedEntry.baseTag.description == "sleep" && parsedEntry.amounts[0].durationType != DurationType.END)) {
			renderJSONGet([success: false, message: parsedEntry.baseTag?.description == "sleep" ?
					"You must enter a time duration, like 'sleep 8 hours 10 mins'" : "Can not understand what you typed"])
			return
		}
		def result
		if (params.entryId) {
			result = doUpdateEntry(params)
		} else {
			if (params.containsKey('mobileSessionId')) {
				params['isHelpEntry'] = true
			}
			result = doAddEntry(params)
		}
		if (result[0] != null) {
			renderJSONGet([
				listEntries(userId, params.timeZoneName, baseDate, currentTime),
				result[1],
				result[2]?.getJSONDesc(),
				result[0].getJSONDesc()
			])
		} else {
			renderStringGet('error')
		}
	}

	def createHelpEntriesData() {
		log.debug "Entries recieved to save: $params and entries: ${params['entries[]']}"

		if (session.showHelp) {
			session.showHelp = null
		}

		String userIdStr = sessionUser().id.toString()

		List entries = []

		// Jquery sends list in parameters with '[]' suffix added to the keyname in the map hence using params['entries[]']
		if (params['entries[]'] instanceof String) {
			entries.push(params['entries[]'])
		} else {
			entries = params['entries[]']
		}

		boolean operationSuccess = true;
		String messageCode = "default.create.label"
		List messageArgs = ["Entries"]
		List createdEntries = []

		if (params.entryId) {
			Map p = [:]
			p.putAll(params)
			p.text = params['entries[]']
			List result = doUpdateEntry(p)

			if (result[0]) {
				createdEntries.push(result[0])
			} else {
				operationSuccess = false
				messageCode = "not.saved.message"
			}
		} else {
			Map p = [userId:userIdStr]
			p.putAll(params)
			int counter = 0;
			Entry.withTransaction { status ->
				// Iterating over all the entries received and creating entries for them
				entries.any({
					p.text = it
					p.tutorial = true
					def result = doAddEntry(p)
					counter++;
					if (!result[0] || result[0].description?.contains("cannot understand what you typed")) {
						operationSuccess = false
						messageCode = !result[0] ? "not.saved.message" : "can.not.understand.entry"
						if (result[0]) {
							// Considering order of incomming entries for sendingeror message specific to the type of entry
							switch(counter) {
								case 1:
									messageArgs = ["Drink"]
									break
								case 2:
									messageArgs = ["Exercise"]
									break
								case 3:
									messageArgs = ["Work"]
									break
								case 4:
									messageArgs = ["Supplements"]
									break
							}
						}
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
				message: g.message(code: messageCode, args: messageArgs)])
		}
	}

	def hideHelpData() {
		if (session.showHelp) {
			session.showHelp = null
		}
		renderJSONGet(success: true)
	}

	def getAutocompleteParticipantsData() {
		String searchString = params.searchString
		if (!searchString) {
			renderJSONGet([success: false])
		}
		boolean absoluteUsername = searchString.contains(/"/)
		String plainSearchString = searchString.replaceAll(/"/, "")
		params.max = params.max ? Math.min(params.int('max'), 50) : 4

		List searchResults
		if (absoluteUsername) {
			searchResults = User.findAllByUsernameAndVirtualInList(plainSearchString, [false, null])
		} else {
			searchResults = User.withCriteria {
				or {
					ilike("username", "%${searchString}%")
					ilike("name", "%${params.searchString}%")
				}

				or {
					eq("virtual", false)
					isNull("virtual")
				}

				maxResults(params.max)
			}
		}

		List displayNames = searchResults.collect {
			// Sending the name in the form "John (johny)" if the name is public. Sending only the username otherwise.
			if (it.settings.isNamePublic()) {
				[label: it.name + "(" + it.username + ")", value: it.username]
			} else {
				[label: it.username, value: it.username]
			}
		}

		renderJSONGet([success: true, usernameList: searchResults.collect { it.username },
					   userIdList: searchResults.collect { it.id }, displayNames: displayNames])
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
				Utils.reportError("Error while saving survey data", e)
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

	def getSprintAdminsData(int offset, int max) {
		Sprint sprint = Sprint.findByHash(params.id)
		if (!sprint) {
			renderJSONGet([success: false, message: g.message(code: "default.not.found.message",
					args: ["sprint", params.id])])
			return
		}

		renderJSONGet([success: true, participants: sprint.getParticipants(max, offset, true)])
	}

	def saveDeviceEntriesStateData(Boolean isCollapsed, String device) {
		User user = sessionUser()
		log.debug "Save collapse/expand state of device entries for $user with $params"

		if (!user) {
			log.warn "No logged in user found"
			return
		}

		ThirdParty thirdPartyDevice

		try {
			thirdPartyDevice = ThirdParty.lookup(device)

			if (isCollapsed) {
				user.settings.collapseDeviceEntries(thirdPartyDevice)
			} else {
				user.settings.expandDeviceEntries(thirdPartyDevice)
			}

			Utils.save(user, true)
			renderJSONPost([success: true])
		} catch (IllegalArgumentException e) {
			Utils.reportError("Error while saving device entries state", e)
			renderJSONPost([success: false, message: e.message])
		} catch(e) {
			Utils.reportError("Error while saving device entries state", e)
			renderJSONPost([success: false, message: e.message])
		}
	}
}
