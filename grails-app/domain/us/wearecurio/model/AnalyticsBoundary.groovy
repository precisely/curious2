package us.wearecurio.model

class AnalyticsBoundary {
	// Description: This table is meant to store training data to automate the detection
	//	of user activity boundaries.
	// Types of boundaries.
	// 1 START
	// 2 STOP

	Long userId
	Long tagId
	Long type
	Date date

	Date createdAt
	Date updatedAt

	static constraints = {
		userId nullable: true
		tagId nullable: true
		type nullable: true
		date nullable: true
		createdAt nullable: true
		updatedAt nullable: true
	}

	static mapping = {
		version false
		userId index: 'userIdx'
		tagId index: 'tagIdx'
		type index: 'typeIdx'
		date index: 'dateIdx'
		createdAt index: 'createdAtIdx'
		updatedAt index: 'updatedAtIdx'
	}

}
