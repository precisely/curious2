
import static org.junit.Assert.*


import java.math.MathContext
import java.text.DateFormat

import org.junit.After
import org.junit.Before
import org.junit.Test

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.Entry
import us.wearecurio.model.DurationType
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.support.EntryStats
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.services.DatabaseService
import us.wearecurio.data.UnitGroupMap

import groovy.transform.TypeChecked

import org.joda.time.DateTimeZone
import org.junit.*
import grails.test.mixin.*
import us.wearecurio.util.ListNode
import us.wearecurio.util.ListNodeList
import us.wearecurio.utility.Utils

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin

@TestMixin(IntegrationTestMixin)
@TypeChecked
class ListNodeTests extends CuriousTestCase {
	static transactional = true

	@Before
	void setUp() {
		super.setUp()
		
		Locale.setDefault(Locale.US)	// For to run test case in any country.
		Utils.resetForTesting()
	}
	
	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testListNode() {
		ListNode a = new ListNode()
		ListNode b = new ListNode()
		ListNode c = new ListNode()
		
		a.append(b)
		
		assert a.next == b
		assert b.previous == a
		assert a.previous == null
		assert b.next == null
		
		a.remove()
		
		assert a.next == null
		assert a.previous == null
		assert b.next == null
		assert b.previous == null
		
		a.prepend(b)
		
		assert a.next == null
		assert a.previous == b
		assert b.next == a
		assert b.previous == null
		
		a.append(c)
		
		assert a.next == c
		assert a.previous == b
		assert b.next == a
		assert b.previous == null
		assert c.next == null
		assert c.previous == a
		
		ListNode node = b
		
		node = b.next
		
		assert node == a
		
		node = a.next
		
		assert node == c
		
		b.removeNext()
		
		assert b.next == null
		assert b.previous == null
		
		assert a.previous == null
		assert a.next == c
		
		assert c.previous == a
		assert c.next == null
	}
	
	@Test
	void testListNodeList() {
		ListNode a = new ListNode()
		ListNode b = new ListNode()
		ListNode c = new ListNode()
		
		ListNodeList<ListNode> list = new ListNodeList<ListNode>()
		
		assert list.isEmpty()
		assert list.first == null
		assert list.last == null
		
		list.addFirst(a)
		
		assert list.first == a
		assert list.last == a

		list.addFirst(b)
		
		assert list.first == b
		assert list.last == a
		assert b.next == a
		assert a.previous == b
		assert a.next == null
		assert b.previous == null
		
		list.addFirst(c)

		assert list.first == c
		assert list.last == a
		assert b.next == a
		assert a.previous == b
		assert a.next == null
		assert b.previous == c
		assert c.next == b
		assert c.previous == null
		
		assert list.removeFirst() == c
		
		assert c.previous == null
		assert c.next == null
		
		assert list.first == b
		assert list.last == a
		assert b.next == a
		assert a.previous == b
		assert a.next == null
		assert b.previous == null
	}
}
