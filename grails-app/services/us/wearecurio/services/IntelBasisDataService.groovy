package us.wearecurio.services

import org.springframework.transaction.annotation.Transactional
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.Status
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyDataDump
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.User
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.thirdparty.TagUnitMap
import us.wearecurio.thirdparty.basis.BasisTagUnitMap
import us.wearecurio.utility.Utils

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

class IntelBasisDataService extends DataService {

	static final String SET_NAME = "Basis"
	static final String SOURCE_NAME = "Basis Data"
	static final String COMMENT = "(Basis)"
	static BasisTagUnitMap tagUnitMap = new BasisTagUnitMap()

	IntelBasisDataService() {
		provider = "Basis"
		typeId = ThirdParty.BASIS
		TagUnitMap.addSourceSetIdentifier(SET_NAME, SOURCE_NAME)
	}

	@Override
	Map getDataDefault(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataRequestContext context) throws InvalidAccessTokenException {
		return null
	}

	@Override
	String getTimeZoneName(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException {
		return null
	}

	@Override
	List<ThirdPartyNotification> notificationHandler(String notificationData) {
		return null
	}

	void processDump(ZipFile dumpFile, ThirdPartyDataDump thirdPartyDataDump) {
		def zippedFiles = dumpFile.entries().findAll {
			(!it.directory && (it.name.contains("sleep.csv") || it.name.contains("bodystates") || it.name.contains("sleep-stages")))
		}

		List unProcessedFiles = thirdPartyDataDump.unprocessedFiles ?: zippedFiles.collect {
			it.name
		}

		Long userId = thirdPartyDataDump.userId
		try {
			zippedFiles.each {
				if (unProcessedFiles.indexOf(it.name) > -1) {
					InputStream csvIn = dumpFile.getInputStream(it)
					Reader reader = new InputStreamReader(csvIn)

					Integer timeZoneId = User.getTimeZoneId(userId)
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					

					DataRequestContext context
					EntryCreateMap creationMap = new EntryCreateMap()
					EntryStats stats = new EntryStats(userId)
					int row

					if (it.name.contains("bodystates")) {
						context = getDataRequestContextInstance(reader, formatter, userId, 1)
						// re opening the closed stream
						csvIn = dumpFile.getInputStream(it)
						reader = new InputStreamReader(csvIn)
						processDataActivity(reader, timeZoneId, formatter, creationMap, stats, userId, context)
					} else if (it.name.contains("sleep-stages")) {
						context = getDataRequestContextInstance(reader, formatter, userId, 4)
						// re opening the closed stream
						csvIn = dumpFile.getInputStream(it)
						reader = new InputStreamReader(csvIn)
						processDataSleepStages(reader, timeZoneId, formatter, creationMap, stats, userId, context)
					} else if (it.name.contains("sleep.csv")) {
						context = getDataRequestContextInstance(reader, formatter, userId, 0)
						// re opening the closed stream
						csvIn = dumpFile.getInputStream(it)
						reader = new InputStreamReader(csvIn)
						processDataSleep(reader, timeZoneId, formatter, creationMap, stats, userId, context)
					}
				}
				unProcessedFiles.remove(it.name)
			}
		} catch (Exception e) {
			Utils.reportError("Error while processing dumpFile: ${thirdPartyDataDump.id}", e)
		} finally {
			if (!unProcessedFiles) {
				thirdPartyDataDump.status = Status.PROCESSED
			} else if (thirdPartyDataDump.attemptCount == 2) {
				thirdPartyDataDump.status = Status.FAILED
				thirdPartyDataDump.attemptCount++
			} else {
				thirdPartyDataDump.status = Status.PARTIALLYPROCESSED
				thirdPartyDataDump.unprocessedFiles = unProcessedFiles
				thirdPartyDataDump.attemptCount++
			}
			Utils.save(thirdPartyDataDump, true)
		}
	}

	private DataRequestContext getDataRequestContextInstance(Reader reader, SimpleDateFormat formatter, long userId, 
			 int dateFieldIndex) {
		Date startDateRange, endDateRange
		int row = 0
		reader.eachCsvLine { tokens ->
			// Reading start date for date range from file
			if (row == 1) {
				startDateRange = formatter.parse(tokens[dateFieldIndex])
			} else if (row) { // Reading end date for date range from file
				endDateRange = formatter.parse(tokens[dateFieldIndex])
			}
			row++
		}

		return new DataRequestContext(startDateRange, endDateRange, COMMENT, userId)
	}
	
	@Transactional
	void processDataSleepStages(Reader reader, Integer timeZoneId, SimpleDateFormat formatter, EntryCreateMap creationMap,
			EntryStats stats, Long userId, DataRequestContext context) {
		Date entryStartDate
		Date entryEndDate
		boolean isFirstLine = true

		reader.eachCsvLine { tokens ->
			if (isFirstLine) {
				isFirstLine = false
				return // means continue in a loop
			}
			entryStartDate = formatter.parse(tokens[4])
			entryEndDate = formatter.parse(tokens[6])
			String tagName = (tokens[8].tokenize(' ')[0]).toLowerCase()
			BigDecimal duration = Utils.getDateDiff(entryStartDate, entryEndDate, TimeUnit.MINUTES)
			tagUnitMap.buildEntry(creationMap, stats, tagName, duration, userId, timeZoneId,
					entryStartDate, COMMENT, SET_NAME, context)
		}

		stats.finish()
	}

	@Transactional
	void processDataSleep(Reader reader, Integer timeZoneId, SimpleDateFormat formatter, EntryCreateMap creationMap,
			EntryStats stats, Long userId, DataRequestContext context) {
		Date entryDate
		boolean isFirstLine = true

		reader.eachCsvLine { tokens ->
			if (isFirstLine) {
				isFirstLine = false
				return // means continue in a loop
			}
			entryDate = formatter.parse(tokens[0])
			BigDecimal duration = new BigDecimal(tokens[4])

			tagUnitMap.buildEntry(creationMap, stats, "total", duration, userId, timeZoneId,
					entryDate, COMMENT, SET_NAME, context)

			duration = new BigDecimal(tokens[10])
			if (duration) {
				tagUnitMap.buildEntry(creationMap, stats, "tosses", duration, userId, timeZoneId,
						entryDate, COMMENT, SET_NAME, context)
			}
		}

		stats.finish()
	}

	@Transactional
	void processDataActivity(Reader reader, Integer timeZoneId, SimpleDateFormat formatter, EntryCreateMap creationMap,
			 EntryStats stats, Long userId, DataRequestContext context) {
		Date entryStartDate
		boolean isFirstLine = true

		reader.eachCsvLine { tokens ->
			if (isFirstLine) {
				isFirstLine = false
				return // means continue in a loop
			}
			entryStartDate = formatter.parse(tokens[1])
			String tagName = tokens[0]
			if (tokens[5] && new BigDecimal(tokens[5])) {
				tagUnitMap.buildEntry(creationMap, stats, tagName + "_calories", new BigDecimal(tokens[5]), userId,
						timeZoneId, entryStartDate, COMMENT, SET_NAME, context)
			}
			if (tokens[6] && new BigDecimal(tokens[5])) {
				tagUnitMap.buildEntry(creationMap, stats, tagName + "_steps", new BigDecimal(tokens[6]), userId,
						timeZoneId, entryStartDate, COMMENT, SET_NAME, context)
			}
			if (tokens[7] && new BigDecimal(tokens[5])) {
				tagUnitMap.buildEntry(creationMap, stats, tagName + "_heart_rate", new BigDecimal(tokens[7]), userId,
						timeZoneId, entryStartDate, COMMENT, SET_NAME, context)
			}
		}

		stats.finish()
	}
}
