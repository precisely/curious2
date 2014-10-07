import us.wearecurio.thirdparty.AuthenticationRequiredException

class UrlMappings {

	static mappings = {

		"/home/graph/signals/$description1/$description2" {
			controller = "home"
			action = "graph_signals"
		}

		"/correlation/index/$flavor" {
			controller = "correlation"
			action = "index"
		}

		"/correlation/$id/$action" {
			controller = "correlation"
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
