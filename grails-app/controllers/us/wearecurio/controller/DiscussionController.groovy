package us.wearecurio.controller

import org.springframework.dao.DataIntegrityViolationException
import us.wearecurio.model.*

class DiscussionController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index() {
        redirect(action: "list", params: params)
    }

    def list(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        [discussionInstanceList: Discussion.list(params), discussionInstanceTotal: Discussion.count()]
    }

    def create() {
        [discussionInstance: new Discussion(params)]
    }

    def save() {
        def discussionInstance = new Discussion(params)
        if (!discussionInstance.save(flush: true)) {
            render(view: "create", model: [discussionInstance: discussionInstance])
            return
        }

        flash.message = message(code: 'default.created.message', args: [message(code: 'discussion.label', default: 'Discussion'), discussionInstance.id])
        redirect(action: "show", id: discussionInstance.id)
    }

    def show(Long id) {
        def discussionInstance = Discussion.get(id)
        if (!discussionInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'discussion.label', default: 'Discussion'), id])
            redirect(action: "list")
            return
        }

        [discussionInstance: discussionInstance]
    }

    def edit(Long id) {
        def discussionInstance = Discussion.get(id)
        if (!discussionInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'discussion.label', default: 'Discussion'), id])
            redirect(action: "list")
            return
        }

        [discussionInstance: discussionInstance]
    }

    def update(Long id, Long version) {
        def discussionInstance = Discussion.get(id)
        if (!discussionInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'discussion.label', default: 'Discussion'), id])
            redirect(action: "list")
            return
        }

        if (version != null) {
            if (discussionInstance.version > version) {
                discussionInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                          [message(code: 'discussion.label', default: 'Discussion')] as Object[],
                          "Another user has updated this Discussion while you were editing")
                render(view: "edit", model: [discussionInstance: discussionInstance])
                return
            }
        }

        discussionInstance.properties = params

        if (!discussionInstance.save(flush: true)) {
            render(view: "edit", model: [discussionInstance: discussionInstance])
            return
        }

        flash.message = message(code: 'default.updated.message', args: [message(code: 'discussion.label', default: 'Discussion'), discussionInstance.id])
        redirect(action: "show", id: discussionInstance.id)
    }

    def delete(Long id) {
        def discussionInstance = Discussion.get(id)
        if (!discussionInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'discussion.label', default: 'Discussion'), id])
            redirect(action: "list")
            return
        }

        try {
            discussionInstance.delete(flush: true)
            flash.message = message(code: 'default.deleted.message', args: [message(code: 'discussion.label', default: 'Discussion'), id])
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
            flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'discussion.label', default: 'Discussion'), id])
            redirect(action: "show", id: id)
        }
    }
}
