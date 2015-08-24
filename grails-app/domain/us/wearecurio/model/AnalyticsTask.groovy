package us.wearecurio.model
import us.wearecurio.utility.Utils
import groovyx.net.http.*
import groovyx.net.http.Method.*
import groovyx.net.http.ContentType.*
import static groovyx.net.http.ContentType.URLENC
import org.apache.commons.logging.LogFactory
import us.wearecurio.services.AnalyticsService
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray
import java.text.SimpleDateFormat

class AnalyticsTask {
	private static def log = LogFactory.getLog(this)
	
	static AnalyticsService analyticsService

	// Possible values for status.
	//If you make changes, make sure these match the status codes in
	//		src/clojure/analytics/src/.../database.clj
	public static Long UNKNOWN = 1
	public static Long OFF = 2
	public static Long RUNNING = 3
	public static Long READY = 4
	public static Long COMPLETED = 5
	public static Long TERMINATED = 6
	public static Long ERROR = 7

	// Possible values for type.
	public static Long PARENT_OF_COLLECTION = 8
	public static Long CHILD_OF_COLLECTION = 9
	public static Long ONE_OFF = 10

	public static statusMap = [
		"unknown": UNKNOWN,
		"off": OFF,
		"running": RUNNING,
		"ready": READY,
		"completed": COMPLETED,
		"terminated": TERMINATED,
		"error": ERROR]

	public static statusMap2En = [
		1: "UNKNOWN",
		2: "OFF",
		3: "RUNNING",
		4: "READY",
		5: "COMPLETED",
		6: "TERMINATED",
		7: "ERROR",
		8: "PARENT",
		9: "CHILD",
		10: "ONE-OFF"]

	String name
	Long type
	String serverAddress
	Long status
	Long userId
	String error
	String notes
	Date createdAt
	Date updatedAt
	Long parentId
	Long maxNumSubtasks
	
	String toString() {
		return "AnalyticsTask(name:" + name + ", type:" + type + ", serverAddress:" + serverAddress + ", status:" + status + ", userId:" + userId + ", error:" + error \
				+ ", notes:" + notes + ", createdAt:" + createdAt + ", updatedAt:" + updatedAt + ", parentId:" + parentId + ", maximumNumSubtasks:" + maximumNumSubtasks + ")"
	}

	static constraints = {
		name nullable: true
		serverAddress nullable: true
		status nullable: true
		userId nullable: true
		error nullable: true
		notes nullable: true
		createdAt nullable: true
		updatedAt nullable: true
		parentId nullable: true
		type nullable: true
		maxNumSubtasks nullable: true
	}

	static mapping = {
		version false
		parentId index: 'parentIdx'
		createdAt index: 'createdAtIdx'
		userId index: 'userIdx'
		type index: 'typeIdx'
		error type: "text"
		notes type: "text"
	}

	AnalyticsTask() {
		status = READY
		userId = null
		error = null
		notes = null
		createdAt = new Date()
		updatedAt = new Date()
	}

	public statusEn() {
		AnalyticsTask.fetchStatus2En(this.status)
	}

	public typeEn() {
		AnalyticsTask.fetchStatus2En(this.type)
	}

	public static def createChild(serverAddress, parentTask) {
		log.debug "createChild() serverAddress:" + serverAddress + ", parentTask:" + parentTask
		return AnalyticsTask.withTransaction {
			def childTask = new AnalyticsTask()
			childTask.name = "cluster intervals & compute correlation for a single user."
			childTask.serverAddress = serverAddress
			childTask.parentId = parentTask.id
			childTask.type = CHILD_OF_COLLECTION
	
			Utils.save(childTask, true)
			childTask.fetchUserId()
			childTask
		}
	}

	public static def createSibling(childTask) {
		log.debug "createSibling() childTask:" + childTask
		return AnalyticsTask.withTransaction {
			def parentTask = AnalyticsTask.get(childTask.parentId)
			if (AnalyticsTask.numChildren(childTask.parentId) >= parentTask.maxNumSubtasks){
				return null
			}
			def siblingTask = new AnalyticsTask()
			siblingTask.name = childTask.name
			siblingTask.serverAddress = childTask.serverAddress
			siblingTask.parentId = childTask.parentId
			siblingTask.type = CHILD_OF_COLLECTION
	
			Utils.save(siblingTask, true)
			siblingTask.fetchUserId()
			siblingTask
		}
	}

