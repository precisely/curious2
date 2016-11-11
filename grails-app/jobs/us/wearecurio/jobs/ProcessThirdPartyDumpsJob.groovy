package us.wearecurio.jobs

import grails.util.Environment
import us.wearecurio.services.DataService
import us.wearecurio.utility.TimerJob

/*
 * This job runs every 30 minutes to find and process dump files(zip) uploaded by users
 */
class ProcessThirdPartyDumpsJob extends TimerJob  {
	static triggers = {
		simple startDelay: 30 * MINUTE, repeatInterval: 30 * MINUTE
	}

	def execute() {
		log.debug("Started ProcessThirdPartyDumpsJob...")
		
		if (Environment.current == Environment.PRODUCTION) {
			DataService.dumpFileProcessor()
		} else {
			log.debug "Not production server, aborted ProcessThirdPartyDumpsJob.."
			return
		}
		
		
		log.debug("Finished ProcessThirdPartyDumpsJob...")
	}
}
