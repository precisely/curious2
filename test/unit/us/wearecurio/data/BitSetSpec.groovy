package us.wearecurio.data

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class BitSetSpec extends Specification {

	void "test BitSet implementation for wrong invalid positions"() {
		when: "A logical position is passed which is less than 1"
		new BitSet().set(0)

		then: "Exception should be thrown"
		Exception e = thrown(IndexOutOfBoundsException)
		e.message == "Bit position can not be less than 1"

		when: "A logical position is passed with is greater than 63"
		new BitSet().set(64)

		then: "Exception should be thrown"
		Exception ee = thrown(IndexOutOfBoundsException)
		ee.message == "Bit index can not be greater than 63"
	}

	void "test all functionalities of BitSet implementation"() {
		expect: "Following should be true"
		new BitSet().flags == 0
		new BitSet(50).flags == 50

		BitSet bitSet1 = new BitSet(6)		// 0110
		bitSet1.get(1) == false
		bitSet1.get(2) == true
		bitSet1.get(3) == true
		bitSet1.get(4) == false

		BitSet bitSet2 = new BitSet(6)		// 0110
		bitSet2.clear(2)					// 0100
		bitSet2.clear(1)					// 0100
		bitSet2.set(1)						// 0101
		bitSet2.flags == 5					// 0101
		bitSet2.get(1) == true
		bitSet2.get(2) == false
		bitSet2.get(3) == true
		bitSet2.get(4) == false

		BitSet bitSet3 = new BitSet()		// 0000
		bitSet3.set(1)						// 0001
		bitSet3.set(2)						// 0011
		bitSet3.set(3)						// 0111
		bitSet3.flags == 7					// 0111
		bitSet3.get(1) == true
		bitSet3.get(2) == true
		bitSet3.get(3) == true
		bitSet3.clear(1)					// 0110
		bitSet3.clear(2)					// 0100
		bitSet3.flags == 4					// 0100
		bitSet3.get(1) == false
		bitSet3.get(2) == false
		bitSet3.get(3) == true
		bitSet3.get(4) == false

		BitSet bitSet4 = new BitSet()		// 0000 0000 0000
		bitSet4.set(12)
		bitSet4.flags == 2048				// 1000 0000 0000
		(1..11).each { position ->
			bitSet4.get(position) == false
		}
		bitSet4.get(12) == true

		// Test setting last bit i.e. MSB
		BitSet bitSet5 = new BitSet()
		bitSet5.set(63)
		bitSet5.flags == 4611686018427387904
		(1..62).each { position ->
			bitSet5.get(position) == false
		}
		bitSet5.get(63) == true
		bitSet5.set(62)
		bitSet5.flags == 6917529027641081856
		bitSet5.clear(63)
		bitSet5.get(63) == false
		bitSet5.flags == 2305843009213693952

		// Test setting each bit i.e. all 63 bit set to 1
		BitSet bitSet6 = new BitSet()
		(1..63).each { position ->
			bitSet6.set(position)
		}
		bitSet6.flags == 9223372036854775807
		(1..63).each { position ->
			bitSet6.get(position) == true
		}
	}
}