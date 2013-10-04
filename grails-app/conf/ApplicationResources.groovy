modules = {
	application {
		resource url: [dir: "js", file: "application.js"]
	}
	basic {
		resource url: [dir: "js", file: "basic.js"]
	}
	selectable {
		resource url: [dir: "js/jquery", file: "jquery.selectable.custom.js"]
	}
	mobileBase {
		resource url: [dir: "js/mobile", file: "mobileBase.js"]
	}
	mobileTrackPage {
		resource url: [dir: "css/mobile", file: "trackPage.css"]
		dependsOn "mobileBase"
	}
}