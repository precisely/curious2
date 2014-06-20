package us.wearecurio.controller

import org.springframework.dao.DataIntegrityViolationException

import us.wearecurio.model.SharedTagGroup;

class SharedTagGroupController {

	static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

	def list(Integer max) {
		params.max = Math.min(max ?: 10, 100)
		[sharedTagGroupInstanceList: SharedTagGroup.list(params), sharedTagGroupInstanceTotal: SharedTagGroup.count()]
	}

	def create() {
		[sharedTagGroupInstance: new SharedTagGroup(params)]
	}

	def save() {
		def sharedTagGroupInstance = new SharedTagGroup(params)
		if (!sharedTagGroupInstance.save(flush: true)) {
			render(view: "create", model: [sharedTagGroupInstance: sharedTagGroupInstance])
			return
		}

		flash.message = message(code: 'default.created.message', args: [message(code: 'sharedTagGroup.label', default: 'SharedTagGroup'), sharedTagGroupInstance.id])
		redirect uri: "sharedTagGroup/list"
	}

	def edit(Long id) {
		def sharedTagGroupInstance = SharedTagGroup.get(id)
		if (!sharedTagGroupInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'sharedTagGroup.label', default: 'SharedTagGroup'), id])
			redirect(action: "list")
			return
		}

		[sharedTagGroupInstance: sharedTagGroupInstance]
	}

	def update(Long id, Long version) {
		def sharedTagGroupInstance = SharedTagGroup.get(id)
		if (!sharedTagGroupInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'sharedTagGroup.label', default: 'SharedTagGroup'), id])
			redirect(action: "list")
			return
		}

		sharedTagGroupInstance.properties = params

		if (!sharedTagGroupInstance.save(flush: true)) {
			render(view: "edit", model: [sharedTagGroupInstance: sharedTagGroupInstance])
			return
		}

		flash.message = message(code: 'default.updated.message', args: [message(code: 'sharedTagGroup.label', default: 'SharedTagGroup'), sharedTagGroupInstance.id])
		redirect uri: "sharedTagGroup/list"
	}

	def delete(Long id) {
		def sharedTagGroupInstance = SharedTagGroup.get(id)
		if (!sharedTagGroupInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'sharedTagGroup.label', default: 'SharedTagGroup'), id])
			redirect(action: "list")
			return
		}

		try {
			sharedTagGroupInstance.delete(flush: true)
			flash.message = message(code: 'default.deleted.message', args: [message(code: 'sharedTagGroup.label', default: 'SharedTagGroup'), id])
			redirect uri: "sharedTagGroup/list"
		} catch (DataIntegrityViolationException e) {
			flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'sharedTagGroup.label', default: 'SharedTagGroup'), id])
			redirect uri: "sharedTagGroup/list"
		}
	}
}