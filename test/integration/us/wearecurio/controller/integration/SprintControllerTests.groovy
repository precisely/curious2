package us.wearecurio.controller.integration

import org.junit.After
import org.junit.Before
import org.junit.Test
import us.wearecurio.controller.SprintController
import us.wearecurio.model.Model
import us.wearecurio.model.Sprint
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

class SprintControllerTests extends CuriousControllerTestCase {
	static transactional = true

	SprintController controller
	Sprint dummySprint
	User dummyUser2

	@Before
	void setUp() {
		super.setUp()
		
		controller = new SprintController()
		dummySprint = Sprint.create(new Date(), user, "demo", Model.Visibility.PRIVATE)
		
		Map params = [username: "a", sex: "F", last: "y", email: "a@a.com", birthdate: "01/01/2001", name: "a", password: "y"]
		dummyUser2 = User.create(params)

		Utils.save(dummyUser2, true)
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void "test start when non member user tries to start the sprint"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["id"] = dummySprint.hash
		
		controller.start()
		assert controller.response.json.success == true
		assert controller.response.json.hash == dummySprint.hash
		assert controller.response.json.participants == dummySprint.getParticipants(10,0)
		assert controller.response.json.totalParticipants == dummySprint.getParticipantsCount()
		assert dummySprint.hasStarted(dummyUser2.getId(), new Date() + 1)
	}

	@Test
	void "test update when wrong id is passed"() {
		controller.request.contentType = "text/json"
		controller.request.content = "{'id': 0, 'name': 'Sprint1', 'description': 'Description'," + 
			"'durationDays': 6}"
		controller.request.method = "PUT"
		controller.update()
		assert controller.response.json.success == false
		assert controller.response.json.message == "Trackathon does not exist."
	}

	@Test
	void "test update when null id is passed"() {
		controller.request.contentType = "text/json"
		controller.request.content = "{'id': null, 'name': 'Sprint1', 'description': 'Description'," + 
			"'durationDays': 6}"
		controller.request.method = "PUT"
		controller.update()
		assert controller.response.json.success == false
		assert controller.response.json.message == "Trackathon does not exist."
	}

	@Test
	void "test update when correct id is passed"() {
		controller.request.contentType = "text/json"
		controller.request.content = "{'id': ${dummySprint.hash}, 'name': 'Sprint1', 'description': 'Description'," + 
			"'durationDays': 6}"
		controller.request.method = "PUT"
		
		controller.update()
		assert controller.response.json.success == true
		assert controller.response.json.hash == dummySprint.hash
		assert dummySprint.name == "Sprint1"
	}

	@Test
	void "test delete when wrong id is passed"() {
		controller.params["id"] = 0
		controller.request.method = "DELETE"
		
		controller.delete()
		assert controller.response.json.success == false
		assert controller.response.json.message == "Trackathon does not exist."
	}

	@Test
	void "test delete when null id is passed"() {
		controller.params["id"] = null
		controller.request.method = "DELETE"
		
		controller.delete()
		assert controller.response.json.success == false
		assert controller.response.json.message == "Trackathon does not exist."
	}

	@Test
	void "test delete when Non admin user tries to delete the sprint"() {
		controller.session.userId = dummyUser2.id
		controller.params["id"] = dummySprint.hash
		controller.request.method = "DELETE"
		controller.delete()
		assert controller.response.json.success == false
		assert controller.response.json.message == "You don't have permission to delete this trackathon."
	}

	@Test
	void "test delete when admin tries to delete the sprint"() {
		controller.session.userId = user.id
		controller.params["id"] = dummySprint.hash
		controller.request.method = "DELETE"
		controller.delete()
		
		assert controller.response.json.success == true
		assert !dummySprint.hasAdmin(user.id)
		assert !dummySprint.hasMember(user.id)
		assert dummySprint.userId == 0l
	}

	@Test
	void "test show when wrong id is passed"() {
		controller.params["id"] = 0
		
		controller.show()
		assert !controller.response.json.success
		assert controller.response.json.message == "Trackathon does not exist."
	}

	@Test
	void "test show when null id is passed"() {
		controller.params["id"] = null
		
		controller.show()
		assert !controller.response.json.success
		assert controller.response.json.message == "Trackathon does not exist."
	}

