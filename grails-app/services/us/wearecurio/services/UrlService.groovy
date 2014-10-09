package us.wearecurio.services

import org.springframework.transaction.annotation.Transactional

import grails.util.Environment;

import org.apache.commons.logging.LogFactory

class UrlService {

	def grailsApplication

	private static def log = LogFactory.getLog(this)
	
	//TODO temporarily, use UrlService as a holder of static methods. When we switch to Grails 2.x, change to instance methods

	static transactional = false
	
	def UrlService() {
	}

	/**
	 * Make an URL from a map, using the default base URL for the server
	 * 
	 * @param map
	 * @return
	 */
	def make(map) {
		return make(map, null)
	}
	
	/**
	 * Make an URL from a map, using the host extracted from the request for the server
	 * 
	 * @param map
	 * @param req
	 * @return
	 */
	def make(map, req, boolean usePublicIP = false) {
		def url = base(req, usePublicIP) + map.controller + '/' + map.action
		if (map.params) {
			url = makeQueryString(url, map.params)
		}
		if (map.fragment) {
			url += '#' + map.fragment
		}
		
		return url
	}

	String makeQueryString(String url, Map params) {
		url + "?" + params.collect { key, value -> "$key=$value" }.join("&")
	}
	
	public static final String URLATTRIBUTE = "us.wearecurio.serverURL"
	public static final String TEMPLATEVERSION = "us.wearecurio.templateVersion"
	
	/**
	 * Return the current base URL for this request, also set the current template version
	 * 
	 * @param req
	 * @return
	 */
	def base(req, boolean usePublicIP = false) {
		if (req == null) {
			if(Environment.current == Environment.DEVELOPMENT && usePublicIP) {
				log.debug "UrlService.base() NULL REQUEST, RETURNING DEFAULT PUBLIC IP SERVER URL"
				return grailsApplication.config.grails.other.serverURL ?: (grailsApplication.config.grails.development.serverURL ?: grailsApplication.config.grails.serverURL)
			}
			log.debug "UrlService.base() NULL REQUEST, RETURNING DEFAULT SERVER URL"
			return grailsApplication.config.grails.serverURL
		}
			
		log.debug "UrlService.base() req:" + req + ", req.requestURL: " + req.requestURL
		
		def retVal = req.getAttribute(URLATTRIBUTE)
		if (retVal == null) {
			URL url = new URL(req.requestURL.toString())
			log.debug "UrlService.base: url host: " + url?.getHost()
			if (url.getHost().startsWith("lamhealth")) {
				retVal = grailsApplication.config.grails.serverURLProtocol + "://" + url.getHost() + (url.getPort() > 0 ? ":" + url.getPort() : "") + "/"
				req.setAttribute(TEMPLATEVERSION, "lhp")
				log.debug "UrlService: LHP version: " + retVal
			} else {
				retVal = grailsApplication.config.grails.serverURL
				req.setAttribute(TEMPLATEVERSION, "main")
				log.debug "UrlService: normal version: " + retVal
			}
			req.setAttribute(URLATTRIBUTE, retVal)
		} else {
			log.debug "UrlService: retVal from attribute: " + retVal
		}
		
		return retVal
	}
	
	/**
	 * Current template version for the current request. Templates can vary depending on the host name.
	 * 
	 * @param req
	 * @return
	 */
	def template(req) {
		def retVal = req.getAttribute(TEMPLATEVERSION)
		
		if (retVal != null)
			return retVal
			
		if (!retVal)
			base(req)
			
		return req.getAttribute(TEMPLATEVERSION)
	}
}
