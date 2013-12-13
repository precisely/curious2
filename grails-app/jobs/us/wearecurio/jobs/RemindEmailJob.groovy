package us.wearecurio.jobs

import us.wearecurio.services.*


class RemindEmailJob extends us.wearecurio.utility.TimerJob {
	RemindEmailService remindEmailService
	
    static triggers = {
		simple repeatInterval: 2 * MINUTE
    }

    def execute() {
        remindEmailService.sendReminders()
    }
}
