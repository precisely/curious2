package us.wearecurio.controller

import grails.converters.JSON
import us.wearecurio.model.AnalyticsTask
import us.wearecurio.utility.Utils
import us.wearecurio.server.BackgroundTask
import us.wearecurio.services.AnalyticsService

//import groovyx.net.http.HTTPBuilder
//import groovyx.net.http.HTTPBuilder.*
//import static groovyx.net.http.Method.*
//import static groovyx.net.http.ContentType.*

import groovyx.net.http.RESTClient
import static groovyx.net.http.ContentType.*


class AnalyticsTaskController {

	def securityService
	def analyticsService

	static allowedMethods = [index: "GET", processUsers: "POST", runNext: "POST"]

	def index() {
		def taskList = AnalyticsTask.findAll("FROM AnalyticsTask at WHERE at.parentId is NULL ORDER by at.id DESC")
		[analyticsTasks: taskList, analyticsTaskCount: AnalyticsTask.count(), servers: AnalyticsService.SERVERS]
	}

	def processUsers() {
		analyticsService.processUsers()
		render(contentType: "text/json") {['message': "ok" ]}
	}

	def runNext() {
		// This controller action expense params['id'] to be set to a completed task's id.
		def id = params.id.toLong()
		def prevTask = AnalyticsTask.get(id)
		// This means the previous task was completed.
		prevTask.status = AnalyticsTask.COMPLETED
		Utils.save(prevTask)

		// Update the userId of the parent task, so that we can get a glimps of overall progress.
		def parentTask = AnalyticsTask.get(prevTask.parentId)
		if (parentTask && prevTask && prevTask.userId && (parentTask.userId < prevTask.userId)) {
			// The parentTask.userId is the id of the highest userId completed.  (But there might be, lower ids
			//	 still in progress, so check the parentTask.status before concluding the job is all done.)
			parentTask.userId = prevTask.userId
			parentTask.updatedAt = new Date()
			Utils.save(parentTask)
		}

		// So, let's start a new one. If there are more tasks than users, nextTask.startProcessing will
		// not do anything.
		def nextTask = AnalyticsTask.createSibling(prevTask)
		if (nextTask.userId && nextTask.userId > 0) {
			analyticsService.prepareUser(nextTask)
			nextTask.startProcessing()
		}
		render(contentType: "text/json") {['message': "ok", 'userId': nextTask.userId ]}
	}

	def serverList() {
		render(contentType: "text/json") {[servers: AnalyticsService.SERVERS]}
	}

}
