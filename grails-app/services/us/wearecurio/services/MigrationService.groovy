package us.wearecurio.services

import grails.util.Environment

import org.springframework.transaction.annotation.Transactional

import org.apache.commons.logging.LogFactory
import org.hibernate.SessionFactory
import org.joda.time.*

import us.wearecurio.model.*
import us.wearecurio.server.Migration
import us.wearecurio.utility.Utils

class MigrationService {

	private static def log = LogFactory.getLog(this)

	static transactional = true
	
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
	
	SessionFactory sessionFactory
	DatabaseService databaseService
	
	boolean skipMigrations = false
	
	public def sql(String statement, args = []) {
		return databaseService.sqlNoRollback(statement, args)
	}
	
	public def sqlRows(String statement, args = []) {
		return databaseService.sqlRows(statement, args)
	}

	public def eachRow(String statement, Closure c) {
		databaseService.eachRow(statement, c)
	}
	
	public def shouldDoMigration(long code) {
		def migration = Migration.findByCode(code)
		if (migration && migration.getHasRun()) {
			return null
		}
		if (migration) return migration
		return Migration.create(code)
	}
	
	def didMigration(Migration migration) {
		migration.setHasRun(true)
		Utils.save(migration, true)
	}
	
	public def tryMigration(long code, Closure closure) {
		def migration
		
		migration = shouldDoMigration(code)
		
		if (migration) {
			def retVal = true
			
			try {
				if (!skipMigrations)
					retVal = closure()
			} catch (Exception e) {
				System.err0.println("EXCEPTION DURING MIGRATION " + code)
				e.printStackTrace()
			}

			didMigration(migration)
				
			return retVal
		}
		
		return false
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
			sql("update entry set comment = 'pinned' where repeat_type in (:repeatIds)", [repeatIds:Entry.CONTINUOUS_IDS])
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

				e.doUpdate(map, null)
			}
		}
	}
}
