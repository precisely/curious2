package us.wearecurio.services

import org.apache.commons.logging.LogFactory

import us.wearecurio.utility.Utils;
import us.wearecurio.model.*;

class RepeatingEventService {

	private static def log = LogFactory.getLog(this)

	static transactional = true

	def createEvents() {
		log.debug "RepeatingEventService.createEvents()"
		
		def c = Entry.createCriteria()

		def repeatingEvents = c {
			and {
				not {
					isNull("repeatType")
				}
				not {
					eq("userId", 0L)
				}
			}
		}
		
		for (Entry event in repeatingEvents) {
			log.debug "Trying to repeat " + event
			event.generateRepeatingEntries()
		}
	}
}
