package us.wearecurio.abstraction

class Callbacks {
	def callbacks = [:]
	
	public Callbacks() {}
	
	public add(def key, Closure c) {
		callbacks[key] = c
	}
	
	public remove(def key) {
		callbacks.remove(key)
	}
	
	public call(arg) {
		for (key in callbacks.keySet()) {
			callbacks[key](arg)
		}
	}
}
