package us.wearecurio.server

import org.apache.commons.logging.LogFactory

class Migration {

	private static def log = LogFactory.getLog(this)

	static constraints = {
		code(unique:true)
	}
	
	static mapping = {
		version false
	}

	Long code
	boolean hasRun
	
	public static create(long code) {
		log.debug "Migration.create() code:" + code
		return new Migration(code)
	}
	
	public Migration() {
	}
	
	public Migration(code) {
		this.code = code
		this.hasRun = false
	}
	
	String toString() {
		return "Migration(id:" + getId() + ", code:" + code + ", hasRun:" + hasRun + ")"
	}
}
