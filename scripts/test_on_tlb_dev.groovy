import us.wearecurio.model.*
import us.wearecurio.services.*

time_file = "start_times.out"
new File(time_file).delete()
logfile = new File(time_file)
log = {
	logfile.withWriterAppend("UTF-8", { writer ->
		writer.write( "CuriousSeries: ${it}\n")
	})
}

start = new Date()
log("START: ${start}")

new File("test.out").delete()
u = User.first()
//u = User.findAll()
cs = new CorrelationService()
cs.updateUserCorrelationsDebug(u)


stop_time = new Date()
log("STOP: ${stop_time}")
log("TOTAL TIME: ${(stop_time.getTime() - start.getTime())/1000.0/60.0} minutes")
