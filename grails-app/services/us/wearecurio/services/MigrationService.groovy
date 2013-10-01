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
	public static final long FIX_OAUTH_UNIQUE_CONSTRAINT_ID = 32L
	
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
		tryMigration(FIX_OAUTH_UNIQUE_CONSTRAINT_ID) {
			sql('alter table oauth_account drop index user_id')
		}
/*
		tryMigration(LENGTHEN_COMMENT_ID) {
			Entry.sql 'ALTER TABLE entry CHANGE COLUMN comment comment TEXT'
		}
		tryMigration(REMOVE_NOT_NULL_CONSTRAINT_ID) {
			TagStats.sql 'ALTER TABLE tag_stats CHANGE COLUMN most_recent_usage most_recent_usage DATETIME NULL'
			TagStats.sql 'ALTER TABLE tag_stats CHANGE COLUMN third_most_recent_usage third_most_recent_usage DATETIME NULL'
		}
		tryMigration(LENGTHEN_PLOT_DATA_ID) {
			Entry.sql 'ALTER TABLE plot_data CHANGE COLUMN json_plot_data json_plot_data TEXT'
		}
		tryMigration(INITIALIZE_DURATIONS_ID) {
			def users = User.list()
			
			for (user in users) {
				Entry.initializeDurationEntries(user)
			}
		}
		tryMigration(UPDATE_DURATIONS_ID) {
			def users = User.list()
			
			for (user in users) {
				Entry.updateDurationEntries(user)
			}
		}
		tryMigration(UPDATE_USER_TABLE_ID) {
			User.sql 'ALTER TABLE `_user` CHANGE COLUMN `birthdate` `birthdate` DATETIME NULL  , CHANGE COLUMN `first` `first` VARCHAR(100) NULL  , CHANGE COLUMN `last` `last` VARCHAR(100) NULL  , CHANGE COLUMN `sex` `sex` VARCHAR(1) NULL'
		}
		tryMigration(INITIALIZE_EVENT_OFFSETS_ID) {
			Entry.sql 'UPDATE `entry` SET time_zone_offset_secs = -21600 WHERE time_zone_offset_secs IS NULL'
		}
		tryMigration(RESET_TAG_STATS_ID) {
			TagStats.sql 'DELETE FROM tag_stats'
		}
		tryMigration(REINITIALIZE_EVENT_OFFSETS_ID) {
			Entry.sql 'UPDATE `entry` SET time_zone_offset_secs = -21600 WHERE time_zone_offset_secs IS NULL'
		}
		tryMigration(ADD_USER_UNIQUE_CONSTRAINTS_ID) {
			Entry.sql 'ALTER TABLE `_user` ADD UNIQUE INDEX `username_UNIQUE` (`username` ASC)'
		}
		tryMigration(AMOUNT_NULLABLE_ID) {
			Entry.sql 'ALTER TABLE `entry` CHANGE COLUMN `amount` `amount` DECIMAL(19,9) NULL'
		}
		tryMigration(CLEAR_TAGSTATS_ID) {
			TagStats.sql 'DELETE FROM tag_stats'
		}
		tryMigration(CLEAR_TAGSTATS2_ID) {
			TagStats.sql 'DELETE FROM tag_stats'
		}
		tryMigration(CLEAR_TAGSTATS3_ID) {
			TagStats.sql 'DELETE FROM tag_stats'
		}
		tryMigration(MIGRATE_DISCUSSIONS_ID) {
			try {
				Discussion.sql "DROP INDEX `plot_data_id_index` ON `discussion_post`"
			} catch (Exception e) {
				e.printStackTrace()
			}
			try {
				Discussion.sql 'ALTER TABLE `discussion` CHANGE COLUMN `first_post_id` `first_post_id` bigint(20) NULL'
			} catch (Exception e) {
				e.printStackTrace()
			}
			try {
				Discussion.sql 'ALTER TABLE `discussion` CHANGE COLUMN `name` `name` varchar(255) NULL'
			} catch (Exception e) {
				e.printStackTrace()
			}
			try {
				Discussion.sql 'ALTER TABLE `discussion_post` CHANGE COLUMN `message` `message` varchar(50000) NULL'
			} catch (Exception e) {
				e.printStackTrace()
			}

			def c = Discussion.createCriteria()
			
			def discussions = c {
				order("created", "asc")
			}
	
			for (Discussion discussion in discussions) {
				Long plotDataId = discussion.getFirstPostId()
				
				discussion.setFirstPostId(null)
				
				PlotData plotData = PlotData.get(plotDataId)
				
				if (plotData == null) {
					Discussion.delete(discussion)
				} else {
					discussion.setName(plotData.getName())
					
					discussion.createPost(DiscussionAuthor.create(plotData.getUserId()), plotDataId, null, discussion.getCreated())
				}
			}
	
			return null
		}
		tryMigration(MERGE_FIRST_TWO_POSTS_ID) {
			def c = Discussion.createCriteria()
			
			def discussions = c {
				order("created", "asc")
			}
	
			for (Discussion discussion in discussions) {
				DiscussionPost firstPost = discussion.getFirstPost()
				
				DiscussionPost secondPost = discussion.getFollowupPosts()[0]

				if (firstPost.getPlotDataId() != null && secondPost != null) {
					firstPost.setMessage(secondPost.getMessage())
					DiscussionPost.delete(secondPost)
					Utils.save(firstPost)
				}
			}
	
			return null
		}
		tryMigration(CALCULATE_DISCUSSION_UPDATE) {
			def c = Discussion.createCriteria()
			
			def discussions = c {
				order("created", "asc")
			}
	
			for (Discussion discussion in discussions) {
				DiscussionPost lastPost = discussion.getLastPost()
				
				if (lastPost)
					discussion.setUpdated(lastPost.getUpdated())

				Utils.save(discussion)
			}
	
			return null
		}
		tryMigration(DISCUSSION_GROUPS) {
			try {
				Discussion.sql "ALTER TABLE discussion DROP COLUMN is_published"
			} catch (Exception e) {
				e.printStackTrace()
			}
			
			def lhp = UserGroup.create("lhp", "LAM Health Project Discussions", "Discussion topics for LAM Health Project members.",
					[isReadOnly:false, defaultNotify:true])
			def curious = UserGroup.create("curious", "Curious Discussions", "Discussion topics for Curious users",
					[isReadOnly:false, defaultNotify:false])
			def announce = UserGroup.create("announce", "Announcements", "Announcements for all users",
					[isReadOnly:true, defaultNotify:true])
			def lhpAnnounce = UserGroup.create("lhp announce", "LAM Health Project Announcements", "Announcements from LAM Health Project",
					[isReadOnly:true, defaultNotify:true])
			def curiousAnnounce = UserGroup.create("curious announce", "Curious Announcements", "Announcements from Curious, Inc.",
					[isReadOnly:true, defaultNotify:false])
			
			Calendar cal = Calendar.getInstance();
			cal.set(2013, Calendar.AUGUST, 2);
			
			def epoch = cal.getTime()
	
			def c = Discussion.createCriteria()
	
			def discussions = c {
				eq("isPublic", true)
				gt("created", epoch)
			}
			
			for (Discussion discussion in discussions) {
				lhp.addDiscussion(discussion)
			}
	
			c = Discussion.createCriteria()
	
			discussions = c {
				eq("isPublic", true)
				le("created", epoch)
			}
			
			for (Discussion discussion in discussions) {
				curious.addDiscussion(discussion)
			}
			
			def users = User.findAll()
			
			for (user in users) {
				announce.addReader(user)
			}
			
			def lhpAdmins = ['rabrusci', 'heathermlam', 'richardabrusci', 'richard']
			def lhpMembers = ['suerlevy', 'kpeiffer', 'ezamp', 'sbacon', 'spoitras', 'holtzb', 'lhpdemo']
			
			for (name in lhpAdmins) {
				def user = User.findByUsername(name)
				lhp.addAdmin(user)
				lhpAnnounce.addAdmin(user)
				lhp.addDefaultFor(user)
				announce.addMember(user)
			}
			
			for (name in lhpMembers) {
				def user = User.findByUsername(name)
				lhp.addMember(user)
				lhpAnnounce.addMember(user)
				lhp.addDefaultFor(user)
				announce.addMember(user)
			}
			
			def curiousAdmins = ['heatheranne', 'x', 'linda', 'mitsu']
			def curiousMembers = ['justin', 'cocochanel', 'z', 'kat', 'demo', 'p', 'testing', 'oldbayo', 'laurie', 'stacy',
					'andy', 'jenn', 'randal', 'christina', 'halle', 'michael', 'pcerrato', 'xyz', 'leslie', 'hugo',
					'delshriver', 'david', 'kristine', 'y', 'tanya', 'amy', 'eric', 'jgheller', 'beau', 'lclagoa', 'milana',
					'raj', 'joshlauer', 'laura', 'm', 'qwe', 'ecasdin', 'kadux', 'qwer', 'visheshd', 'kellie', 'kelliee',
					'eli', 'ekleinwort', 'qwert', 'derby', 'papaslug', 'rcarnese', 'foo', 'happydaisy',
					'sarahpoitras', 'susan f', 'pamlaird', 'lgmd2i', 'lgmd', 'i']
			
			for (name in curiousAdmins) {
				def user = User.findByUsername(name)
				announce.addAdmin(user)
				curious.addAdmin(user)
				curiousAnnounce.addAdmin(user)
				curious.addDefaultFor(user)
			}
			
			for (name in curiousMembers) {
				def user = User.findByUsername(name)
				announce.addMember(user)
				curious.addMember(user)
				curiousAnnounce.addMember(user)
				curious.addDefaultFor(user)
			}
		}
		tryMigration(CREATE_TEST_GROUPS) {
			def test1 = UserGroup.create("test1", "Test 1 group", "Test 1 group",
					[isReadOnly:false, defaultNotify:true])
			
			def t1User = User.findByUsername('test1')
			def t2User = User.findByUsername('test2')
			def t3User = User.findByUsername('test3')
			
			test1.addAdmin(t1User)
			test1.addAdmin(t2User)
			test1.addMember(t3User)
			
			test1.addDefaultFor(t3User)
		}
		tryMigration(RE_ADD_GROUPS) {
			def curious = UserGroup.lookup("curious")
			
			def curiousAdmins = ['heatheranne', 'x', 'linda', 'mitsu']
			
			for (name in curiousAdmins) {
				def user = User.findByUsername(name)
				curious.addAdmin(user)
				curious.addDefaultFor(user)
			}
		}*/
	}
}
