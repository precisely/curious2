import us.wearecurio.thirdparty.AuthenticationRequiredException

class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}

		"/authentication/$provider" {
			controller = "authentication"
			action = {
				return "" + params.provider.toString() + "Auth"
			}
		}

		"/mobile/cache.manifest" (controller: "mobile", action: "cachemanifest")
		
		"500" (controller: "authentication", action: "authenticateProvider", exception: AuthenticationRequiredException)

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
