package us.wearecurio.services

import us.wearecurio.model.InputType
import us.wearecurio.model.Tag
import us.wearecurio.model.TagInputType
import us.wearecurio.model.ValueType
import us.wearecurio.utility.Utils
/**
 * A service class to perform operations on properties of TagInputType domain.
 */
class TagInputTypeService {

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
		boolean success

		if (!csvFile || !csvFile.exists()) {
			return [success: success, message: 'CSV file does not exist']
		}

		csvFile.toCsvReader().eachLine { tagInputTypeData ->
			rowNumber++
			if (rowNumber == 1) {
				List expectedTitles = ['tag description', 'default unit', 'max', 'min', 'number of levels',
					    'input type', 'value type', 'override']
				List actualTitles = tagInputTypeData*.trim()*.toLowerCase() // Removing space around titles.
				actualTitles.removeAll(expectedTitles)

				if (actualTitles) {
					log.debug 'Titles in the csv file do not match the expected value'

					throw new IllegalArgumentException('Titles in the csv file do not match the expected value' +
							"Invalid titles are ${actualTitles}")
				}

				return
			}

			if (tagInputTypeData.size() != 8) {
				invalidRows.add([row: rowNumber, error: 'The row must have 8 columns'])

				return false
			}

			try {
				String tagDescription = tagInputTypeData[0].toString().trim().toLowerCase()
				Tag tag = Tag.look(tagDescription) // Creates a new Tag if Tag with given description does not exist.
				Long tagId = tag.id
				String defaultUnit = tagInputTypeData[1].toString().trim()
				int max = tagInputTypeData[2].toString().trim().toInteger()
				int min = tagInputTypeData[3].toString().trim().toInteger()
				int noOfLevels = tagInputTypeData[4].toString().trim().toInteger()
				InputType inputType = tagInputTypeData[5].toString().trim().toUpperCase()
				ValueType valueType = tagInputTypeData[6].toString().trim().toUpperCase() ?: ValueType.DISCRETE
				boolean override = tagInputTypeData[7].toString().toBoolean()

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

					Utils.save(tagInputType, true)
				}

				if (!tagInputType) {
					log.debug "Creating new TagInputType for tag ${tagDescription}"

					tagInputType = new TagInputType(tagId: tagId, defaultUnit: defaultUnit, max: max,
							min: min, noOfLevels: noOfLevels, inputType: inputType, valueType: valueType)

					Utils.save(tagInputType, true)
				}
			} catch (IllegalArgumentException e) {
				log.error "Invalid argument in CSV file on line number ${rowNumber}, stacktrace - ${e}"

				invalidRows.add([row: rowNumber, error: 'This row contains invalid data'])
			}

		}

		// updating cache
		TagInputType.cache(TagInputType.fetchTagInputTypeInfo())

		if (!invalidRows) {
			success = true
		}

		return [success: success, invalidRows: invalidRows]
	}
}
