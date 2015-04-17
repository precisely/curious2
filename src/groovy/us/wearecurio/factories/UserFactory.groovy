package us.wearecurio.factories

import us.wearecurio.model.User

class UserFactory {

	static String generate_email(username) {
		"${username}@${username}.com"
	}

	static User make(Map params = [:]) {
		params['username'] = params['username'] ?: 'y'
		params['email'] = params['email'] ?: generate_email(params['username'])
		params['sex'] = params['sex'] ?: 'F'
		params['birthdate'] = params['birthdate'] ?: '01/01/2001'
		if ((!params['name']) && (params['first'] || params['last'])) {
			params['name'] = (params['first'] ?: '') + ' ' + (params['last'] ?: '')
		}
		params['name'] = params['name'] ?: params['username']
		params['password'] = params['password'] ?: params['username']

		def user = User.findByUsername(params['username'])

		if (user == null) {
			user = User.create(params)
		}

		for (p in params) {
			if (p.key == 'birthdate') {
				p.value = new Date(p.value)
			}
			user.setProperty(p.key, p.value)
		}
		user.save()
		return user
	}
}
