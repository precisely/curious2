package us.wearecurio.controller

import static org.springframework.http.HttpStatus.*
import grails.converters.*
import groovy.json.*
import grails.gorm.DetachedCriteria
import grails.gsp.PageRenderer
import grails.converters.JSON

import org.codehaus.groovy.grails.web.json.JSONObject
import org.joda.time.DateTime
import org.springframework.http.HttpStatus

import us.wearecurio.exceptions.*
import us.wearecurio.model.*
import us.wearecurio.services.DataService
import us.wearecurio.services.SearchService
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.JawboneService
import us.wearecurio.services.MovesDataService
import us.wearecurio.services.Twenty3AndMeDataService
import us.wearecurio.services.TwitterDataService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.support.EntryStats
import us.wearecurio.thirdparty.AuthenticationRequiredException
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.utility.Utils

class HomeController extends DataController {

	static allowedMethods = [notifyJawbone: "POST", notifyfitbit: "POST"]

	TwitterDataService twitterDataService
	WithingsDataService withingsDataService
	FitBitDataService fitBitDataService
	JawboneService jawboneService
	MovesDataService movesDataService
	def jawboneUpDataService
	def oauthService
	SearchService searchService
	PageRenderer groovyPageRenderer
	Twenty3AndMeDataService twenty3AndMeDataService

	static debug(str) {
		log.debug(str)
	}
	
	def HomeController() {
		debug "HomeController()"
	}
	
	def causeexception() throws AuthenticationRequiredException {
		debug "HomeController.causeexception() params:" + params
		
		throw new AuthenticationRequiredException("test only please ignore")
	}
		
	def registerwithings() throws AuthenticationRequiredException {
		debug "HomeController.registerwithings() params:" + params

		User user = sessionUser()
		if (!user) {
			debug "auth failure"
			return
		}
		Long userId = user.id
		
		debug "userId: $userId"
		session.deniedURI = toUrl(controller: 'home', action: 'userpreferences', params: [userId: userId])
		
		Map result = [:]
		
		try {
			result = withingsDataService.subscribe(userId)
		} catch (MissingOAuthAccountException e) {
			throw new AuthenticationRequiredException("withings")
		} catch (InvalidAccessTokenException e) {
			e = e
			throw new AuthenticationRequiredException("withings")
		}

		flash.args = ["Withings"]

		if (result.success) {
			OAuthAccount account = result.account
			if (!account.lastPolled) {	// Check to see if first time subscription.
				log.info "Setting notification to get previous data for account: $account"
				withingsDataService.saveNotificationForPreviousData(account)
			}
			flash.message = "thirdparty.subscribe.success.message"
		} else {
			debug "Failed to subscribe: " + (result.message ?: "")
			flash.message = "thirdparty.subscribe.failure.message"
			flash.args << result.message ?: ""
		}

		redirect(url: session.deniedURI)
	}

	def unregisterwithings() {
		debug "HomeController.unregisterwithings() params:" + params
		User user = sessionUser()
		Long userId = user.id
		Map result = [:]

		try {
			result = withingsDataService.unsubscribe(userId)
		} catch (InvalidAccessTokenException e) {
			throw new AuthenticationRequiredException("withings")
		} catch (MissingOAuthAccountException e) {
			result = [success: false, message: "No subscription found."]
		}

		flash.args = ["Withings"]

		if (result.success) {
			debug "Succeeded in unsubscribing"
			flash.message = "thirdparty.unsubscribe.success.message"
		} else {
			debug "Failed to unsubscribe: " + (result.message ?: "")
			flash.message = "thirdparty.unsubscribe.failure.message"
			flash.args << result.message ?: ""
		}
		redirect (url: toUrl(controller: 'home', action: 'userpreferences', params: [userId: sessionUser().id]))
	}

	def register23andme() {
		debug "HomeController.register23andme() params:" + params
		User user = sessionUser()
		Long userId = user.id
		session.deniedURI = toUrl(controller: 'home', action: 'userpreferences', params: [userId: userId])

		Map result = [:]

		try {
			result = twenty3AndMeDataService.storeGenomesData(userId)
		} catch (MissingOAuthAccountException e) {
			throw new AuthenticationRequiredException("Twenty3AndMe")
		} catch (InvalidAccessTokenException e) {
			throw new AuthenticationRequiredException("Twenty3AndMe")
		}

		if (result.success) {
			debug "Succeeded in importing"
			flash.message = message(code: "twenty3andme.import.success.message")
		} else {
			debug "Failed to import"
			flash.message = message(code: "twenty3andme.import.failure.message")
		}

		redirect(url: session.deniedURI)
	}

