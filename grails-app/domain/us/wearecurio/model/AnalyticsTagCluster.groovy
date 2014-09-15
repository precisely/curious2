package us.wearecurio.model

// A join table for tag clusters.
class AnalyticsTagCluster {

	static constraints = {
		clusterRunId	 (nullable:true)
		userId								 (nullable:true)
	}

	static mapping = {
		table 'analytics_tag_cluster'
		version false

		clusterRunId column: 'analytics_cluster_run_id', index:	'cluster_run_index'
		userId column: 'user_id', index:	'user_id_index'
	}

	Long userId
	Long clusterRunId

}
