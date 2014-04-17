import us.wearecurio.model.*
import us.wearecurio.services.*

time_file = "progress.out"
new File(time_file).delete()
logfile = new File(time_file)
log = {
	logfile.withWriterAppend("UTF-8", { writer ->
		writer.write( "CuriousSeries: ${it}\n")
	})
}

users = User.findAll()
cs = new CorrelationService()

count = 0
for (user in users) {
	start = new Date()
	cs.updateUserCorrelations(user)
	stop_time = new Date()
	total_time = (stop_time.getTime() - start.getTime())/1000.0/60.0
	log("#${count} ${total_time} minutes, User: ${user.id}")
	count += 1
}


