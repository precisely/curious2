package us.wearecurio.controller

import us.wearecurio.security.NoAuth
import grails.gsp.PageRenderer
import org.codehaus.groovy.grails.web.json.JSONObject
import us.wearecurio.model.Discussion
import us.wearecurio.model.InitialLoginConfiguration
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.services.DataService
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.UserRegistrationService
import us.wearecurio.services.JawboneService
import us.wearecurio.services.MovesDataService
import us.wearecurio.services.OuraDataService
import us.wearecurio.services.Twenty3AndMeDataService
import us.wearecurio.services.TwitterDataService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.thirdparty.AuthenticationRequiredException
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.utility.Utils

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.UNAUTHORIZED

class HomeController extends DataController {

	static allowedMethods = [notifyJawbone: "POST", notifyfitbit: "POST"]

	UserRegistrationService userRegistrationService
	
	TwitterDataService twitterDataService
	WithingsDataService withingsDataService
	FitBitDataService fitBitDataService
	JawboneService jawboneService
	MovesDataService movesDataService
	OuraDataService ouraDataService
	def jawboneUpDataService

	def oauthService
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

		Map result = [:]

		try {
			result = withingsDataService.subscribe(userId)
		} catch (MissingOAuthAccountException e) {
			throw new AuthenticationRequiredException("withings")
		} catch (InvalidAccessTokenException e) {
			Utils.reportError("Error while registering withings", e)
			throw new AuthenticationRequiredException("withings")
		}

		String message
		if (result.success) {
			OAuthAccount account = result.account
			if (!account.lastPolled) {	// Check to see if first time subscription.
				log.info "Setting notification to get previous data for account: $account"
				withingsDataService.saveNotificationForPreviousData(account)
			}
			message = g.message(code: "thirdparty.subscribe.success.message", args: ["Withings"])
		} else {
			debug "Failed to subscribe: " + (result.message ?: "")
			message = g.message(code: "thirdparty.subscribe.failure.message", args: ["Withings"])
			//result.message is a misnomer it should actually have been named result.apiResponseMessage
			flash.args = []
			flash.args << result.message ?: ""
		}

		if (params.mobileRequest) {
			session.deniedURI = "success=" + result.success + "&message=" + message
		} else {
			flash.message = message
			session.deniedURI = toUrl(controller: 'home', action: 'userpreferences', params: [userId: sessionUser().id])
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
			Utils.reportError("Error while unregistering withings", e)
			throw new AuthenticationRequiredException("withings")
		} catch (MissingOAuthAccountException e) {
			Utils.reportError("Error while registering withings", e)
			result = [success: false, message: "No subscription found."]
		}

