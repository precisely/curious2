grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.work.dir = "target/work"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.project.fork = [
    // configure settings for compilation JVM, note that if you alter the Groovy version forked compilation is required
    //  compile: [maxMemory: 256, minMemory: 64, debug: false, maxPerm: 256, daemon:true],

    // configure settings for the test-app JVM, uses the daemon by default
    //test: [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 256, daemon:true],
    test: false,
    // configure settings for the run-app JVM
    //run: [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the run-war JVM
    war: [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the Console UI JVM
    console: [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 256]
]

if (Environment.current == Environment.DEVELOPMENT) {
	grails.project.plugins.dir = "./plugins"
}

grails.project.dependency.authentication = {
	credentials {
		id = "curiousRepo"
		username = "curious"
		password = "eracEMbleN"
	}
}

grails.project.dependency.resolver = "maven" // or ivy
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        mavenRepo ([id: "curiousRepo", url: "http://curious-maven.causecode.com"])
        grailsPlugins()
        grailsHome()
        mavenLocal()
        grailsCentral()
        mavenCentral()
        // uncomment these (or add new ones) to enable remote dependency resolution from public Maven repositories
		mavenRepo "http://repo.grails.org/grails/core"
		mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }

    dependencies {
		// specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes e.g.
		compile 'com.ganyo:gcm-server:1.0.2'
		compile 'com.notnoop.apns:apns:1.0.0.Beta6'
        runtime 'mysql:mysql-connector-java:5.1.29'
        // runtime 'org.postgresql:postgresql:9.3-1101-jdbc41'
        test "org.grails:grails-datastore-test-support:1.0-grails-2.4"
		runtime 'commons-dbcp:commons-dbcp:1.4' //or 1.3 if java 1.5
		compile 'org.codehaus.groovy.modules.http-builder:http-builder:0.7.1'
    }

    plugins {
        // plugins for the build system only
        build ":tomcat:8.0.22"

        // plugins for the compile step
        compile ":scaffolding:2.1.2"
        compile ':cache:1.1.8'
        //compile ":asset-pipeline:2.1.5"

		compile ":mail:1.0.7"
		compile ":oauth:2.6.1"
		compile ":csv:0.3.1"
		compile ":message-digest:1.1"
		runtime ':elasticsearch:0.0.4.2'
		compile ":quartz:1.0.2"
		// File Uploader Plugin
		compile "com.causecode.plugins:file-uploader:2.5.2"
		//compile ":spring-security-core:2.0-RC5"
		//compile ":searchable:0.6.9"
		
        // plugins needed at runtime but not for compilation
        //runtime ":hibernate4:4.3.8.1" // or
		runtime ":hibernate:3.6.10.19"
		//runtime ':hibernate4:4.3.5.5'
		//runtime "org.grails.plugins:hibernate4:5.0.0.RC1"
        //runtime ":database-migration:1.4.0"

        // Uncomment these to enable additional asset-pipeline capabilities
        //compile ":sass-asset-pipeline:1.9.0"
        //compile ":less-asset-pipeline:1.10.0"
        //compile ":coffee-asset-pipeline:1.8.0"
        //compile ":handlebars-asset-pipeline:1.3.0.3"
    }
}

grails.war.resources = { stagingDir, args ->
	println "Deleting some files from WAR"
	delete(file: "${stagingDir}/WEB-INF/classes/us/wearecurio/controller/DummyController.class")
}
