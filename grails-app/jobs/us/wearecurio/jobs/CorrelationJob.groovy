package us.wearecurio.jobs

import us.wearecurio.services.CorrelationService

class CorrelationJob extends us.wearecurio.utility.TimerJob {
		def correlationService

		static triggers = {
			cron name:'correlationTrigger', startDelay: 2 * HOUR, cronExpression: '0 45 2 * * ? *' //2:45 AM 
		}

		def execute() {
			correlationService.recalculateMipss()
		}
}