	public static def createParent() {
		log.debug "createParent()"
		return AnalyticsTask.withTransaction {
			def parentTask = new AnalyticsTask()
			parentTask.name = "Collection of analytics jobs: cluster intervals & compute correlation"
			parentTask.serverAddress = null
			parentTask.parentId = null
			parentTask.type = PARENT_OF_COLLECTION
			parentTask.maxNumSubtasks = User.count()
			Utils.save(parentTask, true)
			parentTask
		}
	}
	
	public static def createOneOff(userId, serverAddress) {
		log.debug "createOneOff() userId:" + userId + ", serverAddress:" + serverAddress
		return AnalyticsTask.withTransaction {
			// For tasks that are just run once.
			def task = new AnalyticsTask()
			task.name = "cluster intervals & compute correlation for user ${userId}"
			task.serverAddress = serverAddress
			task.parentId = null
			task.type = ONE_OFF
			task.userId = userId
			Utils.save(task, true)
			task
		}
	}

	def obtainUserId() {
		// Precondition: `this` is a child Task with a parent.	Both should already be
		//	saved to the database and have unique ids.
		def userIds = User.executeQuery('select u.id from User u order by u.id')
		def childIds = AnalyticsTask.executeQuery('select ac.id from AnalyticsTask ac ' +
			"where ac.parentId = :parentId order by ac.id", [parentId:getParentId()])
		def i = childIds.findIndexOf { it == getId() }
		if (i < userIds.size) {
			userIds[i]
		} else {
			return -1
		}
	}

	def fetchUserId() {
		// Use the userId if it was set before. Otherwise, determine it from obtainUserID().
		if (! getUserId()) {
			userId = obtainUserId()
			Utils.save(this, true)
		}
		userId
	}

	def baseUrl() {
		this.serverAddress
	}

	def startUri() {
		"/cluster/user/${fetchUserId()}/run"
	}

	def statusUri() {
		"/status"
	}

	def httpPost(url, path, params) {
		log.debug "httpPost() url:" + url + ", path:" + path + ", params" + params
		def http = new HTTPBuilder(url)
		log.debug "DEBUG: ${url}"
		log.debug "DEBUG: ${path}"
		log.debug "DEBUG: ${params}"

		http.post(path: path, body: params, requestContentType: URLENC) { resp, json ->
			log.debug "returned from post: " + json
			json
		}
	}

	def httpGet(url, path, params) {
		log.debug "httpGet() url:" + url + ", path:" + path + ", params" + params
		def http = new HTTPBuilder(url)
		http.get( path: path, contentType: JSON, query: params) { resp, json ->
			log.debug "returned from get: " + json
			json
		}
	}

	def startProcessing() {
		log.debug "startProcessing()"
		AnalyticsTask.withTransaction {
			def userId = fetchUserId()
			if (userId == null || userId < 0) {
				this.delete(flush:true)
				userId = null
			}
		}

		// Make an http POST request to: "${serverAddress}/cluster/user/${userId}/run"
		// Set the POST variable taskId=${fetchUserId()} so that the process can POST back the
		// status when it finishes, gets terminated or errors out.
		// In Clojure, after that task is done, the Clojure worker/process will send a request
		// to /analytics_task/${id}/next
	 	if (userId)
			httpPost(baseUrl(), startUri(), ['task-id': getId(), 'task-type': getType()])
	}

	def httpGetStatus() {
		log.debug("httpGetStatus()")
		httpGet(baseUrl(), statusUri(), null)
	}

	def markAsCompleted() {
		log.debug "markAsCompleted()"
		AnalyticsTask.withTransaction {
			status = COMPLETED
			Utils.save(this, true)
		}
	}

//--- For checking the status of the analytics processes.

	public static fetchStatus(theState) {
		if (statusMap[theState]) {
			statusMap[theState].intValue()
		} else if (theState.class == Integer) {
			theState
		} else {
			UNKNOWN.intValue()
		}
	}

	public static fetchStatus2En(statusId) {
		if (statusMap2En[statusId]) {
			statusMap2En[statusId]
		} else if (statusId != null && statusId.class == Integer) {
			statusId
		} else {
			"NULL"
		}
	}

	public static makeHttp(baseAddr) {
		def http = new AsyncHTTPBuilder( poolSize:5, uri: baseAddr )
		http.handler.failure = { resp ->
			def i = analyticsService.servers.findIndexOf { it == baseAddr }
			analyticsService.responses.set(i, fetchStatus(resp.status)	)
			resp.status
		}
		http
	}

	public static makeRequest(http, numServers, i) {
		http.get( path: '/status', query: [:] ) { resp, json ->
			analyticsService.responses.set(i, fetchStatus(json.state))
		}
	}

