package us.wearecurio.marshaller

import grails.converters.JSON
import us.wearecurio.model.AnalyticsCorrelationInterval

class AnalyticsCorrelationMarshaller {
	void register() {
		JSON.registerObjectMarshaller( AnalyticsCorrelationInterval ) { AnalyticsCorrelationInterval correlationInterval ->
			return [
				id: correlationInterval.id,
				userId: correlationInterval.userId,
				startMs: correlationInterval.startMs,
				stopMs: correlationInterval.stopMs,
			]
		}
	}
}
