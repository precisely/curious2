package us.wearecurio.taglib

import grails.util.Environment

import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder

import us.wearecurio.model.User
import us.wearecurio.model.UserGroup

class GenericTagLib {

	static namespace = "c"

	def securityService

	/**
	 * A generic tag to prevent CSRF which uses the infrastructure of grails built in
	 * method <b>org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder</b>
	 * to mimic this in java-script calls.
	 * 
	 * @attr keys REQUIRED A unique key in each page to generate tokens for.
	 * 
	 * <code>
	 *     <c:jsCSRFToken keys="getDataCSRF, showTagGroupCSRF, xyz, abc" />
	 * </code>
	 * Will output to:
	 * <code>
	 *     App.CSRF.getDataCSRF = "63cdd116-8dc2-4309-8ef3-c1eda00151f1";
	 *     App.CSRF.showTagGroupCSRF = "6382674c-f294-45a9-9726-b4f5f85e7292";
	 *     App.CSRF.xyz = "bea9863b-f465-415b-be33-06cc88e078ef";
	 *     App.CSRF.abc = "511b0bca-b37c-4037-889a-5f8e45b84ecf";
	 * </code>
	 */
	def jsCSRFToken = { attrs, body ->
		if(!attrs.keys) {
			throwTagError("Tag [jsCSRFToken] requires a keys attribute.")
		}
		String lineSeperator = ""
		if(Environment.current == Environment.DEVELOPMENT) {
			lineSeperator = "\n"
		}
		SynchronizerTokensHolder tokensHolder = SynchronizerTokensHolder.store(session)

		if(!attrs.noScriptTag) {
			out << "<script type=\"text/javascript\">"
		}
		out << "\$(function() {$lineSeperator"
		out << "var App = window.App;$lineSeperator"
		attrs.keys.tokenize(",").each { token ->
			String key = token.trim()
			out << "App.CSRF.${key } = \"${tokensHolder.generateToken(key) }\";$lineSeperator"
		}
		out << "});"
		if(!attrs.noScriptTag) {
			out << "</script>"
		}
	}

	def ifAdmin = { attrs, body ->
		def (boolean authorized, User user) = securityService.checkLogin(actionName, request, params, flash, session)
		if (!authorized || !session.userId) {
			return
		}

		if (!UserGroup.hasAdmin(UserGroup.lookupOrCreateSystemGroup().id, session.userId)) {
			return
		}

		out << body()
	}

}