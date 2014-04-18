import us.wearecurio.handlers.CuriousExceptionResolver

// Place your Spring DSL code here
beans = {
	exceptionHandler(CuriousExceptionResolver) {
		exceptionMappings = [
			'java.lang.Exception': '/error'
		]
	}
}
