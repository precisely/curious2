import grails.util.Environment

eventConfigureTomcat = {tomcat ->

	if (Environment.current == Environment.DEVELOPMENT) {
		println "Changing the symlink configuration for tomcat environment: $Environment.current"
		def ctx = tomcat.host.findChild("") 
		ctx.getResources().allowLinking = true 
		println "Configuration changed"
	}
} 
