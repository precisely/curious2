package us.wearecurio.services

import com.causecode.fileuploader.CDNProvider
import com.causecode.fileuploader.FileUploaderService
import grails.util.Environment
import grails.util.Holders
import org.apache.commons.logging.LogFactory
import org.grails.plugins.elasticsearch.ElasticSearchService
import org.springframework.transaction.annotation.Transactional
import us.wearecurio.data.UnitGroupMap
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.Discussion
import us.wearecurio.model.DurationType
import us.wearecurio.model.Entry
import us.wearecurio.model.Identifier
import us.wearecurio.model.Model
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.PlotData
import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.model.TagProperties
import us.wearecurio.model.TagStats
import us.wearecurio.model.TagUnitStats
import us.wearecurio.model.TagValueStats
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.model.VerificationStatus
import us.wearecurio.server.Migration
import us.wearecurio.utility.Utils

class MigrationService {

	private static def log = LogFactory.getLog(this)

	static transactional = false
	
	public static final long TEST_MIGRATION_ID = 30L
	public static final long SKIP_INITIAL_MIGRATIONS_ID = 31L
	public static final long MIGRATE_REPEATS_ID = 31L
	public static final long FIX_OAUTH_UNIQUE_CONSTRAINT_ID = 32L
	public static final long REPEAT_END_NULLABLE_ID = 36L
	public static final long NULLIFY_OLD_REPEATS_ID = 42L
	public static final long RANGE_OLD_REPEATS_ID = 43L
	public static final long REPEAT_END_INDEX_ID = 44L
	public static final long CREATE_TIME_ZONES_ID = 46L
	public static final long CREATE_CONCRETE_GHOSTS_ID = 47L
	public static final long CHANGE_CONTINUOUS_REPEATS_TO_PINNED_ID = 50L
	public static final long CHANGE_TWENTY3ANDME_DATA_TYPE = 52L
	public static final long CHANGE_TOKEN_FIELD_LENGTH = 53L
	public static final long RECOMPUTE_FIRST_POSTS = 55L
	public static final long CHANGE_NOTIFICATION_TABLE_NAME = 56L
	public static final long ADD_TYPE_ID_FIELD_TO_NOTIFICATION_TABLE = 57L
	public static final long REMOVE_23ANDME_UNIQUE_CONSTRAINT = 59L
	public static final long FIX_DATEPRECISION = 60L
	public static final long FIX_DATEPRECISION2 = 61L
	public static final long REMOVE_DUPLICATE_IMPORTS = 65L
	public static final long REMOVE_OBSOLETE_PINNED = 71L
	public static final long RESET_WITHINGS_ACCOUNTS = 72L
	public static final long REMOVE_DUPLICATE_IMPORTSB = 74L
	public static final long RESET_WITHINGS_ACCOUNTSB = 75L
	public static final long FIX_WITHINGS_SUMMARIES = 76L
	public static final long FIX_REPEAT_END = 77L
	public static final long ELIMINATE_AT_UNITS_AND_REPEAT_UNITS = 78L
	public static final long REMOVE_VERSION = 80L
	public static final long ADD_TAG_UNIT_STATS = 81L
	public static final long FIX_TAG_PROPERTIES = 84L
	public static final long FIX_TAG_PROPERTIES2 = 85L
	public static final long HISTORICAL_INTRA_DAY = 86L
	public static final long DROP_EXTRA_TAG_XREF = 88L
	public static final long REMOVE_OBSOLETE_ENTRY_FIELDS = 89L
	public static final long ADD_TAG_UNIT_STATS_AGAIN = 90L
	public static final long SHARED_TAG_GROUP = 91L
	public static final long MIGRATION_CODES = 92L

	DatabaseService databaseService
	WithingsDataService withingsDataService
	ElasticSearchService elasticSearchService
	EntryParserService entryParserService
	def elasticSearchAdminService
	OuraDataService ouraDataService
	UrlService urlService
	EmailService emailService
	OauthAccountService oauthAccountService
	FileUploaderService fileUploaderService
	def grailsApplication
	LegacyOuraDataService legacyOuraDataService
	
	boolean skipMigrations = false
	
	@Transactional
	public def sql(String statement, args = [:]) {
		return databaseService.sqlNoRollback(statement, args)
	}
	
	@Transactional
	public def sqlRows(String statement, args = [:]) {
		return databaseService.sqlRows(statement, args)
	}

	@Transactional
	public def eachRow(String statement, Closure c) {
		databaseService.eachRow(statement, c)
	}
	
	@Transactional
	public def shouldDoMigration(def code) {
		def migration = Migration.findByTag("" + code)
		if (migration && migration.getHasRun()) {
			return null
		}
		if (migration) return migration
		return Migration.create(code)
	}
	
	@Transactional
	def didMigration(Migration migration) {
		migration.setHasRun(true)
		Utils.save(migration, true)
		
		log.debug("Finished migration" + migration)
	}
	
	protected def toUrl(map) {
		map.controller = map.controller ? map.controller : name
		return urlService.make(map, request)
	}
	
	public def tryMigration(def code, Closure closure) {
		def migration
		
		migration = shouldDoMigration(code)
		
		if (migration) {
			log.debug("Starting migration: " + migration)
			
			def retVal = true
			
			try {
				if (!skipMigrations)
					retVal = closure()
			} catch (Exception e) {
				System.err.println("EXCEPTION DURING MIGRATION " + code)
				e.printStackTrace()
				return false
			}

			didMigration(migration)
				
			return retVal
		}
		
		return false
	}