	@Test
	void "test show when non admin user tries to fetch data"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["id"] = dummySprint.hash

		controller.show()
		assert !controller.response.json.success
		assert controller.response.json.message == "You don't have permission to edit this trackathon."
	}

	@Test
	void "test show when admin tries to fetch data"() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		dummySprint.addMember(dummyUser2.getId())
		controller.show()
		assert controller.response.json.sprint.id == dummySprint.id
		assert controller.response.json.participants.size() == 2
	}

	@Test
	void "test start when wrong id is passed"() {
		controller.session.userId = user.getId()
		controller.params["id"] = 0
		
		controller.start()
		assert controller.response.json.success == false
		assert !dummySprint.hasStarted(user.getId(), new Date())
		assert controller.response.json.message == "Trackathon does not exist."
	}

	@Test
	void "test start when null id is passed"() {
		controller.session.userId = user.getId()
		controller.params["id"] = null
		
		controller.start()
		assert controller.response.json.success == false
		assert !dummySprint.hasStarted(user.getId(), new Date())
		assert controller.response.json.message == "Trackathon does not exist."
	}

	@Test
	void start() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		controller.params["now"] = "Wed, 25 Feb 2015 10:44:07 GMT"
		
		controller.start()
		assert controller.response.json.success == true
		assert controller.response.json.hash == dummySprint.hash
		assert controller.response.json.participants == dummySprint.getParticipants(10,0)
		assert controller.response.json.totalParticipants == dummySprint.getParticipantsCount()
		assert dummySprint.hasStarted(user.getId(), new Date())
	}

	@Test
	void "test stop when wrong id is passed"() {
		controller.session.userId = user.getId()
		controller.params["id"] = 0
		
		controller.stop()
		assert controller.response.json.success == false
		assert !dummySprint.hasStarted(user.getId(), new Date())
		assert controller.response.json.message == "Trackathon does not exist."
	}

	@Test
	void "test stop when null id is passed"() {
		controller.session.userId = user.getId()
		controller.params["id"] = null
		
		controller.stop()
		assert controller.response.json.success == false
		assert !dummySprint.hasStarted(user.getId(), new Date())
		assert controller.response.json.message == "Trackathon does not exist."
	}

	@Test
	void "test stop when non member user tries to stop the sprint"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["id"] = dummySprint.hash
		
		controller.stop()
		assert controller.response.json.success == false
		assert !dummySprint.hasStarted(dummyUser2.getId(), new Date())
		assert controller.response.json.message == "You are not following this trackathon."
	}

	@Test
	void "test stop when a member has not started the sprint"() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		
		controller.params["now"] = "Wed, 25 Feb 2015 10:44:07 GMT"
		
		controller.stop()
		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("can.not.stop.sprint", [] as Object[], null) 
	}

	@Test
	void stop() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		controller.params["now"] = "Wed, 25 Feb 2015 10:44:07 GMT"
		
		controller.start()
		controller.response.reset()
		controller.params["now"] = "Wed, 25 Feb 2015 11:44:07 GMT"
		
		controller.stop()
		assert controller.response.json.success == true
		assert dummySprint.hasEnded(user.getId(), new Date())
	}

	@Test
	void "test addMember when wrong sprintHash is passed"() {
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = 0
		
		controller.addMember()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == messageSource.getMessage("add.sprint.participant.failed", [] as Object[], null)
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "test addMember when null sprintHash is passed"() {
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = null
		
		controller.addMember()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == messageSource.getMessage("add.sprint.participant.failed", [] as Object[], null)
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "test addMember when user name is null"() {
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addMember()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == "No user with such username found."
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "test addMember when user name is wrong"() {
		controller.params["username"] = "z"
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addMember()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == "No user with such username found."
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "test addMember when a non admin performs the action"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addMember()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == "You don't have permission to add members to this trackathon."
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "test addMember"() {
		controller.session.userId = user.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addMember()
		assert controller.response.json.success == true
		assert dummySprint.hasInvited(dummyUser2.getId()) == true
	}

	@Test
	void "test addAdmin when wrong sprintHash is passed"() {
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = 0
		
		controller.addAdmin()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == messageSource.getMessage("add.sprint.admin.failed", [] as Object[], null)
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "test addAdmin when null sprintHash is passed"() {
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = null
		
		controller.addAdmin()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == messageSource.getMessage("add.sprint.admin.failed", [] as Object[], null)
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "test addAdmin when user name is null"() {
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addAdmin()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == "No user with such username found."
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "test addAdmin when user name is wrong"() {
		controller.params["username"] = "z"
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addAdmin()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == "No user with such username found."
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "test addAdmin when a non admin performs the action"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addAdmin()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == "You don't have permission to add admins to this trackathon."
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "test addAdmin"() {
		controller.session.userId = user.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addAdmin()
		assert controller.response.json.success == true
		assert dummySprint.hasInvitedAdmin(dummyUser2.getId()) == true
	}

	@Test
	void "test deleteMember when wrong sprintHash is passed"() {
		controller.params["username"] = user.username
		controller.params["sprintHash"] = 0
		
		controller.deleteMember()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.participant.failed", [] as Object[], null)
		assert dummySprint.hasMember(user.getId()) == true
	}

	@Test
	void "test deleteMember when null sprintHash is passed"() {
		controller.params["username"] = user.username
		controller.params["sprintHash"] = null
		
		controller.deleteMember()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.participant.failed", [] as Object[], null)
		assert dummySprint.hasMember(user.getId()) == true
	}

	@Test
	void "test deleteMember when user name is null"() {
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteMember()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.participant.failed", [] as Object[], null)
		assert dummySprint.hasMember(user.getId()) == true
	}

	@Test
	void "test deleteMember when user name is wrong"() {
		controller.params["username"] = "z"
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteMember()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.participant.failed", [] as Object[], null)
	}

	@Test
	void "test deleteMember when a non admin performs the action"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["username"] = user.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteMember()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == "You don't have permission to remove followers from this trackathon."
		assert dummySprint.hasMember(user.getId()) == true
	}

	@Test
	void "test deleteMember when a non member is being deleted"() {
		controller.session.userId = user.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteMember()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == "No such follower to delete from this trackathon."
	}

	@Test
	void "test deleteMember"() {
		controller.session.userId = user.getId()
		controller.params["username"] = user.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteMember()
		assert controller.response.json.success == true
		assert dummySprint.hasMember(user.getId()) == false
	}

	@Test
	void "test deleteAdmin when wrong sprintId is passed"() {
		controller.params["username"] = user.username
		controller.params["sprintId"] = 0
		
		controller.deleteAdmin()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.admin.failed", [] as Object[], null)
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "test deleteAdmin when null sprintHash is passed"() {
		controller.params["username"] = user.username
		controller.params["sprintHash"] = null
		
		controller.deleteAdmin()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.admin.failed", [] as Object[], null)
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "test deleteAdmin when user name is null"() {
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteAdmin()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.admin.failed", [] as Object[], null)
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "test deleteAdmin when user name is wrong"() {
		controller.params["username"] = "z"
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteAdmin()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.admin.failed", [] as Object[], null)
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "test deleteAdmin when a non admin performs the action"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["username"] = user.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteAdmin()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == "You don't have permission to delete admins of this trackathon."
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "test deleteAdmin when a non admin is being deleted"() {
		controller.session.userId = user.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteAdmin()
		assert !controller.response.json.success
		assert controller.response.json.errorMessage == "No such admin to delete from this trackathon."
	}

	@Test
	void "test deleteAdmin"() {
		controller.session.userId = user.getId()
		controller.params["username"] = user.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteAdmin()
		assert controller.response.json.success == true
		assert dummySprint.hasAdmin(user.getId()) == false
	}

	@Test
	void "test leave when wrong id is passed"() {
		controller.session.userId = user.getId()
		controller.params["id"] = 0
		
		controller.leave()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("sprint.not.exist", [] as Object[], null)
	}

	@Test
	void "test leave when null id is passed"() {
		controller.session.userId = user.getId()
		controller.params["id"] = null
		
		controller.leave()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("sprint.not.exist", [] as Object[], null)
	}

	@Test
	void "test leave when non member user tries to leave the sprint"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["id"] = dummySprint.hash
		
		controller.leave()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("not.sprint.member", [] as Object[], null)
	}

	@Test
	void "test leave when a member leaves the sprint"() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		
		controller.leave()
		assert controller.response.json.success == true
		assert controller.response.json.hash == dummySprint.hash
		assert controller.response.json.participants == dummySprint.getParticipants(10,0)
		assert controller.response.json.totalParticipants == dummySprint.getParticipantsCount()
		assert dummySprint.hasMember(user.getId()) == false
	}

	@Test
	void "test join when wrong id is passed"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["id"] = 0
		
		controller.join()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("sprint.not.exist", [] as Object[], null)
	}

	@Test
	void "test join when null id is passed"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["id"] = null
		
		controller.join()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("sprint.not.exist", [] as Object[], null)
	}

	@Test
	void "test join when member user tries to join the sprint"() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		
		controller.join()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("already.joined.sprint", [] as Object[], null)
	}

	@Test
	void "test join when a non member joins the sprint"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["id"] = dummySprint.hash
		
		controller.join()
		assert controller.response.json.success == true
		assert controller.response.json.hash == dummySprint.hash
		assert controller.response.json.participants == dummySprint.getParticipants(10,0)
		assert controller.response.json.totalParticipants == dummySprint.getParticipantsCount()
		assert dummySprint.hasMember(dummyUser2.getId()) == true
	}

	@Test
	void "test discussions for updating the 'DISCUSSION_DETAIL_VISIT' bits as the user visits a discussion from mobile app"() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash

		// Sending the mobileSessionId parameter
		controller.params.mobileSessionId = "abcdefghij1245231"

		int DISCUSSION_DETAIL_VISIT_1 = 16
		int DISCUSSION_DETAIL_VISIT_2 = 17
		int DISCUSSION_DETAIL_VISIT_3 = 18

		// First clearing the previously set bits. (Just to ensure that this call sets the bits correctly)
		user.settings.clear(DISCUSSION_DETAIL_VISIT_1)
		user.settings.clear(DISCUSSION_DETAIL_VISIT_2)
		user.settings.clear(DISCUSSION_DETAIL_VISIT_3)

		assert !user.settings.get(DISCUSSION_DETAIL_VISIT_1)
		assert !user.settings.get(DISCUSSION_DETAIL_VISIT_2)
		assert !user.settings.get(DISCUSSION_DETAIL_VISIT_3)
		assert !user.settings.hasDiscussionDetailVisitCountCompleted()

		// For first visit
		controller.discussions(dummySprint.hash, 10, 0)

		assert user.settings.get(DISCUSSION_DETAIL_VISIT_1)
		assert !user.settings.get(DISCUSSION_DETAIL_VISIT_2)
		assert !user.settings.get(DISCUSSION_DETAIL_VISIT_3)
		assert !user.settings.hasDiscussionDetailVisitCountCompleted()

		// For second visit
		controller.discussions(dummySprint.hash, 10, 0)

		assert user.settings.get(DISCUSSION_DETAIL_VISIT_1)
		assert user.settings.get(DISCUSSION_DETAIL_VISIT_2)
		assert !user.settings.get(DISCUSSION_DETAIL_VISIT_3)
		assert !user.settings.hasDiscussionDetailVisitCountCompleted()

		// For third visit
		controller.discussions(dummySprint.hash, 10, 0)

		assert user.settings.get(DISCUSSION_DETAIL_VISIT_1)
		assert user.settings.get(DISCUSSION_DETAIL_VISIT_2)
		assert user.settings.get(DISCUSSION_DETAIL_VISIT_3)
		assert user.settings.hasDiscussionDetailVisitCountCompleted()
	}

	@Test
	void "test discussions for not updating the 'DISCUSSION_DETAIL_VISIT' bits as the user visits a discussion from web"() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash

		int DISCUSSION_DETAIL_VISIT_1 = 16
		int DISCUSSION_DETAIL_VISIT_2 = 17
		int DISCUSSION_DETAIL_VISIT_3 = 18

		// First clearing the previously set bits. (Just to ensure that this call sets the bits correctly)
		user.settings.clear(DISCUSSION_DETAIL_VISIT_1)
		user.settings.clear(DISCUSSION_DETAIL_VISIT_2)
		user.settings.clear(DISCUSSION_DETAIL_VISIT_3)

		assert !user.settings.get(DISCUSSION_DETAIL_VISIT_1)
		assert !user.settings.get(DISCUSSION_DETAIL_VISIT_2)
		assert !user.settings.get(DISCUSSION_DETAIL_VISIT_3)
		assert !user.settings.hasDiscussionDetailVisitCountCompleted()

		controller.discussions(dummySprint.hash, 10, 0)

		// There will be no change in the user settings bits for discussion detail visit
		assert !user.settings.get(DISCUSSION_DETAIL_VISIT_1)
		assert !user.settings.get(DISCUSSION_DETAIL_VISIT_2)
		assert !user.settings.get(DISCUSSION_DETAIL_VISIT_3)
		assert !user.settings.hasDiscussionDetailVisitCountCompleted()
	}

	@Test
	void "test start for updating the TRACKATHON_STARTED bits when user starts a trackathon from the mobile app"() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		controller.params["now"] = "Wed, 25 Feb 2015 10:44:07 GMT"

		int TRACKATHON_STARTED_1 = 13
		int TRACKATHON_STARTED_2 = 14
		int TRACKATHON_STARTED_3 = 15

		// First clearing the previously set bits. (Just to ensure that this call sets the bits correctly)
		user.settings.clear(TRACKATHON_STARTED_1)
		user.settings.clear(TRACKATHON_STARTED_2)
		user.settings.clear(TRACKATHON_STARTED_3)

		assert !user.settings.get(TRACKATHON_STARTED_1)
		assert !user.settings.get(TRACKATHON_STARTED_2)
		assert !user.settings.get(TRACKATHON_STARTED_3)
		assert !user.settings.hasTrackathonStartCountCompleted()

		// Sending the mobileSessionId parameter
		controller.params.mobileSessionId = "abcdefghij1245231"

		// For first trackathon start
		controller.start()

		assert user.settings.get(TRACKATHON_STARTED_1)
		assert !user.settings.get(TRACKATHON_STARTED_2)
		assert !user.settings.get(TRACKATHON_STARTED_3)
		assert !user.settings.hasTrackathonStartCountCompleted()

		// Stop this trackathon (Just for testing, so that we can test the start counts without creating new sprints)
		controller.response.reset()
		controller.params["now"] = "Wed, 25 Feb 2015 11:44:07 GMT"
		controller.stop()

		// For second trackathon start
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		controller.params["now"] = "Wed, 26 Feb 2015 10:44:07 GMT"
		controller.start()

		assert user.settings.get(TRACKATHON_STARTED_1)
		assert user.settings.get(TRACKATHON_STARTED_2)
		assert !user.settings.get(TRACKATHON_STARTED_3)
		assert !user.settings.hasTrackathonStartCountCompleted()

		controller.response.reset()
		controller.params["now"] = "Wed, 26 Feb 2015 11:44:07 GMT"
		controller.stop()

		// For third trackathon start
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		controller.params["now"] = "Wed, 27 Feb 2015 10:44:07 GMT"
		controller.start()

		assert user.settings.get(TRACKATHON_STARTED_1)
		assert user.settings.get(TRACKATHON_STARTED_2)
		assert user.settings.get(TRACKATHON_STARTED_3)
		assert user.settings.hasTrackathonStartCountCompleted()
	}

	@Test
	void "test start for not updating the TRACKATHON_STARTED bits when user starts a trackathon from the web"() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		controller.params["now"] = "Wed, 25 Feb 2015 10:44:07 GMT"

		int TRACKATHON_STARTED_1 = 13
		int TRACKATHON_STARTED_2 = 14
		int TRACKATHON_STARTED_3 = 15

		// First clearing the previously set bits. (Just to ensure that this call sets the bits correctly)
		user.settings.clear(TRACKATHON_STARTED_1)
		user.settings.clear(TRACKATHON_STARTED_2)
		user.settings.clear(TRACKATHON_STARTED_3)

		assert !user.settings.get(TRACKATHON_STARTED_1)
		assert !user.settings.get(TRACKATHON_STARTED_2)
		assert !user.settings.get(TRACKATHON_STARTED_3)
		assert !user.settings.hasTrackathonStartCountCompleted()

		// For first trackathon start
		controller.start()

		// There will be no change in the user settings bits for trackathon start
		assert !user.settings.get(TRACKATHON_STARTED_1)
		assert !user.settings.get(TRACKATHON_STARTED_2)
		assert !user.settings.get(TRACKATHON_STARTED_3)
		assert !user.settings.hasTrackathonStartCountCompleted()
	}
}
