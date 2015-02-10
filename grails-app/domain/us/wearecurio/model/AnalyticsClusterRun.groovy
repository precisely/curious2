package us.wearecurio.model

class AnalyticsClusterRun {

	static constraints = {
		name					 (nullable:true)
		userId				 (nullable:true)
		parentId			 (nullable:true)
		startDate			 (nullable:true)
		stopDate			 (nullable:true)
		minN					 (nullable:true)
		intervalSizeMs (nullable:true)

		created				 (nullable:true)
		updated				 (nullable:true)
		finished			 (nullable:true)
	}

	static mapping = {
		table 'analytics_cluster_run'
		version false

		name					 column: 'name',						 index: 'name_index'
		userId				 column: 'user_id',					 index:	'user_id_index'
		parentId			 column: 'parent_id',				 index:	'parent_id_index'
		startDate			 column: 'start_date',			 index: 'start_date_index'
		stopDate			 column: 'stop_date',				 index: 'stop_date_index'
		minN					 column: 'min_n',						 index: 'min_n_index'
		intervalSizeMs column: 'interval_size_ms', index: 'interval_size_ms'

		updated				 column: 'updated',					 index: 'updated_index'
		created				 column: 'created',					 index: 'created_index'
		finished			 column: 'finished',				 index: 'finished_index'
	}

	String	 name
	Long		 userId
	Long		 parentId
	Date		 startDate
	Date		 stopDate
	Long		 minN
	Long		 intervalSizeMs

	Date		 created = new Date()
	Date		 updated
	Date		 finished

}
