package us.wearecurio.services

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

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

	private static DEBUG = false
	
	static def service
	
	static def set(s) {
		service = s
		AnalyticsTask.analyticsService = s
	}

	static EmailService get() { return service }
	
	def grailsApplication
	
	def servers

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
	
	// For counting the number of ready analytics servers.
	//	Need to use thread-safe data structures, because AsyncHTTPBuilder uses
	//	threaads to make the HTTP requests asynchronous.
	public AtomicInteger numWebServersBusy = new AtomicInteger(0)
	public AtomicIntegerArray responses

	AnalyticsService() {
		if (!grailsApplication)
			grailsApplication = new org.codehaus.groovy.grails.commons.DefaultGrailsApplication()
		servers = grailsApplication.config.curiousanalytics.servers
		responses = new AtomicIntegerArray(servers.size())
	}

	def refreshSeriesCache(userId, tagId) {
		AnalyticsTask.withTransaction {
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
				Utils.save(ts, false)
			} // data_points.each
		}
	}

	def classifyAsEventLike(userId, tagId) {
		AnalyticsTask.withTransaction {
			//log.debug "Classify Event:: uid: " + userId + " tid: " + tagId
			def property = TagProperties.createOrLookup(userId, tagId)
			// Set the is_event value of the user-tag property.
			// This will save the property.
			Utils.save(property.classifyAsEvent())
		}
	}

	def saveTagMembership(userId) {
		log.debug "saveTagMembership() userId:" + userId
		AnalyticsTask.withTransaction {
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
					Utils.save(atm, false)
				} // iterate over tags in tagGroup
			}// iterate over tagGroup
		}
	}

	def classifyProperty(TagProperties property) {
		// Set the is_event value of the user-tag property.
		// This will save the property.
		AnalyticsTask.withTransaction {
			Utils.save(property.classifyAsEvent(), false)
		}
	}

	def prepareUser(analyticsTask) {
		log.debug "preapreUser() analyticsTask:" + analyticsTask
		Long userId = analyticsTask.userId
		if (null == userId)
			return null
		def user = User.get(userId)
		if (user == null)
			return null
		def tagIds = user.tags().collect { it.id }

		if (DEBUG) {
			if (tagIds.size > 25) {
				tagIds = tagIds[0..25]
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
		if (!DEBUG) {
			AnalyticsTimeSeries.executeUpdate('delete from AnalyticsTimeSeries at where at.userId = :userId', [userId:userId])

			// Refresh the analytics_time_series dump for the user.
			log.debug "user id ${userId}: refreshSeriesCache(${userId})"
			tagIds.each { tagId ->
				AnalyticsTask.withTransaction {
					refreshSeriesCache(userId, tagId)
				}
			}
			log.debug "user id ${userId}: refreshSeriesCache(${userId}) complete."
		}
	}

	boolean processOneOfManyUsers(AnalyticsTask childTask) {
		log.debug "processOneOfManyUsers() childTask:" + childTask
		try {
			log.debug "AnalyticsTask processOneOfManyUsers @ ${childTask.serverAddress}: start"
			AnalyticsTask.incBusy()
			prepareUser(childTask)
			childTask.startProcessing()
		} catch(e) {
			e.printStackTrace()
			return false
		} finally {
			AnalyticsTask.decBusy()
		}
			
		return true
	}
	
	// mark given task as finished and process next one
	Long processNextTask(AnalyticsTask prevTask) {
		log.debug "processNextTask() prevTask:" + prevTask
		if (!prevTask) {
			return null
		}
		
		// This means the previous task was completed.
		log.debug "Mark prevTask completed: " + prevTask
		prevTask.markAsCompleted()
	
		// Update the parentTask status.
		def parentTask = AnalyticsTask.get(prevTask.parentId)
		if (parentTask && prevTask && prevTask.userId && (parentTask.userId < prevTask.userId)) {
			// The parentTask.userId is the id of the highest userId completed.  (But there might be, lower ids
			//	 still in progress, so check the parentTask.status before concluding the job is all done.)
			parentTask.userId = prevTask.userId
			parentTask.updatedAt = new Date()
			Utils.save(parentTask, true)
		}
	
		// So, let's start a new one. If there are more tasks than users, nextTask.startProcessing will
		// not do anything.
		def nextTask = AnalyticsTask.createSibling(prevTask)
		def userId = null
		if (nextTask && nextTask.userId && nextTask.userId > 0) {
			if (!processOneOfManyUsers(nextTask)) {
				// server error while posting to analytics server
				return processNextTask(nextTask) // end error task and go on to next one
			}
			userId = nextTask.userId
		}
		
		return userId
	}

	def processUsers(parentId=null) {
		log.debug "processUsers: parentId " + parentId
		
		def parentTask
		if (parentId) {
			parentTask = AnalyticsTask.get(parentId)
		} else {
			parentTask = AnalyticsTask.createParent()
		}
		parentTask.status = AnalyticsTask.RUNNING
		Utils.save(parentTask, true)

		def incompleteTasks = AnalyticsTask.childrenIncomplete(parentId)
		AnalyticsTask childTask
		servers.eachWithIndex { serverAddress, i ->
			boolean successfulLaunch = false
			while (!successfulLaunch) {
				childTask = null
				if (incompleteTasks && i < incompleteTasks.size) {
					childTask = incompleteTasks[i]
				} else {
					childTask = AnalyticsTask.createChild(serverAddress, parentTask)
				}
				if (!childTask)
					successfulLaunch = true
				else if (!processOneOfManyUsers(childTask)) {
					childTask.markAsCompleted()
				} else {
					successfulLaunch = true
					Utils.save(childTask, true)
				}
			}
		}
		incompleteTasks.collect { it.userId }
	}

}
