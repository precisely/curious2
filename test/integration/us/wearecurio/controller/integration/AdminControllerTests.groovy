package us.wearecurio.controller.integration

import org.apache.commons.fileupload.disk.DiskFileItem
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.web.multipart.commons.CommonsMultipartFile
import us.wearecurio.controller.AdminController
import us.wearecurio.model.TagInputType
import us.wearecurio.model.User
import us.wearecurio.services.SecurityService
import us.wearecurio.services.TagInputTypeService
import us.wearecurio.utility.Utils

class AdminControllerTests extends CuriousControllerTestCase {

	AdminController controller
	TagInputTypeService tagInputTypeService

	@Before
	void setUp() {
		super.setUp()

		controller = new AdminController()
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	byte[] getDefaultFileContent() {
		return "tag description, default unit, max, min, number of levels, input type, value type, override" +
				" \n sleep, hours, 10, 0, 5, thumbs, , \n mood, , 10, 0, 5, smiley, ,\n" +
				"misbehavior, , 10, 0 , 5, boolean, , \n , , , \n bowell movement, , 10, 0, 5, slider, ," +
				" true\n energy, , 10, 0, 5, level, ," as byte[]
	}

	File getFile(String filePath) {
		File file = new File(filePath)
		file.createNewFile()

		return file
	}

	DiskFileItem getDiskFileItemInstance(File file, byte[] fileContent = getDefaultFileContent()) {
		DiskFileItem fileItem = new DiskFileItem('tagInputTypeCSV', 'application/vnd.ms-excel', false,
				file.name, (int) file.length() , file.parentFile)
		fileItem.outputStream.write(fileContent)

		return fileItem
	}

	@Test
	void "test importTagInputTypeFromCSV action to import TagInputType from an incorrect CSV file"() {
		given: 'A CSV file which contains TagInputType data'
		File file = getFile('./target/temp.csv')
		DiskFileItem fileItem = getDiskFileItemInstance(file)
		CommonsMultipartFile commonsMultipartFile = new CommonsMultipartFile(fileItem)

		and: 'Mocked getCurrentUser method'
		User user = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", name: "Mark Leo",
							  password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		Utils.save(user, true)

		tagInputTypeService.securityService = [getCurrentUser: { ->
			return user
		}] as SecurityService
		controller.tagInputTypeService = tagInputTypeService

		when: 'importTagInputTypeFromCSV action is hit and file has invalid data'
		controller.request.addFile(commonsMultipartFile)
		controller.importTagInputTypeFromCSV()

		then: 'New TagInputType are created for valid rows in csv file and invalid rows are sent in the response'
		assert controller.response.status == 200
		assert TagInputType.count() == 5
		assert controller.modelAndView.model.message == 'The CSV file you uploaded contains some invalid rows. ' +
				'A CSV file containing the invalid rows has been emailed to you. Please fix the file and re-upload. ' +
				'Syntax error in lines [[row:5, error:The row must have 8 columns]]'
		file.delete()
		fileItem.delete()
	}

	@Test
	void "test importTagInputTypeFromCSV action to import TagInputType from a correct CSV file"() {
		when: 'A valid CSV file is passed'
		byte[] content = "tag description, default unit, max, min, number of levels, input type, value type," +
				"override \n sleep, hours, 10, 0, 5, level, , \n activity, cal, 10, 0, 5, smiley, " +
				"continuous, \n readiness,score, 10, 0 , 5, thumbs, , \n sleep, mins, 10, 0, 10, slider, " +
				"continuous, true \n melatonin,mg,10,0,5,default,," as byte[]
		File file = getFile('./target/temp.csv')
		DiskFileItem fileItem = getDiskFileItemInstance(file, content)
		CommonsMultipartFile commonsMultipartFile = new CommonsMultipartFile(fileItem)
		controller.request.addFile(commonsMultipartFile)
		controller.importTagInputTypeFromCSV()

		then: 'New TagInputType are created for all the entries and a proper message is sent in response'
		assert controller.response.status == 200
		assert controller.modelAndView.model.message == 'Successfully imported all TagInputType from CSV'
		assert TagInputType.count() == 4 // sleep gets overridden
		file.delete()
		fileItem.delete()
	}
}