	def notifywithings() {
		debug "HomeController.notifywithings() from IP: [$request.remoteAddr] with params:" + params

		withingsDataService.notificationHandler(new JSONObject(params).toString())

		renderStringGet('success')
	}

	def registermoves() {
		debug "HomeController.registermoves() params:" + params
		User user = sessionUser()
		Long userId = user.id
		session.deniedURI = toUrl(controller: 'home', action: 'userpreferences', params: [userId: sessionUser().id])

		Map result = [:]

		try {
			result = movesDataService.subscribe(userId)
		} catch (InvalidAccessTokenException e) {
			throw new AuthenticationRequiredException("moves")
		} catch (MissingOAuthAccountException e) {
			throw new AuthenticationRequiredException("moves")
		}

		flash.args = ["Moves"]
		if (result.success) {
			debug "Succeeded in subscribing"
			flash.message = "thirdparty.subscribe.success.message"
		} else {
			debug "Failed to subscribe"
			flash.message = "thirdparty.subscribe.failure.message"
		}
		redirect(url: session.deniedURI)
	}

	def unregistermoves() {
		debug "HomeController.unregistermoves() params:" + params
		User user = sessionUser()
		Long userId = user.id
		Map result = [:]

		try {
			result = movesDataService.unsubscribe(userId)
		} catch (InvalidAccessTokenException e) {
			throw new AuthenticationRequiredException("moves")
		} catch (MissingOAuthAccountException e) {
			result = [success: false, message: "No subscription found."]
		}

		flash.args = ["Moves"]
		if (result.success) {
			debug "Succeeded in unsubscribing"
			flash.message = "thirdparty.unsubscribe.success.message"
		} else {
			debug "Failure while unsubscribing" + result.message
			flash.message = "thirdparty.unsubscribe.failure.message"
			flash.args << result.message ?: ""
		}
		redirect (url: toUrl(controller: 'home', action: 'userpreferences', params: [userId: sessionUser().id]))
	}

	def registerJawboneUp() {
		debug "HomeController.registerJawboneUp() params:" + params
		User user = sessionUser()
		Long userId = user.id
		session.deniedURI = toUrl(controller: "home", action: "userpreferences", params: [userId: userId])

		Map result = [:]

		try {
			result = jawboneUpDataService.subscribe(userId)
		} catch (InvalidAccessTokenException e) {
			throw new AuthenticationRequiredException("jawboneup")
		} catch (MissingOAuthAccountException e) {
			throw new AuthenticationRequiredException("jawboneup")
		}

		flash.args = ["JawboneUp"]
		if (result.success) {
			debug "Succeeded in subscribing"
			flash.message = "thirdparty.subscribe.success.message"
		} else {
			debug "Failed to subscribe"
			flash.message = "thirdparty.subscribe.failure.message"
		}

		redirect(url: session.deniedURI)
	}

	def unregisterJawboneUp() {
		debug "unregisterJawboneUp() params: $params"
		User user = sessionUser()
		Long userId = user.id
		Map result = [:]

		try {
			result = jawboneUpDataService.unsubscribe(userId)
		} catch (InvalidAccessTokenException e) {
			throw new AuthenticationRequiredException("jawboneup")
		} catch (MissingOAuthAccountException e) {
			result = [success: false, message: "No subscription found."]
		}

		flash.args = ["JawboneUp"]
		if (result.success) {
			debug "Succeeded in unsubscribing"
			flash.message = "thirdparty.unsubscribe.success.message"
		} else {
			debug "Failure while unsubscribing" + result.message
			flash.message = "thirdparty.unsubscribe.failure.message"
			flash.args << result.message ?: ""
		}
		redirect (url: toUrl(controller: 'home', action: 'userpreferences', params: [userId: sessionUser().id]))
	}

	def polldevices() {
		debug "HomeController.polldevices() request:" + request
		
		User user = sessionUser()
		
		if (user == null) {
			debug "auth failure"
			return
		}

		DataService.pollAllForUserId(user.getId())
		
		redirect(url:toUrl(controller:'home', action:'index'))
	}

