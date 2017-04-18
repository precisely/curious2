package us.wearecurio.controller

import org.springframework.web.multipart.MultipartFile
import us.wearecurio.model.Entry
import us.wearecurio.model.Sprint
import us.wearecurio.model.SurveyAnswer
import us.wearecurio.model.SurveyQuestion
import us.wearecurio.model.TagInputType
import us.wearecurio.model.User
import us.wearecurio.services.TagInputTypeService
import us.wearecurio.utility.Utils

import java.text.SimpleDateFormat

class AdminController extends LoginController {

	def databaseService
	TagInputTypeService tagInputTypeService

	def dashboard() {
		
	}

	def survey(Long id) {
		Map model = null
		if (id) {
			model = [surveyQuestion: SurveyQuestion.get(id)]
		}
		model
	}

	def createOrUpdateQuestion() {
		log.debug "create or update question: $params"
		println "$params"
		if (params.id) {
			SurveyQuestion.withTransaction {
				SurveyQuestion surveyQuestion = SurveyQuestion.get(params.id)
	
				if (!surveyQuestion) {
					redirect(uri: "admin/survey/$params.id")
					flash.message = g.message(code: "default.not.found.message",
							args: ["Survey question", params.id])
				}
	
				surveyQuestion.update(params)
				surveyQuestion.validate()
		
				if (!surveyQuestion.hasErrors()) {
					Utils.save(surveyQuestion, true)
					redirect(uri: "admin/listSurveyQuestions")
				} else {
					redirect(uri: "admin/survey?id=" + params.id)
					flash.message = g.message(code: "default.not.updated.message", args: ["Survey question"])
				}
			}
		} else {
			SurveyQuestion.withTransaction {
				SurveyQuestion surveyQuestion = SurveyQuestion.create(params)
				
				if (surveyQuestion) {
					Map model = [surveyQuestion: surveyQuestion]
					redirect(uri: "admin/addPossibleAnswers?id=$surveyQuestion.id")
				} else {
					redirect(uri: "admin/survey")
					flash.message = g.message(code: "not.created.message", args: ['Survey question'])
				}
			}
		}
	}

	def addPossibleAnswers(SurveyQuestion surveyQuestion) {
		if (!surveyQuestion) {
			redirect(uri: "admin/survey")
			flash.message = g.message(code: "default.not.found.message", args: ['Survey question', params.id])
			return
		} else {
			Map model = [surveyQuestion: surveyQuestion]
			render(view: "/admin/createPossibleAnswers", model: model)
		}
	}

	def createAnswersData() {
		SurveyQuestion surveyQuestion = SurveyQuestion.get(params.questionId)
		if (!surveyQuestion) {
			renderJSONPost([success: false, message: g.message(code: "default.not.found.message", 
					args: ["Survey question", params.questionId])])
					return
		}

		SurveyAnswer.withTransaction { status ->
			surveyQuestion.update(['possibleAnswers':params['possibleAnswers']])
			surveyQuestion.possibleAnswers.removeAll([null])
			surveyQuestion.possibleAnswers.sort()
			surveyQuestion.validate()
			
			List answersValidation = surveyQuestion.possibleAnswers*.validate()
			
			if (surveyQuestion.hasErrors() || answersValidation.contains(false)) {
				status.setRollbackOnly()
				renderJSONPost([success: false, message: g.message(code: "not.created.message", 
					args: ["Survey answers"])])
				return
			} 
			Utils.save(surveyQuestion, true)
			renderJSONPost([success: true])
		}
	}

	def listSurveyQuestions() {
		List questions = SurveyQuestion.findAll()
		Map model = [questions: questions]
		model
	}

	/**
	 * Returns a field value escaped for special characters
	 * @param input A String to be evaluated
	 * @return A properly formatted String
	 */
	static String csvEscape(String input) {
		if (input.contains(",") || input.contains("\n") || input.contains('"') || (!input.trim().equals(input))) {
			return '"' + input.replaceAll('"', '""') + '"'
		} else {
			return '"' + input + '"'
		}
	}

	static writeCSV(Writer writer, str) {
		writer.write(csvEscape(str))
	}

	static writeNumber(Writer writer, str) {
		writer.write(str)
	}

