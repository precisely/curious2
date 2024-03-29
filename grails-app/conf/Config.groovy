import com.causecode.fileuploader.CDNProvider
import com.causecode.fileuploader.util.Time
import org.scribe.builder.api.TwitterApi
import org.scribe.model.SignatureType
import us.wearecurio.thirdparty.fitbit.FitBitApi
import us.wearecurio.thirdparty.human.HumanApi
import us.wearecurio.thirdparty.jawbone.JawboneUpApi
import us.wearecurio.thirdparty.moves.MovesApi
import us.wearecurio.thirdparty.oura.LegacyOuraApi
import us.wearecurio.thirdparty.oura.OuraApi
import us.wearecurio.thirdparty.ttandme.Twenty3AndMeApi
import us.wearecurio.thirdparty.withings.WithingsApi
import org.apache.log4j.DailyRollingFileAppender
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

// The ACCEPT header will not be used for content negotiation for user agents containing the following strings (defaults to the 4 major rendering engines)
grails.mime.disable.accept.header.userAgents = ['Gecko', 'WebKit', 'Presto', 'Trident']
grails.mime.types = [ // the first one is the default format
	all:           '*/*', // 'all' maps to '*' or the first available format in withFormat
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
	hal:           ['application/hal+json','application/hal+xml'],
	xml:           ['text/xml', 'application/xml']
]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// Switch back stupid Grails default
grails.databinding.convertEmptyStringsToNull = false

// Grails default layout
grails.sitemesh.default.layout = 'nodefaultlayout'

// use javascript date format
grails.converters.json.date = 'javascript'

// Legacy setting for codec used to encode data with ${}
grails.views.default.codec = "html"

// The default scope for controllers. May be prototype, session or singleton.
// If unspecified, controllers are prototype scoped.
grails.controllers.defaultScope = 'singleton'

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']
grails.resources.adhoc.excludes = ['**/WEB-INF/**','**/META-INF/**']

// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true

// GSP settings
grails {
	views {
		gsp {
			encoding = 'UTF-8'
			htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
			codecs {
				expression = 'html' // escapes values inside ${}
				scriptlet = 'html' // escapes output from scriptlets in GSPs
				taglib = 'none' // escapes output from taglibs
				staticparts = 'none' // escapes output from static template parts
			}
		}
		// escapes all not-encoded output at final stage of outputting
		// filteringCodecForContentType.'text/html' = 'html'
	}
}

grails.converters.encoding = "UTF-8"
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

// configure passing transaction's read-only attribute to Hibernate session, queries and criterias
// set "singleSession = false" OSIV mode in hibernate configuration after enabling
grails.hibernate.pass.readonly = false
// configure passing read-only to OSIV session by default, requires "singleSession = false" OSIV mode
grails.hibernate.osiv.readonly = false

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

pushNotification {
	apns {
		pathToCertificate = "${userHome}/ios-cert/dev/iphone_dev.p12"
		password = "causecode.11"
		environment = "sandbox"
	}
	
	gcm {
		senderID = "AIzaSyCcKBWFkLYNu-lsJ1lRHNfa0QLV_HTX2Qk"
	}
}

oauth {
	providers {
		// For development & test environment
		jawboneup {
			key = "NnEi0A9cnvQ"
			secret = "6731781ff5f8478129d2c0e7e724286c083a19ce"
		}
	}
}

elasticSearch {
	datastoreImpl = 'hibernateDatastore'
	bulkIndexOnStartup = false
}

