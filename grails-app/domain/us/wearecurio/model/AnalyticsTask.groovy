package us.wearecurio.model
import us.wearecurio.utility.Utils
import groovyx.net.http.*
import groovyx.net.http.Method.*
import groovyx.net.http.ContentType.*
import static groovyx.net.http.ContentType.URLENC
import org.apache.commons.logging.LogFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray
import java.text.SimpleDateFormat

class AnalyticsTask {
	public static def SERVERS = [
		'http://127.0.0.1:8090',
		'http://127.0.0.1:8091'
	]

	private static def log = LogFactory.getLog(this)

	// For counting the number of ready analytics servers.
	//	Need to use thread-safe data structures, because AsyncHTTPBuilder uses
	//	threaads to make the HTTP requests asynchronous.
	public static AtomicInteger numWebServersBusy = new AtomicInteger(0)
	public static AtomicIntegerArray responses = new AtomicIntegerArray(SERVERS.size())

	// Possible values for status.
	//If you make changes, make sure these match the status codes in
	//		src/clojure/analytics/src/.../database.clj
	public static int UNKNOWN = 1
	public static int OFF = 2
	public static int RUNNING = 3
	public static int READY = 4
	public static int COMPLETED = 5
	public static int TERMINATED = 6
	public static int ERROR = 7

	// Possible values for type.
	public static int PARENT_OF_COLLECTION = 8
	public static int CHILD_OF_COLLECTION = 9
	public static int ONE_OFF = 10

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
	int type
	String serverAddress
	int status
	Long userId
	String error
	String notes
	Date createdAt
	Date updatedAt
	Long parentId
	Long maxNumSubtasks

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
			"where ac.parentId = ? order by ac.id", [getParentId()])
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
		def http = new HTTPBuilder(url)
		log.debug "DEBUG: ${url}"
		log.debug "DEBUG: ${path}"
		log.debug "DEBUG: ${params}"

		http.post(path: path, body: params, requestContentType: URLENC) { resp, json ->
			System.out << json
			json
		}
	}

	def httpGet(url, path, params) {
		def http = new HTTPBuilder(url)
		http.get( path: path, contentType: JSON, query: params) { resp, json ->
			System.out << json
			json
		}
	}

	def startProcessing() {
		def userId = fetchUserId()
		if (userId == null || userId < 0) {
			this.delete()
			return null
		}

		// Make an http POST request to: "${serverAddress}/cluster/user/${userId}/run"
		// Set the POST variable taskId=${fetchUserId()} so that the process can POST back the
		// status when it finishes, gets terminated or errors out.
		// In Clojure, after that task is done, the Clojure worker/process will send a request
		// to /analytics_task/${id}/next
		httpPost(baseUrl(), startUri(), ['task-id': getId(), 'task-type': getType()])
	}

	def httpGetStatus() {
		httpGet(baseUrl(), statusUri(), null)
		println "\n\nDONE getStatus"
	}

	def markAsCompleted() {
		status = COMPLETED
		Utils.save(this, true)
	}

//--- For checking the status of the analytics processes.

	public static fetchStatus(theState) {
		if (statusMap[theState]) {
			statusMap[theState]
		} else if (theState.class == Integer) {
			theState
		} else {
			UNKNOWN
		}
	}

	public static fetchStatus2En(statusId) {
		if (statusMap2En[statusId]) {
			statusMap2En[statusId]
		} else if (statusId.class == Integer) {
			statusId
		} else {
			"NULL"
		}
	}

	public static makeHttp(baseAddr) {
		def http = new AsyncHTTPBuilder( poolSize:5, uri: baseAddr )
		http.handler.failure = { resp ->
			def i = SERVERS.findIndexOf { it == baseAddr }
			responses.set(i, fetchStatus(resp.status)	)
			resp.status
		}
		http
	}

	public static makeRequest(http, numServers, i) {
		http.get( path: '/status', query: [:] ) { resp, json ->
			responses.set(i, fetchStatus(json.state))
		}
	}

	public static makeServerList(responses) {
		def objArr = []
		SERVERS.eachWithIndex { server, i ->
			def obj =  ['url': server,
									'status': fetchStatus2En(responses.get(i))]
			objArr[i] = obj
		}
		objArr
	}

	public static webServerStatus() {
		if (0 == AnalyticsTask.numWebServersBusy.get()) {
			"READY"
		} else {
			"BUSY"
		}
	}

	public static incBusy() {
		if (null == numWebServersBusy.get()) {
			numWebServersBusy = new AtomicInteger()
			numWebServersBusy.set(0)
		}
		numWebServersBusy.getAndIncrement();
	}

	public static decBusy() {
		numWebServersBusy.getAndDecrement();
	}

	public static pingWebServers() {
		[['url': "WEB", 'status': webServerStatus()]]
	}

	public static pingAnalyticsServers() {
		def clients = []
		SERVERS.eachWithIndex { server, i ->
			clients[i] = makeHttp(server)
		}

		// Initialize response counts and response Array.
		responses = new AtomicIntegerArray(SERVERS.size())
		def requests = []

		// Make simultaneous requests to all servers.
		clients.eachWithIndex { http, i ->
			requests[i] = makeRequest(http, SERVERS.size(), i)
		}

		// Rethrow caught errors if any.
		requests.eachWithIndex { request, i ->
			try {
				// Throw any caught errors.
				request.get()
			} catch(e) {
				if (e.message =~ /refused/) {
					responses.set(i, OFF)
					//println "\nCould not connect to analytics server.  Maybe it's not running."
					//println "Cause: ${e.getCause()}"
				} else {
					responses.set(i, ERROR)
					println "\nUnknown error ${e.class}"
					println "Cause: ${e.getCause()}"
					println "Message: ${e.message}"
				}
			}
		}
		def serverList = makeServerList(responses)
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
