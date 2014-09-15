package us.wearecurio.model

// A join table between AnalyticsTagCluster and Tag.
class AnalyticsTagClusterTag {

	static constraints = {
		tagClusterId	 (nullable:true)
		tagId								 (nullable:true)
		loglike								 (nullable:true)
	}

	static mapping = {
		table 'analytics_tag_cluster_tag'
		version false

		tagClusterId column: 'analytics_tag_cluster_id', index:	'tag_cluster_index'
		tagId column: 'tag_id', index:	'tag_id_index'
	  loglike column: 'loglike', index: 'loglike_index'
	}

	Long tagId
	Long tagClusterId
  BigDecimal loglike
}
