package us.wearecurio.model

class FitbitNotification {

	String collectionType
	Date date
	String ownerId
	String ownerType
	String subscriptionId
	Status status = Status.UNPROCESSED

	static mapping = {
		table 'fitbit_notification'
		date column:'log_date'
		ownerId column:'owner_id', index:'owner_id_index'
		ownerType column:'owner_type'
		subscriptionId column:'subscription_id', index:'subscription_id_index'
		status column:'status', index:'status_index'
	}
	
	public static enum Status {
		UNPROCESSED(0), PROCESSED(1)
		
		final Integer id
		
		Status(Integer id) {
			this.id = id
		}
	}
}
