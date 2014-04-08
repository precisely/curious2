package us.wearecurio.services.integration

import static org.junit.Assert.*
import org.junit.*

import us.wearecurio.services.TagService
import us.wearecurio.services.CorrelationService
import us.wearecurio.factories.EntryFactory
import us.wearecurio.model.User
import us.wearecurio.model.Tag
import us.wearecurio.model.Entry
import us.wearecurio.model.Series
import us.wearecurio.model.Correlation
import org.apache.commons.logging.LogFactory


class CorrelationServiceTests extends CuriousServiceTestCase {
    def correlationService
    def tagService

    static def log = LogFactory.getLog(this)

    def assertApproximatelyEqual(x, y) {
      Double epsilon = 0.00001
      def p = x + epsilon > y 
      def q = x - epsilon < y 
      assert p && q
    }

    @Test
    void testIterateOverTagPairs() {
      EntryFactory.makeN(3, { it }, ['tag_description': 'tag1'])
      EntryFactory.makeN(3, { it }, ['tag_description': 'tag2'])
      def pairs = []
      def f = { tag1, tag2 -> [tag1, tag2] }
      assert User.count()  == 1
      assert Tag.count()   == 2
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
        def series1 = Series.create(tag1, user.id)
        def series2 = Series.create(tag2, user.id)
        correlationService.saveCorrelation(series1, series2)
      })
      assert Correlation.count() == 4

      def correlations = Correlation.findAll().collect { it.corValue }
      assertApproximatelyEqual correlations[0], 1.0
      assertApproximatelyEqual correlations[1], 0.73387
      assertApproximatelyEqual correlations[2], 0.73387
      assertApproximatelyEqual correlations[3], 1.0
    }

    //@Test
    //void testUpdateUserCorrelations() {
    //  //  Assume there are two users with a different set of tags.
    //  def entries1 = EntryFactory.makeN(3, { Math.sin(it) },     ['tag_description': 'tag1', 'username': 'a'])
    //  def entries2 = EntryFactory.makeN(3, { Math.cos(it) },     ['tag_description': 'tag2', 'username': 'a'])
    //  def entries3 = EntryFactory.makeN(4, { Math.sin(it) },     ['tag_description': 'tag3', 'username': 'b'])
    //  def entries4 = EntryFactory.makeN(4, { Math.cos(it) },     ['tag_description': 'tag4', 'username': 'b'])
    //  def entries5 = EntryFactory.makeN(4, { Math.cos(1.2*it) }, ['tag_description': 'tag5', 'username': 'b'])
    //  assert Tag.count() == 5
    //  assert Entry.count() == 18
    //  def user_a = User.findWhere(username: 'a')
    //  correlationService.updateUserCorrelations(user_a)
    //  assert Correlation.count() == 4

    //  def user_b = User.findWhere(username: 'b')
    //  correlationService.updateUserCorrelations(user_b)
    //  assert Correlation.count() == 4 + 9

    //  def correlations = Correlation.findAll().collect { it.corValue }
    //  assertApproximatelyEqual correlations[0], 1.0
    //  assertApproximatelyEqual correlations[1], 0.73387
    //  assertApproximatelyEqual correlations[2], 0.73387
    //  assertApproximatelyEqual correlations[3], 1.0
    //}

//    @Test
//    void testIterateOverAllTaggables() {
//      EntryFactory.makeN(3, { it }, ['tag_description': 'tag1'])
//      EntryFactory.makeN(3, { 5*it }, ['tag_description': 'tag2'])
//      EntryFactory.makeN(3, { 7*it }, ['tag_description': 'tag3'])
//      TagGroupFactory.make('group1', ['tag1', 'tag2'])
//
//      def pairs = []
//      assert User.count()  == 1
//      assert Tag.count()   == 3
//      assert TagGroup.count() == 1
//      assert Entry.count() == 9
//      def tags = Tag.findAll()
//      pairs = correlationService.iterateOverTagablePairs(user, { tagable1, tagable2 -> [tagable1, tagable2] })
//      // If you have 3 tags and 1 tag group, that's 4 tagables.
//      assert pairs.size() == 4
//      assert pairs[0] == [tags[0], tags[0]]
//      assert pairs[1] == [tags[0], tags[1]]
//      assert pairs[2] == [tags[0], tags[2]]
//      assert pairs[3] == [tags[0], TagGroup.first()]
//
//      assert pairs[4] == [tags[1], tags[0]]
//      assert pairs[5] == [tags[1], tags[1]]
//      assert pairs[6] == [tags[1], tags[2]]
//      assert pairs[7] == [tags[1], TagGroup.first()]
//      // etc.
//    }

//    @Test
//    void test() {
//    }
//
//    @Test
//    void test() {
//    }
//
//    @Test
//    void test() {
//    }

}
