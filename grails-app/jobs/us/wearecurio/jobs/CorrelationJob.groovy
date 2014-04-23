package us.wearecurio.jobs

//import us.wearecurio.services.CorrelationService

class CorrelationJob extends us.wearecurio.utility.TimerJob {
		def correlationService

		static triggers = {
			simple startDelay: 2 * MINUTE, repeatInterval: DAY
		}

		def execute() {
			 //log.debug "Running CorrelationService.updateAllUserCorrelations()"
			 //correlationService.updateAllUserCorrelations()
		}
}
