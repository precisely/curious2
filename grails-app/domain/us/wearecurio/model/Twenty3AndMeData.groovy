package us.wearecurio.model

class Twenty3AndMeData {

	Date dateCreated
	Date lastUpdated

	OAuthAccount account

	String data

	Twenty3AndMeDataType type = Twenty3AndMeDataType.GENOMES

	static constraints = {
		dateCreated bindable: false
		lastUpdated bindable: false
	}

	static mapping = {
		data type: "text"
	}

}

enum Twenty3AndMeDataType {
	GENOMES
}