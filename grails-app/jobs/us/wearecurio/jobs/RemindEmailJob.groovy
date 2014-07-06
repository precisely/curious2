package us.wearecurio.jobs

import us.wearecurio.services.*


class RemindEmailJob extends us.wearecurio.utility.TimerJob {
	static transactional = false
	RemindEmailService remindEmailService
	
    static triggers = {
		simple startDelay: 30 * MINUTE, repeatInterval: 2 * MINUTE
    }

    def execute() {
        remindEmailService.sendReminders()
    }
}
