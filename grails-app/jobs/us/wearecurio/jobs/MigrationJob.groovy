package us.wearecurio.jobs

import us.wearecurio.services.CorrelationService

import org.apache.commons.logging.LogFactory
import org.hibernate.SessionFactory

class MigrationJob extends us.wearecurio.utility.TimerJob {
	private static def log = LogFactory.getLog(this)

	def migrationService
	
	static transactional = false
	
	static triggers = {
		simple startDelay: 1 * MINUTE
	}

	def execute() {
		log.debug("STARTING MIGRATION JOB...")
		migrationService.doMigrationJob()
		log.debug("FINISHING MIGRATION JOB...")
	}
}
