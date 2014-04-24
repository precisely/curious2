package us.wearecurio.model


class TagGroup extends GenericTagGroup {
	static hasMany = [tags: Tag]
	
	static mapping = {
		version false
	}
}
