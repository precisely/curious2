package us.wearecurio.server

import org.apache.commons.logging.LogFactory

class Migration {

	private static def log = LogFactory.getLog(this)

	static constraints = {
		tag(unique:false)
	}
	
	static mapping = {
		version false
	}

	String tag
	boolean hasRun
	
	public static create(def tag) {
		log.debug "Migration.create() tag:" + tag
		return new Migration(tag)
	}
	
	public Migration() {
	}
	
	public Migration(def tag) {
		this.tag = tag
		this.hasRun = false
	}
	
	String toString() {
		return "Migration(id:" + getId() + ", tag:" + tag + ", hasRun:" + hasRun + ")"
	}
}
