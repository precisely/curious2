package us.wearecurio.model
import us.wearecurio.utility.Utils
import groovyx.net.http.*
import static groovyx.net.http.Method.*
import static groovyx.net.http.ContentType.*
import org.apache.commons.logging.LogFactory


class AnalyticsTask {

	private static def log = LogFactory.getLog(this)

	// Possible values for status.
	public static final String RUNNING = "running"
	public static final String NEW = "new"
	public static final String COMPLETED = "completed"
	public static final String TERMINATED = "terminated"
	public static final String ERROR = "error"

	// Possible values for type.
	public static final String PARENT_OF_COLLECTION = "collection-parent"
	public static final String CHILD_OF_COLLECTION = "collection-child"
	public static final String ONE_OFF = "one-off"

	String name
	String type
	String serverAddress
	String status
	Long userId
	String error
	String notes
	Date createdAt
	Date updatedAt
	Long parentId

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
		status = NEW
		userId = null
		error = null
		notes = null
		createdAt = new Date()
		updatedAt = new Date()
	}

	public static def createChild(serverAddress, parentTask) {
		def childTask = new AnalyticsTask()
		childTask.name = "cluster intervals & compute correlation for a single user."
		childTask.serverAddress = serverAddress
		childTask.parentId = parentTask.id
		childTask.type = CHILD_OF_COLLECTION

		Utils.save(childTask)
		childTask.fetchUserId()
		childTask
	}

	public static def createSibling(childTask) {
		def siblingTask = new AnalyticsTask()
		siblingTask.name = childTask.name
		siblingTask.serverAddress = childTask.serverAddress
		siblingTask.parentId = childTask.parentId
		siblingTask.type = CHILD_OF_COLLECTION

		Utils.save(siblingTask)
		siblingTask.fetchUserId()
		siblingTask
	}

	public static def createParent() {
		def parentTask = new AnalyticsTask()
		parentTask.name = "Collection of analytics jobs: cluster intervals & compute correlation"
		parentTask.serverAddress = null
		parentTask.parentId = null
		parentTask.type = PARENT_OF_COLLECTION
		parentTask.maxNumSubtasks = User.count()
		Utils.save(parentTask)
		parentTask
	}

	public static def createOneOff(userId, serverAddress) {
		// For tasks that are just run once.
		def task = new AnalyticsTask()
		task.name = "cluster intervals & compute correlation for user ${userId}"
		task.serverAddress = serverAddress
		task.parentId = null
		task.type = ONE_OFF
		task.userId = userId
		Utils.save(task)
	}

	def obtainUserId() {
		// Precondition: `this` is a child Task with a parent.	Both should already be
		//	saved to the database and have unique ids.
		def userIds = User.executeQuery('select u.id from User u order by u.id')
		def childIds = AnalyticsTask.executeQuery('select ac.id from AnalyticsTask ac ' +
			"where ac.parentId = ? order by ac.id", [getParentId()])
		println "childIds ${childIds}"
		println "userIds ${userIds}"
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
			Utils.save(this)
		}
		userId
	}

	def baseUrl() {
		"http://${getServerAddress()}/"
	}

	def startUri() {
		"/cluster/user/${fetchUserId()}/run"
	}

	def statusUri() {
		"/cluster/status"
	}

	def httpPost(url, path, params) {
		def http = new HTTPBuilder(url)
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
		def uid = fetchUserId()
		if (uid == null || uid < 0) {
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
		Utils.save(this)
	}

}
