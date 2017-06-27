package us.wearecurio.marshaller

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONWriter
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller
import us.wearecurio.model.survey.Question

/**
 * This is to render all the Answers of each Question within a Survey.
 */
class QuestionMarshaller implements ObjectMarshaller<JSON> {

	@Override
	void marshalObject(Object object, JSON converter) throws ConverterException {
		Question questionInstance = object as Question
		JSONWriter writer = converter.getWriter()

		writer.object()

		writer.key('id')
		converter.value(questionInstance.id)

		writer.key('status')
		converter.convertAnother(questionInstance.status)

		writer.key('question')
		converter.convertAnother(questionInstance.question)

		writer.key('priority')
		converter.convertAnother(questionInstance.priority)

		writer.key('answerType')
		converter.convertAnother(questionInstance.answerType)

		writer.key('isRequired')
		converter.convertAnother(questionInstance.isRequired)

		writer.key('answers')
		converter.convertAnother(questionInstance.answers)

		writer.endObject()
	}

	@Override
	boolean supports(Object object) {
		return object instanceof Question
	}
}
