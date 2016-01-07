package us.wearecurio.taglib

import grails.util.Environment

import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder

import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.services.SecurityService

class GenericTagLib {

	static namespace = "c"

	static encodeAsForTags = [renderJSTemplate: "none"]

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
		if (Environment.current != Environment.PRODUCTION) {
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

	def ifLoggedin = { attrs, body ->
		def (boolean authorized, User user) = securityService.checkLogin(actionName, request, params, flash, session)
		if (!authorized || !session.userId) {
			return
		}

		out << body()
	}

	/**
	 * Used to output a GSP template which is used for jQuery template at client side. This taglib simply wraps
	 * the actual template content inside the HTML "script" tag passing an id.<br>
	 * 
	 * Example: To render a JS template located at "grails-app/views/templates/discussion/_list.gsp"
	 * 
	 * <pre>
	 * {@code
	 *     <c:renderJSTemplate template="/discussion/list" id="discussions" />
	 * }
	 * <pre><br>
	 * 
	 * This will render the HTML snippet:
	 * 
	 * <pre>
	 * {@code
	 *     <script type="text/html id="discussions">
	 *     		// original content from _list.gsp will be here
	 *     </script>
	 * }
	 * </pre><br>
	 * 
	 * Now, in our Javascript code, we can write:
	 * 
	 * <pre>
	 * {@code
	 * 		var discussionsTemplate = $("script#discussions").html();	// will give content inside _list.gsp
	 * }
	 * </pre>
	 * 
	 * @attr template REQUIRED Location of the template inside the "grails-app/views/templates" directory.
	 * @attr id REQUIRED ID of the template to assign to the "script" tag
	 */
	def renderJSTemplate = { attrs, body ->
		out << """<script type="text/html" id="${attrs.id}">"""

		out << g.render(template: "/templates" + attrs.template)

		out << """</script>"""
	}

	def setExplanationCardUserPreferences = { attrs, body ->
		out << """<script>"""

		out << "closedExplanationCardTrackathon = ${securityService.currentUser?.settings?.hasClosedTrackathonExplanation()};"
		out << "closedExplanationCardCuriosity = ${securityService.currentUser?.settings?.hasClosedCuriositiesExplanation()};"

		out << """</script>"""
	}
}