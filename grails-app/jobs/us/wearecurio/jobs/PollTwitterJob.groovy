package us.wearecurio.jobs

import us.wearecurio.services.*


/* class PollTwitterJob extends us.wearecurio.utility.TimerJob {
	TwitterDataService twitterDataService
	def grailsApplication
	
    static triggers = {
		simple repeatInterval: 30 * SECOND
    }

    def execute() {
		if (!grailsApplication.config.wearecurious.runImportJobs) {
			log.debug "Not job server, aborting..."
			return
		}
		
        //twitterDataService.poll()
    }
}*/
