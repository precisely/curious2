package us.wearecurio.controller

class DiscussionPostController extends LoginController{

	static allowedMethods = [create: "POST", update: "POST", delete: "DELETE"]
    def index() { }
}
