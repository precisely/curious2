import us.wearecurio.handlers.CuriousExceptionResolver
import util.marshalling.CustomObjectMarshallers
import us.wearecurio.marshaller.CorrelationMarshaller

// Place your Spring DSL code here
beans = {
	customObjectMarshallers( CustomObjectMarshallers ) {
		marshallers = [ new CorrelationMarshaller() ]
	}

	exceptionHandler(CuriousExceptionResolver) {
		exceptionMappings = [
			'java.lang.Exception': '/error'
		]
	}
}
