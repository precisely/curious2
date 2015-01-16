import us.wearecurio.handlers.CuriousExceptionResolver
import util.marshalling.CustomObjectMarshallers
import us.wearecurio.marshaller.AnalyticsCorrelationMarshaller
import us.wearecurio.marshaller.AnalyticsTaskMarshaller

// Place your Spring DSL code here
beans = {
	customObjectMarshallers( CustomObjectMarshallers ) {
		marshallers = [ new AnalyticsCorrelationMarshaller(), new AnalyticsTaskMarshaller(), ]
	}

	exceptionHandler(CuriousExceptionResolver) {
		exceptionMappings = [
			'java.lang.Exception': '/error'
		]
	}
}
