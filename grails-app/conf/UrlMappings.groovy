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

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
