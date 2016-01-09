import us.wearecurio.thirdparty.AuthenticationRequiredException

class UrlMappings {

	static mappings = {

		"/search"(controller: "search", action: "index")
		"/home/graph/curiosities/$description1/$description2" {
			controller = "home"
			action = "graphCuriosities"
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

		"/third-party/sign-up/$provider" {
			controller = "authentication"
			action = "thirdPartySignUp"
		}

		"/third-party/sign-in/$provider" {
			controller = "authentication"
			action = "thirdPartySignIn"
		}

		/**
		* This mapping is used to replicate grails rest default mapping. Request data can
		* be passed as request body or as request URL depending on type of operation.
		* Like, create & update operation must use request body to send parameters with
		* either POST or PUT reqeusts.
		* 
		* @see Nested Resources in http://grails.org/doc/latest/guide/single.html#restfulMappings
		* 
		* @example
		*              GET     "/api/discussion" will call index action of DiscussionController
		*              POST    "/api/discussion" will call save action of DiscussionController
		*              PUT     "/api/discussion/2" will call update action of DiscussionController with id 2
		*              DELETE  "/api/discussion/2" will call delete action of DiscussionController with id 2
		*              GET     "/api/discussion/2" will call show action of DiscussionController with id 2
		*              GET     "/api/discussion/action/autocomplete" will call autocomplete action of DiscussionController with null id
		*/

		"/api/$controller/$resourceId?/$customAction?" {
			action = {
				Map actionMethodMap = [GET: params.resourceId ? "show" : "index", POST: "save", PUT: "update", DELETE: "delete"]
			
				return params.customAction ?: actionMethodMap[request.method.toUpperCase()]
			}
			id = {
				if (params.resourceId == "action") {
					return params.id
				}
				return params.resourceId
			}
		}

		"/mobile/cache.manifest" (controller: "mobile", action: "cachemanifest")

		"500" (controller: "authentication", action: "authenticateProvider", exception: AuthenticationRequiredException)

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
