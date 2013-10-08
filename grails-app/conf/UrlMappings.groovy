class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}
		"/mobile/cache.manifest" (controller: "mobile", action: "cachemanifest")

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