	protected def doExportStudyUser(OutputStreamWriter writer, User user, Date begin, Date end) {
		debug "AdminController.doExportStudyUser()" + user.getId()

		def c = Entry.createCriteria()
		def results = c {
			and {
				eq("userId", user.getId())
				not {
					isNull("date")
				}
				le("date", end)
				ge("date", begin)
			}
			order("date","asc")
		}

		TimeZone timeZone = Utils.createTimeZone(0, "GMT", false)
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
		dateFormat.setTimeZone(timeZone)

		for (Entry entry in results) {
			writeCSV(writer, user.username)
			writer.write(",")
			writeCSV(writer, dateFormat.format(entry.getDate()))
			writer.write(",")
			writeCSV(writer, entry.getTag().getDescription())
			writer.write(",")
			writeNumber(writer, entry.getAmount().toString())
			writer.write(",")
			writeCSV(writer, entry.getUnits())
			writer.write(",")
			writeCSV(writer, entry.getComment())
			writer.write(",")
			writeNumber(writer, entry.getRepeatType() ? entry.getRepeatType().getId().toString() : "-1")
			writer.write(",")
			writeNumber(writer, entry.getAmountPrecision().toString())
			writer.write(",")
			writeNumber(writer, entry.getDatePrecisionSecs().toString())
			writer.write(",")
			writeCSV(writer, entry.fetchTimeZoneName())
			writer.write(",")
			if (entry.getBaseTag())
				writeCSV(writer, entry.getBaseTag().getDescription())
			else
				writeCSV(writer, entry.getTag().getDescription())

			writer.write("\n")
		}

		writer.flush()
	}

	static Date parseDate(String dateStr) {
		if (dateStr == null) return null
		return Date.parse("yyyy-MM-dd", dateStr)
	}

	protected def doExportStudy(OutputStream out) {
		debug "AdminController.doExportStudy()"

		Sprint sprint = Sprint.get(471)

		Long sprintGroupId = sprint.virtualGroupId

		Writer writer = new OutputStreamWriter(out)

		writeCSV(writer,"Username")
		writer.write(",")
		writeCSV(writer,"Date (GMT)")
		writer.write(",")
		writeCSV(writer,"Tag")
		writer.write(",")
		writeCSV(writer,"Amount")
		writer.write(",")
		writeCSV(writer,"Units")
		writer.write(",")
		writeCSV(writer,"Comment")
		writer.write(",")
		writeCSV(writer,"RepeatType")
		writer.write(",")
		writeCSV(writer,"Amount Precision")
		writer.write(",")
		writeCSV(writer,"Date Precision")
		writer.write(",")
		writeCSV(writer,"Time Zone")
		writer.write(",")
		writeCSV(writer,"Base Tag")
		writer.write("\n")

		def users = sprint.fetchUserGroup().getReaderUsers()

		for (User user in users) {
			doExportStudyUser(writer, user, parseDate("2016-09-28"), parseDate("2016-10-08"))
		}

		writer.flush()
	}

	def exportStudy() {
		debug "AdminController.exportStudy()"

		response.setHeader "Content-disposition", "attachment; filename=export.csv"
		response.contentType = 'text/csv'

		doExportStudy(response.outputStream)

		response.outputStream.flush()
	}

	def showSurveyQuestion(SurveyQuestion surveyQuestion) {
		if (!surveyQuestion) {
			redirect(uri: "admin/listSurveyQuestions")
			flash.message = g.message(code: "default.not.found.message", args: ["Survey question", params.id])
			return
		}
		[surveyQuestion: surveyQuestion]
	}

	def deletePossibleAnswerData(Long answerId, Long questionId) {

		log.debug "Delete survey possible answer with id: $answerId"
		if (!answerId) {
			renderJSONPost([success: false, message: g.message(code: "default.null.message", 
					args: ["id", "Answer"])])
			return
		}

		SurveyQuestion.withTransaction {
			SurveyQuestion surveyQuestion = SurveyQuestion.get(questionId)
			if (!surveyQuestion) {
				renderJSONPost([success: false, message: g.message(code: "default.not.found.message", 
						args: ["Survey question", questionId])])
				return
			}

			SurveyAnswer surveyAnswer = surveyQuestion.possibleAnswers.find{ answerInstance-> answerInstance.id == answerId}
	
			if (!surveyAnswer) {
				renderJSONPost([success: false, message: g.message(code: "default.not.found.message",
						args: ["Survey answer", answerId])])
				return
			}
			surveyQuestion.removeFromPossibleAnswers(surveyAnswer)
			Utils.save(surveyQuestion, true)
		}
		renderJSONPost([success: true])
	}

