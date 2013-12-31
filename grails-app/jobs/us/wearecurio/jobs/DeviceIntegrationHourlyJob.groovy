package us.wearecurio.jobs

import us.wearecurio.utility.TimerJob

class DeviceIntegrationHourlyJob extends TimerJob {

	def IHealthDataService
	def movesDataService

	static triggers = {
		simple repeatInterval: HOUR
	}

	def execute() {
		IHealthDataService.poll()
		movesDataService.poll()
	}

}