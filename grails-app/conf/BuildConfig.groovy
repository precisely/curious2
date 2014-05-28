
import grails.util.Environment

grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

// uncomment (and adjust settings) to fork the JVM to isolate classpaths
//grails.project.fork = [
//   run: [maxMemory:1024, minMemory:64, debug:false, maxPerm:256]
//]
if (Environment.current == Environment.DEVELOPMENT) {
	grails.project.plugins.dir="./plugins"
}

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
		excludes 'dbcp'
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()

        mavenRepo "http://snapshots.repository.codehaus.org"
        mavenRepo "http://repository.codehaus.org"
        mavenRepo "http://download.java.net/maven/2/"
        mavenRepo "http://repository.jboss.com/maven2/"
		mavenRepo "https://github.com/slorber/gcm-server-repository/raw/master/releases/"
    }

    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes e.g.
		compile 'com.ganyo:gcm-server:1.0.2'
		compile 'com.notnoop.apns:apns:0.1.6'
		runtime 'mysql:mysql-connector-java:5.1.22'
		runtime 'commons-dbcp:commons-dbcp:1.4' //or 1.3 if java 1.5
    }

    plugins {
        compile ":hibernate:$grailsVersion"
        compile ":jquery:1.8.3"
		compile ":message-digest:1.1"
		
		compile ":platform-core:1.0.0"
        // Uncomment these (or add new ones) to enable additional resources capabilities
        //runtime ":zipped-resources:1.0"
        //runtime ":cached-resources:1.0"
        //runtime ":yui-minify-resources:0.1.5"

        build ":tomcat:$grailsVersion"

        compile ":database-migration:1.3.2"

        compile ':cache:1.0.1'
		
		compile ":mail:1.0.1"
		
		compile ":quartz:1.0-RC9"
		if (Environment.current != Environment.PRODUCTION) {
			compile (":quartz-monitor:0.3-RC3") {
				excludes "quartz"
			}
		}

		compile ":oauth:2.4"
		compile ":csv:0.3.1"
    }
}
