package us.wearecurio.jobs

//import us.wearecurio.services.CorrelationService

class CorrelationJob extends us.wearecurio.utility.TimerJob {
		def correlationService

		static triggers = {
			//simple startDelay: 0 * MINUTE, repeatInterval: DAY
			simple startDelay: 1 * MINUTE, repeatInterval: DAY
		}

		def execute() {
			 //correlationService.updateAllUserCorrelations()
			 correlationService.updateTimeSeries()
		}
}
