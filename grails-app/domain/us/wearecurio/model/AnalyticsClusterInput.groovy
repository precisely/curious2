package us.wearecurio.model

// A class to represent a matrix, which is required for the Python clustering algorithm.
//	Each row in the table represents an entry in the matrix with coordinates i,j.
class AnalyticsClusterInput {

	static constraints = {
		userId	(nullable:true)
		i (nullable:true)
		j (nullable:true)
		value		(nullable:true)
	}

	static mapping = {
		table 'analytics_cluster_input'
		version false
		userId	column: 'user_id',	index:	'user_id_index'
		i column: 'i_id', index:	'i_index'
		j column: 'j_id', index:	'j_index'
		value		column: 'value',		index:	'value_index'
	}

	Long userId
	Long i
	Long j
	BigDecimal value

}
