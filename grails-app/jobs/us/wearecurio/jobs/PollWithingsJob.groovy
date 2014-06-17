package us.wearecurio.jobs

import us.wearecurio.services.*
import org.apache.commons.logging.LogFactory

class PollWithingsJob extends us.wearecurio.utility.TimerJob {

	private static def log = LogFactory.getLog(this)

    static triggers = {
		simple repeatInterval: DAY
    }

	WithingsDataService withingsDataService

    def execute() {
		def timestamp = System.currentTimeMillis()
		log.debug "PollWithingsJob: Started at ${timestamp}"
		withingsDataService.pollAll()
		log.debug "PollWithingsJob: Job started at ${timestamp} ended"
    }
}
