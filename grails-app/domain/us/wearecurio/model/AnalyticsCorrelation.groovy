package us.wearecurio.model

import us.wearecurio.utility.Utils
import org.hibernate.criterion.Restrictions as R
import org.hibernate.criterion.Order as O

class AnalyticsCorrelation {

	private static final Long minOverlap = 3

	Long id
	String series1Type
	Long series1Id
	String series2Type
	Long series2Id

	Long userId

	Date created
	Date updated

	Date saved
	Date noise
	Date viewed

	enum ValueType {
		MIPSS(0), COR(1), TRIGGER(2)
		final Integer id
		ValueType(Integer id) {
			this.id = id
		}
	}

	ValueType valueType
	Double value
	Long overlapN
	String auxJson
	Double signalLevel

	static constraints = {
		valueType nullable: true
		value nullable: true
		auxJson nullable: true
		overlapN nullable: true
		signalLevel nullable:true

		saved nullable: true
		noise nullable: true
		viewed nullable: true
		created nullable: true
		updated nullable: true
	}

	static mapping = {
		version false
		overlapN index: 'overlapNIdx'
		userId index: 'updateIdx,overlapNIdx'
		series1Id index: 'updateIdx'
		series1Type index: 'updateIdx'
		series2Id index: 'updateIdx'
		series2Type index: 'updateIdx'
	}

	public static userCorrelations(Long userId, Integer max, String flavor) {
		def criteria = AnalyticsCorrelation.createCriteria()
		criteria = criteria.add( R.eq("userId", userId) )
		criteria = criteria.add( R.ltProperty("series1Id", "series2Id") )
		criteria = criteria.add( R.ge( "overlapN", minOverlap ) )

		def column = null
		switch(flavor) {

			case "triggered":
				column = "value"
				break;

			case "saved":
				column = "saved"
				criteria.add( R.isNotNull("saved") )
				break

			default:
				column = "value"
				break
		}
		def orderRestriction = (flavor == "negative" ? O.asc(column) : O.desc(column))
		criteria.addOrder( orderRestriction )
		criteria = criteria.setMaxResults( max )
		criteria.list()
	}

	public static updateSignalLevel(Long theId, Double signalLevel) {
		AnalyticsCorrelation correlation = AnalyticsCorrelation.get(theId)
		if (signalLevel == 0) {
			correlation.noise = new Date()
		}
		if (signalLevel >= 4) {
			correlation.saved = new Date()
		}
		correlation.signalLevel = signalLevel
		Utils.save(correlation, true)
	}

	public static markViewed(Long theId) {
		AnalyticsCorrelation correlation = AnalyticsCorrelation.get(theId)
		correlation.viewed = new Date()
		Utils.save(correlation, true)
	}

	public static markSaved(Long theId) {
		AnalyticsCorrelation correlation = AnalyticsCorrelation.get(theId)
		correlation.saved = new Date()
		Utils.save(correlation, true)
	}

	def savedAsLong() { saved ? saved.getTime() : null }
	def viewedAsLong() { viewed ? viewed.getTime() : null }
	def noiseAsLong() { noise ? noise.getTime() : null }

	def description1() { Tag.get(series1Id).description }
	def description2() { Tag.get(series2Id).description }

}
