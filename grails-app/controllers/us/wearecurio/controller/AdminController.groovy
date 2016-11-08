package us.wearecurio.controller

import org.grails.databinding.SimpleMapDataBindingSource
import us.wearecurio.model.Entry
import us.wearecurio.model.Sprint
import us.wearecurio.model.SurveyAnswer
import us.wearecurio.model.SurveyQuestion
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

import java.text.SimpleDateFormat

class AdminController extends LoginController {

	def databaseService

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

	def listDumpFileDetails() {
		List dumpFileInstances = ThirdPartyDataDump.findAll()
		Map model = [dumpFileInstances: dumpFileInstances]
		model
	}
}