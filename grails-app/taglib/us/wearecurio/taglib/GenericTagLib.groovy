package us.wearecurio.taglib

import grails.util.Environment;

import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder

class GenericTagLib {

	static namespace = "c"

	/**
	 * A generic tag to prevent CSRF which uses the infrastructure of grails built in
	 * method <b>org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder</b>
	 * to mimic this in java-script calls.
	 * 
	 * @attr keys REQUIRED A unique key in each page to generate tokens for.
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

		out << "<script type=\"text/javascript\">"
		out << "\$(function() {$lineSeperator"
		out << "var App = window.App;"
		out << "App.CSRF = App.CSRF || {};"
		out << lineSeperator
		attrs.keys.tokenize(",").each {
			String key = it.trim()
			out << "App.CSRF.${key } = \"${tokensHolder.generateToken(key) }\";$lineSeperator"
		}
		out << "});"
		out << "</script>"
	}

}