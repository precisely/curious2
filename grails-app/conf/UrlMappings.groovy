import us.wearecurio.thirdparty.AuthenticationRequiredException

class UrlMappings {

	static mappings = {

		"/correlation/index/$flavor" {
			controller = "correlation"
			action = "index"
		}

		"/correlation/patch/$id/markViewed" {
			controller = "correlation"
			action = "markViewed"
		}

		"/correlation/patch/$id/markNoise" {
			controller = "correlation"
			action = "markNoise"
		}

		"/correlation/patch/$id/markSaved" {
			controller = "correlation"
			action = "markSaved"
		}

		"/$controller/$action?/$id?(.$format)?"{
			constraints {
				// apply constraints here
			}
		}

		"/authentication/$provider/$status" {
			controller = "authentication"
			action = {
				return "" + params.provider.toString() + "Auth"
			}
			constraints {
				status validator: {  //Used to distinguish URL's like '/authenticate/twenty3andme/success'
					!it.isNumber()
				}
			}
		}

		"/mobile/cache.manifest" (controller: "mobile", action: "cachemanifest")

		"500" (controller: "authentication", action: "authenticateProvider", exception: AuthenticationRequiredException)

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
