package us.wearecurio.marshaller

import grails.converters.JSON
import us.wearecurio.model.AnalyticsCorrelation

class AnalyticsCorrelationMarshaller {
	void register() {
		JSON.registerObjectMarshaller( AnalyticsCorrelation ) { AnalyticsCorrelation correlation ->
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
				overlapn: correlation.overlapN,
				saved: correlation.savedAsLong(),
				noise: correlation.noiseAsLong(),
				viewed: correlation.viewedAsLong()
			]
		}
	}
}
