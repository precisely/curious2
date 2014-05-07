package us.wearecurio.handlers

import org.apache.commons.logging.LogFactory
import us.wearecurio.utility.Utils

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver
import org.codehaus.groovy.runtime.InvokerInvocationException
import org.springframework.web.servlet.ModelAndView

class CuriousExceptionResolver extends GrailsExceptionResolver {
	private static def log = LogFactory.getLog(this)
	
	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) {
		Throwable t = e
		
		while (t != null) {
			if (t instanceof us.wearecurio.thirdparty.AuthenticationRequiredException)
				// don't notify on this exception, should be handled by Grails
				return super.resolveException(request, response, handler, e);
			t = t.getCause()
		}
		
		ByteArrayOutputStream os = new ByteArrayOutputStream()
		e.printStackTrace(new PrintStream(os))
		String output = os.toString("UTF8");
		   
		log.error("INTERCEPTED EXCEPTION")
		log.error(output)
	   
	   	def messageBody = "Error while executing Curious app:\n" + output
		def messageSubject = "CURIOUS SERVER ERROR"
		Utils.getMailService().sendMail {
			to "server@wearecurio.us"
			from "server@wearecurio.us"
			subject messageSubject
			body messageBody
		}
		
		return super.resolveException(request, response, handler, e);
	}
}
