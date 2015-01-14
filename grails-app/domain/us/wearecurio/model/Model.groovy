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

	static enum Visibility {
		PRIVATE(0), UNLISTED(1), PUBLIC(2)
	
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
