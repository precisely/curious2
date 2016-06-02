package us.wearecurio.model

import us.wearecurio.utility.Utils

class InterestSubject {
	String name
	String description
	
	static hasMany = [interestTags: String, bookmarks: String]

    static constraints = {
		name(maxSize:32, unique:true, blank:false, nullable:false)
		description(maxSize:128, unique:false, blank:true, nullable:true)
    }
	
	private InterestSubject(String name, String description, List interestTags, List bookmarks) {
		this.name = name
		this.description = description
		interestTags.each {this.addToInterestTags(it)}
		bookmarks.each {this.addToBookmarks(it)}
	}
	
	static List scrubList(List values) {
		values.findAll{it != null && it.class == String && it != ""}
	}
	
	static InterestSubject createInterestSubject(
		String name,
		List interestTags,
		List bookmarks,
		String description="",
		Boolean save=true
	) {
		if (name == null || name == "") {
			return null
		}
		
		List tags = scrubList(interestTags)
		List bkmarks = scrubList(bookmarks)
		if (tags.size == 0 && bkmarks.size == 0) {
			return null
		}
		
		if ( InterestSubject.findByName(name) != null ) {
			return null
		}
		
		InterestSubject ret = new InterestSubject(name, description, tags, bkmarks)
		
		if (save) {
			Utils.save(ret, true)
		}
		
		return ret
	}
}
