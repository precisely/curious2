package us.wearecurio.services.integration

import com.causecode.fileuploader.CDNProvider
import com.causecode.fileuploader.UFile
import com.causecode.fileuploader.UFileType
import grails.test.mixin.TestFor
import us.wearecurio.model.*
import us.wearecurio.services.DataService
import us.wearecurio.services.IntelBasisDataService
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats

import java.text.SimpleDateFormat
import java.util.zip.ZipFile

@TestFor(IntelBasisDataService)
class IntelBasisDataServiceTests extends CuriousServiceTestCase {

	UFile file
	ZipFile dumpFile
	ThirdPartyDataDump thirdPartyDataDump
	def setup() {
		dumpFile = new ZipFile(new File("./test/integration/test-files/basis/intel-dump1.zip"))

		file = new UFile([name : "dump-blank", extension: "zip", fileGroup: "dumpFile", type: UFileType.LOCAL,
						  provider: CDNProvider.GOOGLE, path: "dummp/path", size: 10001003, expiresOn: (new Date() + 20)])
		file.save(flush: true)

		thirdPartyDataDump = new ThirdPartyDataDump([userId: userId, dumpFile: file, type:
				ThirdParty.BASIS])
		thirdPartyDataDump.save(flush: true)
	}

	def cleanup() {
	}

	void "test processDump when zip file has all the three files records"() {
		given: "dump file and the ThirdPartyDataDump instance"
		assert !Entry.count()
		assert thirdPartyDataDump.status == Status.UNPROCESSED

		when: "processDump method is called"
		service.processDump(dumpFile, thirdPartyDataDump)

		then: "dump file instance is marked processed and entries are created"
		Entry.count() == 10
		Entry.countByBaseTag(Tag.findByDescription("sleep")) == 6
		Entry.countByBaseTag(Tag.findByDescription("activity")) == 2
		Entry.countByBaseTag(Tag.findByDescription("heart rate")) == 2
		thirdPartyDataDump.status == Status.PROCESSED
	}

	void "test processDump when zip file has all the three file records but some exceptoin occures while creating entries"() {
		given: "dump file and the ThirdPartyDataDump instance and mocked method processDataActivity"
		service.metaClass.processDataActivity = { Reader reader, Integer timeZoneId, SimpleDateFormat formatter,
				EntryCreateMap creationMap, EntryStats stats, Long userId, DataService.DataRequestContext context ->
			throw new NumberFormatException()
		}
		assert !Entry.count()

		when: "processDump method is called"
		service.processDump(dumpFile, thirdPartyDataDump)

		then: "dump file instance is marked unprocessed and no entries are created"
		!Entry.count()
		thirdPartyDataDump.status == Status.PARTIALLYPROCESSED
		thirdPartyDataDump.attemptCount == 1
		thirdPartyDataDump.unprocessedFiles.size() == 3
		thirdPartyDataDump.unprocessedFiles.indexOf("sleep.csv") > -1
		thirdPartyDataDump.unprocessedFiles.indexOf("sleep-stages.csv") > -1
		thirdPartyDataDump.unprocessedFiles.indexOf("bodystates/bodystates-2014-08.csv") > -1
	}

	void "test processDump when zip file processing fails more then 3 times"() {
		given: "dump file and the ThirdPartyDataDump instance with attemp count 2 and mocked method processDataActivity"
		service.metaClass.processDataActivity = { Reader reader, Integer timeZoneId, SimpleDateFormat formatter,
				  EntryCreateMap creationMap, EntryStats stats, Long userId, DataService.DataRequestContext context ->
			throw new NumberFormatException()
		}
		thirdPartyDataDump.attemptCount = 2
		thirdPartyDataDump.save(flush: true)
		assert !Entry.count()

		when: "processDump method is called"
		service.processDump(dumpFile, thirdPartyDataDump)

		then: "dump file instance is marked unprocessed and no entries are created"
		!Entry.count()
		thirdPartyDataDump.status == Status.FAILED
		thirdPartyDataDump.attemptCount == 3
	}
}