	def registerfitbit() {
		debug "HomeController.registerfitbit() params:" + params
		User user = sessionUser()
		
		if (user == null) {
			debug "auth failure"
			return
		}
		Long userId = user.id
		
		debug "userId: $userId"
		session.deniedURI = toUrl(controller: 'home', action: 'userpreferences', params: [userId: userId])
		
		Map result = [:]

		try {
			result = fitBitDataService.subscribe(userId)
		} catch (MissingOAuthAccountException e) {
			throw new AuthenticationRequiredException("fitbit")
		} catch (InvalidAccessTokenException e) {
			throw new AuthenticationRequiredException("fitbit")
		}

		flash.args = ["FitBit"]
		flash.args << result.message ? ", " + result.message : ""
		if(result.success) {
			debug "Succeeded in subscribing"
			flash.message = "thirdparty.subscribe.success.message"
		} else {
			debug "Failure in unsubscribing:" + result.message
			flash.message = "thirdparty.subscribe.failure.message"
		}
		redirect(url: session.deniedURI)
	}

	def unregisterfitbit() {
		debug "HomeController.unregisterfitbit() params:" + params
		User user = sessionUser()
		if (user == null) {
			debug "auth failure"
			return
		}
		Long userId = user.id

		Map result = [:]

		try {
			result = fitBitDataService.unsubscribe(userId)
		} catch (InvalidAccessTokenException e) {
			throw new AuthenticationRequiredException("fitbit")
		} catch (MissingOAuthAccountException e) {
			result = [success: false, message: "No subscription found."]
		}

		flash.args = ["FitBit"]
		if (result.success) {
			debug "Succeeded in unsubscribing"
			flash.message = "thirdparty.unsubscribe.success.message"
		} else {
			debug "Failure in unsubscribing:" + result.message
			flash.message = "thirdparty.unsubscribe.failure.message"
			flash.args << result.message ?: ""
		}

		redirect (url: toUrl(controller: 'home', action: 'userpreferences', params: [userId: sessionUser().id]))
	}

	/**
	 * FitBit Subscriber Endpoint
	 */
	def notifyfitbit() {
		// Fitbit now sends notification data as request body
		String notificationData = request.JSON.toString()
		debug "HomeController.notifyfitbit() from IP: [$request.remoteAddr] with params: $params and data: $notificationData"
		
		fitBitDataService.notificationHandler(notificationData)
		render status: 204
		return
	}

	def notifyJawbone() {
		def requestBodyData = request.JSON
		def requestFileData = request.getFile("file")?.inputStream?.text

		debug "notifyJawbone() from IP: [$request.remoteAddr] params:" + params + ", body: $requestBodyData & file: $requestFileData"

		jawboneUpDataService.notificationHandler(requestBodyData?.file ?: requestFileData)

		renderStringGet('success')
	}
	
	def registertwitter() {
		debug "HomeController.registertwitter() request:" + request
		
		redirect(url:twitterDataService.twitterAuthorizationURL(toUrl(controller:'home', action:'doregistertwitter')))
	}

	def doregistertwitter() {
		debug "HomeController.doregistertwitter() params:" + params
		
		User user = sessionUser()
		
		if (user == null) {
			debug "auth failure"
			return
		}
		
		debug "userId:" + user.getId()

		def twitterUsername = null
		try {
			twitterUsername = twitterDataService.usernameFromAuthTokens(params.oauth_token,
					params.oauth_verifier)
		} catch (Throwable t) {
			t.printStackTrace()
		}
		if (twitterUsername == null) {
			debug "Failed to authorize Twitter"
			flash.message = "Failed to authorize Twitter account"
		} else {
			debug "Authorized Twitter account: " + twitterUsername
			user.setTwitterAccountName(twitterUsername)
		}
		render(view:"/home/userpreferences",
				model:[precontroller:flash.precontroller ?: 'home', preaction:flash.preaction ?: 'index', user:user, templateVer:urlService.template(request)])
	}
	
	static processPrefs(prefs) {
		def p = [:]
		
		for (param in prefs) {
			def k = param.key
			def v = param.value
			
			int i = k.indexOf("_profile_")
			
			if (i > 0)
				p[k.substring(0, i)] = v
			else
				p[k] = v
		}
			
		return p
	}

	def userpreferences() {
		debug "HomeController.userpreferences() params:" + params
		
		User user = userFromIdStr(params.userId)
		
		if (user == null) {
			redirect(url:toUrl(action:'index'))
			return
		}
		
		debug "Trying to edit user preferences for:" + user
		
		render(view:"/home/userpreferences",
				model:[precontroller:flash.precontroller ?: 'home', preaction:flash.preaction ?: 'index', user:user,
					prefs:user.getPreferences(), templateVer:urlService.template(request)])
	}

