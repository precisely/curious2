import grails.test.*

import us.wearecurio.integration.CuriousTestCase;
import us.wearecurio.model.*
import us.wearecurio.services.EntryParserService
import us.wearecurio.support.EntryStats
import us.wearecurio.utility.*

import java.text.DateFormat
import java.util.Locale;

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

class IdentifierTests extends CuriousTestCase {
	static transactional = true
	
	EntryParserService entryParserService

	@Before
	void setUp() {
		super.setUp()
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testCreate() {
		Identifier ident = Identifier.look("foo")
		ident.save()
		
		assert ident.getId() != null
		
		Identifier ident2 = Identifier.get(ident.getId())
		
		assert ident.getId() == ident2.getId()
	}
	
	@Test
	void testSaveEntry() {
		def dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US)
		def currentTime = dateFormat.parse("July 1, 2010 3:30 pm")
		def timeZone = "America/Los_Angeles"
		def baseDate = dateFormat.parse("July 1, 2010 12:00 am")
		def parsed = entryParserService.parse(currentTime, timeZone, "bread 5 repeat", baseDate, true)
		def entry = Entry.create(userId, parsed, new EntryStats())
		
		entry.save(flush:true)
		
		assert entry.getId() != null
		
		def entry2 = Entry.get(entry.getId())
		
		assert entry.getId() == entry2.getId()
	}
}
