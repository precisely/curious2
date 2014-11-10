package us.wearecurio.services

import groovy.time.*
import us.wearecurio.utility.Utils
import us.wearecurio.model.AnalyticsTimeSeries
import us.wearecurio.model.AnalyticsTagMembership
import us.wearecurio.model.TagProperties
import us.wearecurio.model.Entry
import us.wearecurio.model.User
import us.wearecurio.model.Tag

import org.apache.commons.logging.LogFactory

import grails.util.Environment
import us.wearecurio.analytics.Interop

class AnalyticsService {
	private static def log = LogFactory.getLog(this)
	static transactional = false
	public static def LOG_FILE_NAME = "/tmp/analytics.${(new Date()).format("YYYY-MM-DD_HH:mm")}.log".toString()
	public static def LOG_FILE = new File( LOG_FILE_NAME )
	public static def log(text) {
		LOG_FILE.withWriterAppend("UTF-8", { writer ->
			def Date logline_now = new Date()
			writer.write( "AnalyticsService ${logline_now.format("YYYY-MM-DD_HH:mm:ss")}: ${text}\n")
		})
	}

	public static refreshSeriesCache(userId, tagId) {
		String time_zone = "Etc/UTC"
		Date now = new Date();
		def user = User.get(userId.toLong())
		def data_points = Entry.fetchPlotData(user, [tagId], null, null, now, time_zone)

		def prop = TagProperties.lookup(userId, tagId)

		data_points.each { point ->
			def init = [
				tagId: tagId,
				userId: userId,
				date: point[0],
				amount: point[1],
				description: point[2],
				dataType: prop.fetchDataType().toString()
			]
			def ts = new AnalyticsTimeSeries(init)
			ts.save(flush:true)
		} // data_points.each

	}

	public static classifyAsEventLike(userId, tagId) {
		//log.debug "Classify Event:: uid: " + userId + " tid: " + tagId
		def property = TagProperties.createOrLookup(userId, tagId)
		// Set the is_event value of the user-tag property.
		// This will save the property.
		property.classifyAsEvent().save()
	}

	public static saveTagMembership(userId) {
		def user = User.get(userId.toLong())
		log.debug("saveTagMembership: " + userId + ", numgroups: " + user.getTagGroups().size)
		//log.debug "Write Tag Group:: uid: " + userId + " num Groups: " + user.getTagGroups().size
		user.getTagGroups().each { tagGroup ->
			def tagGroupId = tagGroup.id
			tagGroup.getTags(userId).each { tag ->
				def init = [
					userId: userId,
					tagGroupId: tagGroupId,
					tagId: tag.id ]
				def atm = new AnalyticsTagMembership(init)
				atm.save(flush: true)
			} // iterate over tags in tagGroup
		}// iterate over tagGroup
	}

	public static classifyProperty(TagProperties property) {
		// Set the is_event value of the user-tag property.
		// This will save the property.
		property.classifyAsEvent().save(flush:true)
	}

	public static processUser(userId) {
		def user = User.get(userId.toLong())
		def tagIds = user.tags().collect { it.id }

		// Write out tagGroup -> tag join table.
		log "user id ${userId}: SaveTagMembership(${userId})"
		saveTagMembership(userId)

		log "user id ${userId}: classifyProperty(${userId})"
		tagIds.each { tagId ->
			classifyProperty(TagProperties.createOrLookup(userId, tagId))
		}

		// Delete the whole caching table to avoid duplicates and orphaned
		//	series of tags that have been completely deleted.
		log "user id ${userId}: delete table analytics_time_series"
		AnalyticsTimeSeries.executeUpdate('delete from AnalyticsTimeSeries')

		log "user id ${userId}: refreshSeriesCache(${userId})"
		tagIds.each { tagId ->
			refreshSeriesCache(userId, tagId)
		}

		// Run analytics on user.
		String environment = Environment.getCurrent().toString()
		log "user id ${userId}: Interop.updateUser(\"${environment}\", ${userId}, ${LOG_FILE_NAME})"
		Interop.updateUser(environment, userId, LOG_FILE_NAME)
	}

	public static processUsers() {
		def user_ids = User.executeQuery('select u.id from User u order by u.id')
		user_ids.each { userId	->
			log.debug "user id ${userId}: AnalyticsService.processUser(${userId})"
			processUser(userId)
			log.debug "user id ${userId}:  finished AnalyticsService.processUser(${userId})"
			log.debug "\n"
		}
	}

}