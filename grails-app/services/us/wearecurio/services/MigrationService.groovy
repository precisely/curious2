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

class MigrationService {

	private static def log = LogFactory.getLog(this)

	static transactional = true
	
	public static final long TEST_MIGRATION_ID = 30L
	public static final long MIGRATE_REPEATS_ID = 31L
	public static final long FIX_OAUTH_UNIQUE_CONSTRAINT_ID = 32L
	public static final long REPEAT_END_NULLABLE_ID = 36L
	
	SessionFactory sessionFactory
	DatabaseService databaseService
	
	public def sql(String statement) {
		databaseService.sqlNoRollback(statement)
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
	}
}
