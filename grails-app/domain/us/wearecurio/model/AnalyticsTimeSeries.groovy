package us.wearecurio.model

class AnalyticsTimeSeries {

	static constraints = {
		amount(scale:9, nullable:true)
		date(nullable:true)
		description(nullable:true)
		dataType(nullable:true)
		tagId(nullable:true)
		userId(nullable:true)
	}

	static mapping = {
		table 'analytics_time_series'
		version false
		dataType column: 'data_type'
		userId   column: 'user_id', index:'user_id_index'
		tagId    column: 'tag_id',  index:'tag_id_index'
	}

	BigDecimal amount
	Date date
	String description
	String dataType
	Long userId
	Long tagId

}
