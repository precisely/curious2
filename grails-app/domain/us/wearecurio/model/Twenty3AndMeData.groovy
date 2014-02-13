package us.wearecurio.model

class Twenty3AndMeData {

	byte[] data
	int sequence

	Date dateCreated
	Date lastUpdated

	OAuthAccount account

	String profileId

	Twenty3AndMeDataType type = Twenty3AndMeDataType.GENOMES

	static constraints = {
		dateCreated bindable: false
		lastUpdated bindable: false
		profileId blank: false
		data maxSize: 1024 * 1024, nullable: true
	}

	static mapping = {
		profileId column:'profile_id', index:'profile_id_index'
	}

	@Override
	String toString() {
		"Twenty3AndMeData [$id] OAuthAccount[$account?.id] ProfileId[$profileId]"
	}

}

enum Twenty3AndMeDataType {
	GENOMES(1)

	final int id

	Twenty3AndMeDataType(int id) {
		this.id = id
	}
}