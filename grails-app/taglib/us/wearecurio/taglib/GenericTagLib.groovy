package us.wearecurio.taglib

import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder

class GenericTagLib {

	static namespace = "c"

	/**
	 * A generic tag to prevent CSRF which uses the infrastructure of grails built in
	 * method <b>org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder</b>
	 * to mimic this in java-script calls.
	 * 
	 * @attr key REQUIRED A unique key in each page to generate tokens for.
	 */
	def jsCSRFToken = { attrs, body ->
		if(!attrs.key) {
			throwTagError("Tag [jsCSRFToken] requires a key attribute.")
		}
		SynchronizerTokensHolder tokensHolder = SynchronizerTokensHolder.store(session)
		writeScript([value: tokensHolder.generateToken(attrs.key), key: attrs.key], out)
	}

	def writeScript(attrs, out) {
		out << "<script type=\"text/javascript\">"
		out << "\$(function() {"
		out << "window.App = window.App || {};"
		out << "var App = window.App;"
		out << "App.CSRF = App.CSRF || {};"
		out << "App.CSRF.${attrs.key } = \"${attrs.value }\"; "
		out << "});"
		out << "</script>"
	}

}