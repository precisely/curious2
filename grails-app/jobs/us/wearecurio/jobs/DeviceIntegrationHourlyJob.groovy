package us.wearecurio.jobs

import us.wearecurio.utility.TimerJob

class DeviceIntegrationHourlyJob extends TimerJob {

	def humanDataService
	def movesDataService

	static triggers = {
		simple repeatInterval: HOUR
	}

	def execute() {
		humanDataService.poll()
		movesDataService.poll()
	}

}