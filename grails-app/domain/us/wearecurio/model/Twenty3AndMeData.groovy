package us.wearecurio.model

class Twenty3AndMeData {

	Date dateCreated
	Date lastUpdated

	OAuthAccount account

	String data
	String profileId

	Twenty3AndMeDataType type = Twenty3AndMeDataType.GENOMES

	static constraints = {
		dateCreated bindable: false
		lastUpdated bindable: false
		profileId blank: false, unique: true
		data nullable: true
	}

	static mapping = {
		data type: "text"
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