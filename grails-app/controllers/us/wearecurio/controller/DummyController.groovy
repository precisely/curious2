package us.wearecurio.controller

class DummyController extends DataController{

	def index() { }

	def saveSurveyData(String foodRelation, String data) {
		log.debug "Dummy.saveSurveyData()"
		log.debug "$foodRelation and data: $data"
		if (data) {
			renderJSONPost([success: true])
			return
		} else {
			renderJSONPost([success: false])
			return
		}
	}
}
