package us.wearecurio.model

import org.codehaus.groovy.grails.validation.ConstrainedProperty
import us.wearecurio.utility.Utils

class UserSurveyAnswer {

	Long userId
	String questionCode
	String answerText

	static constraints = {
		questionCode(nullable: false)
	}

	public static UserSurveyAnswer create(User user, String questionCode, String answerText) {
		SurveyQuestion surveyQuestion = SurveyQuestion.findByCode(questionCode)

		if (!surveyQuestion) {
			return null
		}

		if (surveyQuestion.validator) {
			GroovyShell shell = new GroovyShell()
			CustomAnswerFieldHolder customAnswerHolder = new CustomAnswerFieldHolder()
			ConstrainedProperty constrainedProperty = new ConstrainedProperty(CustomAnswerFieldHolder, "answerText", String)
			
			Closure validationClosure = shell.evaluate(surveyQuestion.validator)
			constrainedProperty.applyConstraint(ConstrainedProperty.VALIDATOR_CONSTRAINT, validationClosure)
			constrainedProperty.validate(customAnswerHolder, answerText, customAnswerHolder.getErrors())
			if (customAnswerHolder.hasErrors()) {
				return null
			}
		}
		
		UserSurveyAnswer userSurveyAnswer = new UserSurveyAnswer()
		userSurveyAnswer.userId = user.id
		userSurveyAnswer.questionCode = questionCode
		userSurveyAnswer.answerText = answerText
		userSurveyAnswer.validate()
		
		if (userSurveyAnswer.hasErrors()) {
			return null
		} else {
			try {
				Utils.save(userSurveyAnswer, false)
				return userSurveyAnswer
			} catch (Exception e) {
				return null
			}
		}
	}
}
