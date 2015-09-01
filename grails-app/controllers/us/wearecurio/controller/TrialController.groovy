package us.wearecurio.controller

import grails.converters.*
import us.wearecurio.model.*
import us.wearecurio.exceptions.*
import us.wearecurio.utility.Utils
import us.wearecurio.services.EntryParserService
import us.wearecurio.services.TwitterDataService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.SimpleTimeZone
import org.apache.commons.logging.LogFactory

class TrialController extends LoginController {
	TwitterDataService twitterDataService
	EntryParserService entryParserService

	private static def log = LogFactory.getLog(this)
	
	static debug(str) {
		log.debug(str)
	}
	
	SimpleDateFormat gmtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

	def TrialController() {
		gmtFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	protected def execLogout() {
		session.username = null
		session.password = null
		super.execLogout()
	}

	protected def doAddMetaEntry(userIdStr, textStr) {
		def user = userFromIdStr(userIdStr)

		if (user == null)
			return [
				null,
				'illegal attempt to access user'
			]

		def parsedEntry = entryParserService.parseMeta(textStr);
		def entry = Entry.create(user.getId(), parsedEntry);

		println("created " + entry)

		return [entry, parsedEntry['status']];
	}

	protected def doUpdateMetaEntry(entryIdStr, textStr) {
		def entry = Entry.get(Long.parseLong(entryIdStr));

		if (entry.getUserId() != sessionUser().getId())
			return null;

		def m = entryParserService.parseMeta(textStr);

		if (entry != null) {
			entry.update(m, null)
			return entry;
		} else {
			return null;
		}
	}

	protected def findMetaEntries(User user) {
		def c = Entry.createCriteria()

		def entries = c {
			and {
				eq("userId", user.getId())
				isNull("date")
			}
		}

		return Utils.listJSONDesc(entries)
	}

	def registertwitter() {
		redirect(url:twitterDataService.twitterAuthorizationURL(toUrl(controller:'trial', action:'doregistertwitter')))
	}
	def doregistertwitter() {
		User user = sessionUser()

		def twitterUsername = null
		try {
			twitterUsername = twitterDataService.usernameFromAuthTokens(params.oauth_token,
					params.oauth_verifier)
		} catch (Throwable t) {
			t.printStackTrace()
		}
		if (twitterUsername == null) {
			flash.message = "Please click on 'Authorize app' to proceed (even if you\'ve done it before)"
			redirect(url:toUrl(controller:'trial', action:'index'))
		} else {
			user.setTwitterAccountName(twitterUsername)
			redirect(url:toUrl(controller:'trial', action:'survey'))
		}
	}
	def survey() {
		def user = sessionUser()
		[prefs:user.getPreferences()]
	}
	def doupdatesurvey() {
		User user = sessionUser()

		for (def param in params) {
			if (param.key.startsWith('tag ')) {
				println "Param: " + param.key.substring(4)  + ":" + param.value
				if (param.value.length() == 0)
					continue
				user.addMetaTag(param.key.substring(4), param.value)
			}
		}

		Utils.save(user)

		redirect(url:toUrl(controller:'trial', action:'instructions'))
	}
	def getMetaData() {
		def user = userFromIdStr(params.userId);
		if (user == null) {
			render "${params.callback}('Illegal user')"
		}
		println("Getting entries for: " + params.userId)
		def entries = findMetaEntries(user)
		println("Entries: " + entries)
		render "${params.callback}(${new JSON(entries)})"
	}
	def addMetaEntryData() {
		println("params: " + params)

		def result = doAddMetaEntry(params.userId, params.text)
		if (result[0] != null) {
			render "${params.callback}(${new JSON([findMetaEntries(userFromIdStr(params.userId)), result[1]])})"
		} else {
			render "${params.callback}('error')"
		}
	}
	def updateMetaEntryData() {
		def entry = doUpdateMetaEntry(params.entryId, params.text)
		if (entry != null) {
			render "${params.callback}(${new JSON(findMetaEntries(userFromId(entry.getUserId())))})"
		} else {
			render "${params.callback}('error')"
		}
	}

	def deleteMetaEntryData() {
		def user = sessionUser()

		if (user == null) {
			render "${params.callback}('invalid user')"
		}

		def entry = Entry.get(params.entryId.toLong());
		def userId = entry.getUserId();
		if (entry.getUserId() != sessionUser().getId()) {
			render "${params.callback}('do not have permission to delete this entry')"
		} else {
			entry.setUserId(0L) // don't delete entry, just zero its user id
			Utils.save(entry, true)
			render "${params.callback}(${new JSON(findMetaEntries(sessionUser()))})"
		}
	}
	def setPreferencesData() {
		def user = sessionUser()

		user.updatePreferences(params)

		Utils.save(user)

		render "${params.callback}({1})"
	}
	// override login
	def dologin() {
		params.username = params.username.toLowerCase()
		def user = User.lookup(params.username, params.password)
		if (user) {
			setLoginUser(user)
			redirect(url:toUrl(controller:params.precontroller, action:params.preaction))
		} else if (User.findByUsername(params.username) != null) {
			flash.message = "User " + params.username + " already exists, but password does not match. Please try a different user name or reenter your password."
			redirect(url:toUrl(action:"login"))
		} else {
			session.username = params.username
			session.password = params.password
			flash.message = "Please register for an account with the username and password you just entered."
			println "prec:" + params.precontroller
			session.precontroller = params.precontroller
			session.preaction = params.preaction
			redirect(url:toUrl(action:"register"))
		}
	}
	// override registration
	def doregister() {
		if (params.username == null)
			params.username = session.username.toLowerCase()
		if (params.password == null)
			params.password = session.password
		if (params.first == null || params.first.length() == 0)
			params.first = 'anonymous'
		if (params.last == null || params.last.length() == 0)
			params.last = 'anonymous'
		if (!params.name)
			params.name = params.first + ' ' + params.last
		println "user " + params.username + " pw " + params.password
		if (User.findByUsername(params.username) != null) {
			flash.message = "User " + params.username + " already exists"
			redirect(url:toUrl(action:"login",
					model:[precontroller:params.precontroller, preaction:params.preaction]))
			return;
		}

		params.sex = 'N'
		params.birthdate = '1/1/1'

		println "Creating user with params: " + params

		def user = User.create(params)
		if (!user.validate()) {
			flash.message = "";
			user.errors.each {
				flash.message = flash.message + it + ";"
			}
			redirect(url:toUrl(action:"register",
					model:[precontroller:params.precontroller, preaction:params.preaction]))
		} else {
			user.save()
			setLoginUser(user)
			redirect(url:toUrl(controller:params.precontroller, action:params.preaction))
		}
	}
	def index() {
		def user = sessionUser()
		[prefs:user.getPreferences()]
	}
	def instructions() {
		def user = sessionUser()
		if (user.getEmail() != null) {
			sendMail {
				to user.getEmail()
				from "contact@wearecurio.us"
				subject "Instructions for Curious coffee"
				html g.render(template:"instructions")
			}
		}
		this.execLogout()
	}
}
