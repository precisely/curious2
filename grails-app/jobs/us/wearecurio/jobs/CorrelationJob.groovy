package us.wearecurio.jobs

import us.wearecurio.services.CorrelationService

class CorrelationJob extends us.wearecurio.utility.TimerJob {
		def correlationService

		static triggers = {
			//simple startDelay: 1 * MINUTE, repeatInterval: 1 * MINUTE
			simple startDelay: 1 * SECOND, repeatInterval: DAY
		}

		def execute() {
			correlationService.recalculateMipss()
		}
}