	int getEntryCountWithCommentAndIdentifier(String thirdParty, Identifier identifier) {
		int totalEntries = Entry.withCriteria {
			// Comments are wrapped in parenthesis like (Oura).
			eq('comment', "($thirdParty)")

			or {
				ne('setIdentifier', identifier)
				isNull('setIdentifier')
			}

			isNotNull('userId')

			projections {
				rowCount()
			}
		} [0]

		return totalEntries
	}

	public void updateThirdPartyEntryIdentifiers(String thirdParty) {
		Identifier identifier = Identifier.look(thirdParty)
		Long identifierId = identifier.id

		int totalEntries = getEntryCountWithCommentAndIdentifier(thirdParty, identifier)

		log.debug "Updating ${totalEntries} entries with valid user for $thirdParty to setIdentifier ${identifierId}"

		int totalUpdatedEntries = 0
		while (totalEntries > 0) {
			Thread.sleep(1000)

			int rowsAffected = sql("UPDATE entry SET set_identifier=${identifierId} WHERE comment='" +
					"($thirdParty)' AND (set_identifier!=${identifierId} OR set_identifier IS NULL) AND " +
					"user_id is not null ORDER BY date DESC LIMIT 40000")

			if (rowsAffected > 0) {
				totalUpdatedEntries += rowsAffected
			}

			totalEntries -= 40000
		}

		log.debug "Updated ${totalUpdatedEntries} entries for $thirdParty"
	}

	public void deleteThirdPartyEntryIdentifiers(String identifierValue) {
		int totalIdentifiers = Identifier.countByValueLike(identifierValue)

		log.debug "Deleting ${totalIdentifiers} Identifiers with value like: $identifierValue"

		int totalDeletedIdentifiers = sql("DELETE FROM identifier WHERE value LIKE BINARY '$identifierValue'")

		log.debug "Deleted ${totalDeletedIdentifiers} Identifiers with value like: $identifierValue"
	}

