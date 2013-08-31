package us.wearecurio.jobs

import us.wearecurio.services.*


class PollWithingsJob extends us.wearecurio.utility.TimerJob {
	WithingsDataService withingsDataService
	
    static triggers = {
		simple repeatInterval: DAY
    }

    def execute() {
		withingsDataService.poll()
    }
}
