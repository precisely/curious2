package us.wearecurio.parse

import static org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Test

import grails.test.mixin.*

class EntryCommaSegmenterTests {
	static transactional = true
	
	@Before
	void setUp() {
	}
	
	@After
	void tearDown() {
	}

	@Test
	void testSegmenter() {
		String str = "carrots, peas, banana cream pie"
		
		EntryCommaSegmenter segmenter = new EntryCommaSegmenter(str);
		
		assert segmenter.next() == "carrots"
		assert segmenter.next() == "peas"
		assert segmenter.next() == "banana cream pie"
		assert segmenter.next() == null
		assert !segmenter.hasNext()
		
		str = "  carrots,  peas, banana cream pie   "
		
		segmenter = new EntryCommaSegmenter(str);
		
		assert segmenter.next() == "carrots"
		assert segmenter.next() == "peas"
		assert segmenter.next() == "banana cream pie"
		assert segmenter.next() == null
		assert !segmenter.hasNext()

		str = "  run 5 miles , eat chicken, vegan sandwich "
		
		segmenter = new EntryCommaSegmenter(str);
		
		assert segmenter.next() == "run 5 miles"
		assert segmenter.next() == "eat chicken"
		assert segmenter.next() == "vegan sandwich"
		assert segmenter.next() == null
		assert !segmenter.hasNext()
		
		str = "on fire (in the work sense) , headed to work (but, this is a comment"
		
		segmenter = new EntryCommaSegmenter(str);
		
		assert segmenter.next() == "on fire (in the work sense)"
		assert segmenter.next() == "headed to work (but, this is a comment"
		assert segmenter.next() == null
		assert !segmenter.hasNext()
		
		str = "ignore (commas, inside comments), feet smelly"
		
		segmenter = new EntryCommaSegmenter(str);
		
		assert segmenter.next() == "ignore (commas, inside comments)"
		assert segmenter.next() == "feet smelly"
		assert segmenter.next() == null
		assert !segmenter.hasNext()

		str = "nested comment (hello (this is not, what I meant), yes?),dallying"
		
		segmenter = new EntryCommaSegmenter(str);
		
		assert segmenter.next() == "nested comment (hello (this is not, what I meant), yes?)"
		assert segmenter.next() == "dallying"
		assert segmenter.next() == null
		assert !segmenter.hasNext()

		str = "unclosed comment (yes this is (unclosed (tell me about it"
		
		segmenter = new EntryCommaSegmenter(str);
		
		assert segmenter.next() == "unclosed comment (yes this is (unclosed (tell me about it"
		assert segmenter.next() == null
		assert !segmenter.hasNext()
	}
	


/**/
	
}
