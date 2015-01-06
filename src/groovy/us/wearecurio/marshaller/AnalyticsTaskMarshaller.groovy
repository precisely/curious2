package us.wearecurio.marshaller

import grails.converters.JSON
import us.wearecurio.model.AnalyticsTask

class AnalyticsTaskMarshaller {
	void register() {
		JSON.registerObjectMarshaller( AnalyticsTask ) { AnalyticsTask task ->
			return [
				id: task.id,
				type: task.typeEn(),
				status: task.statusEn(),
				serverAddress: task.serverAddress,
				userId: task.userId,
				error: task.error,
				parentId: task.parentId,
				percentSuccessful: task.percentSuccessful(),
				createdAt: task.createdAt.format('ddMMMyyyy'),
				updatedAt: task.updatedAt.format('ddMMMyyyy')
			]
		}
	}
}
