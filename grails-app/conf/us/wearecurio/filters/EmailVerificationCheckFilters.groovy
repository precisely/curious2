package us.wearecurio.filters

import grails.converters.JSON
import grails.util.Holders
import org.codehaus.groovy.grails.commons.GrailsApplication
import us.wearecurio.annotations.EmailVerificationRequired
import us.wearecurio.model.User
import us.wearecurio.model.VerificationStatus
import us.wearecurio.services.SecurityService

import javax.servlet.http.HttpServletResponse

class EmailVerificationCheckFilters {

	static List<Map> emailVerificationEndpoints = []
	SecurityService securityService
	HttpServletResponse response
	def messageSource

	def filters = {
		all(controller: '*', action: '*') {
			before = {
				boolean isEndpointPresent = emailVerificationEndpoints.any { it.controller == controllerName &&
							(!it.actions || it.actions.contains(actionName)) }
				if (!isEndpointPresent) {
					return true
				}
				User currentUserInstance = securityService.getCurrentUser()
				if (currentUserInstance?.emailVerified == VerificationStatus.UNVERIFIED) {
					render([success: false, message: messageSource.getMessage("unverified.user.response", null, null)] as JSON)
					return false
				}
			}
		}
	}

	static void populateEmailVerificationEndpoints() {
		GrailsApplication grailsApplication = Holders.getGrailsApplication()
		emailVerificationEndpoints = []

		grailsApplication.controllerClasses.each { controllerArtefact ->
			Class controllerClass = controllerArtefact.getClazz()
			String controllerName = controllerArtefact.getLogicalPropertyName()

			if (controllerClass.isAnnotationPresent(EmailVerificationRequired)) {
				emailVerificationEndpoints.push([controller: controllerName])
			}

			controllerClass.methods.each { method ->
				if (method.isAnnotationPresent(EmailVerificationRequired)) {
					Map controllerEndpoint = emailVerificationEndpoints.find { it.controller == controllerName }
					if (!controllerEndpoint) {
						controllerEndpoint = [controller: controllerName]
						emailVerificationEndpoints.push(controllerEndpoint)
					}

					controllerEndpoint.actions = controllerEndpoint.actions ?: new HashSet<String>()

					controllerEndpoint.actions.add(method.name)
				}
			}
		}
	}
}
