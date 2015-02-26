package us.wearecurio.model

// The intervals that corresponds to the intersection between two time series.
class AnalyticsCorrelationInterval {

	static constraints = {
		correlationId	 (nullable:true)
		userId				 (nullable:true)
		startMs			 (nullable:true) // number of milliseconds since 1970.
		stopMs			 (nullable:true)
	}

	static mapping = {
		table 'analytics_correlation_interval'
		version false

		correlationId column: 'analytics_correlation_id', index: 'tag_correlation_id_index'
		userId column: 'user_id', index: 'user_id_index'
		startDate column: 'start_ms', index: 'start_ms_index'
		stopDate column: 'stop_ms', index: 'stop_ms_index'
	}

	Long correlationId
	Long userId
	Long startMs
	Long stopMs

	static def userCorrelationIntervals(Long userId) {
		AnalyticsCorrelationInterval.executeQuery("select aci.id from AnalyticsCorrelationInterval aci where aci.userId = ? order by aci.id", [userId])
	}

	static def list(Long correlationId) {
		AnalyticsCorrelationInterval.findAllByCorrelationId(correlationId)
	}

	def asJson () {
		[ id: id,
			userId: userId,
			startMs: startMs,
			stopMs: stopMs ]
	}
}
