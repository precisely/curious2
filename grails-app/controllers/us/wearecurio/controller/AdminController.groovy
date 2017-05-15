package us.wearecurio.controller

import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import us.wearecurio.model.Entry
import us.wearecurio.model.Sprint
import us.wearecurio.model.survey.PossibleAnswer
import us.wearecurio.model.survey.Question
import us.wearecurio.model.User
import us.wearecurio.utility.Utils
import grails.converters.JSON

import java.text.SimpleDateFormat

class AdminController extends LoginController {

	def databaseService

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
}