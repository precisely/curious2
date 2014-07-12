package us.wearecurio.marshaller

import grails.converters.JSON

import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller
import org.codehaus.groovy.grails.web.json.JSONWriter

/**
 * This is to render enum name directly as value instead of object.
 * @author causecode
 */
class EnumMarshaller implements ObjectMarshaller<JSON> {

	@Override
	void marshalObject(Object object, JSON converter) throws ConverterException {
		JSONWriter writer = converter.getWriter()

		writer.value(object)
	}

	@Override
	boolean supports(Object object) {
		return object instanceof Enum
	}
}