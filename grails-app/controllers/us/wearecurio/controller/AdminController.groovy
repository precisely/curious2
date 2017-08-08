package us.wearecurio.controller

import org.grails.plugins.elasticsearch.ElasticSearchService
import us.wearecurio.model.UpdateSubscription
import java.text.SimpleDateFormat
import org.springframework.web.multipart.MultipartFile
import us.wearecurio.model.Entry
import us.wearecurio.model.Sprint
import us.wearecurio.model.TagInputType
import us.wearecurio.model.User
import us.wearecurio.services.TagInputTypeService
import us.wearecurio.utility.Utils
import org.springframework.dao.DataIntegrityViolationException


class AdminController extends LoginController {

	def databaseService
	TagInputTypeService tagInputTypeService
	ElasticSearchService elasticSearchService

	def dashboard() {
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

	def uploadTagInputTypeCSV() {
		render(view: 'uploadTagInputTypeCSV')
	}

	def esReindex() {
		log.debug("Elastic Search Reindexing..")

		Thread.start {
			/**
			 * Unindexes all searchable instances of the specified class.
			 * If call without arguments, unindex ALL searchable instances.
			 */
			elasticSearchService.unindex()

			/**
			 * Indexes all searchable instances of the specified class.
			 * If call without arguments, index ALL searchable instances.
			 */
			elasticSearchService.index()
		}

		render "Reindexing in progress.."
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

	/**
	 * An endpoint to delete user subscription .
	 * @param get id as param
	 * @return success is true or false.
	 */
	def deleteSubscription() {
		try {
			UpdateSubscription subscriptionDetails = UpdateSubscription.findById(params.id)
			subscriptionDetails?.delete(flush: true)
			renderJSONGet([success: true])
			return
		} catch (DataIntegrityViolationException e) {
			e.message = "exception caught"
			renderJSONGet([success: false])
			return
		}
	}

	def download() {
		response.setHeader "Content-disposition", "attachment; filename=export.csv"
		response.contentType = 'text/csv'
		doExportSubscriptionCSV(response.outputStream)
		response.outputStream.flush()
	}

	protected def doExportSubscriptionCSV(OutputStream out) {
		Writer writer = new OutputStreamWriter(out)
		writeCSV(writer,"email")
		writer.write(",")
		writeCSV(writer,"categories")
		writer.write(",")
		writeCSV(writer,"Description")
		writer.write("\n")

		List subscriptionDetails = UpdateSubscription.all
		subscriptionDetails.each { subscriptionDetail->
			writeCSV(writer, subscriptionDetail.email)
			writer.write(",")
			writeCSV(writer, subscriptionDetail.categories)
			writer.write(",")
			writeCSV(writer, subscriptionDetail.description)
			writer.write("\n")
		}
		writer.flush()
	}

	def subscriptions(Integer max) {
		params.max = Math.min(max ?: 10, 100)
		[subscriptionList: UpdateSubscription.list(params), detailInstanceTotal: UpdateSubscription.count()]
	}
}