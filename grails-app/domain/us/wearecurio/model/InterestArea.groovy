package us.wearecurio.model

class InterestArea {
	String name
	static hasMany = [interestSubjects: InterestSubjectBoolean]
    static constraints = {
		name(maxSize:32, unique:true, blank:false, nullable:false)
    }	
}