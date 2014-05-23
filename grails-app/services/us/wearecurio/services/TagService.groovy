package us.wearecurio.services

import org.springframework.transaction.annotation.Transactional

import us.wearecurio.model.Tag
import us.wearecurio.model.Entry
import us.wearecurio.model.TagGroup

class TagService {

	static transactional = true

	def databaseService

	def getTagsByUser(def userId) {
		def tags = databaseService.sqlRows("select t.id as id, t.description as description, count(e.id) as c, CASE prop.data_type_computed WHEN 'CONTINUOUS' THEN 1 ELSE 0 END as iscontinuous, prop.show_points as showpoints from entry e inner join tag t on e.tag_id = t.id left join tag_properties prop on prop.user_id = e.user_id and prop.tag_id = t.id where e.user_id = " + new Long(userId) + " and e.date is not null group by t.id order by t.description")
		return tags
	}

	def getTagsByDescription(def userId, def description) {
		def tags = Entry.executeQuery("select new Map(entry.tag.id as id,entry.tag.description as description) from Entry as entry where entry.userId=:userId "+
			"and entry.tag.description like :desc group by entry.tag.id", [userId:userId,desc:"%${description}%"])
		return tags
	}

	def getTagGroupsByUser(def userId) {
		def tagGroups = databaseService.sqlRows("""select tg.id,tg.description, tgp.is_continuous as iscontinuous,
			tgp.show_points as showpoints,class as type from tag_group as tg,tag_group_properties as tgp where tg.id=tgp.tag_group_id and
			tgp.user_id=""" + new Long(userId))
		return tagGroups
	}

}
