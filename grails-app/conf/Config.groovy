// locations to search for config files that get merged into the main config;
// config files can be ConfigSlurper scripts, Java properties files, or classes
// in the classpath in ConfigSlurper format

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [
    all:           '*/*',
    atom:          'application/atom+xml',
    css:           'text/css',
    csv:           'text/csv',
    form:          'application/x-www-form-urlencoded',
    html:          ['text/html','application/xhtml+xml'],
    js:            'text/javascript',
    json:          ['application/json', 'text/json'],
    multipartForm: 'multipart/form-data',
    rss:           'application/rss+xml',
    text:          'text/plain',
    xml:           ['text/xml', 'application/xml']
]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// Grails default layout
grails.sitemesh.default.layout = 'nodefaultlayout'

// use javascript date format
grails.converters.json.date = 'javascript'

// no default sitemesh layout
grails.sitemesh.default.layout = 'noexistinglayout' // when specifying a non-existent layout, it skips the layout

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']

// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

grails.app.context = '/'

// For background-thread plugin
backgroundThread {
  queueSize = 1000 // Maximum number of tasks to queue up
  threadCount = 5 // Number of threads processing background tasks.
  tasksPerDrain = 100 // See Note
}

android.gcm.api.key = 'AIzaSyCcKBWFkLYNu-lsJ1lRHNfa0QLV_HTX2Qk'
android.gcm.time.to.live=1419200
android.gcm.delay.'while'.idle=false
android.gcm.retries=3

environments {
    development {
        grails.logging.jul.usebridge = true
        grails.serverURL = "http://127.0.0.1:8080/"
		grails.serverURLProtocol = "http"
		grails.config.locations = ["file:grails-app/conf/LocalConfig.groovy"]
		
		api {
			weatherunderground {
				key = "0de9ca6314e3b2ee"
			}
			bingMapKey {
				key = "AmyVz6cE2PiwaTJV8fI9a-yxgZnHe3mjALQeL27Llt_S867hN10N7pcA6Y_zYW0n"
			}
			withings {
				key = "d2560d2384cd32bcf3d96b72bc25e4d802781cb935f9e18141269c92f"
				secret = "767464759048b87ef4d6e4d2f8456010bb085eefbfd83215e5f147626fc24"
			}
			fitbit {
				key = "949f93d631c0401b853b333d7747a574"
				secret = "6c813aceab794174a32b7ea1532f7401"
				apiVersion = "1"
			}
		}
    }
    production {
        grails.logging.jul.usebridge = false
        grails.serverURL = "https://dev.wearecurio.us/"
		grails.serverURLProtocol = "https"
		api {
			weatherunderground {
				key = "0de9ca6314e3b2ee"
			}
			bingMapKey {
				key = "AmyVz6cE2PiwaTJV8fI9a-yxgZnHe3mjALQeL27Llt_S867hN10N7pcA6Y_zYW0n"
			}
			withings {
				key = "74b17c41e567dc3451092829e04c342f5c68c04806980936e1ec9cfeb8f3"
				secret = "78d839937ef5c44407b4996ed7c204ed6c55b3e76318d1371c608924b994db"
			}
			fitbit {
				key = "b2610f22a2314bdc804c3463aa666876"
				secret = "2b7472411c834c4f9b8c8e611d8e6350"
				apiVersion = "1"
			}
		}
    }
    test {
        grails.serverURL = "http://127.0.0.1:8080/"
		grails.serverURLProtocol = "http"
		grails.config.locations = ["file:grails-app/conf/LocalConfig.groovy"]
		api {
			weatherunderground {
				key = "0de9ca6314e3b2ee"
			}
			bingMapKey {
				key = "AmyVz6cE2PiwaTJV8fI9a-yxgZnHe3mjALQeL27Llt_S867hN10N7pcA6Y_zYW0n"
			}
			withings {
				key = "d2560d2384cd32bcf3d96b72bc25e4d802781cb935f9e18141269c92f"
				secret = "767464759048b87ef4d6e4d2f8456010bb085eefbfd83215e5f147626fc24"
			}
			fitbit {
				key = "b2610f22a2314bdc804c3463aa666876"
				secret = "2b7472411c834c4f9b8c8e611d8e6350"
				apiVersion = "1"
			}
		}
    }
}

// log4j configuration
log4j = {
    // Example of changing the log pattern for the default console appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}
	
	appenders {
		console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
	}

	debug  'us.wearecurio.model',
		   'us.wearecurio.controller',
		   'us.wearecurio.services',
		   'us.wearecurio.server',
		   'us.wearecurio.util',
		   'us.wearecurio.exceptions',
		   'us.wearecurio.utility',
		   'grails.app.controllers',
		   'grails.app.service'

	warn   'us.wearecurio.parse'

	warn   'org.codehaus.groovy.grails.web.servlet'  //  controllers

	warn   'org.codehaus.groovy.grails.web.pages', //  GSP
		   'org.codehaus.groovy.grails.web.sitemesh', //  layouts
		   'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
		   'org.codehaus.groovy.grails.web.mapping', // URL mapping
		   'org.codehaus.groovy.grails.commons', // core / classloading
		   'org.codehaus.groovy.grails.plugins', // plugins
		   'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
		   'org.springframework',
		   'org.hibernate'

	warn   'org.mortbay.log'
			
}
