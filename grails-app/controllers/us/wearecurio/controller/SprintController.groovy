package us.wearecurio.controller

import grails.converters.JSON
import org.springframework.transaction.annotation.Transactional	 
import us.wearecurio.model.*
import us.wearecurio.utility.*
import us.wearecurio.support.EntryStats

class SprintController extends LoginController {

	static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

	def index() {

	}

	def save() {
		User currentUser = sessionUser()

		Sprint sprintInstance = Sprint.create(new Date(), currentUser, "Untitled Sprint", Model.Visibility.NEW);
		JSON.use("jsonDate") {
			render sprintInstance.getJSONDesc() as JSON
		}
	}

	def update() {
		log.debug "request parameters to update sprint for id($params.id) : ${request.JSON}"
		Map params = request.JSON
		Sprint sprintInstance = Sprint.findByHash(params.id)

		if (!sprintInstance) {
			renderJSONPost([success: false, message: g.message(code: "sprint.not.exist")])
			return
		} else {
			Sprint.withTransaction { status ->
				sprintInstance.update(params)
				sprintInstance.validate()

				if (sprintInstance.hasErrors()) {
					status.setRollbackOnly()
					renderJSONPost([success: false, message: g.message(code: "can.not.update.sprint")])
					return
				}
				Utils.save(sprintInstance, true)
				renderJSONPost([success: true, hash: sprintInstance.hash])
			}
		}
	}

	def show() {
		Sprint sprintInstance = Sprint.findByHash(params.id)

		if (!sprintInstance) {
			renderJSONGet([error: true, message: g.message(code: "sprint.not.exist")])
			return
		}

		if (!sprintInstance.hasAdmin(sessionUser().id)) {
			renderJSONGet([error: true, message: g.message(code: "edit.sprint.permission.denied")])
			return
		}

		List<Entry> entries = Entry.findAllByUserId(sprintInstance.virtualUserId)*.getJSONDesc()
		List memberReaders = GroupMemberReader.findAllByGroupId(sprintInstance.virtualGroupId)
		List<User> participants = memberReaders.collect {User.get(it.memberId)}
		List memberAdmins = GroupMemberAdmin.findAllByGroupId(sprintInstance.virtualGroupId)
		List<User> admins = memberAdmins.collect {User.get(it.memberId)}

		renderJSONGet([sprint: sprintInstance, entries: entries, participants: participants, admins: admins])
	} 

	def delete() {
		Sprint sprintInstance = Sprint.findByHash(params.id)
		User currentUser = sessionUser()

		if (!sprintInstance) {
			renderJSONGet([success: false, message: g.message(code: "sprint.not.exist")])
			return
		}

		if (!sprintInstance.hasAdmin(currentUser.id)) {
			renderJSONGet([success: false, message: g.message(code: "delete.sprint.permission.denied")])
			return
		}

		Sprint.delete(sprintInstance)
		renderJSONGet([success: true])
	}

	def addMember() {
		User memberInstance = params.username ? User.findByUsername(params.username) : null
		Sprint sprintInstance = Sprint.findByHash(params.sprintHash)

		if (!sprintInstance) {
			renderJSONPost([error: true, errorMessage: g.message(code: "add.sprint.participant.failed")])
			return
		}

		if (!memberInstance) {
			renderJSONPost([error: true, errorMessage: g.message(code: "user.not.found")])
			return
		}

		if (!sprintInstance.hasAdmin(sessionUser().id)) {
			renderJSONPost([error: true, errorMessage: g.message(code: "add.sprint.member.permission.denied")])
			return
		}

		if (sprintInstance.hasMember(memberInstance.id)) {
			renderJSONPost([error: true, errorMessage: g.message(code: "sprint.member.already.added")])
			return
		}
		sprintInstance.addInvited(memberInstance.id)
		renderJSONPost([success: true])
	}

	def addAdmin() {
		User adminInstance = params.username ? User.findByUsername(params.username) : null
		Sprint sprintInstance = Sprint.findByHash(params.sprintHash)

		if (!sprintInstance) {
			renderJSONPost([error: true, errorMessage: g.message(code: "add.sprint.admin.failed")])
			return
		}

		if (!adminInstance) {
			renderJSONPost([error: true, errorMessage: g.message(code: "user.not.found")])
			return
		}

		if (!sprintInstance.hasAdmin(sessionUser().id)) {
			renderJSONPost([error: true, errorMessage: g.message(code: "add.sprint.admin.permission.denied")])
			return
		}

		if (sprintInstance.hasAdmin(adminInstance.id)) {
			renderJSONPost([error: true, errorMessage: g.message(code: "sprint.admin.already.added")])
			return
		}

		sprintInstance.addInvitedAdmin(adminInstance.id)
		renderJSONPost([success: true])
	}


