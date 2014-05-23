package us.wearecurio.services

import us.wearecurio.model.Entry
import us.wearecurio.model.Tag

class EntryService {

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
		def all_times = Entry.findAllWhere(userId: userId.toLong()).collect { it.date }
		all_times.removeAll([null])
		all_times.min()
	}

	def userStopTime(userId) {
		def all_times = Entry.findAllWhere(userId: userId.toLong()).collect { it.date }
		all_times.removeAll([null])
		all_times.max()
	}
}