	def doupdateuserpreferences() {
		debug "HomeController.doupdateuserpreferences() params:" + params
		
		User user = userFromIdStr(params.userId)

		if (user == null) {
			flash.message = "Must be logged in"
			redirect(url:toUrl(action:'upload'))
			return
		}
		
		def p = processPrefs(params)

		if (p.twitterDefaultToNow != 'on')
			p.twitterDefaultToNow = 'off';
			
		if (p.password != null && p.password.length() > 0) {
			if (!user.checkPassword(p.oldPassword)) {
				flash.message = "Error updating user preferences: old password does not match"
				log.warn "Error updating user preferences: old password does not match"
				render(view:"/home/userpreferences",
						model:[precontroller:flash.precontroller ?: 'home', preaction:flash.preaction ?: 'index', user:user,
							prefs:user.getPreferences(), templateVer:urlService.template(request)])
				return
			}
		}

		user.update(p)

		Utils.save(user, true)
		
		if (!user.validate()) {
			flash.message = "Error updating user preferences: missing field or email already in use"
			log.warn "Error updating user preferences: $user.errors"
			render(view:"/home/userpreferences",
					model:[precontroller:flash.precontroller ?: 'home', preaction:flash.preaction ?: 'index', user:user,
						prefs:user.getPreferences(), templateVer:urlService.template(request)])
		} else {
			flash.message = "User preferences updated"
			redirect(url:toUrl(controller:p.controller, action:p.preaction))
		}
	}

	def doUpload() {
		debug "HomeController.doUpload() params:" + params
		
		def user = sessionUser()

		if (user == null) {
			flash.message = "Must be logged in"
			redirect(url:toUrl(action:'upload'))
			return
		}

		debug "CSV type: " + params.csvtype
		def f = request.getFile('csvFile')
		if (!f.empty) {
			def csvIn = f.getInputStream()
			if (params.csvtype == "jawbone") {
				jawboneService.parseJawboneCSV(csvIn, user.getId())
			} else {
				doParseCSVDown(csvIn, user.getId())
			}
			redirect(url:toUrl(action:'index'))
		}
		else {
			flash.message = 'file cannot be empty'
			redirect(url:toUrl(action:'upload'))
		}
	}

	def index() {
		debug "HomeController.index()"
		def user = sessionUser()
		[prefs:user.getPreferences(), showTime:params.showTime?:0, templateVer:urlService.template(request)]
	}

	def load() {
		debug "HomeController.load()"
		def user = sessionUser()
		[prefs:user.getPreferences(), templateVer:urlService.template(request)]
	}

	def graph() {
		debug "HomeController.graph()"
		def user = sessionUser()
		[prefs:user.getPreferences(), templateVer:urlService.template(request)]
	}

	def graphSignals() {
		debug "HomeController.graphSignals()" + params.description1 + " - " + params.description2
		def user = sessionUser()
		[prefs:user.getPreferences(), templateVer:urlService.template(request)]
	}

	def upload() {
		debug "HomeController.upload()"
		def user = sessionUser()
		[prefs:user.getPreferences(), templateVer:urlService.template(request)]
	}

	def download() {
		debug "HomeController.download()"
		
		def user = sessionUser()
		
		if (user == null) {
			debug "auth failure"
			flash.message = "Must be logged in"
			redirect(url:toUrl(action:'index'))
			return
		}

		response.setHeader "Content-disposition", "attachment; filename=export.csv"
			response.contentType = 'text/csv'
		
		doExportCSVAnalysis(response.outputStream, user)
		
				response.outputStream.flush()
	}

	def termsofservice() {
		debug "HomeController.termsofservice()"
		[templateVer:urlService.template(request)]
	}

	def termsofservice_home() {
		debug "HomeController.termsofservice_home()"
		[templateVer:urlService.template(request)]
	}

	def viewgraph() {
		debug "HomeController.viewgraph()"
		
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			flash.message = "Must be logged in"
			redirect(url:toUrl(action:'index'))
			return
		}
		