	public static makeServerList(responses) {
		def objArr = []
		analyticsService.servers.eachWithIndex { server, i ->
			def obj =  ['url': server,
									'status': fetchStatus2En(responses.get(i))]
			objArr[i] = obj
		}
		objArr
	}

	public static webServerStatus() {
		if (0 == analyticsService.numWebServersBusy.get()) {
			"READY"
		} else {
			"BUSY"
		}
	}

	public static incBusy() {
		if (null == analyticsService.numWebServersBusy.get()) {
			analyticsService.numWebServersBusy = new AtomicInteger()
			analyticsService.numWebServersBusy.set(0)
		}
		analyticsService.numWebServersBusy.getAndIncrement();
	}

	public static decBusy() {
		analyticsService.numWebServersBusy.getAndDecrement();
	}

	public static pingWebServers() {
		[['url': "WEB", 'status': webServerStatus()]]
	}

	public static pingAnalyticsServers() {
		def clients = []
		analyticsService.servers.eachWithIndex { server, i ->
			clients[i] = makeHttp(server)
		}

		// Initialize response counts and response Array.
		analyticsService.responses = new AtomicIntegerArray(analyticsService.servers.size())
		def requests = []

		// Make simultaneous requests to all servers.
		clients.eachWithIndex { http, i ->
			requests[i] = makeRequest(http, analyticsService.servers.size(), i)
		}

		requests.eachWithIndex { request, i ->
			try {
				request.get()
			} catch(e) {
				if (e.message =~ /refused/) {
					analyticsService.responses.set(i, OFF.intValue())
					//println "\nCould not connect to analytics server.  Maybe it's not running."
					//println "Cause: ${e.getCause()}"
				} else {
					analyticsService.responses.set(i, ERROR.intValue())
					println "\nUnknown error ${e.class}"
					println "Cause: ${e.getCause()}"
					println "Message: ${e.message}"
				}
			}
		}
		def serverList = makeServerList(analyticsService.responses)
		serverList
	}

	public static pingServers() {
		pingWebServers() + pingAnalyticsServers()
	}

	public static allReady(servers) {
		! servers.collect{ it['status'] }.any { it != "READY" }
	}

	public static children(theParentId) {
		AnalyticsTask.findAllByParentId(theParentId, [sort: "id", order: "asc"])
	}

	public numChildrenCompleted() {
		def c = AnalyticsTask.createCriteria()
		def numCompleted = c.count {
			eq("parentId", id)
			eq("status", COMPLETED)
		}
		numCompleted
	}

	public static childrenIncomplete(parentId) {
		if (null == parentId) { return [] }
		parentId = parentId.toLong()
		def c = AnalyticsTask.createCriteria()
		def numCompleted = c.list {
			eq("parentId", parentId)
			ne("status", COMPLETED)
		}
		numCompleted
	}

	public static numChildren(theParentId) {
		def pid = theParentId.toLong()
		AnalyticsTask.countByParentId(pid)
	}

	public percentSuccessful() {
		def percentComputed
		if (type == PARENT_OF_COLLECTION) {
			percentComputed = Math.round(10000 * numChildrenCompleted() / maxNumSubtasks) / 100
		} else {
			return null
		}
		if (percentComputed >= 100 && status != COMPLETED) {
			status = COMPLETED
			Utils.save(this, true)
		}
		percentComputed
	}

	public static getLatestParent() {
		AnalyticsTask.findByParentId(null, [sort: "id", order: "desc", max: 1])
	}

	public static hasStatus(children, s) {
		children.collect { it.status }.any { it == s}
	}

	public static hasError(children) {
		hasStatus(children, ERROR)
	}

	public static hasTerminated(children) {
		hasStatus(children, TERMINATED)
	}

	public static updateParentStatus(parentTask) {
		return AnalyticsTask.withTransaction {
			if (parentTask == null || parentTask.type != PARENT_OF_COLLECTION) { return null }
			def children = AnalyticsTask.children(parentTask.id)
			if (!children || children.size() == 0) { return null }
			if (hasError(children)) {
				parentTask.status = ERROR
			} else if (hasTerminated(children)) {
				parentTask.status = TERMINATED
			} else if (parentTask.percentSuccessful() >= 100) {
				parentTask.status = COMPLETED
			} else {
				parentTask.status = RUNNING
			}
			Utils.save(parentTask, true)
			parentTask.status
		}
	}

	public static updateLatestParent() {
		AnalyticsTask.withTransaction {
			def parentTask = getLatestParent()
			if (parentTask == null) { return null }
			updateParentStatus(parentTask)
		}
	}
}
