package us.wearecurio.model

import grails.converters.*

import org.apache.commons.logging.LogFactory

class Model {

	private static def log = LogFactory.getLog(this)

	static constraints = {
	}
	
	static mapping = {
		version false
	}
	
	static enum SearchType {
		DISCUSSION(0), DISCUSSION_POST(1), SPRINT(2), USER(3)
		
		final Integer id
		
		SearchType(int id) {
			this.id = id
		}
		
		int getId() {
			return id
		}
	}
	
	static String getSearchId(SearchType type, Object obj) {
		def typeId = type.id
		def objId = obj.id
		if (type == SearchType.DISCUSSION_POST) {
			typeId = SearchType.DISCUSSION.id
			objId = obj.discussionId
		}
				
		return objId.toString() + String.format('%03d', typeId)
	}
	
	static SearchType parseSearchIdForType(String searchId) {
		def searchIdLong = searchId.toLong()
		return SearchType.values()[(Integer)(searchIdLong - (((Long)(searchIdLong/1000))*1000))]
	}
	
	static Long parseSearchIdForId(String searchId) {
		return (searchId.toLong() / 1000)
	}
	
	static enum Visibility {
		PRIVATE(0), UNLISTED(1), PUBLIC(2), NEW(3)
	
		final Integer id
		
		static Visibility get(int id) {
			if (id == 0) return PRIVATE
			else if (id == 1) return UNLISTED
			else return PUBLIC
		}
		
		Visibility(int id) {
			this.id = id
		}
		
		int getId() {
			return id
		}
		
		boolean isListed() {
			return this.id == PUBLIC
		}
		
		boolean isPrivate() {
			return this.id == PRIVATE
		}

		boolean isVisibleWithUrl() {
			return this.id != PRIVATE
		}
	}
}