	def doMigrations() {
		if (Environment.getCurrent().equals(Environment.TEST))
			return; // don't run in test environment

		tryMigration(SKIP_INITIAL_MIGRATIONS_ID) {
			// if this is running on a brand new instance, skip initial migrations
			skipMigrations = true
		}
		tryMigration(MIGRATE_REPEATS_ID) {
			sql('UPDATE entry SET repeat_type = 5 WHERE repeat_type = 3')
			sql('UPDATE entry SET repeat_type = 6 WHERE repeat_type = 4')
		}
		tryMigration(FIX_OAUTH_UNIQUE_CONSTRAINT_ID) {
			sql('alter table oauth_account drop index user_id')
		}
		tryMigration(REPEAT_END_NULLABLE_ID) {
			sql('ALTER TABLE entry CHANGE COLUMN repeat_end repeat_end datetime DEFAULT NULL')
			sql("update entry set repeat_end = null where repeat_end = '0000-00-00 00:00:00'")
		}
		tryMigration(NULLIFY_OLD_REPEATS_ID) {
			def rows = sqlRows("select entry.id from entry where repeat_type is not null")
			
			for (row in rows) {
				Entry entry = Entry.get(row['id'])
				
				entry.createRepeat()
				
				Utils.save(entry, true)
			}
		}
		tryMigration(RANGE_OLD_REPEATS_ID) {
			def rows = sqlRows("select entry.id from entry where repeat_end is null and repeat_type is not null and date < '2013-10-01'")
			
			for (row in rows) {
				Entry entry = Entry.get(row['id'])
				
				entry.repeatEnd = entry.date + 1
				
				Utils.save(entry, true)
			}
		}
		tryMigration(REPEAT_END_INDEX_ID) {
			sql("create index repeat_end_index ON entry (repeat_end)")
		}
		tryMigration(CREATE_TIME_ZONES_ID) {
			def entries = Entry.list()
			
			for (Entry e in entries) {
				if (e.getTimeZoneId() == null) {
					def timeZoneName = TimeZoneId.guessTimeZoneName(e.getDate(), e.getTimeZoneOffsetSecs())
					
					def timeZoneId = TimeZoneId.look(timeZoneName)
					
					e.setTimeZoneId((int)timeZoneId.getId())
					
					Utils.save(e)
				}
			}
		}
		tryMigration(CREATE_CONCRETE_GHOSTS_ID) {
			def rows = sqlRows("select entry.id from entry where repeat_type in (:repeatIds)", [repeatIds:Entry.REPEAT_IDS])
			
			for (row in rows) {
				Entry entry = Entry.get(row['id'])
				
				entry.getRepeatType().makeConcreteGhost()
				
				Utils.save(entry, true)
			}
		}
		tryMigration(CHANGE_CONTINUOUS_REPEATS_TO_PINNED_ID) {
			sql("update entry set comment = 'favorite' where repeat_type in (:repeatIds)", [repeatIds:Entry.CONTINUOUS_IDS])
		}
		tryMigration(CHANGE_TWENTY3ANDME_DATA_TYPE) {
			sql("ALTER TABLE twenty3and_me_data CHANGE data data mediumblob")
		}
		tryMigration(CHANGE_TOKEN_FIELD_LENGTH) {
			sql("alter table oauth_account change column access_token access_token varchar(1024)")
		}
		tryMigration(RECOMPUTE_FIRST_POSTS) {
			sql("update discussion set first_post_id = NULL")
		}
		tryMigration(CHANGE_NOTIFICATION_TABLE_NAME) {
			sql("DROP TABLE third_party_notification")	// Deleting blank table created by mapping.
			sql("RENAME TABLE fitbit_notification TO third_party_notification")
		}
		tryMigration(ADD_TYPE_ID_FIELD_TO_NOTIFICATION_TABLE) {
			sql("ALTER TABLE third_party_notification ADD COLUMN type_id int(11) NOT NULL DEFAULT 2")
		}
		tryMigration(REMOVE_23ANDME_UNIQUE_CONSTRAINT) {
			sql("DROP INDEX profile_id ON twenty3and_me_data")
		}
		tryMigration(FIX_DATEPRECISION) {
			sql("UPDATE entry SET date_precision_secs = " + Entry.DEFAULT_DATEPRECISION_SECS + " WHERE date_precision_secs IS NULL")
		}
		tryMigration(FIX_DATEPRECISION2) {
			sql("UPDATE entry SET date_precision_secs = " + Entry.DEFAULT_DATEPRECISION_SECS + " WHERE date_precision_secs IS NULL")
		}
		tryMigration(REMOVE_DUPLICATE_IMPORTS) {
			boolean hasRows = true
			
			while (hasRows) {
				hasRows = false
				def rows = sqlRows("select _user.username, entry.id, date, tag.description, set_name, count(entry.id) as c from entry, tag, _user where _user.id = entry.user_id and set_name IS NOT NULL and user_id > 0 and entry.tag_id = tag.id group by user_id, date, tag_id, amount, units, comment, set_name having c > 1 order by date")
				for (row in rows) {
					hasRows = true
					
					log.debug "Deleting duplicate entry " + row.username + ", " + row.date + ", " + row.id + ", " + row.description + ", " + row.set_name
					
					Entry entry = Entry.get(row['id'])
					
					Entry.delete(entry, null)
				}
			}
		}
		tryMigration(REMOVE_OBSOLETE_PINNED) {
			sql("update entry set user_id = 0 where entry.user_id is not null and entry.user_id > 0 and entry.repeat_end is not null and entry.repeat_type in (?)",
					[Entry.CONTINUOUS_IDS])
		}
		tryMigration(RESET_WITHINGS_ACCOUNTS) {
			sql("delete from oauth_account where type_id = 1")
		}
		tryMigration(REMOVE_DUPLICATE_IMPORTSB) {
			boolean hasRows = true
			
			while (hasRows) {
				hasRows = false
				def rows = sqlRows("select _user.username, entry.id, date, tag.description, set_name, count(entry.id) as c from entry, tag, _user where _user.id = entry.user_id and set_name IS NOT NULL and user_id > 0 and entry.tag_id = tag.id group by user_id, date, tag_id, amount, units, comment, set_name having c > 1 order by date")
				for (row in rows) {
					hasRows = true
					
					log.debug "Deleting duplicate entry " + row.username + ", " + row.date + ", " + row.id + ", " + row.description + ", " + row.set_name
					
					Entry entry = Entry.get(row['id'])
					
					Entry.delete(entry, null)
				}
			}
		}
		tryMigration(RESET_WITHINGS_ACCOUNTSB) {
			sql("delete from oauth_account where type_id = 1")
			sql("delete from entry where set_name = 'withings import'")
		}
		tryMigration(FIX_WITHINGS_SUMMARIES) {
			sql("update entry e inner join tag t on e.tag_id = t.id set date = date_add(date, interval 721 minute), date_precision_secs = 86400 where right(t.description, 8) = ' summary' and comment = '(Withings)'")
			sql("update entry e set units = 'cal' where e.units = 'kcal' and e.comment = '(Withings)'")
		}
		tryMigration(FIX_REPEAT_END) {
			sql("update entry e set repeat_end = date where repeat_end < date")
		}
		tryMigration(ELIMINATE_AT_UNITS_AND_REPEAT_UNITS) {
			sql("update entry e set units = '' where units = 'at'")
			def entries = Entry.findAllByUnits("repeat")
			for (e in entries) {
				def map = e.entryMap()
				def (remain, comment, repeatType) = Entry.matchRepeatStr(map['units'])
				if (repeatType != null) {
					map['units'] = remain
					map['repeatType'] = repeatType
					Entry.appendComment(map, comment)
				}

				e.update(map, null)
			}
		}
		tryMigration(REMOVE_VERSION) {
			sql ("alter table _user drop column version")
			sql ("alter table anonymous_author drop column version")
			sql ("alter table correlation drop column version")
			sql ("alter table curious_series drop column version")
			sql ("alter table date_record drop column version")
			sql ("alter table discussion drop column version")
			sql ("alter table discussion_author drop column version")
			sql ("alter table discussion_post drop column version")
			sql ("alter table entry drop column version")
			sql ("alter table entry_tag drop column version")
			sql ("alter table group_member_admin drop column version")
			sql ("alter table group_member_default_for drop column version")
			sql ("alter table group_member_discussion drop column version")
			sql ("alter table group_member_notified drop column version")
			sql ("alter table group_member_notified_major drop column version")
			sql ("alter table group_member_reader drop column version")
			sql ("alter table group_member_writer drop column version")
			sql ("alter table migration drop column version")
			sql ("alter table model drop column version")
			sql ("alter table oauth_account drop column version")
			sql ("alter table password_recovery drop column version")
			sql ("alter table plot_data drop column version")
			sql ("alter table push_notification_device drop column version")
			sql ("alter table session drop column version")
			sql ("alter table stats drop column version")
			sql ("alter table tag drop column version")
			sql ("alter table tag_group drop column version")
			sql ("alter table tag_group_properties drop column version")
			sql ("alter table tag_group_tag drop column version")
			sql ("alter table tag_properties drop column version")
			sql ("alter table tag_stats drop column version")
			sql ("alter table third_party_notification drop column version")
			sql ("alter table time_zone_id drop column version")
			sql ("alter table twenty3and_me_data drop column version")
			sql ("alter table twitter_data drop column version")
			sql ("alter table user_group drop column version")
			sql ("alter table user_time_zone drop column version")
		}
		tryMigration(FIX_TAG_PROPERTIES2) {
			sql ("ALTER TABLE `tag_properties` DROP COLUMN `data_type_computed`, DROP COLUMN `data_type_manual`")
			sql ("ALTER TABLE `tag_properties` ADD COLUMN `data_type_computed` INT NULL DEFAULT NULL	, ADD COLUMN `data_type_manual` INT NULL DEFAULT NULL")
		}
		tryMigration(HISTORICAL_INTRA_DAY) {
			sql("delete from entry where set_name like 'withings import%'")
			sql("update oauth_account set last_polled = null where type_id = 1")
		}
		tryMigration(DROP_EXTRA_TAG_XREF) {
			sql("drop table `tlb_dev`.`entry_tag`")
		}
		tryMigration(REMOVE_OBSOLETE_ENTRY_FIELDS) {
			try {
				sql ("ALTER TABLE `entry` DROP COLUMN `tweet_id`, DROP COLUMN `time_zone_offset_ticks`")
			} catch (Throwable t) {
			}
			def rows = sqlRows("select entry.id, entry.set_name from entry where set_name IS NOT NULL")
			for (row in rows) {
				String setName = row['set_name']
				Identifier identifier = Identifier.look(setName)
				sql ("UPDATE entry set entry.set_identifier_id = :id where entry.id = :entryid", [id:identifier?.getId(), entryid:row['id']])
			}
			sql ("ALTER TABLE `entry` DROP COLUMN `set_name`")
		}
		tryMigration("New string migration codes") {
			try {
				sql ("ALTER TABLE `migration` CHANGE COLUMN `tag` tag varchar(255) DEFAULT NULL")
			} catch (Throwable t) {
			}
			try {
				sql ("UPDATE migration SET tag = CAST(code AS char)")
			} catch (Throwable t) {
			}
			try {
				sql ("ALTER TABLE `migration` DROP COLUMN `code`")
			} catch (Throwable t) {
			}
		}
		tryMigration("Null discussion name means skip in discussion list") {
			try {
				sql ("ALTER TABLE `discussion` CHANGE COLUMN `name` name varchar(255) DEFAULT NULL")
			} catch (Throwable t) {
			}
			try {
				sql ("UPDATE `discussion` SET name = NULL where name = 'New question or discussion topic?'")
			} catch (Throwable t) {
			}
		}
		tryMigration("Nullable unit group id") {
			try {
				sql ("ALTER TABLE `tag_unit_stats` CHANGE COLUMN `unit_group_id` unit_group_id bigint(20) DEFAULT NULL")
			} catch (Throwable t) {
			}
		}
		tryMigration("Add unit index to TagUnitStats") {
			sql("create index unit_index ON tag_unit_stats (unit)")
		}
		tryMigration("Change accountId field length") {
			sql("alter table oauth_account change column account_id account_id varchar(24)")
		}
		tryMigration("Change continuous repeats back to pinned") {
			sql("update entry set comment = 'pinned' where repeat_type in (:repeatIds)", [repeatIds:Entry.CONTINUOUS_IDS])
		}
		tryMigration("Add admin users") {
			UserGroup.lookupOrCreateSystemGroup().addAdmin(User.findByUsername("x"))
		}
		tryMigration("Remove user location") {
			try {
				sql("ALTER TABLE `user` DROP COLUMN `location`")
			} catch (Throwable t) {
			}

		}
		tryMigration("Change username length") {
			try {
				sql("ALTER TABLE `user` CHANGE COLUMN `username` username varchar(80) DEFAULT NULL")
			} catch (Throwable t) {
			}

		}
		tryMigration("Modify DurationType") {
			try {
				def entries = Entry.findAllByUnitsAndDurationType("hours", DurationType.NONE)
				
				for (Entry entry : entries) {
					if (entry.oldFetchIsGenerated()) {
						entry.setDurationType(DurationType.GENERATEDDURATION)
						Utils.save(entry, true)
					}
				}
			} catch (Throwable t) {
			}
		}
		tryMigration("Drop Session version") {
			sql ("alter table session drop column version")
		}
		tryMigration("Drop Sprint version") {
			sql ("alter table sprint drop column version")
		}
		tryMigration("Remove old discussion author columns") {
			try {
				sql("ALTER TABLE `discussion_author` DROP COLUMN `authorid`")
				sql("ALTER TABLE `discussion_author` DROP COLUMN `userid`")
			} catch (Throwable t) {
			}
		}
		tryMigration("Get rid of first and last name fields") {
			try {
				sql("UPDATE _user SET name = CONCAT(first, ' ', last)")
			} catch (Throwable t) {
			}
			try {
				sql("ALTER TABLE _user DROP COLUMN first")
				sql("ALTER TABLE _user DROP COLUMN last")
			} catch (Throwable t) {
			}
		}
		tryMigration("Get rid of anonymous author") {
			try {
				sql("ALTER TABLE discussion_post DROP COLUMN author_author_id")
			} catch (Throwable t) {
			}
		}
		tryMigration("Set user virtual to false") {
			try {
				sql("UPDATE _user SET virtual = false WHERE virtual IS NULL")
			} catch (Throwable t) {
			}
		}
		tryMigration("Drop unique constraint on user email") {
			try {
				sql("ALTER TABLE _user DROP INDEX email_idx")
				sql("CREATE INDEX email_idx ON _user(email) USING BTREE")
			} catch (Throwable t) {
			}
		}
		tryMigration("Update hash for all instances of discussion, sprint and user table") {
			try {
				List discussions = Discussion.findAll()
				
				for (Discussion discussion : discussions) {
					discussion.hash = new DefaultHashIDGenerator().generate(12)
					Utils.save(discussion, true)
				}
			} catch (Throwable t) {
			}
			try {
				List users = User.findAll()
				
				for (User user : users) {
					user.hash = new DefaultHashIDGenerator().generate(12)
					Utils.save(user, true)
				}
			} catch (Throwable t) {
			}
			try {
				List sprints = Sprint.findAll()
				
				for (Sprint sprint : sprints) {
					sprint.hash = new DefaultHashIDGenerator().generate(12)
					Utils.save(sprint, true)
				}
			} catch (Throwable t) {
			}
		}
		tryMigration("Drop EntryGroup version") {
			sql ("alter table entry_group drop column version")
		}
		tryMigration("Drop TagProperties isContinuous") {
			sql ("alter table tag_properties drop column is_continuous")
		}
		tryMigration("Drop Discussion isPublic") {
			sql ("alter table discussion drop column is_public")
		}
		tryMigration("Remove user group unique constraint") {
			sql('alter table user_group drop index full_name')
		}
		tryMigration("Change repeat type column") {
			if (sql('select * from entry where repeat_type is null limit 1') > 0) {
				if (sql('alter table entry change repeat_type repeat_type_id int(11)') < 0) {
					sql('alter table entry drop column repeat_type_id')
					sql('alter table entry change repeat_type repeat_type_id int(11)')
				}
			}
		}
		tryMigration("Nullable userId for UserActivity") {
			try {
				sql ("ALTER TABLE `user_activity` CHANGE COLUMN `user_id` user_id bigint(20) DEFAULT NULL")
			} catch (Throwable t) {
			}
		}
		tryMigration("Create virtual user groups") {
			for (User user in User.list()) {
				if (user.virtualUserGroupIdFollowers == null || user.virtualUserGroupIdDiscussions == null) {
					user.fetchVirtualUserGroupIdDiscussions()
					user.fetchVirtualUserGroupIdFollowers()
					Utils.save(user, true)
				}
			}
		}
		tryMigration("Add newuser tag to all accounts and flush") {
			Tag tag = Tag.look('newuser')
			for (User user in User.list()) {
				user.addInterestTag(tag)
				Utils.save(user, true)
			}
		}
		tryMigration("Change continuous repeats to bookmark") {
			sql("update entry set comment = 'bookmark' where comment = 'pinned'")
		}
		tryMigration("Rename [weight] and [duration] tags 3") {
			for (Tag tag in Tag.list()) {
				if (tag.description.endsWith('[weight]')) {
					tag.description = tag.description.replaceAll('\\[weight\\]', '\\[amount\\]')
					Utils.save(tag, true)
				}
				if (tag.description.endsWith('[duration]')) {
					tag.description = tag.description.replaceAll('\\[duration\\]', '\\[time\\]')
					Utils.save(tag, true)
				}
			}
			Map<String, Tag> map = new HashMap<String, Tag>()
			Set<Tag> duplicates = new HashSet<Tag>()
			for (Tag tag in Tag.list()) {
				Tag other = map.get(tag.description)
				if (other != null) {
					duplicates.add(tag)
				} else {
					map.put(tag.description, tag)
				}
			}
			for (Tag duplicate in duplicates) {
				Tag originalTag = map.getAt(duplicate.description)
				def duplicateEntries = Entry.findAllByTag(duplicate)
				for (Entry entry in duplicateEntries) {
					entry.tag = originalTag
					Utils.save(entry, true)
				}
				duplicate.delete(flush:true)
			}
			map = map
		}
		tryMigration("Add empty first post to all discussions") {
			Discussion.list().each { discussion ->
				log.debug "Checking first post for $discussion"

				if (!discussion.getFirstPost()) {
					log.debug "No first post found for $discussion"
					discussion.createPost(User.get(discussion.fetchUserId()), "")
				}
			}
		}
		tryMigration("Remove version from Survey domains") {
			["survey_question", "survey_answer", "user_survey_answer"].each { table ->
				sql ("alter table ${table} drop column version")
			}
		}
		tryMigration("Migrate PlotData") {
			for (PlotData plotData in PlotData.list()) {
				plotData.recompressAndSave()
			}
		}
		tryMigration("Migrate PlotData 2") {
			sql("ALTER TABLE `plot_data` CHANGE COLUMN `json_plot_data` `json_plot_data` MEDIUMTEXT NULL DEFAULT NULL")
		}
		tryMigration("Mark all users email verified") {
			sql("alter table _user drop column is_verified")
			sql("update _user set email_verified = :status", [status: VerificationStatus.VERIFIED.id])
		}
		tryMigration("Set last polled for all OAuthAccounts & add not null constraint") {
			// Set last polled to (now - 4 days)
			sql("update oauth_account set last_polled = (now() - interval 4 day) where last_polled is null")
			sql("ALTER TABLE oauth_account MODIFY COLUMN last_polled datetime NOT NULL")
		}
		tryMigration("Fix typo in fullName in user_group") {
			sql("update user_group set full_name = replace(full_name, 'grouo', 'group')")
		}
		tryMigration("Re-import Oura data from beginning to fix duration entries - take two") {
			OAuthAccount.withCriteria {
				eq("typeId", ThirdParty.OURA)
				ne("accessToken", "")
			}.each { oauthAccount ->
				log.debug "Updating lastPolled for $oauthAccount"

				User userInstance = User.get(oauthAccount.userId)
				Date firstMarch = new Date("03/01/2016")

				Date lastPolled = firstMarch > userInstance.created ? firstMarch : userInstance.created
				log.debug "New last polled is [$lastPolled]"

				oauthAccount.lastPolled = lastPolled
				Utils.save(oauthAccount, true)
			}
		}
		tryMigration("Re-add discussions to public 3") {
			UserGroup group = UserGroup.findByFullName('Curious Discussions')
			Date cutoff = new Date() - 60
			for (Discussion discussion in Discussion.list()) {
				if (discussion.created > cutoff) {
					User user = User.get(discussion.userId)
					if (user != null) {
						log.debug "Adding discussion to public " + discussion
						discussion.setIsPublic(true)
						group.addDiscussion(discussion)
						group.updateWriter(user)
						Utils.save(discussion)
					}
				}
			}
		}
		tryMigration("Re-index all sprints") {
			for (Sprint sprint in Sprint.list()) {
				log.debug "Reindex sprints"

				if (sprint.deleted)
					continue
				
				sprint.reindex()
			}
		}
		tryMigration("Change UFile path type to longText") {
			sql("ALTER TABLE `ufile` MODIFY `path` TEXT NOT NULL")
		}
		tryMigration("Move all avatar files from RackSpace to Google Cloud Storage") {
			String containerName = grailsApplication?.config?.fileuploader?.groups?.avatar?.container
			if (containerName) {
				if (Environment.current == Environment.DEVELOPMENT) {
					containerName = containerName + "-development"
				}
				log.debug "Move to container $containerName"
				fileUploaderService.moveToNewCDN(CDNProvider.GOOGLE, containerName)
			} else {
				log.error "Container not defined. Please fix and rerun the migration."
			}
		}

		tryMigration('Update size of account_id column in OAuthAccount domain') {
			sql("ALTER TABLE oauth_account MODIFY COLUMN account_id varchar(32)")
		}
	}
	
