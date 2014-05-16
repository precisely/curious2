package us.wearecurio.controller

import grails.converters.*

import org.codehaus.groovy.grails.web.json.JSONObject

import us.wearecurio.exceptions.*
import us.wearecurio.model.*
import us.wearecurio.services.DataService
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.JawboneService
import us.wearecurio.services.MovesDataService
import us.wearecurio.services.Twenty3AndMeDataService
import us.wearecurio.services.TwitterDataService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.thirdparty.AuthenticationRequiredException
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.utility.Utils

class HomeController extends DataController {
	
	TwitterDataService twitterDataService
	WithingsDataService withingsDataService
	FitBitDataService fitBitDataService
	JawboneService jawboneService
	MovesDataService movesDataService
	def oauthService
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
			throw new AuthenticationRequiredException("withings")
		}

		if (result.success) {
			OAuthAccount account = result.account
			if (!account.lastPolled) {	// Check to see if first time subscription.
				log.info "Setting notification to get previous data for account: $account"
				withingsDataService.saveNotificationForPreviousData(account)
			}
			flash.message = g.message(code: "withings.subscribe.success.message")
		} else {
			debug "Failed to subscribe: " + (result.message ?: "")
			flash.message = g.message(code: "withings.subscribe.failure.message", args: [result.message ?: ""])
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

		if (result.success) {
			debug "Succeeded in unsubscribing"
			flash.message = g.message(code: "withings.unsubscribe.success.message")
		} else {
			debug "Failed to unsubscribe: " + (result.message ?: "")
			flash.message = g.message(code: "withings.unsubscribe.failure.message", args: [result.message ?: ""])
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

		if (result.success) {
			debug "Succeeded in subscribing"
			flash.message = message(code: "moves.subscribe.success.message")
		} else {
			debug "Failed to subscribe"
			flash.message = message(code: "moves.subscribe.failure.message")
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

		if (result.success) {
			debug "Succeeded in unsubscribing"
			flash.message = g.message(code: "moves.unsubscribe.success.message")
		} else {
			debug "Failure while unsubscribing" + result.message
			flash.message = g.message(code: "moves.unsubscribe.failure.message", args: [result.message ?: ""])
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

		if(result.success) {
			debug "Succeeded in subscribing"
			flash.message = g.message(code: "fitbit.subscribe.success.message", args: [result.message ?: ""])
		} else {
			debug "Failure in unsubscribing:" + result.message
			flash.message = g.message(code: "fitbit.subscribe.failure.message", args: [result.message ?: ""])
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

		if (result.success) {
			debug "Succeeded in unsubscribing"
			flash.message = g.message(code: "fitbit.unsubscribe.success.message")
		} else {
			debug "Failure in unsubscribing:" + result.message
			flash.message = g.message(code: "fitbit.unsubscribe.failure.message", args: [result.message ?: ""])
		}

		redirect (url: toUrl(controller: 'home', action: 'userpreferences', params: [userId: sessionUser().id]))
	}

	/**
	 * FitBit Subscriber Endpoint
	 */
	def notifyfitbit() {
		debug "HomeController.notifyfitbit() params:" + params
		debug "File text as is: " + request.getFile("updates").inputStream.text
		
		fitBitDataService.notificationHandler(request.getFile("updates").inputStream.text)
		render status: 204
		return
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

		user.setParameters(p)

		Utils.save(user)
		
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

	def community() {
		debug "HomeController.community()"
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			flash.message = "Must be logged in"
			redirect(url:toUrl(action:'index'))
			return
		}
		
		Long discussionId = params.discussionId ? Long.parseLong(params.discussionId) : null

		if (discussionId) {
			Discussion discussion = Discussion.get(discussionId)
	
			if (discussion == null) {
				debug "no discussion for discussionId " + discussionId
				flash.message = "No discussion found"
				return
			}
						
			if (params.unpublish && params.unpublish.equals("true")) {
				if (UserGroup.canAdminDiscussion(user, discussion)) {
					discussion.setIsPublic(false)
					Utils.save(discussion, true)
				}
			}
			if (params.publish && params.publish.equals("true")) {
				if (UserGroup.canAdminDiscussion(user, discussion)) {
					discussion.setIsPublic(true)
					Utils.save(discussion, true)
				}
			}
		}
		def groupMemberships = UserGroup.getGroupsForReader(user)
		def feedName = "Home Feed"

		groupMemberships.each { group ->
			if (group[0]?.name.equals(params.userGroupNames)) {
				feedName = group[0].fullName ?: group[0].name
			}
		}
		log.debug("HomeController.community: User has read memberships for :" + groupMemberships.dump())
		[prefs:user.getPreferences(), userId:user.getId(), templateVer:urlService.template(request),
			groupMemberships: groupMemberships, feedName: feedName]
	}

	def discuss() {
		/*
		 * Old discussion format used plotDataId to identify discussions (maintain compatibility)
		 * 
		 * New discussion format uses discussionId to identify discussions
		 * 
		 * plotIdMessage contains the plotId of the plot to be added to the discussion
		 * 
		 * Should support both modes
		 */
		debug "HomeController.discuss()"
		
		def user = sessionUser()
		
		def p = params
		
		Long plotIdMessage = params.plotIdMessage ? Long.parseLong(params.plotIdMessage) : null
		Long plotDataId = params.plotDataId ? Long.parseLong(params.plotDataId) : null
		Long discussionId = params.discussionId ? Long.parseLong(params.discussionId) : null
		Long deletePostId = params.deletePostId ? Long.parseLong(params.deletePostId) : null
		Long clearPostId = params.clearPostId ? Long.parseLong(params.clearPostId) : null
		UserGroup group = params.group ? UserGroup.lookup(params.group) : UserGroup.getDefaultGroupForUser(user)
		
		if (plotIdMessage == null && plotDataId==null && discussionId==null && params.createTopic == null) {
			flash.message = "Blank discussion call"
			redirect(url:toUrl(action:'index'))
			return
		}
		
		Discussion discussion
		
		if (discussionId) {
			discussion = Discussion.get(discussionId)
			if (discussion == null) {
				debug "DiscussionId not found: " + discussionId
				flash.message = "That discussion topic no longer exists."
				redirect(url:toUrl(action:'community'))
				return
			}
		} else if (plotDataId) {
			discussion = Discussion.getDiscussionForPlotDataId(plotDataId)
			debug "Discussion for plotDataId not found: " + plotDataId
			if (discussion == null) {
				flash.message = "That shared graph discussion no longer exists."
				redirect(url:toUrl(action:'community'))
				return
			}
		}
		
		if (discussion == null) {
			def name = "New question or discussion topic?"
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
			if (discussion != null)
				Utils.save(discussion, true)
			else {
				flash.message = "Failed to create new discussion topic: internal error"
				redirect(url:toUrl(action:'index'))
				return
			}
			if (group) group.addDiscussion(discussion)
		}
		if (!discussion.getIsPublic()) {
			if (!user) {
				flash.message = "Must be logged in"
				redirect(url:toUrl(action:'index'))
				return
			}
		}
		if (params.publish && params.publish.equals("true")) {
			def discussionUserId = discussion.getUserId()
			if ((user != null && user.getId() == discussionUserId) || (discussionUserId == null)) {
				discussion.setIsPublic(true)
				Utils.save(discussion, true)
			}
		}
		/*
		if (!discussion.getIsPublished()) {
			if ((!user) || (discussion.getUserId() != user.getId())) {
				flash.message = "Don't have permission to access this discussion"
				redirect(url:toUrl(action:'index'))
				return
			}
		}*/
		if (deletePostId) {
			debug "Attemtping to delete post id " + deletePostId
			DiscussionPost post = DiscussionPost.get(deletePostId)
			if (post != null && post.getDiscussionId() != discussion.getId()) {
				flash.message = "Can't delete that post --- mismatching discussion id"
			} else {
				if (post != null && (user == null || (post.getUserId() != user.getId() && (!UserGroup.canAdminDiscussion(user, discussion))))) {
					flash.message = "Can't delete that post"
				} else {
					discussion.setUpdated(new Date())
					DiscussionPost.delete(post)
				}
			}
			Utils.save(discussion, true)
		}
		if (clearPostId) {
			debug "Attemtping to clear post id " + clearPostId
			DiscussionPost post = DiscussionPost.get(clearPostId)
			if (post != null && (user == null || post.getUserId() != user.getId())) {
				flash.message = "Can't delete that post"
			} else {
				post.setMessage(null)
				discussion.setUpdated(new Date())
			}
			Utils.save(post, true)
		}
		if (params.message || plotIdMessage) {
			debug "Attemping to add comment '" + params.message + "', plotIdMessage: " + plotIdMessage
			if (!UserGroup.canWriteDiscussion(user, discussion)) {
				flash.message = "You don't have permission to add a comment to this discussion"
				redirect(url:toUrl(action:'index'))
				return
			}
			DiscussionPost post = discussion.getFirstPost()
			if (post && (!plotIdMessage) && discussion.getNumberOfPosts() == 1 && post.getPlotDataId() != null && post.getMessage() == null) {
				// first comment added to a discussion with a plot data at the top is assumed to be a caption on the plot data
				post.setMessage(params.message)
				Utils.save(post)
			} else if (user) {
				post = discussion.createPost(user, plotIdMessage, params.message)
			} else if (params.postname && params.postemail){
				post = discussion.createPost(params.postname, params.postemail, params.postsite, plotIdMessage, params.message)
			}
			if (post == null) {
				flash.message = "Cannot add comment"
				redirect(url:toUrl(action:'index'))
				return
			} else {
				redirect(url:toUrl(action:'discuss', params:[discussionId:discussion.getId()],
						fragment:'comment' + post.getId()))
			}
		} else {
			def model = [discussionId:discussion.getId(), notLoggedIn:user?false:true, userId:user?.getId(),
					username:user?user.getUsername():'(anonymous)', discussionTitle:discussion.getName(), firstPost:discussion.getFirstPost(),
					posts:(discussion.getFirstPost()?.getPlotDataId() != null ? discussion.getFollowupPosts() : discussion.getPosts()),
					isNew:discussion.isNew(), isAdmin:UserGroup.canAdminDiscussion(user, discussion),
					templateVer:urlService.template(request)]
			render(view:"/home/discuss", model:model)
		}
	}
	
	def lgmd2iproject() {
			def model = []
			render(view:"/home/lgmd2iproject", model:model)
	}
}