environments {
	development {
		grails.mail.disabled=true

		grails.logging.jul.usebridge = false
		grails.serverURL = "http://127.0.0.1:8080/"	/** If last `/` is removed, modify url's in oauth provider configurations **/
		grails.serverURLProtocol = "http"
		grails.config.locations = ["file:grails-app/conf/LocalConfig.groovy"]
		facebookAppId = "714092418734227"
		
		curiousanalytics.servers = [
			'http://127.0.0.1:8090'
		]

		wearecurious.adminKey = "nethgoau9er8gih5q78q9u3iyq84f98q38gq7t9qthqethqtj"
		
		/*
		elasticSearch.client.mode = 'transport'
		
		elasticSearch.client.hosts = [
			   [host:'127.0.0.1', port:9300]
		]
		*/
		elasticSearch.client.mode = 'local'
		api {
			weatherunderground {
				key = "0de9ca6314e3b2ee"
			}
			bingMapKey {
				key = "AmyVz6cE2PiwaTJV8fI9a-yxgZnHe3mjALQeL27Llt_S867hN10N7pcA6Y_zYW0n"
			}
		}
		
		oauth {
			providers {
				fitbit {
					key = "949f93d631c0401b853b333d7747a574"
					secret = "6c813aceab794174a32b7ea1532f7401"
					apiVersion = "1"
				}
				human {
					key = "0708a0bf2529bc24042d0bc1b2a3be09660a905f"
					secret = "24f75e7f789a2a0c1f94b58aa77cd067f10c8c34"
				}
				moves {
					key = "XB8ZcuJjcK2f8dI9jHDzheNG1pEnX3oK"
					secret = "92O48_cAvZ25tpxKV6hmi763zfMFIZFk2MbCdVeVW9i4iCwtgO3E96XZv6RzA6HP"
				}
				twenty3andme {
					key = "96de99b2227025cacb6807e28df20367"
					secret = "f00f94c857cba5d166463ad6f2c1aab0"
				}

				withings {
					key = "74b17c41e567dc3451092829e04c342f5c68c04806980936e1ec9cfeb8f3"
					secret = "78d839937ef5c44407b4996ed7c204ed6c55b3e76318d1371c608924b994db"
				}
				ouraLegacy {
					key = "curious-dev"
					secret = "\$2a\$10\$8IkT2RjJR0WXGnZWYZEHxe2HwcbhLEIxjfcDEqliG/2CKBxI8sso.​"
				}
				oura {
					key = 'WSMOSZBZYDCCINZ2'
					secret = '6K6E4XCJPFUFEIRMGVGSHQTCEWQ2AMDP'
				}
				/**withings {
					key = "d2560d2384cd32bcf3d96b72bc25e4d802781cb935f9e18141269c92f"
					secret = "767464759048b87ef4d6e4d2f8456010bb085eefbfd83215e5f147626fc24"
				} **/
				twitter {
					key = "gI2yYQiHUaCSaMemzkfXWPWSv"
					secret = "9v5XAQ860KaYQuk1w0Ez2hjfJ3Dpl58Sg5s3tdhbWWY9WHOmQh"
				}
			}
			debug = true
		}
		/*
		 * Workaround to prevent an error of "net.sf.ehcache.CacheException: Another unnamed CacheManager already exists in the same VM."
		 * while hot reloading application in development.
		 * https://github.com/grails/grails-core/releases/tag/v2.5.0
		 */
		beans {
			cacheManager {
				shared = true
			}
		}
	}
	qa {
		grails.mail.disabled=true
		
		grails.logging.jul.usebridge = false
		// serverUrl now must be specified in LocalConfig.groovy
		//grails.serverURL = "https://qa.precise.ly/"
		grails.serverURLProtocol = "https"
		facebookAppId = "174406919567104"
		
		curiousanalytics.servers = [
			'http://analytics-svc.qa-curious:8090',
		]
		
		/*elasticSearch.client.mode = 'node'

		elasticSearch.client.hosts = [
			   [host:'127.0.0.1', port:9300],
		]*/
		
		grails.serverURLProtocol = "https"
		def extConfig = System.properties.getProperty('CURIOUSCONFIGHOME');
		if (!extConfig) extConfig = "/home/precisely/localconfig"
		grails.config.locations = [ "file:${extConfig}/LocalConfig.groovy" ]

		elasticSearch.client.mode = 'transport'
		
		elasticSearch.client.hosts = [
			   [host:'elasticsearch-svc.qa-curious', port:9300],
		]
		
		api {
			weatherunderground {
				key = "0de9ca6314e3b2ee"
			}
			bingMapKey {
				key = "AmyVz6cE2PiwaTJV8fI9a-yxgZnHe3mjALQeL27Llt_S867hN10N7pcA6Y_zYW0n"
			}
		}
		oauth {
			providers {
				fitbit {
					key = "b2610f22a2314bdc804c3463aa666876"
					secret = "2b7472411c834c4f9b8c8e611d8e6350"
					apiVersion = "1"
				}
				human {
					key = "6c735bf30082353bab85112a831bb7a9ec7b1154"
					secret = "6db556720f847133ccdd92ce93224c9fb481010b"
				}
				moves {
					key = "CNhtcT6smpiRts939C84qjUJlS5MJTC6"
					secret = "Fi4TT55XOFGm6DrOwt189Sk4Oap3cBhWdP1nCMJ8950nSfpjWBa9Ot4kszJxV22X"
				}
				twenty3andme {
					key = "96de99b2227025cacb6807e28df20367"
					secret = "f00f94c857cba5d166463ad6f2c1aab0"
				}
				// temporary fix to use development app key
				withings {
					key = "d2560d2384cd32bcf3d96b72bc25e4d802781cb935f9e18141269c92f"
					secret = "767464759048b87ef4d6e4d2f8456010bb085eefbfd83215e5f147626fc24"
				}
				/* withings {
					key = "74b17c41e567dc3451092829e04c342f5c68c04806980936e1ec9cfeb8f3"
					secret = "78d839937ef5c44407b4996ed7c204ed6c55b3e76318d1371c608924b994db"
				}*/
				jawboneup {
					key = "zNg9SWw1G74"
					secret = "55de868ea52b843d07400c3dc79f5e533a478006"
				}
				oura {
					key = 'WSMOSZBZYDCCINZ2'
					secret = '6K6E4XCJPFUFEIRMGVGSHQTCEWQ2AMDP'
				}
				twitter {
					key = "gI2yYQiHUaCSaMemzkfXWPWSv"
					secret = "9v5XAQ860KaYQuk1w0Ez2hjfJ3Dpl58Sg5s3tdhbWWY9WHOmQh"
				}
			}
		}
		
		pushNotification {
			apns {
				pathToCertificate = "${userHome}/ios-cert/iphone_prod.p12"
				password = "causecode.11"
				environment = "production"
			}
			
			gcm {
				senderID = "AIzaSyCcKBWFkLYNu-lsJ1lRHNfa0QLV_HTX2Qk"
			}
		}
		
/*		api {
			weatherunderground {
				key = "0de9ca6314e3b2ee"
			}
			bingMapKey {
				key = "AmyVz6cE2PiwaTJV8fI9a-yxgZnHe3mjALQeL27Llt_S867hN10N7pcA6Y_zYW0n"
			}
		}
		oauth {
			providers {
				fitbit {
					key = "b2610f22a2314bdc804c3463aa666876"
					secret = "2b7472411c834c4f9b8c8e611d8e6350"
					apiVersion = "1"
				}
				human {
					key = "6c735bf30082353bab85112a831bb7a9ec7b1154"
					secret = "6db556720f847133ccdd92ce93224c9fb481010b"
				}
				moves {
					key = "CNhtcT6smpiRts939C84qjUJlS5MJTC6"
					secret = "Fi4TT55XOFGm6DrOwt189Sk4Oap3cBhWdP1nCMJ8950nSfpjWBa9Ot4kszJxV22X"
				}
				twenty3andme {
					key = "96de99b2227025cacb6807e28df20367"
					secret = "f00f94c857cba5d166463ad6f2c1aab0"
				}
				// temporary fix to use development app key
				// withings {
				//	key = "d2560d2384cd32bcf3d96b72bc25e4d802781cb935f9e18141269c92f"
				//	secret = "767464759048b87ef4d6e4d2f8456010bb085eefbfd83215e5f147626fc24"
				//}
				withings {
					key = "74b17c41e567dc3451092829e04c342f5c68c04806980936e1ec9cfeb8f3"
					secret = "78d839937ef5c44407b4996ed7c204ed6c55b3e76318d1371c608924b994db"
				}
				jawboneup {
					key = "VUDxNbIdPYE"
					secret = "23007f9c511576608bd991bdbad53b6703840ef9"
				}
			}
		}
		
		pushNotification {
			apns {
				pathToCertificate = "${userHome}/ios-cert/iphone_prod.p12"
				password = "causecode.11"
				environment = "production"
			}
			
			gcm {
				senderID = "AIzaSyCcKBWFkLYNu-lsJ1lRHNfa0QLV_HTX2Qk"
			}
		}*/
	}
	production {
		grails.mail.disabled=false
		grails.logging.jul.usebridge = false
		grails.serverURL = "https://www.precise.ly/"
		grails.serverURLProtocol = "https"
		def extConfig = System.properties.getProperty('CURIOUSCONFIGHOME');
		if (!extConfig) extConfig = "/home/precisely/localconfig"
		grails.config.locations = [ "file:${extConfig}/LocalConfig.groovy" ]
		facebookAppId = "164274177247045"

		curiousanalytics.servers = [
				'http://analytics-svc.prod-curious:8090'
		]
		
		elasticSearch.client.mode = 'transport'
		
		elasticSearch.client.hosts = [
				[host:'elasticsearch-svc.prod-curious', port:9300],
		]
		
		api {
			weatherunderground {
				key = "0de9ca6314e3b2ee"
			}
			bingMapKey {
				key = "AmyVz6cE2PiwaTJV8fI9a-yxgZnHe3mjALQeL27Llt_S867hN10N7pcA6Y_zYW0n"
			}
		}
		oauth {
			providers {
				fitbit {
					key = "b2610f22a2314bdc804c3463aa666876"
					secret = "2b7472411c834c4f9b8c8e611d8e6350"
					apiVersion = "1"
				}
				human {
					key = "6c735bf30082353bab85112a831bb7a9ec7b1154"
					secret = "6db556720f847133ccdd92ce93224c9fb481010b"
				}
				moves {
					key = "CNhtcT6smpiRts939C84qjUJlS5MJTC6"
					secret = "Fi4TT55XOFGm6DrOwt189Sk4Oap3cBhWdP1nCMJ8950nSfpjWBa9Ot4kszJxV22X"
				}
				twenty3andme {
					key = "96de99b2227025cacb6807e28df20367"
					secret = "f00f94c857cba5d166463ad6f2c1aab0"
				}
				withings {
					key = "2ef70026d181d3eb3cd9800186bc19b9d0525de83cf6527d4b2f08dc9e38f1b"
					secret = "c2fe6d781329f53e53b85c6944eb48ccc08fc555d497cd1b6291350de3d80f9"
				}
				jawboneup {
					key = "zNg9SWw1G74"
					secret = "55de868ea52b843d07400c3dc79f5e533a478006"
				}
				twitter {
					key = "8w0fMihxLuRAEgiu7ucnX80qP"
					secret = "PETZAiFGhW9tVAbcYAIYbbMIZOLZnYKkCZlIP3q5NaGRIskV4c"
				}
			}
		}
		
		pushNotification {
			apns {
				pathToCertificate = "/home/precisely/ios-cert/iphone_prod.p12"
				password = "causecode.11"
				environment = "production"
			}
			
			gcm {
				senderID = "AIzaSyCcKBWFkLYNu-lsJ1lRHNfa0QLV_HTX2Qk"
			}
		}
	}
	test {
		grails.serverURL = "http://127.0.0.1:8080/"
		grails.serverURLProtocol = "http"
		grails.config.locations = ["file:grails-app/conf/LocalConfig.groovy"]
		facebookAppId = "714092418734227"
		
		curiousanalytics.servers = [
			'http://127.0.0.1:8090'
		]
				
		elasticSearch.client.mode = 'local'

		pushNotification {
			apns {
				pathToCertificate = "ios-cert/may-2017/iphone_dev_test.p12"
				password = "causecode.11"
				environment = "dev"
			}
		}
		
		api {
			weatherunderground {
				key = "0de9ca6314e3b2ee"
			}
			bingMapKey {
				key = "AmyVz6cE2PiwaTJV8fI9a-yxgZnHe3mjALQeL27Llt_S867hN10N7pcA6Y_zYW0n"
			}
		}
		oauth {
			providers {
				fitbit {
					key = "b2610f22a2314bdc804c3463aa666876"
					secret = "2b7472411c834c4f9b8c8e611d8e6350"
					apiVersion = "1"
				}
				human {
					key = "0708a0bf2529bc24042d0bc1b2a3be09660a905f"
					secret = "24f75e7f789a2a0c1f94b58aa77cd067f10c8c34"
				}
				moves {
					key = "XB8ZcuJjcK2f8dI9jHDzheNG1pEnX3oK"
					secret = "92O48_cAvZ25tpxKV6hmi763zfMFIZFk2MbCdVeVW9i4iCwtgO3E96XZv6RzA6HP"
				}
				twenty3andme {
					key = "96de99b2227025cacb6807e28df20367"
					secret = "f00f94c857cba5d166463ad6f2c1aab0"
				}
				withings {
					key = "d2560d2384cd32bcf3d96b72bc25e4d802781cb935f9e18141269c92f"
					secret = "767464759048b87ef4d6e4d2f8456010bb085eefbfd83215e5f147626fc24"
				}
				twitter {
					key = "gI2yYQiHUaCSaMemzkfXWPWSv"
					secret = "9v5XAQ860KaYQuk1w0Ez2hjfJ3Dpl58Sg5s3tdhbWWY9WHOmQh"
				}
				oura {
					key = 'WSMOSZBZYDCCINZ2'
					secret = '6K6E4XCJPFUFEIRMGVGSHQTCEWQ2AMDP'
				}
			}
		}
	}
}

