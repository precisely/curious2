package us.wearecurio.model

class AnalyticsTagMembership {

	static constraints = {
		userId(nullable:true)
		tagGroupId(nullable:true)
		tagId(nullable:true)
	}

	static mapping = {
		table 'analytics_tag_membership'
		version false
		userId		 column: 'user_id',			 index: 'user_id_index'
		tagGroupId column: 'tag_group_id', index: 'tag_group_id_index'
		tagId			 column: 'tag_id',			 index: 'tag_id_index'
	}

	Long userId
	Long tagGroupId
	Long tagId

}
