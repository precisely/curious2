import grails.test.*

import us.wearecurio.integration.CuriousUserTestCase;
import us.wearecurio.model.PlotData
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

class PlotDataTests extends CuriousUserTestCase {
	static transactional = true

	@Before
	void setUp() {
		super.setUp()
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testCreateAndDelete() {
		PlotData plotData = PlotData.create(user, "Name of Plot", '{}', false)
		
		assert plotData.getUserId() == userId
		assert plotData.getName().equals("Name of Plot")
		assert plotData.fetchJsonPlotData().equals('{"username":"y"}')
		assert !plotData.getIsSnapshot()
		
		Utils.save(plotData, true)
		
		def plotDataId = plotData.getId()
		
		assert PlotData.get(plotDataId).getId() == plotDataId
		
		PlotData.delete(plotData)
		
		assert PlotData.get(plotDataId) == null
	}
	
	@Test
	void testCreateAndDeleteId() {
		PlotData plotData = PlotData.create(user, "Name of Plot", '{}', false)
		
		assert plotData.getUserId() == userId
		assert plotData.getName().equals("Name of Plot")
		assert plotData.fetchJsonPlotData().equals('{"username":"y"}')
		assert !plotData.getIsSnapshot()
		
		Utils.save(plotData, true)
		
		def plotDataId = plotData.getId()
		
		assert PlotData.get(plotDataId).getId() == plotDataId
		
		PlotData.deleteId(plotDataId)
		
		assert PlotData.get(plotDataId) == null
	}

	@Test
	void testGetJSONDesc() {
		PlotData plotData = PlotData.create(user, "Name of Plot", "{}", true)
		
		assert plotData.getIsSnapshot()

		assert plotData.getJSONDesc().equals([
			id:plotData.getId(),
			name:"Name of Plot",
			created:plotData.getCreated(),
			modified:plotData.getCreated()
		])		
	}
	
	@Test
	void testGetIsDynamic() {
		PlotData plotData = PlotData.create(user, "Name of Plot", "{}", true)

		assert plotData.getIsDynamic() == false
	}
	
	@Test
	void testToString() {
		PlotData plotData = PlotData.create(user, "Name of Plot", "{}", true)

		assert plotData.toString().equals("PlotData(id:null, name:Name of Plot, created:" +  Utils.dateToGMTString(plotData.getCreated()) + ", isSnapshot:true)")
	}
}
