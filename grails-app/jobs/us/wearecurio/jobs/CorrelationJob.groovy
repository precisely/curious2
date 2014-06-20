package us.wearecurio.jobs

import us.wearecurio.services.CorrelationService

class CorrelationJob extends us.wearecurio.utility.TimerJob {
		def correlationService

		static triggers = {
			cron name:'correlationTrigger', cronExpression: '0 15 2 * * ? *' //2:15 AM 
		}

		def execute() {
			correlationService.recalculateMipss()
		}
}
