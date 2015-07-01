package us.wearecurio.controller.integration

import static org.junit.Assert.*
import org.junit.*
import org.scribe.model.Response

import us.wearecurio.model.Model.Visibility
import us.wearecurio.controller.SprintController
import us.wearecurio.model.*
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.utility.Utils
import us.wearecurio.hashids.DefaultHashIDGenerator

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
	void "Test update when wrong id is passed"() {
		controller.request.contentType = "text/json"
		controller.request.content = "{'id': 0, 'name': 'Sprint1', 'description': 'Description'," + 
			"'durationDays': 6}"
		controller.request.method = "PUT"
		controller.update()
		assert controller.response.json.success == false
		assert controller.response.json.message == "Sprint does not exist."
	}

	@Test
	void "Test update when null id is passed"() {
		controller.request.contentType = "text/json"
		controller.request.content = "{'id': null, 'name': 'Sprint1', 'description': 'Description'," + 
			"'durationDays': 6}"
		controller.request.method = "PUT"
		controller.update()
		assert controller.response.json.success == false
		assert controller.response.json.message == "Sprint does not exist."
	}

	@Test
	void "Test update when correct id is passed"() {
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
		assert controller.response.json.message == "Sprint does not exist."
	}

	@Test
	void "test delete when null id is passed"() {
		controller.params["id"] = null
		controller.request.method = "DELETE"
		
		controller.delete()
		assert controller.response.json.success == false
		assert controller.response.json.message == "Sprint does not exist."
	}

	@Test
	void "test delete when Non admin user tries to delete the sprint"() {
		controller.session.userId = dummyUser2.id
		controller.params["id"] = dummySprint.hash
		controller.request.method = "DELETE"
		controller.delete()
		assert controller.response.json.success == false
		assert controller.response.json.message == "You don't have permission to delete this sprint."
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
	void "Test show when wrong id is passed"() {
		controller.params["id"] = 0
		
		controller.show()
		assert controller.response.json.error == true
		assert controller.response.json.message == "Sprint does not exist."
	}

	@Test
	void "Test show when null id is passed"() {
		controller.params["id"] = null
		
		controller.show()
		assert controller.response.json.error == true
		assert controller.response.json.message == "Sprint does not exist."
	}

	@Test
	void "Test show when non admin user tries to fetch data"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["id"] = dummySprint.hash

		controller.show()
		assert controller.response.json.error == true
		assert controller.response.json.message == "You don't have permission to edit this sprint."
	}

	@Test
	void "Test show when admin tries to fetch data"() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		dummySprint.addMember(dummyUser2.getId())
		controller.show()
		assert controller.response.json.sprint.id == dummySprint.id
		assert controller.response.json.participants.size() == 3	// Including virtual user
	}

	@Test
	void "Test startSprint when wrong id is passed"() {
		controller.session.userId = user.getId()
		controller.params["id"] = 0
		
		controller.startSprint()
		assert controller.response.json.success == false
		assert !dummySprint.hasStarted(user.getId(), new Date())
		assert controller.response.json.message == "Sprint does not exist."
	}

	@Test
	void "Test startSprint when null id is passed"() {
		controller.session.userId = user.getId()
		controller.params["id"] = null
		
		controller.startSprint()
		assert controller.response.json.success == false
		assert !dummySprint.hasStarted(user.getId(), new Date())
		assert controller.response.json.message == "Sprint does not exist."
	}

	@Test
	void "Test startSprint when non member user tries to start the sprint"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["id"] = dummySprint.hash
		
		controller.startSprint()
		assert controller.response.json.success == false
		assert !dummySprint.hasStarted(dummyUser2.getId(), new Date())
		assert controller.response.json.message == "You are not a member of this sprint."
	}

	@Test
	void startSprint() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		controller.params["now"] = "Wed, 25 Feb 2015 10:44:07 GMT"
		
		controller.startSprint()
		assert controller.response.json.success == true
		assert dummySprint.hasStarted(user.getId(), new Date())
	}

	@Test
	void "Test stopSprint when wrong id is passed"() {
		controller.session.userId = user.getId()
		controller.params["id"] = 0
		
		controller.stopSprint()
		assert controller.response.json.success == false
		assert !dummySprint.hasStarted(user.getId(), new Date())
		assert controller.response.json.message == "Sprint does not exist."
	}

	@Test
	void "Test stopSprint when null id is passed"() {
		controller.session.userId = user.getId()
		controller.params["id"] = null
		
		controller.stopSprint()
		assert controller.response.json.success == false
		assert !dummySprint.hasStarted(user.getId(), new Date())
		assert controller.response.json.message == "Sprint does not exist."
	}

	@Test
	void "Test stopSprint when non member user tries to stop the sprint"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["id"] = dummySprint.hash
		
		controller.stopSprint()
		assert controller.response.json.success == false
		assert !dummySprint.hasStarted(dummyUser2.getId(), new Date())
		assert controller.response.json.message == "You are not a member of this sprint."
	}

	@Test
	void "Test stopSprint when a member has not started the sprint"() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		
		controller.params["now"] = "Wed, 25 Feb 2015 10:44:07 GMT"
		
		controller.stopSprint()
		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("can.not.stop.sprint", [] as Object[], null) 
	}

	@Test
	void stopSprint() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		controller.params["now"] = "Wed, 25 Feb 2015 10:44:07 GMT"
		
		controller.startSprint()
		controller.response.reset()
		controller.params["now"] = "Wed, 25 Feb 2015 11:44:07 GMT"
		
		controller.stopSprint()
		assert controller.response.json.success == true
		assert dummySprint.hasEnded(user.getId(), new Date())
	}

	@Test
	void "Test addMember when wrong sprintHash is passed"() {
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = 0
		
		controller.addMember()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == messageSource.getMessage("add.sprint.participant.failed", [] as Object[], null)
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "Test addMember when null sprintHash is passed"() {
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = null
		
		controller.addMember()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == messageSource.getMessage("add.sprint.participant.failed", [] as Object[], null)
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "Test addMember when user name is null"() {
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addMember()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "No user with such username found."
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "Test addMember when user name is wrong"() {
		controller.params["username"] = "z"
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addMember()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "No user with such username found."
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "Test addMember when a non admin performs the action"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addMember()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "You don't have permission to add members to this sprint."
		assert dummySprint.hasMember(dummyUser2.getId()) == false
	}

	@Test
	void "Test addMember"() {
		controller.session.userId = user.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addMember()
		assert controller.response.json.success == true
		assert dummySprint.hasInvited(dummyUser2.getId()) == true
	}

	@Test
	void "Test addAdmin when wrong sprintHash is passed"() {
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = 0
		
		controller.addAdmin()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == messageSource.getMessage("add.sprint.admin.failed", [] as Object[], null)
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "Test addAdmin when null sprintHash is passed"() {
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = null
		
		controller.addAdmin()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == messageSource.getMessage("add.sprint.admin.failed", [] as Object[], null)
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "Test addAdmin when user name is null"() {
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addAdmin()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "No user with such username found."
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "Test addAdmin when user name is wrong"() {
		controller.params["username"] = "z"
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addAdmin()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "No user with such username found."
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "Test addAdmin when a non admin performs the action"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addAdmin()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "You don't have permission to add admins to this sprint."
		assert dummySprint.hasAdmin(dummyUser2.getId()) == false
	}

	@Test
	void "Test addAdmin"() {
		controller.session.userId = user.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.addAdmin()
		assert controller.response.json.success == true
		assert dummySprint.hasInvitedAdmin(dummyUser2.getId()) == true
	}

	@Test
	void "Test deleteMember when wrong sprintHash is passed"() {
		controller.params["username"] = user.username
		controller.params["sprintHash"] = 0
		
		controller.deleteMember()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.participant.failed", [] as Object[], null)
		assert dummySprint.hasMember(user.getId()) == true
	}

	@Test
	void "Test deleteMember when null sprintHash is passed"() {
		controller.params["username"] = user.username
		controller.params["sprintHash"] = null
		
		controller.deleteMember()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.participant.failed", [] as Object[], null)
		assert dummySprint.hasMember(user.getId()) == true
	}

	@Test
	void "Test deleteMember when user name is null"() {
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteMember()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.participant.failed", [] as Object[], null)
		assert dummySprint.hasMember(user.getId()) == true
	}

	@Test
	void "Test deleteMember when user name is wrong"() {
		controller.params["username"] = "z"
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteMember()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.participant.failed", [] as Object[], null)
	}

	@Test
	void "Test deleteMember when a non admin performs the action"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["username"] = user.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteMember()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "You don't have permission to delete members of this sprint."
		assert dummySprint.hasMember(user.getId()) == true
	}

	@Test
	void "Test deleteMember when a non member is being deleted"() {
		controller.session.userId = user.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteMember()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "No such member to delete from this sprint."
	}

	@Test
	void "Test deleteMember"() {
		controller.session.userId = user.getId()
		controller.params["username"] = user.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteMember()
		assert controller.response.json.success == true
		assert dummySprint.hasMember(user.getId()) == false
	}

	@Test
	void "Test deleteAdmin when wrong sprintId is passed"() {
		controller.params["username"] = user.username
		controller.params["sprintId"] = 0
		
		controller.deleteAdmin()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.admin.failed", [] as Object[], null)
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "Test deleteAdmin when null sprintHash is passed"() {
		controller.params["username"] = user.username
		controller.params["sprintHash"] = null
		
		controller.deleteAdmin()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.admin.failed", [] as Object[], null)
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "Test deleteAdmin when user name is null"() {
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteAdmin()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.admin.failed", [] as Object[], null)
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "Test deleteAdmin when user name is wrong"() {
		controller.params["username"] = "z"
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteAdmin()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == messageSource.getMessage("delete.sprint.admin.failed", [] as Object[], null)
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "Test deleteAdmin when a non admin performs the action"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["username"] = user.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteAdmin()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "You don't have permission to delete admins of this sprint."
		assert dummySprint.hasAdmin(user.getId()) == true
	}

	@Test
	void "Test deleteAdmin when a non admin is being deleted"() {
		controller.session.userId = user.getId()
		controller.params["username"] = dummyUser2.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteAdmin()
		assert controller.response.json.error == true
		assert controller.response.json.errorMessage == "No such admin to delete from this sprint."
	}

	@Test
	void "Test deleteAdmin"() {
		controller.session.userId = user.getId()
		controller.params["username"] = user.username
		controller.params["sprintHash"] = dummySprint.hash
		
		controller.deleteAdmin()
		assert controller.response.json.success == true
		assert dummySprint.hasAdmin(user.getId()) == false
	}

	@Test
	void "Test leaveSprint when wrong id is passed"() {
		controller.session.userId = user.getId()
		controller.params["id"] = 0
		
		controller.leaveSprint()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("sprint.not.exist", [] as Object[], null)
	}

	@Test
	void "Test leaveSprint when null id is passed"() {
		controller.session.userId = user.getId()
		controller.params["id"] = null
		
		controller.leaveSprint()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("sprint.not.exist", [] as Object[], null)
	}

	@Test
	void "Test leaveSprint when non member user tries to leave the sprint"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["id"] = dummySprint.hash
		
		controller.leaveSprint()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("not.sprint.member", [] as Object[], null)
	}

	@Test
	void "Test leaveSprint when a member leaves the sprint"() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		
		controller.leaveSprint()
		assert controller.response.json.success == true
		assert dummySprint.hasMember(user.getId()) == false
	}

	@Test
	void "Test joinSprint when wrong id is passed"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["id"] = 0
		
		controller.joinSprint()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("sprint.not.exist", [] as Object[], null)
	}

	@Test
	void "Test joinSprint when null id is passed"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["id"] = null
		
		controller.joinSprint()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("sprint.not.exist", [] as Object[], null)
	}

	@Test
	void "Test joinSprint when member user tries to join the sprint"() {
		controller.session.userId = user.getId()
		controller.params["id"] = dummySprint.hash
		
		controller.joinSprint()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("already.joined.sprint", [] as Object[], null)
	}

	@Test
	void "Test joinSprint when a non member joins the sprint"() {
		controller.session.userId = dummyUser2.getId()
		controller.params["id"] = dummySprint.hash
		
		controller.joinSprint()
		assert controller.response.json.success == true
		assert dummySprint.hasMember(dummyUser2.getId()) == true
	}
}
