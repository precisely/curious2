package us.wearecurio.controller

import org.springframework.dao.DataIntegrityViolationException

import us.wearecurio.model.SharedTagGroup;

class SharedTagGroupController {

	static allowedMethods = [createOrUpdate: "POST", delete: "POST"]

	def tagGroupService

	def create() {
		[sharedTagGroupInstance: new SharedTagGroup(params)]
	}

	def save(Long groupId, String description) {
		log.debug "Create or update shared tag group $params"
		if (!groupId) {
			flash.message = "Please select a UserGroup."
			render(view: "create", model: [sharedTagGroupInstance: new SharedTagGroup(params)])
			return
		}

		SharedTagGroup tagGroupInstance = SharedTagGroup.lookupWithinGroup(description, groupId)

		if (tagGroupInstance) {
			flash.message = "There is already an SharedTagGroup exists within the group selected."
			redirect(uri: "sharedTagGroup/edit/$tagGroupInstance.id")
			return
		}

		tagGroupInstance = tagGroupService.createOrLookupSharedTagGroup(description, groupId, params)
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
			redirect uri: "sharedTagGroup/list"
			return
		}

		[sharedTagGroupInstance: sharedTagGroupInstance]
	}

	def list(Integer max) {
		params.max = Math.min(max ?: 10, 100)
		[sharedTagGroupInstanceList: SharedTagGroup.list(params), sharedTagGroupInstanceTotal: SharedTagGroup.count()]
	}

	def update(Long id, Long groupId, String description) {
		log.debug "Update shared tag group $params"

		SharedTagGroup sharedTagGroupInstance = SharedTagGroup.get(id)

		if (!sharedTagGroupInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'sharedTagGroup.label'), id])
			redirect uri: "sharedTagGroup/list"
			return
		}

		// Search for any existing tag group with same name
		SharedTagGroup tagGroupInstance = SharedTagGroup.lookupWithinGroup(description, groupId)

		if (tagGroupInstance && tagGroupInstance.id != id) {
			flash.message = "There is already an SharedTagGroup exists with name [$description] within the group selected."
			redirect(uri: "sharedTagGroup/edit/$sharedTagGroupInstance.id")
			return
		}

		sharedTagGroupInstance.updateData(description, groupId)

		flash.message = message(code: 'default.updated.message', args: [message(code: 'sharedTagGroup.label'), sharedTagGroupInstance.id])
		redirect uri: "sharedTagGroup/list"
	}
}