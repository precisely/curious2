package us.wearecurio.model;

class AnalyticsTimeSeries {
	static constraints = {
		amount(scale:9, nullable:true)
		date(nullable:true)
		description(nullable:true)
		tagId(nullable:true, index: 'idx_analytics_time_series_tag')
		userId(nullable:true, index: 'idx_analytics_time_series_user')
	}

	static mapping = {
		table 'analytics_time_series'
		version false
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
	}

	BigDecimal amount
	Date date
	String description

	Long userId
	Long tagId
}
