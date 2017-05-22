package us.wearecurio.controller.integration

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.web.json.JSONElement
import spock.lang.Unroll
import us.wearecurio.controller.profiletag.ProfileTagController
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.model.profiletags.ProfileTag
import us.wearecurio.model.profiletags.ProfileTagStatus

class ProfileTagControllerSpec extends IntegrationSpec {

	User user
	ProfileTagController controller

	void setup() {
		Map params = [username: 'z', sex: 'M', name: 'abc', email: 'abc@xyz.com', birthdate:'01/01/2005', password:'z']
		user = User.create(params)

		controller = new ProfileTagController()
	}

	void "test getInterestTags action for fetching public and private profile tag lists"() {
		given: '2 public interest tags and two private interest tags'
		ProfileTag.addPublicInterestTag(Tag.look('tea'), user.id)
		ProfileTag.addPublicInterestTag(Tag.look('sleep'), user.id)

		ProfileTag.addPrivateInterestTag(Tag.look('mood'), user.id)
		ProfileTag.addPrivateInterestTag(Tag.look('coffee'), user.id)

		when: 'The getInterestTags action is hit for this user'
		controller.session.userId = user.id
		controller.getInterestTags()

		then: 'The response should have two public interest tags and two private interest tags'
		JSONElement json = JSON.parse(controller.response.text)
		json.privateInterestTags.size() == 2
		json.publicInterestTags.size() == 2

		json.privateInterestTags[0].status == 'PRIVATE'
		json.privateInterestTags[1].status == 'PRIVATE'
		json.publicInterestTags[0].status == 'PUBLIC'
		json.publicInterestTags[1].status == 'PUBLIC'
	}

	@Unroll
	void "test addInterestTag for adding public and private tags"() {
		when: 'The addInterestTag action is hit for this user with tagName and status'
		controller.session.userId = user.id
		controller.params.tagName = tagName
		controller.params.tagStatus = tagStatus
		controller.addInterestTag()

		then: 'The newly created profiletag should be present in the response'
		JSONElement json = JSON.parse(controller.response.text)
		json.success == true
		json.profileTag.tag.description == tagName
		json.profileTag.status == tagStatus
		ProfileTag.count() == 1

		ProfileTag profileTagInstance = ProfileTag.first()
		profileTagInstance.tag.description == tagName
		profileTagInstance.status == tagStatus as ProfileTagStatus

		where:
		tagName  | tagStatus
		'run'    | 'PUBLIC'
		'sleep'  | 'PRIVATE'
		'tea'    | 'PUBLIC'
		'coffee' | 'PRIVATE'
 	}

	@Unroll
	void "test addInterestTag for when either of the two params are missing"() {
		when: 'The addInterestTag action is hit for this user with tagName and status'
		controller.session.userId = user.id
		controller.params.tagName = tagName
		controller.params.tagStatus = tagStatus
		controller.addInterestTag()

		then: 'The newly created profiletag should be present in the response'
		controller.response.text == errorText

		where:
		tagName  | tagStatus | errorText
		''       | 'PUBLIC'  | "'No tag name specified'"
		'sleep'  | ''        | "'No tag status specified'"
	}

	void "test addInterestTag for preventing duplicates"() {
		given: 'A ProfileTag instance'
		ProfileTag.addPublicInterestTag(Tag.look('tea'), user.id)

		when: 'The addInterestTag action is hit for this user with same tagName and status'
		controller.session.userId = user.id
		controller.params.tagName = 'tea'
		controller.params.tagStatus = 'PUBLIC'
		controller.addInterestTag()

		then: 'The request should to add new tag'
		controller.response.text == "'Error adding interest tag'"
	}

	void "test deleteInterestTag to successfully delete profile tags"() {
		given: 'A ProfileTag instance'
		ProfileTag.addPublicInterestTag(Tag.look('tea'), user.id)

		assert ProfileTag.count() == 1

		when: 'The deleteInterestTag action is hit with profile tag instance id'
		controller.session.userId = user.id
		controller.params.id = ProfileTag.first().id
		controller.deleteInterestTag()

		then: 'The request should delete the profile tag'
		JSONElement json = JSON.parse(controller.response.text)
		json.success == true
		ProfileTag.count() == 0
	}

	void "test deleteInterestTag when instance id is invalid"() {
		when: 'The deleteInterestTag action is hit with invalid profile tag instance id'
		controller.session.userId = user.id
		controller.params.id = 1
		controller.deleteInterestTag()

		then: 'The request should delete the profile tag'
		controller.response.text == "'No profile tag id specified'"
	}
}
