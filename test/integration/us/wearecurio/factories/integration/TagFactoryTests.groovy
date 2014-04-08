package us.wearecurio.factories.integration

import static org.junit.Assert.*

import org.junit.*

import us.wearecurio.factories.TagFactory
import us.wearecurio.model.Tag


class TagFactoryTests {
  @Test
  void testMake() {
	  def tag = TagFactory.make() 
	  assert tag.description == 'tag1'
    assert Tag.count() == 1
  }
  
  @Test
  void testMakeWithNull() {
	  def tag = TagFactory.make(null) 
	  assert tag.description == 'tag1'
    assert Tag.count() == 1
  }

  @Test
  void testMakeN() {
	  def tags = TagFactory.makeN(3)
	  assert tags.size == 3
	  assert tags.first().description == 'tag1'
	  assert tags[1].description == 'tag2'
	  assert tags.last().description == 'tag3'
  }
  
  @Test
  void testIdempotencyOfMake() {
	def tag1 = TagFactory.make()
	def tag2 = TagFactory.make()
	def tag3 = TagFactory.make()
	assert tag1.description == 'tag1'
	assert tag1.description == 'tag1'
	assert tag1.description == 'tag1'
	assert Tag.countByDescription('tag1') == 1
	assert tag1 == tag2
	assert tag2 == tag3
  }
  
  @Test
  void testIdempotencyOfMakeN() {
	  def tag1 = TagFactory.make()
	  assert Tag.countByDescription('tag1') == 1
	  def tags = TagFactory.makeN(3)
	  assert tags.size == 3
	  assert tags.first() == tag1
	  assert tags[1].description == 'tag2'
	  assert tags.last().description == 'tag3'
  }
}
