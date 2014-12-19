package us.wearecurio.services

import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.User

class SearchService {
	
	static transactional = true
	
	static SearchService service
	
	static set(SearchService s) { service = s }
	
	static SearchService get() { service }

	// params has the structure:
	// [ key:[offset:<offset>, max:<max>], ... ]
	//
	// returns:
	// [ key:[<json description of item>, ... ], ... ]
	
	def list(Long userId, String searchString, Map params) {
		def retVal = [:]
		for (e in params) {
			String k = e.key
			Map v = e.value
			int offset = v['offset']
			int max = v['max']
			
			def results
			
			if (k.equals('discussions')) {
				retVal[k] = this.listDiscussions(searchString, offset, max)
			} else if (k.equals('users')) {
				retVal[k] = this.listUsers(searchString, offset, max)
			} else if (k.equals('sprints')) {
				retVal[k] = this.listSprints(searchString, offset, max)
			} else if (k.equals('groups')) {
				// not searching groups for now
				// retVal[k] = this.listGroups(searchString, offset, max)
			}
		}
	}
	
	def listDiscussions(User user, String searchString, int offset, int max) {
		
	}
	
	def listUsers(User user, String searchString, int offset, int max) {
		
	}
	
	def listSprints(User user, String searchString, int offset, int max) {
		
	}
	
	def listGroups(User user, String searchString, int offset, int max) {
		
	}
}
