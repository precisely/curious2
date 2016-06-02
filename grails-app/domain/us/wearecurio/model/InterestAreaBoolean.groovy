package us.wearecurio.model

//prefer to user Tuple2<InterestArea,Boolean>, but does not work in hasMany relationship
class InterestAreaBoolean {
	InterestArea interest
	Boolean preSelect
	
	InterestAreaBoolean(InterestArea interest, Boolean preSelect) {
		this.interest = interest
		this.preSelect = preSelect
	}

    static constraints = {
   		preSelect(nullable:false)
	}
}
