package us.wearecurio.controller

import org.springframework.dao.DataIntegrityViolationException

import us.wearecurio.model.SharedTagGroup;

class SharedTagGroupController {

	static allowedMethods = [createOrUpdate: "POST", delete: "POST"]

	def tagGroupService

	def create() {
		[sharedTagGroupInstance: new SharedTagGroup(params)]
	}

	def createOrUpdate(Long userId, Long groupId) {
		if (!groupId && !userId) {
			flash.message = "Please select either User or UserGroup."
			render(view: "create", model: [sharedTagGroupInstance: new SharedTagGroup(params)])
			return
		}

		SharedTagGroup tagGroupInstance = tagGroupService.createOrLookupSharedTagGroup(params.description, userId, groupId, params)
		if (!tagGroupInstance || tagGroupInstance.hasErrors()) {
			log.warn "Unable to save shared taggroup instance. ${tagGroupInstance?.errors}"
			render(view: "create", model: [sharedTagGroupInstance: tagGroupInstance ?: new SharedTagGroup()])
			return
		}

		flash.message = message(code: 'default.created.message', args: [message(code: 'sharedTagGroup.label'), tagGroupInstance.id])
		redirect uri: "sharedTagGroup/list"
	}

	def edit(Long id) {
		def sharedTagGroupInstance = SharedTagGroup.get(id)
		if (!sharedTagGroupInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'sharedTagGroup.label'), id])
			redirect(action: "list")
			return
		}

		[sharedTagGroupInstance: sharedTagGroupInstance]
	}

	def list(Integer max) {
		params.max = Math.min(max ?: 10, 100)
		[sharedTagGroupInstanceList: SharedTagGroup.list(params), sharedTagGroupInstanceTotal: SharedTagGroup.count()]
	}
}