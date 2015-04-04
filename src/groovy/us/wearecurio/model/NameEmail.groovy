package us.wearecurio.model

class NameEmail {
	static searchable = {
		only = ['username', 'name', 'email', 'site']
	}
	
	static transients = ['username', 'name', 'email', 'site']
	
	// overridden in subclasses
	
	String getUsername() {}
	String getName() {}
	String getEmail() {}
	String getSite() {}
}