	def deleteMember() {
		User memberInstance = params.username ? User.findByUsername(params.username) : null
		Sprint sprintInstance = Sprint.findByHash(params.sprintHash)

		if (!sprintInstance || !memberInstance) {
			renderJSONPost([error: true, errorMessage: g.message(code: "delete.sprint.participant.failed")])
			return
		}

		if (!sprintInstance.hasAdmin(sessionUser().id)) {
			renderJSONPost([error: true, errorMessage: g.message(code: "delete.sprint.member.permission.denied")])
			return
		}

		if (!sprintInstance.hasMember(memberInstance.id)) {
			renderJSONPost([error: true, errorMessage: g.message(code: "no.sprint.member.to.delete")])
			return
		}

		if (sprintInstance.hasStarted(memberInstance.id, new Date())) {
			def now = params.now ? null : parseDate(params.now)
			EntryStats stats = new EntryStats()
			def baseDate = Utils.getStartOfDay(now)
			sprintInstance.stop(memberInstance.id, baseDate, now, params.timeZoneName, stats)
		}
		sprintInstance.removeMember(memberInstance.id)
		renderJSONPost([success: true])
	}

	def deleteAdmin() {
		User adminInstance = params.username ? User.findByUsername(params.username) : null
		Sprint sprintInstance = Sprint.findByHash(params.sprintHash)

		if (!sprintInstance || !adminInstance) {
			renderJSONPost([error: true, errorMessage:  g.message(code: "delete.sprint.admin.failed")])
			return
		}

		if (!sprintInstance.hasAdmin(sessionUser().id)) {
			renderJSONPost([error: true, errorMessage: g.message(code: "delete.sprint.admin.permission.denied")])
			return
		}

		if (!sprintInstance.hasAdmin(adminInstance.id)) {
			renderJSONPost([error: true, errorMessage: g.message(code: "no.sprint.admin.to.delete")])
			return
		}

		sprintInstance.removeAdmin(adminInstance.id)
		renderJSONPost([success: true])
	}

	def startSprint() {
		Sprint sprintInstance = Sprint.findByHash(params.id)

		if (!sprintInstance) {
			renderJSONGet([success: false, message: g.message(code: "sprint.not.exist")])
			return
		}

		if (!sprintInstance.hasMember(sessionUser().id)) {
			renderJSONGet([success: false, message: g.message(code: "not.sprint.member")])
			return
		}

		User currentUser = sessionUser()
		def now = params.now ? parseDate(params.now) : null
		def timeZoneName = params.timeZoneName ? params.timeZoneName : TimeZoneId.guessTimeZoneNameFromBaseDate(now)
		def baseDate = Utils.getStartOfDay(now)
		EntryStats stats = new EntryStats()

		if (sprintInstance.start(currentUser.id, baseDate, now, timeZoneName, stats)) {
			renderJSONGet([success: true])
			return
		}
		renderJSONGet([success: false, message: g.message(code: "can.not.start.sprint")])
	}

	def stopSprint() {
		Sprint sprintInstance = Sprint.findByHash(params.id)

		if (!sprintInstance) {
			renderJSONGet([success: false, message: g.message(code: "sprint.not.exist")])
			return
		}

		if (!sprintInstance.hasMember(sessionUser().id)) {
			renderJSONGet([success: false, message: g.message(code: "not.sprint.member")])
			return
		}

		def now = params.now ? parseDate(params.now) : null
		def timeZoneName = params.timeZoneName ? params.timeZoneName : TimeZoneId.guessTimeZoneNameFromBaseDate(now)
		def baseDate = Utils.getStartOfDay(now)
		EntryStats stats = new EntryStats()

		if (sprintInstance.stop(sessionUser().id, baseDate, now, timeZoneName, stats)) {
			renderJSONGet([success: true])
			return
		}
		renderJSONGet([success: false, message: g.message(code: "can.not.stop.sprint")])
	}

	def leaveSprint() {
		Sprint sprintInstance = Sprint.findByHash(params.id)
		User currentUser = sessionUser()
		
		if (!sprintInstance) {
			renderJSONGet([success: false, message: g.message(code: "sprint.not.exist")])
			return
		}
		if (!sprintInstance.hasMember(currentUser.id)) {
			renderJSONGet([success: false, message: g.message(code: "not.sprint.member")])
			return
		}
		
		def now = params.now ? parseDate(params.now) : null
		def baseDate = Utils.getStartOfDay(now)
		def timeZoneName = params.timeZoneName ? params.timeZoneName : TimeZoneId.guessTimeZoneNameFromBaseDate(now)
		EntryStats stats = new EntryStats()
		sprintInstance.stop(currentUser.id, baseDate, now, timeZoneName, stats)

		sprintInstance.removeMember(currentUser.id)
		renderJSONGet([success: true])
	}
	
	def joinSprint() {
		Sprint sprintInstance = Sprint.findByHash(params.id)
		User currentUser = sessionUser()

		if (!sprintInstance) {
			renderJSONGet([success: false, message: g.message(code: "sprint.not.exist")])
			return
		}
		if (sprintInstance.hasMember(currentUser.id)) {
			renderJSONGet([success: false, message: g.message(code: "already.joined.sprint")])
			return
		}

		sprintInstance.addMember(currentUser.id)
		renderJSONGet([success: true])
	}
}
