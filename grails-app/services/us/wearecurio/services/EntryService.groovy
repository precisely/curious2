package us.wearecurio.services

import us.wearecurio.model.Entry
import us.wearecurio.model.Tag

class EntryService {
	
	static EntryService service
	
	static set(EntryService s) { service = s }
	
	static EntryService get() { service }

	def getEntriesByUserAndTag(Long userId, String tagDescription) {
		// TODO: write more efficient SQL.
		Tag tag = Tag.findByDescription(tagDescription)
			Entry.findAllWhere(userId: userId, tag: tag)
	}

	def getEntriesByUserAndTaggable(userId, taggable) {
		def tags = taggable.tags()
		def entries = []
		for (tag in tags) {
			entries += getEntriesByUserAndTag(userId, tag.description)
		}
		entries
	}

	def userStartTime(userId) {
		def results = Entry.executeQuery("select min(e.date) from Entry e")
		if (results && results[0])
			return results[0]
		
		return new Date(0L)
	}

	def userStopTime(userId) {
		def results = Entry.executeQuery("select max(e.date) from Entry e")
		if (results && results[0])
			return results[0]
		
		return new Date(0L)
	}
}
