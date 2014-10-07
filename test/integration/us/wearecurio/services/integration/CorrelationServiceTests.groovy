package us.wearecurio.services.integration

import static org.junit.Assert.*
import org.junit.*

import us.wearecurio.services.TagService
import us.wearecurio.services.CorrelationService
import us.wearecurio.factories.EntryFactory
import us.wearecurio.model.User
import us.wearecurio.model.Tag
import us.wearecurio.model.Entry
import us.wearecurio.model.CuriousSeries
import us.wearecurio.model.Correlation
import org.apache.commons.logging.LogFactory


class CorrelationServiceTests extends CuriousServiceTestCase {
	def correlationService
	def tagService
	
	@Before
	void setUp() {
		Entry.executeUpdate("delete Entry")
		Tag.executeUpdate("delete Tag")
		User.executeUpdate("delete User")
		super.setUp()
	}
	
	@After
	void tearDown() {
		super.tearDown()
		
		Entry.executeUpdate("delete Entry")
		Tag.executeUpdate("delete Tag")
		User.executeUpdate("delete User")
	}
	
	private static def LOG = new File("debug.out")
	public static def log(text) {
		LOG.withWriterAppend("UTF-8", { writer ->
			writer.write( "CuriousSeries: ${text}\n")
		})
	}
	
	@Test
	void testIterateOverTagPairs() {
		EntryFactory.makeN(3, { it }, ['tag_description': 'tag1'])
		EntryFactory.makeN(3, { it }, ['tag_description': 'tag2'])
		def pairs = []
		def f = { tag1, tag2 -> [tag1, tag2]}
		assert User.count()  == 1
		assert Tag.count()	 == 2
		assert Entry.count() == 6
		def tags = Entry.findAllWhere('userId': User.first().id.toLong()).collect { it.tag }.unique()
		assert tags.size() == 2
		pairs = correlationService.iterateOverTagPairs(user, f)
		def t0 = Tag.first()
		def t1 = Tag.last()
		assert pairs.size() == 4
		assert pairs[0] == [t0, t0]
		assert pairs[1] == [t0, t1]
		assert pairs[2] == [t1, t0]
		assert pairs[3] == [t1, t1]
	}
	
	
}
