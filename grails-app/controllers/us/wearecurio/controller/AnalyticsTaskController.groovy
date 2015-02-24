package us.wearecurio.controller

import grails.converters.JSON
import us.wearecurio.model.AnalyticsTask
import us.wearecurio.utility.Utils
import us.wearecurio.server.BackgroundTask
import us.wearecurio.services.AnalyticsService

class AnalyticsTaskController {

	def securityService
	def analyticsService

	static allowedMethods = [index: "GET", processUsers: "POST", runNext: "POST", pingServers: "GET", children: "POST", parentSummary: "POST"]

	def index() {
		def parentTaskList = AnalyticsTask.findAll("FROM AnalyticsTask at WHERE at.parentId is NULL ORDER by at.id DESC")

		[analyticsTasks: parentTaskList, analyticsTaskCount: AnalyticsTask.count(), servers: AnalyticsTask.pingServers()]
	}

	def processUsers() {
		def message = "servers busy"
		def servers = AnalyticsTask.pingServers()
		if (AnalyticsTask.allReady(servers)){
			message = "ok"
			BackgroundTask.launch {
				analyticsService.processUsers()
			}
		}
		render(contentType: "text/json") { ['message': message, 'servers': servers] }
	}

	def runNext() {
		// This controller action expense params['id'] to be set to a completed task's id.
		def id = params.id.toLong()
		def prevTask = AnalyticsTask.get(id)
		// This means the previous task was completed.
		prevTask.status = AnalyticsTask.COMPLETED
		Utils.save(prevTask, true)

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
			analyticsService.processOneOfManyUsers(nextTask)
			userId = nextTask.userId
		}
		render(contentType: "text/json") {['message': "ok", 'userId': userId ]}
	}

	def children() {
		println "receved id: ${params.int('id')}"
		render(contentType: "text/json") {AnalyticsTask.children(params.int('id'))}
	}

	def pingServers() {
		render(contentType: "text/json") { AnalyticsTask.pingServers() }
	}

	def getLatestParent() {
		AnalyticsTask.updateLatestParent()
		if (null == AnalyticsTask.getLatestParent()) {
			render(contentType: "text/json") { ['message': "No tasks to display."] }
		} else {
			render(contentType: "text/json") { AnalyticsTask.getLatestParent() }
		}
	}

	def getTasks() {
		def paramsTaskIds = params['taskIds']
		def taskList = []
		if (paramsTaskIds) {
			def list = []
			List<Long> tids = JSON.parse(params.taskIds)
			tids.each { tid ->
				def parentTask = AnalyticsTask.get(tid)
				AnalyticsTask.updateParentStatus(parentTask)
				list.push tid
			}
			taskList = AnalyticsTask.getAll(list)
		}
		render(contentType: "text/json") { taskList }
	}

	def rerunParent() {
		def message = ""
		def parentId = params.int('parentId').toLong()
		def parentTask = AnalyticsTask.get(parentId)
		AnalyticsTask.updateParentStatus(parentTask)
		def userIds = AnalyticsTask.childrenIncomplete(parentId)
		def servers = AnalyticsTask.pingServers()
		def percent = parentTask.percentSuccessful()
		def ready = AnalyticsTask.allReady(servers)
		if (ready && percent < 100){
			userIds = analyticsService.processUsers(parentId)
			message = "ok"
		} else if (percent >= 100) {
			message = "already completed"
		} else if (!ready) {
			message = "servers not ready"
		}
		render(contentType: "text/json") { ['message': message, 'userIds': userIds ]}
	}
}
