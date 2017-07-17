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

	static UserSurvey createOrUpdate(User user, Survey survey, Status status) throws IllegalArgumentException {

		if (!user || !survey || !status) {
			throw new IllegalArgumentException('Error while saving UserSurvey instance: params missing')
		}

		UserSurvey userSurvey = findByUserAndSurvey(user, survey) ?: new UserSurvey([user: user, survey: survey])
		userSurvey.status = status
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
