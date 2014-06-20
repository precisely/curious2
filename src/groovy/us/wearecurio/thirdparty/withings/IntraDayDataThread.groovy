package us.wearecurio.thirdparty.withings

import us.wearecurio.services.*
import us.wearecurio.model.*
import us.wearecurio.thirdparty.*
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GA
import java.util.NoSuchElementException

class IntraDayDataThread extends Thread {

	private static def log = LogFactory.getLog(this)
	def ctx = SCH.servletContext.getAttribute(GA.APPLICATION_CONTEXT)

	WithingsDataService withingsDataService = ctx.withingsDataService
    public void run() {
		def account
		IntraDayQueueItem queueItem
		boolean lastRequestFailed = false
		IntraDayQueueItem.withNewSession { session ->
			while(true) {
				def timestamp = System.currentTimeMillis()
				log.debug "IntraDayDataThread: Iteration started at ${timestamp}"
				try {
					if (!queueItem || !lastRequestFailed) {
						queueItem = WithingsDataService.intraDayQueue.remove()
						lastRequestFailed = false
					}
					if (account?.id != queueItem.id) {
						log.debug "IntraDayDataThread: Getting OAuthAccount with id ${queueItem.oauthAccountId}"
						account = OAuthAccount.get(queueItem.oauthAccountId)
						log.debug "IntraDayDataThread: OAuthAccount retrieved ${account?.dump()}"
					}
					log.debug "IntraDayDataThread: Querying intra day data for ${account?.id} and date ${queueItem?.queryDate}"
					withingsDataService.getDataIntraDayActivity(account, queueItem.queryDate, queueItem.queryDate + 1)
					queueItem?.delete()
				} catch (TooManyRequestsException te) {
					log.debug "IntraDayDataThread: TooManyRequestsException - Exceeded query rate"
					lastRequestFailed = true
					Thread.sleep(61000)
				} catch (NoSuchElementException nse) {
					log.debug "IntraDayDataThread: NoSuchElementException - intraDayQueue empty"
					withingsDataService.populateIntraDayQueue()
					if (WithingsDataService.intraDayQueue.size() < 1) {
						synchronized(WithingsDataService.intraDayQueue) {
							WithingsDataService.intraDayQueue.wait()
						}
					}
				}
				log.debug "IntraDayDataThread: Iteration ${timestamp} ended"
			}
		}
    }
}
