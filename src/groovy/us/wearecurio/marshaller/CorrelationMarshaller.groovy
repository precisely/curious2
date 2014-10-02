package us.wearecurio.marshaller

import grails.converters.JSON
import us.wearecurio.model.Correlation

class CorrelationMarshaller {
	void register() {
		JSON.registerObjectMarshaller( Correlation ) { Correlation correlation ->
			return [
				id: correlation.id,
				series1Type: correlation.series1Type,
				series2Type: correlation.series2Type,
				series1Id: correlation.series1Id,
				series2Id: correlation.series2Id,
				description1: correlation.description1(),
				description2: correlation.description2(),
				valueType: correlation.valueType,
				value: correlation.value,

				saved: correlation.savedAsLong(),
				noise: correlation.noiseAsLong(),
				viewed: correlation.viewedAsLong()
			]
		}
	}
}
