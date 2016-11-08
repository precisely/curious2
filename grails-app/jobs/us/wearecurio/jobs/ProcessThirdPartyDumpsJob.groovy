package us.wearecurio.jobs

import grails.util.Environment
import us.wearecurio.services.DataService
import us.wearecurio.utility.TimerJob

class ProcessThirdPartyDumpsJob extends TimerJob  {
	static triggers = {
		simple startDelay: 30 * MINUTE, repeatInterval: 30 * MINUTE
	}

	def execute() {
		log.debug("Started ProcessThirdPartyDumpsJob...")
		
		if (Environment.current != Environment.PRODUCTION) {
			log.debug "Not production server, aborted ProcessThirdPartyDumpsJob.."
			return
		}
		
		DataService.dumpFileProcessor()
		
		log.debug("Finished ProcessThirdPartyDumpsJob...")
	}
}
