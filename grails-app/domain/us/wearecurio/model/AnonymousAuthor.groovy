package us.wearecurio.model;

import org.apache.commons.logging.LogFactory
import us.wearecurio.utility.Utils

class AnonymousAuthor extends NameEmail {

	private static def log = LogFactory.getLog(this)

	String name
	String email
	String site

	static constraints = {
		name(maxSize:100)
		email(maxSize:200)
		site(maxSize:250, nullable:true)
	}
	static transients = ['username']
	static mapping = {
		version false
		table 'anonymous_author'
		email column:'email', index:'email_index'
	}
	
	public AnonymousAuthor() {
	}
	
	public static AnonymousAuthor create(String name, String email, String site) {
		log.debug "AnonymousAuthor.create() name:" + name + ", email:" + email + ", site:" + site
		def retVal = new AnonymousAuthor(name, email, site)
		Utils.save(retVal, true)
		
		return retVal
	}
	
	public AnonymousAuthor(String name, String email, String site) {
		this.name = name
		this.email = email
		this.site = site
	}

	public String getUsername() {
		return ""
	}
	
	String toString() {
		return "AnonymousAuthor(name:" + name + ", email:" + email + ", site:" + site + ")"
	}
}
