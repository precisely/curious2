package us.wearecurio.controller

import grails.converters.JSON
import org.springframework.transaction.annotation.Transactional	 
import us.wearecurio.model.*
import us.wearecurio.utility.*
import us.wearecurio.support.EntryStats
import us.wearecurio.services.SearchService

class SprintController extends LoginController {

	static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]
	SearchService searchService

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
		Map requestData = request.JSON
		log.debug "update sprint for id($requestData.id) with data $requestData"
		Sprint sprintInstance = Sprint.findByHash(requestData.id)

		if (!sprintInstance) {
			renderJSONPost([success: false, message: g.message(code: "sprint.not.exist")])
			return
		} 

		Sprint.withTransaction { status ->
			sprintInstance.update(requestData)

			if (!Utils.save(sprintInstance, true)) {
				status.setRollbackOnly()
				renderJSONPost([success: false, message: g.message(code: "can.not.update.sprint")])
				return
			}
			
			renderJSONPost([success: true, hash: sprintInstance.hash])
		}
	}

	def show() {
		Sprint sprintInstance = Sprint.findByHash(params.id)

		if (!sprintInstance) {
			renderJSONGet([success: false, message: g.message(code: "sprint.not.exist")])
			return
		}

		if (!sprintInstance.hasMember(sessionUser().id) && !sprintInstance.isPublic()) {
			renderJSONGet([success: false, message: g.message(code: "edit.sprint.permission.denied")])
			return
		}

		List<Entry> entries = Entry.findAllByUserId(sprintInstance.virtualUserId)*.getJSONDesc()
		List<User> participants = sprintInstance.getParticipants(10, 0)
		List memberAdmins = GroupMemberAdmin.findAllByGroupId(sprintInstance.virtualGroupId)
		List<User> admins = memberAdmins.collect {User.get(it.memberId)}
		Map sprintDiscussions = searchService.getDiscussionsList(sessionUser(), 0, 5, [sprintInstance.virtualGroupId])

		Map sprintInstanceMap = sprintInstance.getJSONDesc()
		sprintInstanceMap["hasAdmin"] = sprintInstance.hasAdmin(sessionUser().id)
		sprintInstanceMap["hasStarted"] = sprintInstance.hasStarted(sessionUser().id, new Date())
		sprintInstanceMap["hasEnded"] = sprintInstance.hasEnded(sessionUser().id, new Date())
		sprintInstanceMap["virtualGroupName"] = sprintInstance.fetchUserGroup().name
		sprintInstanceMap["hasMember"] = sprintInstance.hasMember(sessionUser().id)

		renderJSONGet([success: true, sprint: sprintInstanceMap, entries: entries, participants: participants, admins: admins, 
			totalParticipants: sprintInstance.getParticipantsCount(), discussions: sprintDiscussions])
	} 

	def delete() {
		Sprint sprintInstance = Sprint.findByHash(params.id)
		User currentUser = sessionUser()
		log.debug "$currentUser trying to delete sprint with id [$params.id]"

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
		Sprint sprintInstance = Sprint.findByHash(params.sprintHash)
		log.debug "Trying to add member [${params.username}] to sprint [$params.sprintHash]"

		if (!sprintInstance) {
			renderJSONPost([error: true, errorMessage: g.message(code: "add.sprint.participant.failed")])
			return
		}

		User memberInstance = params.username ? User.findByUsername(params.username) : null
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
		Sprint sprintInstance = Sprint.findByHash(params.sprintHash)
		log.debug "Trying to add admin [${params.username}] to sprint [$params.sprintHash]"

		if (!sprintInstance) {
			renderJSONPost([error: true, errorMessage: g.message(code: "add.sprint.admin.failed")])
			return
		}

		User adminInstance = params.username ? User.findByUsername(params.username) : null
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
		Sprint sprintInstance = Sprint.findByHash(params.sprintHash)
		User memberInstance = params.username ? User.findByUsername(params.username) : null
		User currentUser = sessionUser()
		log.debug "$currentUser Trying to detele member [${params.username}] from sprint [$params.sprintHash]"

		if (!sprintInstance || !memberInstance) {
			renderJSONPost([error: true, errorMessage: g.message(code: "delete.sprint.participant.failed")])
			return
		}

		if (!sprintInstance.hasAdmin(currentUser.id)) {
			renderJSONPost([error: true, errorMessage: g.message(code: "delete.sprint.member.permission.denied")])
			return
		}

		if (!sprintInstance.hasMember(memberInstance.id)) {
			renderJSONPost([error: true, errorMessage: g.message(code: "no.sprint.member.to.delete")])
			return
		}

		if (sprintInstance.hasStarted(memberInstance.id, new Date())) {
			Date now = params.now ? null : parseDate(params.now)
			EntryStats stats = new EntryStats()
			Date baseDate = Utils.getStartOfDay(now)
			sprintInstance.stop(memberInstance.id, baseDate, now, params.timeZoneName, stats)
		}
		sprintInstance.removeMember(memberInstance.id)
		renderJSONPost([success: true])
	}

	def deleteAdmin() {
		User adminInstance = params.username ? User.findByUsername(params.username) : null
		Sprint sprintInstance = Sprint.findByHash(params.sprintHash)
		User currentUser = sessionUser()
		log.debug "$currentUser Trying to detele admin [${params.username}] from sprint [$params.sprintHash]"

		if (!sprintInstance || !adminInstance) {
			renderJSONPost([error: true, errorMessage:  g.message(code: "delete.sprint.admin.failed")])
			return
		}

		if (!sprintInstance.hasAdmin(currentUser.id)) {
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

	def start() {
		Sprint sprintInstance = Sprint.findByHash(params.id)
		User currentUser = sessionUser()
		log.debug "Starting sprint[${params.id}] for user $currentUser"

		if (!sprintInstance) {
			renderJSONGet([success: false, message: g.message(code: "sprint.not.exist")])
			return
		}

		if (!sprintInstance.hasMember(currentUser.id)) {
			renderJSONGet([success: false, message: g.message(code: "not.sprint.member")])
			return
		}

		Date now = params.now ? parseDate(params.now) : null
		String timeZoneName = params.timeZoneName ? params.timeZoneName : TimeZoneId.guessTimeZoneNameFromBaseDate(now)
		Date baseDate = Utils.getStartOfDay(now)
		EntryStats stats = new EntryStats()

		if (sprintInstance.start(currentUser.id, baseDate, now, timeZoneName, stats)) {
			renderJSONGet([success: true])
			return
		}
		renderJSONGet([success: false, message: g.message(code: "can.not.start.sprint")])
	}

	def stop() {
		Sprint sprintInstance = Sprint.findByHash(params.id)
		User currentUser = sessionUser()
		log.debug "Trying to stop sprint[${params.id}] for user $currentUser"

		if (!sprintInstance) {
			renderJSONGet([success: false, message: g.message(code: "sprint.not.exist")])
			return
		}

		if (!sprintInstance.hasMember(currentUser.id)) {
			renderJSONGet([success: false, message: g.message(code: "not.sprint.member")])
			return
		}

		Date now = params.now ? parseDate(params.now) : null
		String timeZoneName = params.timeZoneName ? params.timeZoneName : TimeZoneId.guessTimeZoneNameFromBaseDate(now)
		Date baseDate = Utils.getStartOfDay(now)
		EntryStats stats = new EntryStats()

		if (sprintInstance.stop(currentUser.id, baseDate, now, timeZoneName, stats)) {
			renderJSONGet([success: true])
			return
		}
		renderJSONGet([success: false, message: g.message(code: "can.not.stop.sprint")])
	}

	def leave() {
		Sprint sprintInstance = Sprint.findByHash(params.id)
		User currentUser = sessionUser()
		log.debug "User $currentUser trying to leave sprint[params.id]"
		
		if (!sprintInstance) {
			renderJSONGet([success: false, message: g.message(code: "sprint.not.exist")])
			return
		}
		if (!sprintInstance.hasMember(currentUser.id)) {
			renderJSONGet([success: false, message: g.message(code: "not.sprint.member")])
			return
		}
		
		Date now = params.now ? parseDate(params.now) : null
		Date baseDate = Utils.getStartOfDay(now)
		String timeZoneName = params.timeZoneName ? params.timeZoneName : TimeZoneId.guessTimeZoneNameFromBaseDate(now)
		EntryStats stats = new EntryStats()
		sprintInstance.stop(currentUser.id, baseDate, now, timeZoneName, stats)

		sprintInstance.removeMember(currentUser.id)
		renderJSONGet([success: true])
	}
	
	def join() {
		Sprint sprintInstance = Sprint.findByHash(params.id)
		User currentUser = sessionUser()
		log.debug "User $currentUser trying to join sprint[params.id]"

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

	def discussions(String sprintHash, int max, int offset) {
		Sprint sprint = Sprint.findByHash(sprintHash)
		User currentUser = sessionUser()

		if (!sprint) {
			renderJSONGet([success: false, message: g.message(code: "sprint.not.exist")])
			return
		}
		
		if (!sprint.hasMember(currentUser.id)) {
			renderJSONGet([success: false, message: g.message(code: "not.sprint.member")])
			return
		}

		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0
		Map sprintDiscussions = searchService.getDiscussionsList(currentUser, params.offset, params.max, [sprint.fetchUserGroup().id])
		renderJSONGet([listItems: sprintDiscussions.listItems, success: true]);
	}
}
