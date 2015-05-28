package us.wearecurio.jobs

import us.wearecurio.model.PasswordRecovery


class CleanupPasswordRecoveryJob extends us.wearecurio.utility.TimerJob {
		static triggers = {
			cron name:'cleanupTrigger', startDelay: DAY, cronExpression: '0 15 2 * * ? *' //2:15 AM
		}

		def execute() {
				PasswordRecovery.deleteStaleRecoveries()
		}
}
