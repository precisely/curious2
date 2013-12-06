package us.wearecurio.jobs

import us.wearecurio.utility.TimerJob

class DeviceIntegrationDailyJob extends TimerJob {

	def withingsDataService

	static triggers = {
		simple repeatInterval: DAY
	}

	def execute() {
		withingsDataService.refreshSubscriptions()
	}

}