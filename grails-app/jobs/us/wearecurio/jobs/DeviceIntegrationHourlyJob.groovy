package us.wearecurio.jobs

import us.wearecurio.utility.TimerJob

class DeviceIntegrationHourlyJob extends TimerJob {

	def movesDataService

	static triggers = {
		simple repeatInterval: HOUR
	}

	def execute() {
		movesDataService.poll()
	}

}