package us.wearecurio.server

import org.apache.commons.logging.LogFactory
import us.wearecurio.model.Model

class BackgroundTask implements Runnable {
	Closure c
	boolean success
	
	static transactional = false
	
	public static launch(Closure c) {
		new Thread(new BackgroundTask(c)).start()
	}
	
	BackgroundTask(Closure c) {
		this.c = c
		this.success = false
	}
	
	void run() {
		try {
			Model.withNewSession {
				c()
			}
		} catch (Exception e) {
			e.printStackTrace()
		}
		success = true
	}
	
	public boolean getSuccess() {
		return success
	}
}