package us.wearecurio.marshaller

import grails.converters.JSON
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller
import org.codehaus.groovy.grails.web.json.JSONWriter
import us.wearecurio.profiletags.ProfileTag

class ProfileTagMarshaller implements ObjectMarshaller<JSON> {

	@Override
	void marshalObject(Object object, JSON converter) throws ConverterException {
		ProfileTag profileTagInstance = object as ProfileTag
		JSONWriter writer = converter.getWriter()

		writer.object()

		writer.key('id')
				converter.value(profileTagInstance.id)

		writer.key('status')
				converter.convertAnother(profileTagInstance.status)

		writer.key('type')
				converter.convertAnother(profileTagInstance.type)

		writer.key('tag')
				converter.convertAnother(profileTagInstance.tag)

		writer.endObject()
	}

	@Override
	boolean supports(Object object) {
		return object instanceof ProfileTag
	}
}
