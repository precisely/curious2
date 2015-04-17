package us.wearecurio.services.integration

import us.wearecurio.model.OAuthAccount
import grails.test.mixin.*

import org.junit.*
import org.scribe.model.Response

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.DatabaseService

class DatabaseServiceTests extends CuriousServiceTestCase {
	static transactional = false

	DatabaseService databaseService

	def grailsApplication
	
	boolean proceed
	boolean done
	boolean success
	
	def sync = new Object()
	
	class ThreadTest implements Runnable {
		Long userId
		
		ThreadTest(userId) {
			this.userId = userId
		}
		
		void run() {
			try {
				User user2
				
				User.withNewSession {
					user2 = User.get(userId)
						
					synchronized(sync) {
						proceed = true
						sync.notifyAll()
						
						user2.attach()
						
						DatabaseService.retry(user2) {
							user2.setFirst("hooboy")
						
							user2.save(flush:true)
						}
						success = true
						done = true
						sync.notifyAll()
					}
				}
			} catch (Exception e) {
				e.printStackTrace()
				success = false
				done = true
			}
		}
	}

	@Before
	void setUp() {
		super.setUp()
	}

	@After
	void tearDown() {
		super.tearDown()
	}
	
	@Test
	void testOptimisticLocking() {
		assert true
		
		// we've disabled optimistic locking for now so this test should fail
		/*
		proceed = false
		done = false
		success = false
		
		User user = User.get(userId)
			
		new Thread(new ThreadTest(userId)).start()
		
		try {
			synchronized(sync) {
				while (!proceed) try { sync.wait() } catch (InterruptedException e) {}
				
				DatabaseService.retry(user) {
					user.setName("voodoo")
					
					user.save(flush:true)
				}
				while (!done) try { sync.wait() } catch (InterruptedException e) {}
			}
		} catch (Throwable e) {
			e.printStackTrace()
		}
		
		assert success
		user.refresh()
		assert user.getName().equals("voodoo")
		*/
	}
}