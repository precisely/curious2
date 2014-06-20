package us.wearecurio.controller

import org.springframework.dao.DataIntegrityViolationException

import us.wearecurio.model.UserGroup

class UserGroupController extends LoginController {

	static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

	def create() {
		[userGroupInstance: new UserGroup(params)]
	}

	def createOrUpdate(String name, String fullName, String description, Long id) {
		UserGroup userGroupInstance = UserGroup.createOrUpdate(name, fullName, description, params)

		flash.message = message(code: 'default.' + (id ? "updated" : "created") + '.message', args:[message(code: 'userGroup.label'), userGroupInstance.id])
		redirect uri: "userGroup/list"
	}

	def delete(Long id) {
		def userGroupInstance = UserGroup.get(id)
		if (!userGroupInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'userGroup.label'), id])
			redirect uri: "userGroup/list"
			return
		}

		if (userGroupInstance.name == UserGroup.SYSTEM_USER_GROUP_NAME) {
			flash.message = "Could not delete UserGroup since it is a System Default Group."
			redirect uri: "userGroup/list"
			return
		}

		try {
			userGroupInstance.delete(flush: true)
			flash.message = message(code: 'default.deleted.message', args: [message(code: 'userGroup.label'), id])
			redirect uri: "userGroup/list"
		} catch (DataIntegrityViolationException e) {
			flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'userGroup.label'), id])
			redirect uri: "userGroup/list"
		}
	}

	def edit(Long id) {
		def userGroupInstance = UserGroup.get(id)
		if (!userGroupInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'userGroup.label'), id])
			redirect uri: "userGroup/list"
			return
		}

		[userGroupInstance: userGroupInstance]
	}

	def index() {
		redirect action: "list", params: params
	}

	def list(Integer max) {
		params.max = Math.min(max ?: 10, 100)
		[userGroupInstanceList: UserGroup.list(params), userGroupInstanceTotal: UserGroup.count()]
	}
}