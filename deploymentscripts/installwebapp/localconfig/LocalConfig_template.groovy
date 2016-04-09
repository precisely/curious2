import com.lucastex.grails.fileuploader.CDNProvider

wearecurious.adminKey = "ADMINKEY"
wearecurious.runImportJobs = false
dataSource {
	url = "jdbc:mysql://curiousdb/DBNAME?zeroDateTimeBehavior=convertToNull"
	username = "DBUSERNAME"
	password = "DBPASSWORD"
}
grails.serverURL = "https://APPHOSTNAME/"
fileuploader {
	provider = CDNProvider.RACKSPACE
	RackspaceUsername = "RACKSPACEUSERNAME"
	RackspaceKey = "RACKSPACEKEY"
}
