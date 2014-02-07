
package us.wearecurio.services

import java.util.Date;

import org.apache.commons.logging.LogFactory
import org.hibernate.Query
import org.hibernate.Session
import org.hibernate.SessionFactory

import us.wearecurio.server.Migration;
import us.wearecurio.utility.Utils;
import us.wearecurio.model.*;
import grails.util.Environment

import groovy.lang.Closure;
import groovy.sql.Sql

import org.joda.time.*

class MigrationService {

	private static def log = LogFactory.getLog(this)

	static transactional = true
	
	public static final long TEST_MIGRATION_ID = 30L
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
	
	SessionFactory sessionFactory
	DatabaseService databaseService
	
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
			def retVal = closure()

			didMigration(migration)
				
			return retVal
		}
		
		return false
	}

	def doMigrations() {
		if (Environment.getCurrent().equals(Environment.TEST))
			return; // don't run in test environment
			
		tryMigration(TEST_MIGRATION_ID) {
			sql('ALTER TABLE entry CHANGE COLUMN comment comment TEXT')
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
	}
}
