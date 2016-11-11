package us.wearecurio.model

import com.causecode.fileuploader.UFile

class ThirdPartyDataDump {

	ThirdParty type
	UFile dumpFile
	Date timeStamp = new Date()
	int attemptCount = 0
	Long userId
	Status status = Status.UNPROCESSED
	List<String> unprocessedFiles = []

	static hasMany = [unprocessedFiles: String]

	static constraints = {
		dumpFile nullable: false
		type nullable: false
		timeStamp nullable: false
		unprocessedFiles nullable: true
		userId nullable: false
	}

	static mappings = {
		type index: true
		userId index: true
		status index: true
		attemptCount index: true
	}
}

enum Status {
	PROCESSED(1),
	UNPROCESSED(2),
	FAILED(3),
	PARTIALLY_PROCESSED(4)

	final int id

	Status(int id) {
		this.id = id
	}

	public String toString() {
		return name() + "(${this.id})"
	}
}