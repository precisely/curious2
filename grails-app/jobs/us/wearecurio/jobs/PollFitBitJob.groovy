package us.wearecurio.jobs

import us.wearecurio.services.*

class PollFitBitJob extends us.wearecurio.utility.TimerJob {
	FitBitDataService fitBitDataService
	
    static triggers = {
		simple repeatInterval: DAY
    }

    def execute() {
		fitBitDataService.poll()
    }
}
