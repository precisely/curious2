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
		super.setUp()
	}
	
	@After
	void tearDown() {
		super.tearDown()
		
		User.executeUpdate("delete User u")
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
	
	@Test
	void testCorrelationsSaved() {
		def entries1 = EntryFactory.makeN(3, { Math.sin(it) }, ['tag_description': 'tag1'])
		def entries2 = EntryFactory.makeN(3, { Math.cos(it) }, ['tag_description': 'tag2'])
		assert Tag.count() == 2
		assert Entry.count() == 6
		correlationService.iterateOverTagPairs(user, { tag1, tag2 ->
			def series1 = CuriousSeries.create(tag1, user.id)
			def series2 = CuriousSeries.create(tag2, user.id)
			correlationService.saveMipss(series1, series2)
			log("series1: ${tag1.description} X series2: ${tag2.description}")
		})
		assert Correlation.count() == 4
	}
	
	@Test
	void testUpdateUserCorrelations() {
		//	Assume there are two users with a different set of tags.
		def entries1 = EntryFactory.makeN(3, { Math.sin(it) },		 ['tag_description': 'tag1', 'username': 'a'])
		def entries2 = EntryFactory.makeN(3, { Math.cos(it) },		 ['tag_description': 'tag2', 'username': 'a'])
		def entries3 = EntryFactory.makeN(4, { Math.sin(it) },		 ['tag_description': 'tag3', 'username': 'b'])
		def entries4 = EntryFactory.makeN(4, { Math.cos(it) },		 ['tag_description': 'tag4', 'username': 'b'])
		def entries5 = EntryFactory.makeN(4, { Math.cos(1.2*it) }, ['tag_description': 'tag5', 'username': 'b'])
		assert Tag.count() == 5
		assert Entry.count() == 18
		def user_a = User.findWhere(username: 'a')
		correlationService.updateUserCorrelations(user_a)
		assert Correlation.count() == 4
		
		def user_b = User.findWhere(username: 'b')
		correlationService.updateUserCorrelations(user_b)
		assert Correlation.count() == 4 + 9
		
		def correlations = Correlation.findAll().collect { it.corValue }
		//assertEquals correlations[0], 1.0, 0.0001
		//assertEquals correlations[1], 0.73387, 0.0001
		//assertEquals correlations[2], 0.73387, 0.0001
		//assertEquals correlations[3], 1.0, 0.0001
	}
	
}
