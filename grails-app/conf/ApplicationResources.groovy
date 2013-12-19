modules = {
	bootstrap {
		resource url: [dir: "css/bootstrap", file: "bootstrap.min.css"]
		resource url: [dir: "js/bootstrap", file: "bootstrap.min.js"]
	}
	application {
		resource url: [dir: "js", file: "application.js"]
	}
	basic {
		resource url: [dir: "js", file: "basic.js"]
	}
	selectable {
		resource url: [dir: "js/jquery", file: "jquery.selectable.custom.js"]
	}
	mobileTrackPage {
		resource url: [dir: "css/mobile", file: "trackPage.css"]
	}
}