		thirdPartyLinkResultHandler(result, "unsubscribe", "Withings")
	}

	def register23andme() {
		debug "HomeController.register23andme() params:" + params
		User user = sessionUser()
		Long userId = user.id

		Map result = [:]

		try {
			result = twenty3AndMeDataService.storeGenomesData(userId)
		} catch (MissingOAuthAccountException e) {
			throw new AuthenticationRequiredException("Twenty3AndMe")
		} catch (InvalidAccessTokenException e) {
			Utils.reportError("Error while registering 23andme", e)
			throw new AuthenticationRequiredException("Twenty3AndMe")
		}

		thirdPartyLinkResultHandler(result, "subscribe", "Twenty3AndMe")
	}

	@NoAuth
	def notifywithings() {
		debug "HomeController.notifywithings() from IP: [$request.remoteAddr] with params:" + params

		withingsDataService.notificationHandler(new JSONObject(params).toString())

		renderStringGet('success')
	}

	def registermoves() {
		debug "HomeController.registermoves() params:" + params
		User user = sessionUser()
		Long userId = user.id

		Map result = [:]

		try {
			result = movesDataService.subscribe(userId)
		} catch (InvalidAccessTokenException e) {
			Utils.reportError("Error while registering Moves", e)
			throw new AuthenticationRequiredException("moves")
		} catch (MissingOAuthAccountException e) {
			throw new AuthenticationRequiredException("moves")
		}

		thirdPartyLinkResultHandler(result, "subscribe", "Moves")
	}

	def unregistermoves() {
		debug "HomeController.unregistermoves() params:" + params
		User user = sessionUser()
		Long userId = user.id
		Map result = [:]

		try {
			result = movesDataService.unsubscribe(userId)
		} catch (InvalidAccessTokenException e) {
			Utils.reportError("Error while unregistering Moves", e)
			throw new AuthenticationRequiredException("moves")
		} catch (MissingOAuthAccountException e) {
			Utils.reportError("Error while unregistering Moves", e)
			result = [success: false, message: "No subscription found."]
		}

		thirdPartyLinkResultHandler(result, "unsubscribe", "Moves")
	}

	def registerJawboneUp() {
		debug "HomeController.registerJawboneUp() params:" + params
		User user = sessionUser()
		Long userId = user.id

		Map result = [:]

		try {
			result = jawboneUpDataService.subscribe(userId)
		} catch (InvalidAccessTokenException e) {
			Utils.reportError("Error while registering Jawbone", e)
			throw new AuthenticationRequiredException("jawboneup")
		} catch (MissingOAuthAccountException e) {
			throw new AuthenticationRequiredException("jawboneup")
		}

		thirdPartyLinkResultHandler(result, "subscribe", "JawboneUp")
	}

	def unregisterJawboneUp() {
		debug "unregisterJawboneUp() params: $params"
		User user = sessionUser()
		Long userId = user.id
		Map result = [:]

		try {
			result = jawboneUpDataService.unsubscribe(userId)
		} catch (InvalidAccessTokenException e) {
			Utils.reportError("Error while unregistering Jawbone", e)
			throw new AuthenticationRequiredException("jawboneup")
		} catch (MissingOAuthAccountException e) {
			Utils.reportError("Error while unregistering Jawbone", e)
			result = [success: false, message: "No subscription found."]
		}

		thirdPartyLinkResultHandler(result, "unsubscribe", "JawboneUp")
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

		Map result = [:]

		try {
			result = fitBitDataService.subscribe(userId)
		} catch (MissingOAuthAccountException e) {
			throw new AuthenticationRequiredException("fitbit")
		} catch (InvalidAccessTokenException e) {
			Utils.reportError("Error while registering FitBit", e)
			throw new AuthenticationRequiredException("fitbit")
		}

		thirdPartyLinkResultHandler(result, "subscribe", "Fitbit")
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
			Utils.reportError("Error while unregistering FitBit", e)
			throw new AuthenticationRequiredException("fitbit")
		} catch (MissingOAuthAccountException e) {
			Utils.reportError("Error while unregistering FitBit", e)
			result = [success: false, message: "No subscription found."]
		}

		thirdPartyLinkResultHandler(result, "unsubscribe", "Fitbit")
	}

	def registerOura() {
		debug "HomeController.registerOura() params:" + params
		User user = sessionUser()

		if (user == null) {
			debug "auth failure"
			return
		}
		Long userId = user.id

		debug "userId: $userId"

		Map result = [:]

		try {
			result = ouraDataService.subscribe(userId)
		} catch (MissingOAuthAccountException e) {
			throw new AuthenticationRequiredException("oura")
		} catch (InvalidAccessTokenException e) {
			Utils.reportError("Error while registering Oura", e)
			throw new AuthenticationRequiredException("oura")
		}

		thirdPartyLinkResultHandler(result, "subscribe", "Oura")
	}

	def unregisterOura() {
		debug "HomeController.unregisterOura() params:" + params
		User user = sessionUser()
		if (user == null) {
			debug "auth failure"
			return
		}
		Long userId = user.id

		Map result = [:]

		try {
			result = ouraDataService.unsubscribe(userId)
		} catch (InvalidAccessTokenException e) {
			Utils.reportError("Error while unregistering Oura", e)
			throw new AuthenticationRequiredException("oura")
		} catch (MissingOAuthAccountException e) {
			Utils.reportError("Error while unregistering Oura", e)
			result = [success: false, message: "No subscription found."]
		}
		thirdPartyLinkResultHandler(result, "unsubscribe", "Oura")
	}

	private void thirdPartyLinkResultHandler(Map result, String action, String thirdPartyType) {
		User user = sessionUser()
		if (result.success) {
			debug "Succeeded in ${(action == 'unsubscribe') ? 'unsubscribing' : 'subscribing'}"
			flash.message = g.message(code: "thirdparty." + action + ".success.message", args: [thirdPartyType])
		} else {
			debug "Failure in ${(action == 'unsubscribe') ? 'unsubscribing' : 'subscribing'}: " + result.message
			flash.message = g.message(code: "thirdparty." + action + ".failure.message", args: [thirdPartyType, ""])
			flash.args = []
			flash.args = [(result.message ?: "")]
		}

		if (params.mobileRequest) {
			redirect(url: "success=" + result.success + "&message=" + flash.message)
		} else {
			redirect(url: toUrl(controller: 'home', action: 'userpreferences', params: [userId: user.id]))
		}
	}

	/**
	 * Oura Subscriber Endpoints
	 */
	@NoAuth
	def notifyOura() {
		String notificationData = request.JSON.toString()
		debug "HomeController.notifyOura() from IP: [$request.remoteAddr] with params: $params data: $notificationData"

		if (ouraDataService.notificationHandler(notificationData)) {
			render status: 204
		} else {
			render status: 500
		}
	}

	/**
	 * FitBit Subscriber Endpoint
	 */
	@NoAuth
	def notifyfitbit() {
		// Fitbit now sends notification data as request body
		String notificationData = request.JSON.toString()
		debug "HomeController.notifyfitbit() from IP: [$request.remoteAddr] with params: $params and data: $notificationData"

		fitBitDataService.notificationHandler(notificationData)
		render status: 204
		return
	}

	@NoAuth
	def notifyJawbone() {
		def requestBodyData = request.JSON

		debug "notifyJawbone() from IP: [$request.remoteAddr] params:" + params + ", body: $requestBodyData"

		jawboneUpDataService.notificationHandler(requestBodyData.toString())

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
			Utils.reportError("Error while registering Twitter", t)
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
			flash.message = "Error updating user preferences: missing field or username/email already in use"
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
		debug "HomeController.index() called with params: $params"
		//**********************************************************************
		//**********************************************************************
		//*********   REMOVE THIS LINE WHEN INTERESTS UI INCORPORATED **********
		//*********   THIS SETS DEFAULT INTEREST SUBJECT (preSelect==true)******
		userRegistrationService.selectPromoCodeDefaults(sessionUser(), params?.promoCode)
		//**********************************************************************
		//**********************************************************************
		def user = sessionUser()
		[	prefs:user.getPreferences(), 
			showTime:params.showTime?:0, templateVer:urlService.template(request),
			initialConfig:userRegistrationService.register(user, params?.promoCode).initialLoginConfig]
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

	def graphCuriosities() {
		debug "HomeController.graphCuriosities()" + params.description1 + " - " + params.description2
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

	@NoAuth
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

	@NoAuth
	def homepage() {
		render(view:"/home/homepage")
	}

	def curiosities() {
		render(view: "/home/curiosities")
	}

	def community(String discussionHash, boolean unpublish, boolean publish) {
		redirect(url:toUrl(action:'social'))
	}

	def feed(String discussionHash, boolean unpublish, boolean publish) {
		redirect(url:toUrl(action:'social'))
	}

	@NoAuth
	def social() {
		debug "HomeController.social(): $params"

		Map model
		// TODO: protect private discussions
		/*
		 * If request is coming from facebook(i.e. while posting discussion to facebook), return model with discussion details to be used in og meta tags.
		 * Og meta tags are used by facebook scrapper to scrap information about the post being shared
		 */
		if (params.hash && request.getHeader("User-Agent").contains("facebook")) {
			Discussion discussion = Discussion.findByHash(params.hash)
			model = discussion?.getJSONDesc()
		} else if (params.hash) {
			redirect(uri: "home/social#" + params.tab + "/" + params.hash);
		}

		model
	}

	def sprint() {

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

	def lgmd2iproject() {
		def model = []
		render(view:"/home/lgmd2iproject", model:model)
	}
}
