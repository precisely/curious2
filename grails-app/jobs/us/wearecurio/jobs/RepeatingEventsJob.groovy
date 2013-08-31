package us.wearecurio.jobs

import us.wearecurio.services.*


class RepeatingEventsJob extends us.wearecurio.utility.TimerJob {
	RepeatingEventService repeatingEventService
	
    static triggers = {
		simple repeatInterval: HOUR
    }

    def execute() {
        repeatingEventService.createEvents()
    }
}
