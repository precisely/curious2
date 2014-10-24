import us.wearecurio.handlers.CuriousExceptionResolver
import util.marshalling.CustomObjectMarshallers
import us.wearecurio.marshaller.AnalyticsCorrelationMarshaller

// Place your Spring DSL code here
beans = {
	customObjectMarshallers( CustomObjectMarshallers ) {
		marshallers = [ new AnalyticsCorrelationMarshaller() ]
	}

	exceptionHandler(CuriousExceptionResolver) {
		exceptionMappings = [
			'java.lang.Exception': '/error'
		]
	}
}
