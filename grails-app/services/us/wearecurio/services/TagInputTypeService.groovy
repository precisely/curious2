package us.wearecurio.services

import org.grails.plugins.csv.CSVWriter
import us.wearecurio.model.InputType
import us.wearecurio.model.Tag
import us.wearecurio.model.TagInputType
import us.wearecurio.model.User
import us.wearecurio.model.ValueType
import us.wearecurio.utility.Utils
/**
 * A service class to perform operations on properties of TagInputType domain.
 */
class TagInputTypeService {

	SecurityService securityService

	/**
	 * This method parses the given CSV file and imports TagInputType.
	 *
	 * @throws IllegalArgumentException: If the titles of the CSV file do not match the expected titles.
	 * @param csvFile
	 * @return Map with success value and list of invalid rows.
	 */
	Map importFromCSV(File csvFile) throws IllegalArgumentException {
		int rowNumber = 0
		List invalidRows = []
		boolean success = true

		if (!csvFile || !csvFile.exists()) {
			return [success: false, message: 'CSV file does not exist']
		}

		List<String> expectedTitles = ['tag description', 'default unit', 'max', 'min', 'number of levels',
				'input type', 'value type', 'override']

		StringWriter stringWriter = new StringWriter()
		CSVWriter csvWriter = new CSVWriter(stringWriter, {
			col1:'tag description' { it.val1 }
			col2:'default unit' { it.val2 }
			col3:'max' { it.val3 }
			col4:'min' { it.val4 }
			col5:'number of levels' { it.val5 }
			col6:'input type' { it.val6 }
			col7:'value type' { it.val7 }
			col8:'override' { it.val8 }
		})

		String tagDescription, defaultUnit
		int max, min, noOfLevels
		InputType inputType
		ValueType valueType
		boolean override

		csvFile.toCsvReader().eachLine { rowData ->
			List tagInputTypeData = rowData as List
			rowNumber++
			if (rowNumber == 1) {
				List actualTitles = tagInputTypeData*.trim()*.toLowerCase() // Removing space around titles.
				actualTitles.removeAll(expectedTitles)

				if (actualTitles) {
					log.debug 'Titles in the csv file do not match the expected value'

					throw new IllegalArgumentException('Titles in the csv file do not match the expected value ' +
							"Invalid titles are ${actualTitles}")
				}

				return
			}

			if (tagInputTypeData.size() != 8) {
				invalidRows.add([row: rowNumber, error: 'The row must have 8 columns'])

				csvWriter << [val1: tagInputTypeData[0], val2: tagInputTypeData[1], val3: tagInputTypeData[2], val4:
						tagInputTypeData[3], val5: tagInputTypeData[4], val6: tagInputTypeData[5],
						val7: tagInputTypeData[6], val8: tagInputTypeData[7]]

				return false
			}

			try {
				tagDescription = tagInputTypeData[0].toString().trim().toLowerCase()
				defaultUnit = tagInputTypeData[1].toString().trim()
				max = tagInputTypeData[2].toString().trim().toInteger()
				min = tagInputTypeData[3].toString().trim().toInteger()
				noOfLevels = tagInputTypeData[4].toString().trim().toInteger()
				inputType = tagInputTypeData[5].toString().trim().toUpperCase()
				valueType = tagInputTypeData[6].toString().trim().toUpperCase() ?: ValueType.DISCRETE
				override = tagInputTypeData[7].toString().toBoolean()


				if (!tagDescription || !(max >= 0) || !(min >= 0) || !(noOfLevels >= 0) || !inputType ||
						!(valueType in [ValueType.DISCRETE, ValueType.CONTINUOUS])) {
					invalidRows.add([row: rowNumber, error: 'This row contains invalid data'])
					csvWriter << [val1: tagDescription, val2: defaultUnit, val3: max, val4: min, val5: noOfLevels,
							val6: inputType, val7: valueType, val8: override]

					return false
				}

				Tag tag = Tag.look(tagDescription) // Creates a new Tag if Tag with given description does not exist.
				Long tagId = tag.id

				TagInputType tagInputType = TagInputType.findByTagId(tagId)

				if (tagInputType && override) {
					log.debug "Overriding TagInputType for tag ${tagDescription}"

					tagInputType.tagId = tagId
					tagInputType.defaultUnit = defaultUnit
					tagInputType.max = max
					tagInputType.min = min
					tagInputType.noOfLevels = noOfLevels
					tagInputType.inputType = inputType
					tagInputType.valueType = valueType

					if (!Utils.save(tagInputType, true)) {
						invalidRows.add([row: rowNumber, error: 'This row contains invalid data'])
						csvWriter << [val1: tagDescription, val2: defaultUnit, val3: max, val4: min, val5: noOfLevels,
								val6: inputType, val7: valueType, val8: override]
					}

					return
				}

				if (!tagInputType) {
					log.debug "Creating new TagInputType for tag ${tagDescription}"

					tagInputType = new TagInputType(tagId: tagId, defaultUnit: defaultUnit, max: max,
							min: min, noOfLevels: noOfLevels, inputType: inputType, valueType: valueType)

					if (!Utils.save(tagInputType, true)) {
						invalidRows.add([row: rowNumber, error: 'This row contains invalid data'])
						csvWriter << [val1: tagDescription, val2: defaultUnit, val3: max, val4: min, val5: noOfLevels,
								val6: inputType, val7: valueType, val8: override]
					}
				}
			} catch (IllegalArgumentException e) {
				log.error "Invalid argument in CSV file on line number ${rowNumber}, stacktrace - ", e

				invalidRows.add([row: rowNumber, error: 'This row contains invalid data'])
				csvWriter << [val1: tagDescription, val2: defaultUnit, val3: max, val4: min, val5: noOfLevels,
					val6: inputType, val7: valueType, val8: override]
			}

		}

		// updating cache
		TagInputType.cache(TagInputType.fetchTagInputTypeInfo())

		if (invalidRows) {
			success = false
			User userInstance = securityService.getCurrentUser()

			log.debug "Sending invalid CSV file to user ${userInstance.name}"

			sendMail {
				multipart true
				to userInstance.email
				from 'server@wearecurio.us'
				subject '[Curious] - CSV upload error.'
				body 'Please fix the attached CSV and re-upload.'
				attach 'InvalidTagInputType.csv', 'application/vnd.ms-excel', csvWriter.writer.toString().getBytes("UTF-8")
			}
		}

		return [success: success, invalidRows: invalidRows]
	}
}