		render(view:"/home/graph", model:[plotDataId:params.plotDataId, templateVer:urlService.template(request)])
	}
	
	def homepage() {
		render(view:"/home/homepage")
	}

	def signals() {
		render(view: "/home/signals")
	}

	def community(String discussionHash, boolean unpublish, boolean publish) {
		redirect(url:toUrl(action:'social'))
	}
	
	def feed(String discussionHash, boolean unpublish, boolean publish) {
		redirect(url:toUrl(action:'social'))
	}
	
	def social(String discussionHash, Long userId, boolean unpublish, boolean publish, boolean listSprint, int max, int offset) {
		debug "HomeController.social(): $params"
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			flash.message = "Must be logged in"
			redirect(url:toUrl(action:'index'))
			return
		} else if (userId && userId != user.id) {
			debug "authorization failure"
			flash.message = "You don't have permission to read these feeds."
			redirect(url:toUrl(action:'index'))
			return
		}

		Map model
		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0

		if (listSprint) {
			// This is to get list of sprints the user belongs to or is admin of
			List<Sprint> sprintList = Sprint.getSprintListForUser(sessionUser().id, params.max, params.offset)
			if (!sprintList) {
				renderJSONGet([listItems: false])
				return
			}
			model = [sprintList: sprintList]
			renderJSONGet([listItems: model])
		} else {
			if (discussionHash) {
				Discussion discussion = Discussion.findByHash(discussionHash)
				if (!discussion) {
					debug "no discussion for discussionHash " + discussionHash
					flash.message = "No discussion found"
					return
				}
				
				if (unpublish) {
					if (UserGroup.canAdminDiscussion(user, discussion)) {
						discussion.setIsPublic(false)
						Utils.save(discussion, true)
					}
				}
				if (publish) {
					if (UserGroup.canAdminDiscussion(user, discussion)) {
						discussion.setIsPublic(true)
						Utils.save(discussion, true)
					}
				}
			}
			
			List groupMemberships = UserGroup.getGroupsForReader(user)
			List associatedGroups = UserGroup.getGroupsForWriter(user)
			String groupName
			String groupFullname = "Social Activity"
					
			groupMemberships.each { group ->
				if (group[0]?.name.equals(params.userGroupNames)) {
					groupFullname = group[0].fullName ?: group[0].name
							groupName = group[0].name
				}
			}

			log.debug("HomeController.social: User has read memberships for :" + groupMemberships.dump())

			model = [
				prefs: user.getPreferences(), 
				userId: user.getId(), 
				templateVer: urlService.template(request), 
				offset: offset,
				groupMemberships: groupMemberships, 
				associatedGroups: associatedGroups, 
				groupName: groupName, 
				groupFullname: groupFullname
				]
		}

		model
	}

	def shareDiscussion(String discussionHash) {
		User currentUserInstance = sessionUser()

		Discussion discussionInstance = Discussion.findByHash(discussionHash)
		if (!discussionInstance) {
			debug "DiscussionId not found: " + discussionHash
			renderJSONPost([message: "Discussion not found."], NOT_FOUND)
			return
		}

		if (!UserGroup.canAdminDiscussion(currentUserInstance, discussionInstance)) {
			debug "DiscussionId not found: " + discussionHash
			renderJSONPost([message: "You don't have admin rights to modify share preference for this discussion"], UNAUTHORIZED)
			return
		}

		List shareOptions = params.shareOptions ? params.shareOptions.tokenize(",") : []
		log.debug "Change sharing option for $discussionInstance by user $currentUserInstance with $shareOptions"

		List associatedGroups = UserGroup.getGroupsForWriter(currentUserInstance)

		/*
		 *	Share option will contain ids of UserGroup to share discussion to,
		 *	and "isPublic" value if user wants to make Discussion visible to the world.
		 */
		boolean isPublic = shareOptions.remove("isPublic")
		shareOptions = shareOptions*.toLong()

		List alreadySharedGroups = []

		associatedGroups.each { userGroup ->
			UserGroup userGroupInstance = UserGroup.get(userGroup["id"])

			// If user selected to share to this group
			if (shareOptions.contains(userGroup["id"])) {
				// If discussion is not shared to this group, then only share it.
				if (!userGroupInstance.hasDiscussion(discussionInstance)) {
					userGroupInstance.addDiscussion(discussionInstance)
				}
			} else {
				userGroupInstance.removeDiscussion(discussionInstance)
			}
		}

		discussionInstance.isPublic = isPublic
		Utils.save(discussionInstance, true)

		renderJSONPost([message: "Your share preferences for this discussion saved successfully."])
	}

	/**
	 * TODO This action is going to be removed in the upcoming pull request that will use JSON  data to render a
	 * particular discussion. Do not change or alter code here instead use the DiscussionController for this.
	 */
	@Deprecated
	def discuss(String discussionHash, Long plotIdMessage) {
		/*
		 * New discussion format uses discussionHash to identify discussions
		 * 
		 * plotIdMessage contains the plotId of the plot to be added to the discussion
		 * 
		 * Should support both modes
		 */
		debug "HomeController.discuss() $params"

		def user = sessionUser()

		UserGroup group = params.group ? UserGroup.lookup(params.group) : UserGroup.getDefaultGroupForUser(user)

		Discussion discussion

		if (discussionHash) {
			discussion = Discussion.findByHash(discussionHash)
		}

		if (discussion == null && plotIdMessage) {
			def name = params.name ?: "New question or discussion title?"
			if (plotIdMessage != null) {
				def plot = PlotData.get(plotIdMessage)
				if (plot)
					name = plot.getName()
			}
			if (group) {
				if (!group.hasWriter(user)) {
					flash.message = "Failed to create new discussion topic: can't post to this group"
					redirect(url:toUrl(action:'index'))
					return
				}
			}
			discussion = Discussion.create(user, name)

			if (discussion) {
				if (group) group.addDiscussion(discussion)
				DiscussionPost.createComment(params.message, user, discussion, plotIdMessage, params)
			}
			redirect(url: toUrl(action: "discuss", params: [discussionHash: discussion.hash]))
			return
		}

		if (!discussion) {
			debug "DiscussionHash not found: " + discussionHash
			flash.message = "That discussion topic no longer exists."
			redirect(url: toUrl(action: 'social'))
			return
		}

		if (true) {		// TODO remove this if condition and fix indentation
			params.max = params.max ?: 5
			params.offset = params.offset ?: 0

			// see duplicated code in DiscussionController.createTopic
			// edits here should be duplicated there
			Map model = discussion.getJSONModel(params)

			model = model << [notLoggedIn: user ? false : true, userId: user?.getId(),
					username: user ? user.getUsername() : '(anonymous)', isAdmin: UserGroup.canAdminDiscussion(user, discussion),
					templateVer: urlService.template(request), discussionHash: discussion.hash]
			log.debug "overall model: ${model.dump()}"

			if (user == null) {
				model.put("associatedGroups", []) // public discussion
			} else {
				List associatedGroups = UserGroup.getGroupsForWriter(user)
				List alreadySharedGroups = [], otherGroups = []
	
				associatedGroups.each { userGroup ->
					if (UserGroup.hasDiscussion(userGroup["id"], discussion.id)) {
						alreadySharedGroups << userGroup.plus([shared: true])
					} else {
						otherGroups << userGroup
					}
				}
				associatedGroups = alreadySharedGroups.sort { it.name }
				associatedGroups.addAll(otherGroups.sort { it.name })
				model.put("associatedGroups", associatedGroups)
			}

			render(view: "/home/discuss", model: model)
		}
	}
	
	def lgmd2iproject() {
		def model = []
		render(view:"/home/lgmd2iproject", model:model)
	}

	def sprint() {
		log.debug "Hash: $params.id"
		Sprint sprintInstance = Sprint.findByHash(params.id)
		if (!sprintInstance?.userId) {
			debug "SprintId not found: $params.id"
			flash.message = g.message(code: "sprint.not.exist")
			redirect(url: toUrl(action:'social'))
			return
		} else if ((sprintInstance.visibility == Model.Visibility.PRIVATE) && 
					!sprintInstance.hasMember(sessionUser().id) && !sprintInstance.hasAdmin(sessionUser().id)) {
			debug "Permission denied for user: ${sessionUser()} to see sprint: ${sprintInstance}"
			flash.message = g.message(code: "not.permitted.to.see.sprint")
			redirect(url: toUrl(action:'social'))
			return
		}
		
		List<Map> entries = Entry.findAllByUserId(sprintInstance.virtualUserId)*.getJSONDesc()
		List<User> participantsList = sprintInstance.getParticipants(10, 0)
		List<Map> participants = participantsList*.getJSONShortDesc()
		Map sprintDiscussions = searchService.getDiscussionsList(sessionUser(), 0, 5, [sprintInstance.virtualGroupId])

		log.debug "Sprint discussions: ${sprintDiscussions.dump()}"

		render(view: "/home/sprint", model: [sprintInstance: sprintInstance, entries: entries, discussions: (sprintDiscussions as JSON).toString(),
			participants : participants , user: sessionUser(), virtualGroupName: sprintInstance.fetchUserGroup().name])
	}
}
