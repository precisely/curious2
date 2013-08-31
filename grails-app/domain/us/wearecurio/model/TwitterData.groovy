package us.wearecurio.model

import org.apache.commons.logging.LogFactory

class TwitterData {

	private static def log = LogFactory.getLog(this)

	static constraints = { lastEntryId(nullable:true) }

	Long lastEntryId;

	public TwitterData() {
		lastEntryId = null
	}
	
	String toString() {
		return "TwitterData(id:" + getId() + ", lastEntryId:" + lastEntryId + ")"
	}
}