	/**
	 * Migrations intended to run in a separate thread, to allow server to finish bootstrapping
	 */
	def doBackgroundMigrations() {
		if (Environment.getCurrent().equals(Environment.TEST))
			return; // don't run in test environment

		if (Environment.isDevelopmentMode() && Holders.getFlatConfig()["background.migration"] == false) {
			// Don't run in development environment if a config "background.migration = false" is available
			return
		}

		tryMigration(ADD_TAG_UNIT_STATS_AGAIN) {
			try {
				sql ("ALTER TABLE `tag_unit_stats` DROP COLUMN `unit_group`")
			} catch (Throwable t) {
			}
			try {
				sql('ALTER TABLE tag_unit_stats CHANGE COLUMN unit_group_id unit_group_id bigint(20) DEFAULT NULL')
			} catch (Throwable t) {
			}
			sql('DELETE FROM tag_unit_stats') // clear current list of tag unit stats

			def users = User.list()
			for (u in users) {
				def c = Entry.createCriteria()
				def entries = c {
					and {
						eq("userId", u.getId())
						not {
							or {
								isNull("units")
								eq("units",'')
							}
						}
					}
				}
				for (e in entries) {
					Model.withTransaction {
						log.debug "Adding TagUnitStats for user ${e.userId}, tag ${e.tag.description}, ${e.units}"
						def tagUnitStats =
							TagUnitStats.createOrUpdate(e.userId, e.baseTag.getId(), e.units == null?'':e.units)
					}
				}
			}
		}
		tryMigration("Shared Tag Group") {
			UserGroup.lookupOrCreateSystemGroup()
			UserGroup systemGroup = UserGroup.lookup(UserGroup.SYSTEM_USER_GROUP_NAME)
			systemGroup.addAdmin(User.findByUsername("x"))
			systemGroup.addAdmin(User.findByUsername("heatheranne"))
			systemGroup.addAdmin(User.findByUsername("linda"))
			systemGroup.addAdmin(User.findByUsername("kim"))
			systemGroup.addAdmin(User.findByUsername("kimdavis"))
			systemGroup.addAdmin(User.findByUsernameIlike("%vishesh%"))
			sql("ALTER TABLE tag_group_properties MODIFY COLUMN user_id bigint;")
		}
		tryMigration("Remove correlation table since we're using analytics_correlation instead.") {
			sql("DROP TABLE IF EXISTS correlation");
		}
		tryMigration("Change analytics_task.status and analytics_task.type to integer.") {
			sql("alter table analytics_task drop column status2;");
			sql("alter table analytics_task change status status2 varchar(255) null default null;")
			sql("alter table analytics_task add status int(11) NULL default NULL;")
			sql("update analytics_task set status = 3 where status2='new';")
			sql("update analytics_task set status = 5 where status2='completed';")
			sql("alter table analytics_task drop status2;")

			sql("alter table analytics_task drop column type2;")
			sql("alter table analytics_task change type type2 varchar(255) null default null;")
			sql("alter table analytics_task add type int(11) NULL default NULL;")
			sql("update analytics_task set type=8 where type2='collection-parent';")
			sql("update analytics_task set type = 9 where type2='collection-child';")
			sql("alter table analytics_task drop type2;")
		}
		tryMigration("Index elasticsearch again") {
			elasticSearchService.index()
		}
		tryMigration("Recompute entry base tags") {
			sql("update entry e set units = '' where e.units in ('at','am','pm','om','repeat','remind','midnight','noon','start','stop','end','undefined','round','with','while','-')")
			sql("update entry e set units = 'mU/ul' where e.units in ('m/ul')")

			def rows = sqlRows("select entry.id from entry where entry.tag_id = entry.base_tag_id and entry.units is not null and length(entry.units) > 0")

			for (row in rows) {
				Entry entry = Entry.get(row['id'])

				String suffix = UnitGroupMap.theMap.suffixForUnits(entry.units)

				//if (suffix == entry.units) {
				//	System.err.println("ANOMALY: " + suffix)
				//}

				String description = entry.tag.description

				if (description.endsWith(' ' + suffix)) {
					entry.baseTag = Tag.look(description.substring(0, description.length() - (suffix.length() + 1)))
				} else {
					entry.tag = entryParserService.tagWithSuffixForUnits(entry.baseTag, entry.units, 0)
				}

				Utils.save(entry, true)
			}
		}
		tryMigration("Recompute null base tags") {
			def rows = sqlRows("select entry.id from entry where entry.base_tag_id is null")

			for (row in rows) {
				Entry entry = Entry.get(row['id'])

				if (entry.units) {
					Tag origTag = entry.tag
					entry.tag = entryParserService.tagWithSuffixForUnits(entry.tag, entry.units, 0)
					entry.baseTag = origTag
				} else {
					entry.baseTag = entry.tag
				}

				Utils.save(entry, true)
			}
		}
		tryMigration("Recompute all base tags with new suffix format again") {
			def rows = sqlRows("select entry.id from entry where entry.user_id is not null")

			for (row in rows) {
				Entry entry = Entry.get(row['id'])

				if (entry.units) {
					def tags = entryParserService.baseTagAndTagWithSuffixForUnits(entry.tag, entry.units, 0)
					entry.baseTag = tags[0]
					entry.tag = tags[1]
					Utils.save(entry, true)
				} else if (entry.baseTag != entry.tag) {
					entry.baseTag = entry.tag
					Utils.save(entry, true)
				}
			}
		}
		tryMigration("Clear and recompute tag values stats for everyone") {
			sql("delete from tag_stats")
			sql("delete from tag_unit_stats")
			sql("delete from tag_value_stats")

			def users = User.list()

			for (u in users) {
				log.debug("Recomputing value stats for user " + u.id)
				TagStats.updateTagStats(u)
				TagUnitStats.updateTagUnitStats(u)
				TagValueStats.updateTagValueStats(u)
			}
		}
		tryMigration("Update UserActivity typeid") {
			sql("update user_activity set type_id = 262 where type_id = 260")
			sql("update user_activity set type_id = 263 where type_id = 261")
		}
		tryMigration("Reclassify tag properties5") {
			TagProperties.reclassifyAll()
		}
		tryMigration("Create virtual follower groups for discussions") {
			for (Discussion discussion in Discussion.list()) {
				if (discussion.createVirtualObjects())
					Utils.save(discussion, true)
			}
		}
		tryMigration("Re-import Oura data") {
			ouraDataService.pollAll()
		}
		tryMigration("Refresh all oauth accounts again 3") {
			oauthAccountService.refreshAll()
		}

		tryMigration("Remove all notifications from Oura test dgdfgdfr") {
			int totalNotifications = ThirdPartyNotification.countByTypeId(ThirdParty.OURA)
			while (totalNotifications > 0) {
				Thread.sleep(1000)
				sql("delete from third_party_notification where type_id = 9 limit 10000")
				totalNotifications -= 10000
			}
		}

// - - - - - - - - - - - - - - - - - Re-run old failed migration- - - - - - - - - - - - - - - - - - - - - - - - - - - -

		tryMigration("New string migration codes Updated") {
			if (sql("ALTER TABLE migration MODIFY COLUMN tag varchar(255) DEFAULT NULL") >= 0) {

				if (sql("UPDATE migration SET tag = CAST(code AS char) WHERE code IS NOT NULL AND" +
						" (tag IS NULL OR tag = '')") > 0) {

					sql("ALTER TABLE migration DROP COLUMN code")
				}
			}
		}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

// - - - - - - - - - - - - - - - - Migrations for Improving Performance - - - - - - - - - - - - - - - - - - - - - - - -

		/*
		 * Cleaning up the database by removing entries with no user and date older than 30 days.
		 * For a total of nearly 9800000+ entries without userId and date older than 30 days, migration will 
		 * approximately take 2-3 hrs to complete.
		 */
		tryMigration('Delete all entries with no user and date older than 30 days') {
			log.debug "Total Entry count before deletion is ${Entry.count()}"

			Date thirtyDayAgo = new Date() - 30
			int totalEntries = Entry.countByDateLessThanAndUserIdIsNull(thirtyDayAgo)

			log.debug "Deleting ${totalEntries} entries with no user and date less than ${thirtyDayAgo}"

			int totalDeletedEntries = sql("DELETE FROM entry WHERE date < :date AND user_id IS NULL",
					[date: thirtyDayAgo])

			log.debug "Deleted total ${totalDeletedEntries} entries."

			log.debug "Total Entry count after deletion ${Entry.count()}"
		}

		/**
		 * Update all device entries with new Identifier.
		 */
		tryMigration("Update entries for Oura with new Identifier") {
			updateThirdPartyEntryIdentifiers('Oura')
		}

		tryMigration("Update entries for Withings with new Identifier") {
			updateThirdPartyEntryIdentifiers('Withings')
		}

		tryMigration("Update entries for FitBit with new Identifier") {
			updateThirdPartyEntryIdentifiers('FitBit')
		}

		tryMigration("Update entries for Jawbone Up with new Identifier") {
			updateThirdPartyEntryIdentifiers('Jawbone Up')
		}

		tryMigration("Update entries for Moves with new Identifier") {
			updateThirdPartyEntryIdentifiers('Moves')
		}

		/*
		* Preventing the delete calls for Identifiers from DataIntegrationViolationException. Making sure that none of
		* the Identifiers being deleted are referenced in any of the entries. The entries with userId has already
		* been assigned new Identifiers, hence the entries left with no userId (if any after the above migration)
		* should be having the new Identifier.
		*/
		tryMigration('Update entries with no user to new set_identifier') {
			log.debug 'Updating set_identifiers for entries with no user so that old identifiers can be deleted.'

			['Oura', 'Withings', 'Moves', 'FitBit', 'Jawbone Up'].each { String thirdParty ->
				int totalEntries = Entry.countByCommentAndUserIdIsNull("($thirdParty)")

				Identifier identifier = Identifier.look(thirdParty)
				Long identifierId = identifier.id

				log.debug "Updating ${totalEntries} entries with no user for $thirdParty to setIdentifier " +
						"${identifierId} to prevent delete calls"

				int totalUpdatedEntries = 0
				while (totalEntries > 0) {
					Thread.sleep(3000)
					
					int rowsAffected = sql("UPDATE entry SET set_identifier=${identifierId} WHERE comment='" +
							"($thirdParty)' AND (set_identifier!=${identifierId} OR set_identifier IS NULL)" +
							"AND user_id IS NULL ORDER BY date DESC LIMIT 40000")

					if (rowsAffected > 0) {
						totalUpdatedEntries += rowsAffected
					}

					totalEntries -= 40000
				}

				log.debug "Updated ${totalUpdatedEntries} entries for $thirdParty to prevent delete calls."
			}
		}

		/**
		 * Removing all Junk identifiers after all entries have been assigned the new identifiers.
		 */
		tryMigration("Remove all Junk Oura Identifiers") {
			deleteThirdPartyEntryIdentifiers('OURA%')
		}

		tryMigration("Remove all Junk Withings Identifiers") {
			deleteThirdPartyEntryIdentifiers('WI%')
		}

		tryMigration("Remove all Junk Fitbit Identifiers") {
			deleteThirdPartyEntryIdentifiers('fitbit%')
		}

		tryMigration("Remove all Junk Jawbone Up Identifiers") {
			deleteThirdPartyEntryIdentifiers('JUP%')
		}

		tryMigration("Remove all Junk Moves Identifiers") {
			deleteThirdPartyEntryIdentifiers('moves%')
		}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

// - - - - - - - - - - - - - - - - Migrations for migrating to new OURA API - - - - - - - - - - - - - - - - - - - - - -

		// This migration will also update the typeId to new Oura typeId i.e 10.
		tryMigration('Get refresh tokens from Legacy Oura Cloud') {
			legacyOuraDataService.renewRefreshTokens()
		}

		// This migration will update the access tokens using the refresh tokens obtained in previous migration.
		tryMigration('Refresh all OURA OAuthAccounts access tokens') {
			ouraDataService.refreshAllOAuthAccounts()
		}

		/*
		 * This migration will update the typeId to new Oura typeId i.e 10 for OAuthAccounts whose refresh tokens are
		 * not present in the Legacy Oura Server.
		 */
		tryMigration('Update remaining OAuthAccounts to new typeId') {
			int noOfRowsUpdated = sql('UPDATE oauth_account SET type_id = :newTypeId where type_id = :oldTypeId',
					[newTypeId: 10, oldTypeId: 9])

			log.debug "Updated ${noOfRowsUpdated} OAuthAccounts to new Oura typeId"
		}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

// - - - - - - - - - - - - Migrations to remove is_default column from tag_input_type table - - - - - - - - - - - - - -

		tryMigration('Remove is_default column from tag_input_type') {
			sql("ALTER TABLE tag_input_type DROP COLUMN is_default")
		}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	}
}