// log4j configuration
log4j.main = {
	appenders {
		console name:'stdout', layout:pattern(conversionPattern: '%-5p %d %c{2} %x - %m%n%n')
		environments {
			development {
				file name: 'extraAppender',
						conversionPattern: '%d{yyyy-MM-dd/HH:mm:ss.SSS} [%t] %x %-5p %c{2} - %m%n',
						file: '/tmp/curious.log'
				debug extraAppender: ['us.wearecurio.model',
					'us.wearecurio.controller',
					'us.wearecurio.services',
					'us.wearecurio.server',
					'us.wearecurio.util',
					'us.wearecurio.exceptions',
					'us.wearecurio.utility',
					'us.wearecurio',
					'grails.app.conf',
					'grails.app.controllers',
					'grails.app.services.us.wearecurio',
					'com.causecode', 
					'grails.app.services.com.causecode',
					'grails.app.jobs.com.causecode',
					'grails.app.jobs.us.wearecurio.jobs']
				trace "org.scribe.model"
			}
			production {
				appender new DailyRollingFileAppender(name: 'debugFile', datePattern: "'.'MM-dd-yyyy",
						fileName: '/opt/jetty/logs/logFile',
						layout: pattern(conversionPattern: '%-5p %d %c{2} %x - %m%n%n'))
				debug debugFile: ['us.wearecurio.model',
						'us.wearecurio.controller',
						'us.wearecurio.services',
						'us.wearecurio.server',
						'us.wearecurio.util',
						'us.wearecurio.exceptions',
						'us.wearecurio.utility',
						'us.wearecurio',
						'grails.app.conf',
						'grails.app.controllers',
						'grails.app.services.us.wearecurio',
						'grails.app.services.com.causecode',
						'grails.app.jobs.com.causecode',
						'com.causecode',
						'grails.app.jobs.us.wearecurio.jobs',
						'SecurityFilters']

				warn debugFile: ['us.wearecurio.parse',
						'org.codehaus.groovy.grails.web.servlet',
						'org.codehaus.groovy.grails.web.pages',
						'org.codehaus.groovy.grails.web.sitemesh',
						'org.codehaus.groovy.grails.web.mapping.filter',
						'org.codehaus.groovy.grails.web.mapping',
						'org.codehaus.groovy.grails.commons',
						'org.codehaus.groovy.grails.plugins',
						'org.codehaus.groovy.grails.orm.hibernate',
						'org.springframework',
						'org.hibernate']
			}
		}
	}

	debug  'us.wearecurio.model',
		   'us.wearecurio.controller',
		   'us.wearecurio.services',
		   'us.wearecurio.server',
		   'us.wearecurio.util',
		   'us.wearecurio.exceptions',
		   'us.wearecurio.utility',
		   'us.wearecurio',
		   'grails.app.conf',
		   'grails.app.controllers',
		   'grails.app.services.us.wearecurio',
		   'grails.app.services.com.causecode',
		   'grails.app.jobs.com.causecode',
		   'com.causecode',
		   'grails.app.jobs.us.wearecurio.jobs',
		   'SecurityFilters'
	

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

	error  'org.springframework.aop.framework.Cglib2AopProxy'

	warn   'org.mortbay.log'
	// Uncomment to see hibernate queries with their values
	//debug 'org.hibernate.SQL'
	//trace 'org.hibernate.type'
}

