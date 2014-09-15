package us.wearecurio.model

// A join table for tag clusters.
class AnalyticsClusterInterval {

	static constraints = {
		tagClusterId	 (nullable:true)
		userId				 (nullable:true)
		startDate			 (nullable:true)
		stopDate			 (nullable:true)
	}

	static mapping = {
		table 'analytics_cluster_interval'
		version false

		tagClusterId column: 'analytics_tag_cluster_id', index: 'tag_cluster_id_index'
		userId column: 'user_id', index: 'user_id_index'
		startDate column: 'start_date', index: 'start_date_index'
		stopDate column: 'stop_date', index: 'stop_date_index'
	}

	Long tagClusterId
	Long userId
	Date		 startDate
	Date		 stopDate
}
