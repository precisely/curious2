
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
import us.wearecurio.utility.Utils

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin

@TestMixin(IntegrationTestMixin)
class UnitGroupMapTests extends CuriousTestCase {
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

	static boolean within(long a, long b, long d) {
		long diff = a - b
		diff = diff < 0 ? -diff : diff
		return diff <= d
	}
	
	static boolean near(double a, double b, double threshold = 0.01d) {
		return Math.abs(a-b) < threshold
	}
	
	static unitSuffix(String units, String suffix) {
		String genSuffix = UnitGroupMap.theMap.suffixForUnits(units)
		assert genSuffix == suffix
	}

	@Test
	void testDefaultSuffixes() {
		unitSuffix("miles", "[distance]")
		unitSuffix("feet climbing", "[distance: climbing]")
		unitSuffix("mg/tsp", "[density]")
		unitSuffix("pg/tsp", "[density]")
	}
	
	@Test
	void testRatioUnits() {
		double ratio = UnitGroupMap.theMap.fetchConversionRatio("g/L", "kgs/L")
		
		ratio = ratio
		
		assert near(ratio, 1.0d/1000.0d, 1.0d/10000.0d)
	}
}
