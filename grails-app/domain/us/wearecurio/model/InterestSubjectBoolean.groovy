package us.wearecurio.model

//prefer to user Tuple2<InterestSubject,Boolean>, but does not work in hasMany relationship
class InterestSubjectBoolean {
	Boolean preSelect
	InterestSubject subject
	InterestSubjectBoolean(InterestSubject subject, Boolean preSelect) {
		this.preSelect = preSelect
		this.subject = subject
	}
    static constraints = {
		preSelect(nullable:false)
    }
}