	def updateSurveyAnswerData() {
		log.debug "Admin.updateSurveyAnswerData: params: $params"
		
		SurveyAnswer.withTransaction { status ->
			SurveyAnswer surveyAnswer = SurveyAnswer.get(params.answerId)
			if (!surveyAnswer) {
				renderJSONPost([success: false, message: g.message(code: "default.not.found.message", 
						args: ['Survey answer', params.answerId])])
				return
			}
	
			surveyAnswer.update(params)
			surveyAnswer.validate()
	
			if (surveyAnswer.hasErrors()) {
				status.setRollbackOnly()
				renderJSONPost([success: false, message: g.message(code: "default.not.updated.message", 
						args: ['Survey answer'])])
			} else {
				Utils.save(surveyAnswer, true)
				renderJSONPost([success: true])
			}
		}
	}

	def deleteSurveyQuestionData(SurveyQuestion surveyQuestion) {

		SurveyQuestion.withTransaction {
			if (!surveyQuestion) {
				renderJSONPost([success: false, message: g.message(code: "default.not.found.message", 
						args: ['Survey question', params.id])])
				return
			}
	
			surveyQuestion.delete(flush: true)
		}
		renderJSONPost([success: true])
	}

	def uploadTagInputTypeCSV() {
		render(view: 'uploadTagInputTypeCSV')
	}

	/**
	 * An endpoint to import TagInputType from uploaded CSV file.
	 *
	 * @return Message with report of the upload and errors in uploaded file.
	 */
	def importTagInputTypeFromCSV() {
		MultipartFile tagInputTypeCSV = request.getFile('tagInputTypeCSV')

		log.debug "TagInputType count before CSV upload - ${TagInputType.count()}"

		if (!tagInputTypeCSV || tagInputTypeCSV.empty) {
			render(view: 'csvUploadResult', model: [message: 'Please attach a CSV File'])

			return
		}

		Long fileSize = tagInputTypeCSV.size
		String fileExtension = tagInputTypeCSV.originalFilename.split('\\.').last()?.toLowerCase()

		if (fileExtension != 'csv' || fileSize > (1024 * 1024 * 2)) {
			render(view: 'csvUploadResult', model: [message: 'Invalid file'])

			return
		}

		File tempTagInputTypeCSV
		String uploadPath = '/tmp/tag-input-type/'

		try {
			File uploadDir = new File(uploadPath)

			if (!uploadDir.exists()) {
				if (!uploadDir.mkdirs()) {
					render(view: 'csvUploadResult', model: [message: 'Failed to create directory for temporary ' +
							'file upload.'])

					return
				}
			}

			tempTagInputTypeCSV = new File(uploadPath, tagInputTypeCSV.originalFilename)
			tagInputTypeCSV.transferTo(tempTagInputTypeCSV)
		} catch (IllegalStateException | IOException e) {
			Utils.reportError("Error while saving CSV file with TagInputType data", e)
			render(view: 'csvUploadResult', model: [message: 'Could not upload CSV file, please contact support.'])

			return
		}

		Map result

		try {
			result = tagInputTypeService.importFromCSV(tempTagInputTypeCSV)
		} catch (IllegalArgumentException e) {
			render(view: 'csvUploadResult', model: [message: e.message])

			return
		}

		log.debug "TagInputType count after CSV upload - ${TagInputType.count()}"

		if (!result.success) {
			render(view: 'csvUploadResult', model: [message: 'The CSV file you uploaded contains some invalid rows' +
					'. A CSV file containing the invalid rows has been emailed to you. Please fix the file and ' +
					"re-upload. Syntax error in lines ${result.invalidRows}"])

			return
		}

		render(view: 'csvUploadResult', model: [message: 'Successfully imported all TagInputType from CSV'])
	}
}