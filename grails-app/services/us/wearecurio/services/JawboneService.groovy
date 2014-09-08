package us.wearecurio.services

import java.text.DateFormat
import java.text.SimpleDateFormat

import org.springframework.transaction.annotation.Transactional

import us.wearecurio.model.User
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats
import us.wearecurio.thirdparty.TagUnitMap
import us.wearecurio.thirdparty.jawbone.JawboneTagUnitMap

class JawboneService {

	static final String COMMENT = "(Jawbone)"
	static final String SET_NAME = "jawbone"

	static transactional = true

	DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd z")

	TagUnitMap tagUnitMap = new JawboneTagUnitMap()

	void parseJawboneCSV(InputStream csvIn, Long userId) {
		def columnList
		Date currentDate
		boolean isFirstLine = true
		Reader reader = new InputStreamReader(csvIn)
		Integer timeZoneId = User.getTimeZoneId(userId)
		
		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		reader.eachCsvLine { tokens ->
			log.debug "Tokens: $tokens"
			if (isFirstLine) {
				isFirstLine = false
				columnList = tokens	// Keeping a list of column names
				return // means continue in a loop
			}
			currentDate = dateFormat.parse(tokens[0] + " GMT")
			/**
			 * Iterating through each column name, checking if it exist in above said map.
			 * If it exists, then current token value pointed by column index will be its amount.
			 * This is necessary, since we are not sure about position of tags.
			 */
			columnList.eachWithIndex { column, index ->
				tagUnitMap.buildEntry(creationMap, stats, column, tokens[index], userId, timeZoneId, COMMENT, SET_NAME)
			}
			tagUnitMap.buildBucketedEntries(creationMap, stats, userId, [comment: COMMENT, setName: SET_NAME, timeZoneId: timeZoneId])
			tagUnitMap.emptyBuckets()
		}
		
		stats.finish()
	}

}