oauth {
	providers {
		fitbit {
			api = FitBitApi
			callback = "${grails.serverURL }oauth/fitbit/callback"
			successUri = "authentication/fitbit/success"
			failureUri = "authentication/fitbit/fail"
			signatureType = SignatureType.Header
		}
		human {
			api = HumanApi
			callback = "${grails.serverURL }oauth/human/callback"
			successUri = "authentication/human/success"
			failureUri = "authentication/human/fail"
		}
		jawboneup {
			api = JawboneUpApi
			callback = "${grails.serverURL }oauth/jawboneup/callback"
			successUri = "authentication/jawboneup/success"
			failureUri = "authentication/jawboneup/fail"
			scope = "basic_read mood_read move_read sleep_read meal_read weight_read"
			signatureType = SignatureType.Header
		}
		moves {
			api = MovesApi
			callback = "${grails.serverURL }oauth/moves/callback"
			successUri = "authentication/moves/success"
			failureUri = "authentication/moves/fail"
			scope = "activity"
			signatureType = SignatureType.QueryString
		}
		twenty3andme {
			api = Twenty3AndMeApi
			callback = "${grails.serverURL }oauth/twenty3andme/callback"
			successUri = "authentication/twenty3andme/success"
			failureUri = "authentication/twenty3andme/fail"
			scope = "names basic genomes"
		}
		withings {
			api = WithingsApi
			callback = "${grails.serverURL }authentication/withingCallback"	// Exceptional case, since withings sends userId as parameter
			successUri = "authentication/withings/success"
			failureUri = "authentication/withings/fail"
			signatureType = SignatureType.QueryString
		}
		ouraLegacy {
			api = LegacyOuraApi
			key = "curious"
			secret = "\$2a\$10\$eBxUN4gGeZV5V4lQdbeSEezI990aDJMEsqkW.4NLy2E6KKJtWyFHK"
			callback = "${grails.serverURL}oauth/oura/callback"
			successUri = "authentication/ouraLegacy/success"
			failureUri = "authentication/ouraLegacy/fail"
			scope = "read"
		}
		oura {
			api = OuraApi
			callback = "${grails.serverURL}oauth/oura/callback"
			successUri = "authentication/oura/success"
			failureUri = "authentication/oura/fail"
			scope = "daily+email+personal"
		}
		twitter {
			callback = "${grails.serverURL}oauth/twitter/callback"
			successUri = "authentication/twitter/success"
			failureUri = "authentication/twitter/fail"
			api = TwitterApi
		}

	}
	debug = true
}

fileuploader {
	groups {
		provider = CDNProvider.GOOGLE
		avatar {
			maxSize = 1024 * 1024 * 10 // 10MB
			allowedExtensions = ["jpg", "jpeg", "gif", "png", "JPG", "JPEG", "GIF", "PNG"]
			storageTypes = "CDN"
			container = "curious-avatar"
			provider = CDNProvider.GOOGLE
			expirationPeriod = Time.DAY * 90 // 90 Day time in seconds. Note this time should be in seconds.
		}
	}
}
