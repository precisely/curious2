package us.wearecurio.services

import groovy.time.*
import groovyx.net.http.*
import groovyx.net.http.Method.*

import us.wearecurio.model.AnalyticsTimeSeries
import us.wearecurio.model.AnalyticsTagMembership
import us.wearecurio.model.AnalyticsTask
import us.wearecurio.model.TagProperties
import us.wearecurio.model.Entry
import us.wearecurio.model.User
import us.wearecurio.model.Tag
import us.wearecurio.server.BackgroundTask
import us.wearecurio.utility.Utils

import org.apache.commons.logging.LogFactory

import grails.util.Environment

class AnalyticsService {

	private static DEBUG = true

	private static def log = LogFactory.getLog(this)
	static transactional = false
	//public static def LOG_FILE_NAME = "/tmp/analytics.${(new Date()).format("YYYY-MM-DD_HH:mm")}.log".toString()
	//public static def LOG_FILE = new File( LOG_FILE_NAME )
	//public static def log(text) {
	//	LOG_FILE.withWriterAppend("UTF-8", { writer ->
	//		def Date logline_now = new Date()
	//		writer.write( "AnalyticsService ${logline_now.format("YYYY-MM-DD_HH:mm:ss")}: ${text}\n")
	//	})
	//}

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

	public static prepareUser(analyticsTask) {
		def userId = analyticsTask.userId
		if (null == userId) {
			return null
		}
		def user = User.get(userId.toLong())
		def tagIds = user.tags().collect { it.id }

		if (DEBUG) {
			if (tagIds.size > 15) {
				tagIds = tagIds[0..15]
			}
		}

		// Write out tagGroup -> tag join table.
		log.debug "user id ${userId}: SaveTagMembership(${userId})"
		saveTagMembership(userId)

		log.debug "user id ${userId}: classifyProperty(${userId})"
		tagIds.each { tagId ->
			classifyProperty(TagProperties.createOrLookup(userId, tagId))
		}

		// Delete the user's realizations of fetchPlotData.
		log.debug "user id ${userId}: delete table analytics_time_series"
		AnalyticsTimeSeries.executeUpdate('delete from AnalyticsTimeSeries at where at.userId=?', [userId])

		log.debug "user id ${userId}: refreshSeriesCache(${userId})"
		tagIds.each { tagId ->
			refreshSeriesCache(userId, tagId)
		}
		log.debug "user id ${userId}: refreshSeriesCache(${userId}) complete."

	}

	public static processOneOfManyUsers(childTask) {

		try {
			log.debug "AnalyticsTask processOneOfManyUsers @ ${childTask.serverAddress}: start"
			AnalyticsTask.incBusy()
			prepareUser(childTask)
			childTask.startProcessing()
		} catch(e) {
			throw e
		} finally {
			AnalyticsTask.decBusy()
		}
	}

	public static processUsers(parentId=null) {
		def parentTask
		if (parentId) {
			parentTask = AnalyticsTask.get(parentId)
		} else {
			parentTask = AnalyticsTask.createParent()
		}
		parentTask.status = AnalyticsTask.RUNNING
		Utils.save(parentTask, true)

		def incompleteTasks = AnalyticsTask.childrenIncomplete(parentId)
		def childTask
		AnalyticsTask.SERVERS.eachWithIndex { serverAddress, i ->
			childTask = null
			BackgroundTask.launch {
				if (incompleteTasks && i < incompleteTasks.size) {
					childTask = incompleteTasks[i]
				} else {
					childTask = AnalyticsTask.createChild(serverAddress, parentTask)
				}
				if (childTask) { processOneOfManyUsers(childTask) }
			}
		}
		incompleteTasks.collect { it.userId }
	}

}
