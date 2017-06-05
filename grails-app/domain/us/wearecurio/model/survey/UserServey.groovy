package us.wearecurio.model.survey

import us.wearecurio.model.User
import us.wearecurio.utility.Utils

/**
 * This class keeps the records of surveys for users.
 */
class UserSurvey {

	User user
	Survey survey
	Status status

	static mappings = {
		version false
		id composite: ['user', 'survey']
	}

	static UserSurvey create(User user, Survey survey, Status status) {
		UserSurvey userSurvey = new UserSurvey([user: user, survey: survey, status: status])
		Utils.save(userSurvey, true)

		return userSurvey
	}

	String toString() {
		return "UserSurvey(id: ${id}, userId: ${user.id}, survyeId: ${survey.id})"
	}
}

enum Status {
	TAKEN(0),
	NOT_TAKEN(1),
	IGNORED(2)

	final int id
	Status(int id) {
		this.id = id
	}
}
